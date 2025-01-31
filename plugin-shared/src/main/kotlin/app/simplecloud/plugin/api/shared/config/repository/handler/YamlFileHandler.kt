package app.simplecloud.plugin.api.shared.config.repository.handler

import app.simplecloud.plugin.api.shared.repository.GenericEnumSerializer
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File

class YamlFileHandler<T : Any>(
    private val clazz: Class<T>
) : FileHandler<T> {

    override val fileExtension: String = ".yml"

    private val loaders = mutableMapOf<File, YamlConfigurationLoader>()

    private fun getOrCreateLoader(file: File): YamlConfigurationLoader {
        return loaders.getOrPut(file) {
            YamlConfigurationLoader.builder()
                .path(file.toPath())
                .nodeStyle(NodeStyle.BLOCK)
                .defaultOptions { options ->
                    options.serializers { builder ->
                        builder.registerAnnotatedObjects(objectMapperFactory())
                        builder.register(Enum::class.java, GenericEnumSerializer)
                    }
                }
                .build()
        }
    }

    override fun load(file: File): T? {
        return try {
            val loader = getOrCreateLoader(file)
            val node = loader.load()
            node.get(clazz)
        } catch (e: Exception) {
            println("Error loading file ${file.name}: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    override fun save(file: File, entity: T) {
        val loader = getOrCreateLoader(file)
        val node = loader.createNode()
        node.set(clazz, entity)
        loader.save(node)
    }
}
