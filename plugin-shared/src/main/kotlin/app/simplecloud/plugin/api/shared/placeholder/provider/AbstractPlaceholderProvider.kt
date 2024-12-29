package app.simplecloud.plugin.api.shared.placeholder.provider

import app.simplecloud.plugin.api.shared.extension.text
import app.simplecloud.plugin.api.shared.placeholder.single.SinglePlaceholderExecutor
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

/**
 * @author Niklas Nieberler
 */

abstract class AbstractPlaceholderProvider<T>(
    private val executor: SinglePlaceholderExecutor<T>
) {

    abstract suspend fun getOtherTagResolvers(): List<TagResolver> // TODO: work in progress oder einfach wie bei single ding zu so multiple bums

    suspend fun getTagResolvers(value: T, prefix: String? = null): List<TagResolver> {
        return listOf(
            *this.executor.getTagResolvers(value, prefix).toTypedArray(),
            *getOtherTagResolvers().toTypedArray()
        )
    }

    suspend fun append(value: T, string: String, prefix: String? = null): Component {
        return text(
            string,
            *getTagResolvers(value, prefix).toTypedArray()
        )
    }

}