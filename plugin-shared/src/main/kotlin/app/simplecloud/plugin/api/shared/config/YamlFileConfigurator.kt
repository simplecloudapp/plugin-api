package app.simplecloud.plugin.api.shared.config

import app.simplecloud.plugin.api.shared.config.serializer.GenericEnumSerializer
import org.spongepowered.configurate.ConfigurationOptions
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
            }
        }

    fun save(file: File, entity: E) {
        val configurationLoader = getOrCreateConfigurationLoader(file)
        val node = configurationLoader.createNode(ConfigurationOptions.defaults())
        node.set(this.javaClass, entity)
        configurationLoader.save(node)
    }

    fun load(file: File): E? {
        val configurationLoader = getOrCreateConfigurationLoader(file)
        val node = configurationLoader.load(ConfigurationOptions.defaults())
        return node.get(this.javaClass)
    }

    fun getOrCreateConfigurationLoader(file: File): YamlConfigurationLoader {
        return this.configurationLoaders.getOrPut(file) {
            this.defaultConfigurationBuilder
                .path(file.toPath())
                .build()
        }
    }

}