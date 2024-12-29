package app.simplecloud.plugin.api.shared.placeholder.single

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.plugin.api.shared.placeholder.async.AsyncPlaceholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

/**
 * @author Niklas Nieberler
 */

interface SinglePlaceholderExecutor<T> {

    /**
     * Gets a list with all available [AsyncPlaceholder]
     * @param controllerApi the instance of [ControllerApi.Coroutine]
     */
    fun getAsyncPlaceholders(controllerApi: ControllerApi.Coroutine): List<AsyncPlaceholder<T>>

    /**
     * Gets a [TagResolver] with all available tag resolvers from the [getAsyncPlaceholders] method
     * @param controllerApi the instance of [ControllerApi.Coroutine]
     * @param value for the placeholder
     * @param prefix first name for the placeholder key
     */
    suspend fun getTagResolver(
        controllerApi: ControllerApi.Coroutine,
        value: T,
        prefix: String? = null,
    ): TagResolver {
        return TagResolver.resolver(getAsyncPlaceholders(controllerApi)
            .map { it.invokeTagResolver(value) })
    }

}