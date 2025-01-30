package app.simplecloud.plugin.api.shared.config

import app.simplecloud.plugin.api.shared.exception.ConfigurationException
import app.simplecloud.plugin.api.shared.repository.GenericEnumSerializer
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.spongepowered.configurate.ConfigurationOptions
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.nio.file.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext

/**
 * A configuration factory that loads, saves and watches configuration files.
 * The factory automatically reloads the configuration when the file changes.
 *
 * Features:
 * - Thread-safe configuration handling
 * - File watching with automatic reloading
 * - Configuration change callbacks
 * - Validation support
 *
 * Usage:
 * ```
 * // Using create
 * val factory = ConfigFactory.create<MyConfig>(File("config.yaml"))
 *
 * // Using create with custom coroutineContext
 * val factory = ConfigFactory.create<MyConfig>(File("config.json"), Dispatchers.Default)
 *
 * // Add change listener
 * factory.onConfigChanged { oldConfig, newConfig ->
 *     println("Config updated from $oldConfig to $newConfig")
 * }
 *
 * // Save the modified config
 * factory.save(modifiedConfig)
 *
 * // Optionally with validation
 * factory.save(modifiedConfig) { config ->
 *     config.someField.isNotEmpty() // return true/false for validation
 * }
 * ```
 */
class ConfigFactory<T : Any>(
    private val file: File,
    private val configClass: Class<T>,
    private val coroutineContext: CoroutineContext = Dispatchers.IO
) : AutoCloseable {

    private val logger = LoggerFactory.getLogger(ConfigFactory::class.java)
    private val configRef = AtomicReference<T>()
    private val path: Path = file.toPath()
    private var watchJob: Job? = null
    private val changeListeners = mutableListOf<suspend (T?, T) -> Unit>()
    private val saveLock = Object()
    private val scope = CoroutineScope(coroutineContext + SupervisorJob())

    private val configurationLoader = YamlConfigurationLoader.builder()
        .path(path)
        .nodeStyle(NodeStyle.BLOCK)
        .defaultOptions { options ->
            options.serializers { builder ->
                builder.registerAnnotatedObjects(objectMapperFactory())
                builder.register(Enum::class.java, GenericEnumSerializer)
            }
        }
        .build()

    /**
     * Loads existing configuration or creates a new one with default values.
     * @param defaultConfig The default configuration to use if no file exists
     * @param validator Optional validation function for the configuration
     */
    fun loadOrCreate(defaultConfig: T, validator: ((T) -> Boolean)? = null) {
        if (!configClass.isInstance(defaultConfig)) {
            throw IllegalArgumentException("Default config must be an instance of ${configClass.name}")
        }

        if (file.exists()) {
            loadConfig(validator)
        } else {
            createDefaultConfig(defaultConfig, validator)
        }

        registerWatcher()
    }

    /**
     * Checks if the configuration file exists.
     * @return true if the configuration file exists
     */
    fun exists(): Boolean = file.exists()

    /**
     * Adds a listener that will be called whenever the configuration changes.
     * @param listener The listener function that receives old and new config
     */
    fun onConfigChanged(listener: suspend (T?, T) -> Unit) {
        synchronized(changeListeners) {
            changeListeners.add(listener)
        }
    }

    private fun createDefaultConfig(defaultConfig: T, validator: ((T) -> Boolean)?) {
        path.parent?.let { Files.createDirectories(it) }
        Files.createFile(path)

        if (validator?.invoke(defaultConfig) == false) {
            throw ConfigurationException("Default configuration failed validation")
        }

        synchronized(saveLock) {
            try {
                val node = configurationLoader.createNode()
                node.set(configClass, defaultConfig)
                configurationLoader.save(node)
                runBlocking(coroutineContext) {
                    updateConfig(defaultConfig)
                }
            } catch (e: Exception) {
                throw ConfigurationException("Failed to save default configuration", e)
            }
        }
    }

    /**
     * Gets the current configuration.
     * @throws IllegalStateException if configuration is not loaded
     */
    fun getConfig(): T {
        return configRef.get() ?: throw IllegalStateException("Configuration not loaded or invalid type")
    }

    /**
     * Manually reloads the configuration from disk.
     * @param validator Optional validation function for the loaded configuration
     * @throws ConfigurationException if loading or validation fails
     */
    @Throws(ConfigurationException::class)
    fun reloadConfig(validator: ((T) -> Boolean)? = null) {
        loadConfig(validator)
    }

    @Throws(ConfigurationException::class)
    private fun loadConfig(validator: ((T) -> Boolean)? = null) {
        try {
            val node = configurationLoader.load(ConfigurationOptions.defaults())
            val loadedConfig = node.get(configClass)
                ?: throw ConfigurationException("Failed to parse configuration file")

            if (validator?.invoke(loadedConfig) == false) {
                throw ConfigurationException("Configuration failed validation")
            }

            runBlocking(coroutineContext) {
                updateConfig(loadedConfig)
            }
        } catch (e: Exception) {
            throw ConfigurationException("Failed to load configuration", e)
        }
    }

    /**
     * Saves the provided configuration to disk.
     * @param config The configuration to save
     * @param validator Optional validation function to run before saving
     * @throws ConfigurationException if saving or validation fails
     */
    @Throws(ConfigurationException::class)
    fun save(config: T, validator: ((T) -> Boolean)? = null) {
        if (validator?.invoke(config) == false) {
            throw ConfigurationException("Configuration failed validation")
        }

        synchronized(saveLock) {
            try {
                val node = configurationLoader.createNode()
                node.set(configClass, config)
                configurationLoader.save(node)
                runBlocking(coroutineContext) {
                    updateConfig(config)
                }
            } catch (e: Exception) {
                throw ConfigurationException("Failed to save configuration", e)
            }
        }
    }

    private suspend fun updateConfig(newConfig: T) {
        val oldConfig = configRef.get()
        configRef.set(newConfig)

        val listeners = synchronized(changeListeners) {
            changeListeners.toList()
        }

        listeners.forEach { listener ->
            try {
                withContext(coroutineContext) {
                    listener(oldConfig, newConfig)
                }
            } catch (e: Exception) {
                logger.error("Error in config change listener", e)
            }
        }
    }

    private fun registerWatcher(): Job {
        val watchService = FileSystems.getDefault().newWatchService()
        path.parent?.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY
        )

        return scope.launch {
            watchService.use { watchService ->
                while (isActive) {
                    val key = watchService.take()
                    key.pollEvents().forEach { event ->
                        handleWatchEvent(event)
                    }
                    if (!key.reset()) {
                        break
                    }
                }
            }
        }.also { watchJob = it }
    }

    private suspend fun handleWatchEvent(event: WatchEvent<*>) {
        val path = event.context() as? Path ?: return
        if (!file.name.contains(path.toString())) return

        when (event.kind()) {
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY -> {
                delay(100)
                try {
                    loadConfig()
                } catch (e: ConfigurationException) {
                    logger.error("Failed to reload configuration: ${e.message}", e)
                }
            }

            else -> {}
        }
    }

    override fun close() {
        scope.cancel()
        watchJob?.cancel()
    }

    companion object {
        /**
         * Creates a new ConfigFactory instance.
         * @param file The configuration file
         * @param coroutineContext The coroutine context to use for async operations
         */
        inline fun <reified T : Any> create(
            file: File,
            coroutineContext: CoroutineContext = Dispatchers.IO
        ): ConfigFactory<T> = ConfigFactory(file, T::class.java, coroutineContext)
    }
}