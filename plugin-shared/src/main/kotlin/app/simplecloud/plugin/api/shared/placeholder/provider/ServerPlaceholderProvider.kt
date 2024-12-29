package app.simplecloud.plugin.api.shared.placeholder.provider

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.plugin.api.shared.placeholder.single.SingleServerPlaceholderExecutor
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

/**
 * @author Niklas Nieberler
 */

class ServerPlaceholderProvider : AbstractPlaceholderProvider<Server>(
    SingleServerPlaceholderExecutor()
) {
    override suspend fun getOtherTagResolvers(): List<TagResolver> {
        TODO("Not yet implemented")
    }
}