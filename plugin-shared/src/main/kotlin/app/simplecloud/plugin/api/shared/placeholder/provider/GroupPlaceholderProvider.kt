package app.simplecloud.plugin.api.shared.placeholder.provider

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.controller.shared.group.Group
import app.simplecloud.plugin.api.shared.placeholder.argument.*
import app.simplecloud.plugin.api.shared.placeholder.argument.group.*
import app.simplecloud.plugin.api.shared.placeholder.single.SingleGroupPlaceholderExecutor

/**
 * @author Niklas Nieberler
 */

class GroupPlaceholderProvider : AbstractPlaceholderProvider<Group>(
    SingleGroupPlaceholderExecutor()
) {

    override suspend fun getArgumentsResolvers(controllerApi: ControllerApi.Coroutine, value: Group) = listOf(
        PropertiesArgumentsResolver(value.properties),
        PlayerCountArgumentsResolver(controllerApi, value),
        ServerCountArgumentsResolver(controllerApi, value)
    )

}