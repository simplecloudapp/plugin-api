package app.simplecloud.plugin.api.shared.config

import app.simplecloud.plugin.api.shared.exception.ConfigurationException
import app.simplecloud.plugin.api.shared.repository.GenericEnumSerializer
import kotlinx.coroutines.*
import org.spongepowered.configurate.ConfigurationOptions
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.nio.file.*
import kotlin.coroutines.CoroutineContext

/**
 * A configuration factory that loads, saves and watches configuration files.
 * The factory automatically reloads the configuration when the file changes.
 *
 * Usage:
 * ```
 * // Using create
 * val factory = ConfigFactory.create<MyConfig>(File("config.yaml"))
 *
 * // Using create with custom coroutineContext
 * val factory = ConfigFactory.create<MyConfig>(File("config.json"), Dispatchers.Default)
 * ```
 */
class ConfigFactory<T>(
    private val file: File,
    private val configClass: Class<T>,
    private val coroutineContext: CoroutineContext = Dispatchers.IO
) : AutoCloseable {

    private var config: T? = null
    private val path: Path = file.toPath()
    private var watchJob: Job? = null

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

    fun loadOrCreate(defaultConfig: T) {
        if (!configClass.isInstance(defaultConfig)) {
            throw IllegalArgumentException("Default config must be an instance of ${configClass.name}")
        }

        if (file.exists()) {
            loadConfig()
        } else {
            createDefaultConfig(defaultConfig)
        }

        registerWatcher()
    }

    private fun createDefaultConfig(defaultConfig: T) {
        path.parent?.let { Files.createDirectories(it) }
        Files.createFile(path)

        val node = configurationLoader.createNode()
        node.set(configClass, defaultConfig)
        configurationLoader.save(node)
        config = defaultConfig
    }

    fun getConfig(): T {
        return config ?: throw IllegalStateException("Configuration not loaded or invalid type")
    }

    @Throws(ConfigurationException::class)
    private fun loadConfig() {
        try {
            val node = configurationLoader.load(ConfigurationOptions.defaults())
            config = node.get(configClass)
                ?: throw ConfigurationException("Failed to parse configuration file")
        } catch (e: Exception) {
            throw ConfigurationException("Failed to load configuration", e)
        }
    }

    private fun registerWatcher(): Job {
        val watchService = FileSystems.getDefault().newWatchService()
        path.parent?.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY
        )

        return CoroutineScope(coroutineContext).launch {
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
                    println("Failed to reload configuration: ${e.message}")
                }
            }
        }
    }

    override fun close() {
        watchJob?.cancel()
    }

    companion object {
        inline fun <reified T : Any> create(
            file: File,
            coroutineContext: CoroutineContext = Dispatchers.IO
        ): ConfigFactory<T> = ConfigFactory(file, T::class.java, coroutineContext)
    }
}