package app.simplecloud.plugin.api.shared.repository

import kotlinx.coroutines.*
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.ConfigurationOptions
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.loader.ParsingException
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.lang.reflect.Type
import java.nio.file.*

abstract class YamlDirectoryRepository<E>(
    private val directory: Path,
    private val clazz: Class<E>,
) {

    private val watchService = FileSystems.getDefault().newWatchService()
    private val loaders = mutableMapOf<File, YamlConfigurationLoader>()
    protected val entities = mutableMapOf<File, E>()

    fun delete(element: E): Boolean {
        val file = entities.keys.find { entities[it] == element } ?: return false
        return deleteFile(file)
    }

    fun getAll(): List<E> {
        return entities.values.toList()
    }

    fun load(): List<E> {
        if (!directory.toFile().exists()) {
            directory.toFile().mkdirs()
        }

        registerWatcher()

        return Files.list(directory)
            .toList()
            .filter { !it.toFile().isDirectory && it.toString().endsWith(".yml") }
            .mapNotNull { load(it.toFile()) }
    }

    open fun watchUpdateEvent(file: File) {}

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

    fun save(fileName: String, entity: E) {
        val file = directory.resolve(fileName).toFile()
        val loader = getOrCreateLoader(file)
        val node = loader.createNode(ConfigurationOptions.defaults().serializers {
            it.register(Enum::class.java, GenericEnumSerializer)
        })
        node.set(clazz, entity)
        loader.save(node)
        entities[file] = entity
    }

    private fun getOrCreateLoader(file: File): YamlConfigurationLoader {
        return loaders.getOrPut(file) {
            YamlConfigurationLoader.builder()
                .path(file.toPath())
                .nodeStyle(NodeStyle.BLOCK)
                .nodeStyle(NodeStyle.BLOCK)
                .defaultOptions { options ->
                    options.serializers { builder ->
                        builder.registerAnnotatedObjects(objectMapperFactory())
                        builder.register(Enum::class.java, GenericEnumSerializer)
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
                for (event in key.pollEvents()) {
                    val path = event.context() as? Path ?: continue
                    val resolvedPath = directory.resolve(path)
                    if (Files.isDirectory(resolvedPath) || !resolvedPath.toString().endsWith(".yml")) {
                        continue
                    }
                    val kind = event.kind()
                    when (kind) {
                        StandardWatchEventKinds.ENTRY_CREATE -> {
                            load(resolvedPath.toFile())
                        }

                        StandardWatchEventKinds.ENTRY_MODIFY -> {
                            load(resolvedPath.toFile())
                            watchUpdateEvent(resolvedPath.toFile())
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

    private object GenericEnumSerializer : TypeSerializer<Enum<*>> {
        override fun deserialize(type: Type, node: ConfigurationNode): Enum<*> {
            val value = node.string ?: throw SerializationException("No value present in node")

            if (type !is Class<*> || !type.isEnum) {
                throw SerializationException("Type is not an enum class")
            }

            @Suppress("UNCHECKED_CAST")
            return try {
                java.lang.Enum.valueOf(type as Class<out Enum<*>>, value)
            } catch (e: IllegalArgumentException) {
                throw SerializationException("Invalid enum constant")
            }
        }

        override fun serialize(type: Type, obj: Enum<*>?, node: ConfigurationNode) {
            node.set(obj?.name)
        }
    }

}