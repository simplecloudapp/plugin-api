package app.simplecloud.plugin.api.shared.repository

import kotlinx.coroutines.*
import org.spongepowered.configurate.ConfigurationOptions
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.loader.ParsingException
import org.spongepowered.configurate.serialize.TypeSerializerCollection
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds

abstract class YamlDirectoryRepository<I, E>(
    private val directory: Path,
    private val clazz: Class<E>,
) : LoadableRepository<I, E> {

    private val watchService = FileSystems.getDefault().newWatchService()
    private val loaders = mutableMapOf<File, YamlConfigurationLoader>()
    private val entities = mutableMapOf<File, E>()

    private var serializers: TypeSerializerCollection? = null

    abstract fun getFileName(identifier: I): String

    override fun delete(element: E): Boolean {
        val file = entities.keys.find { entities[it] == element } ?: return false
        return deleteFile(file)
    }

    override fun getAll(): List<E> {
        return entities.values.toList()
    }

    override fun load(serializers: TypeSerializerCollection?): List<E> {
        this.serializers = serializers

        if (!directory.toFile().exists()) {
            directory.toFile().mkdirs()
        }

        registerWatcher()

        return Files.walk(directory)
            .toList()
            .filter { !it.toFile().isDirectory && it.toString().endsWith(".yml") }
            .mapNotNull { load(it.toFile()) }
    }

    private fun load(file: File): E? {
        try {
            val loader = getOrCreateLoader(file)
            val node = loader.load(ConfigurationOptions.defaults())
            val entity = node.get(clazz) ?: return null
            entities[file] = entity
            return entity
        } catch (ex: ParsingException) {
            val existedBefore = entities.containsKey(file)
            if (existedBefore) {
                return null
            }

            return null
        }
    }

    private fun deleteFile(file: File): Boolean {
        val deletedSuccessfully = file.delete()
        val removedSuccessfully = entities.remove(file) != null
        return deletedSuccessfully && removedSuccessfully
    }

    protected fun save(fileName: String, entity: E) {
        val file = directory.resolve(fileName).toFile()
        val loader = getOrCreateLoader(file)
        val node = loader.createNode(ConfigurationOptions.defaults())
        node.set(clazz, entity)
        loader.save(node)
        entities[file] = entity
    }

    private fun getOrCreateLoader(file: File): YamlConfigurationLoader {
        return loaders.getOrPut(file) {
            YamlConfigurationLoader.builder()
                .path(file.toPath())
                .nodeStyle(NodeStyle.BLOCK)
                .defaultOptions { options ->
                    options.serializers { builder ->
                        serializers?.let { builder.registerAll(it) }

                        builder.registerAnnotatedObjects(objectMapperFactory())
                    }
                }.build()
        }
    }

    private fun registerWatcher(): Job {
        directory.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY
        )

        return CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val key = watchService.take()

                key.pollEvents().forEach { event ->
                    val path = event.context() as? Path ?: return@forEach
                    if (!path.toString().endsWith(".yml")) return@forEach

                    val resolvedPath = directory.resolve(path)
                    if (Files.isDirectory(resolvedPath)) return@forEach

                    when (event.kind()) {
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY -> {
                            delay(100)
                            load(resolvedPath.toFile())
                        }

                        StandardWatchEventKinds.ENTRY_DELETE -> {
                            deleteFile(resolvedPath.toFile())
                        }
                    }
                }

                key.reset()
            }
        }
    }
}