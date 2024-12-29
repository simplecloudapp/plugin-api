package app.simplecloud.plugin.api.shared.placeholder.provider

import app.simplecloud.controller.shared.group.Group
import app.simplecloud.plugin.api.shared.placeholder.single.SingleGroupPlaceholderExecutor
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

/**
 * @author Niklas Nieberler
 */

class GroupPlaceholderProvider : AbstractPlaceholderProvider<Group>(
    SingleGroupPlaceholderExecutor()
) {
    override suspend fun getOtherTagResolvers(): List<TagResolver> {
        TODO("Not yet implemented")
    }
}