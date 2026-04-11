package app.simplecloud.plugin.api.shared.config

import app.simplecloud.plugin.api.shared.config.serializer.GenericEnumSerializer
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File

/**
 * @author Niklas Nieberler
 */

class YamlFileConfigurator<E>(
    private val javaClass: Class<E>
) {

    private val configurationLoaders = hashMapOf<File, YamlConfigurationLoader>()

    private val defaultConfigurationBuilder = YamlConfigurationLoader.builder()
        .nodeStyle(NodeStyle.BLOCK)
        .defaultOptions { options ->
            options.serializers { builder ->
                builder.registerAnnotatedObjects(objectMapperFactory())
                builder.register(Enum::class.java, GenericEnumSerializer)
                builder.register({ type -> type is Class<*> && type.isEnum }, GenericEnumSerializer)
            }
        }

    fun save(file: File, entity: E) {
        val (node, loader) = buildNode(file)
        node.set(this.javaClass, entity)
        loader.save(node)
    }

    fun load(file: File): E? {
        val (node, _) = buildNode(file)
        return node.get(this.javaClass)
    }

    fun buildNode(file: File): Pair<CommentedConfigurationNode, YamlConfigurationLoader> {
        val loader = getOrCreateConfigurationLoader(file)
        return Pair(loader.load(), loader)
    }

    fun getOrCreateConfigurationLoader(file: File): YamlConfigurationLoader {
        return this.configurationLoaders.getOrPut(file) {
            this.defaultConfigurationBuilder
                .path(file.toPath())
                .build()
        }
    }

}
