package app.simplecloud.plugin.api.shared.placeholder.provider

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.plugin.api.shared.extension.text
import app.simplecloud.plugin.api.shared.placeholder.argument.ArgumentsResolver
import app.simplecloud.plugin.api.shared.placeholder.single.SinglePlaceholderExecutor
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

/**
 * @author Niklas Nieberler
 */

abstract class AbstractPlaceholderProvider<T>(
    private val executor: SinglePlaceholderExecutor<T>,
) {

    private val controllerApi = ControllerApi.createCoroutineApi()

    /**
     * Gets the list of all available [ArgumentsResolver]
     * @param controllerApi the instance of [ControllerApi.Coroutine]
     * @param value for the placeholder
     */
    abstract suspend fun getArgumentsResolvers(
        controllerApi: ControllerApi.Coroutine,
        value: T,
    ): List<ArgumentsResolver>

    /**
     * Gets the sum of all [TagResolver]
     * @param value for the placeholder
     * @param prefix of the placeholder key
     */
    suspend fun getTagResolver(
        value: T,
        prefix: String? = null,
        vararg argumentsResolver: ArgumentsResolver,
    ): TagResolver {
        val availableArgumentsResolver = listOf(
            *getArgumentsResolvers(this.controllerApi, value).toTypedArray(),
            *argumentsResolver
        )
        val singleTagResolver = this.executor.getTagResolver(this.controllerApi, value, prefix)
        return TagResolver.resolver(
            singleTagResolver,
            *availableArgumentsResolver
                .map { convertArgumentsResolverToTagResolver(it, prefix) }
                .toTypedArray()
        )
    }

    /**
     * Serializes the string to a [Component]
     * @param value for the placeholder
     * @param string the message
     * @param prefix of the placeholder key
     */
    suspend fun append(
        value: T,
        string: String,
        prefix: String? = null,
        vararg argumentsResolver: ArgumentsResolver,
    ): Component {
        return text(
            string,
            getTagResolver(value, prefix, *argumentsResolver),
        )
    }

    private fun convertArgumentsResolverToTagResolver(resolver: ArgumentsResolver, prefix: String?): TagResolver {
        val key = resolver.getKey()
        val resolvedKey = prefix?.let { "${it}_$key" } ?: key
        return TagResolver.resolver(resolvedKey) { arguments, _ ->
            return@resolver runBlocking { resolver.resolve(arguments) }
        }
    }

}