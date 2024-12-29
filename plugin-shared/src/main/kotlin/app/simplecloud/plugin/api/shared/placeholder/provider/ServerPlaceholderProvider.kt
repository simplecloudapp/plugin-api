package app.simplecloud.plugin.api.shared.placeholder.provider

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.plugin.api.shared.placeholder.argument.*
import app.simplecloud.plugin.api.shared.placeholder.single.SingleServerPlaceholderExecutor

/**
 * @author Niklas Nieberler
 */

class ServerPlaceholderProvider : AbstractPlaceholderProvider<Server>(
    SingleServerPlaceholderExecutor()
) {

    override suspend fun getArgumentsResolvers(controllerApi: ControllerApi.Coroutine, value: Server) = listOf(
        PropertiesArgumentsResolver(value.properties)
    )

}