package app.simplecloud.plugin.api.shared.config

import app.simplecloud.plugin.api.shared.repository.GenericEnumSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.spongepowered.configurate.ConfigurationOptions
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.kotlin.toNode
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds

/**
 * @author Niklas Nieberler
 */

class ConfigFactory<E>(
    private val file: File,
    private val defaultConfig: E
) {

    private var config = defaultConfig
    private val path = file.toPath()

    private val configurationLoader = YamlConfigurationLoader.builder()
        .path(this.path)
        .nodeStyle(NodeStyle.BLOCK)
        .defaultOptions { options ->
            options.serializers { builder ->
                builder.registerAnnotatedObjects(objectMapperFactory())
                builder.register(Enum::class.java, GenericEnumSerializer)
            }
        }
        .build()

    fun loadOrCreate() {
        registerWatcher()
        if (this.file.exists()) {
            loadConfig()
            return
        }
        createDefaultConfig()
    }

    private fun createDefaultConfig() {
        this.path.parent?.let { Files.createDirectories(it) }
        Files.createFile(this.path)

        val configurationNode = this.configurationLoader.load(ConfigurationOptions.defaults())
        this.defaultConfig!!.toNode(configurationNode)
        this.configurationLoader.save(configurationNode)
    }

    fun getConfig(): E = this.config

    private fun loadConfig() {
        val configurationNode = this.configurationLoader.load(ConfigurationOptions.defaults())
        this.config = configurationNode.get(this.defaultConfig!!::class.java)
            ?: throw IllegalStateException("Config could not be loaded")
    }

    private fun registerWatcher(): Job {
        val watchService = FileSystems.getDefault().newWatchService()
        this.path.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY
        )

        return CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val key = watchService.take()

                key.pollEvents().forEach { event ->
                    val path = event.context() as? Path ?: return@forEach
                    if (!file.name.contains(path.toString())) return@launch

                    when (event.kind()) {
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY -> {
                            delay(100)
                            loadConfig()
                        }
                    }
                }

                key.reset()
            }
        }
    }

}