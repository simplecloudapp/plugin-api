package app.simplecloud.plugin.api.shared.placeholder.single

import app.simplecloud.plugin.api.shared.placeholder.async.AsyncPlaceholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

/**
 * @author Niklas Nieberler
 */

interface SinglePlaceholderExecutor<T> {

    /**
     * Gets a list with all available [AsyncPlaceholder]
     */
    fun getAsyncPlaceholders(): List<AsyncPlaceholder<T>>

    /**
     * Gets a list with all available tag resolvers from the [getAsyncPlaceholders] method
     * @param value for the placeholder
     * @param prefix first name for the placeholder key
     */
    suspend fun getTagResolvers(value: T, prefix: String? = null): List<TagResolver> {
        return getAsyncPlaceholders()
            .map { it.invokeTagResolver(value) }
    }

}