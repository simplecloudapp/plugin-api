package app.simplecloud.plugin.api.shared.matcher.operation

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
object StartsWithOperationMatcher : OperationMatcher {

    override fun matches(name: String, value: String): Boolean {
        return name.startsWith(value, ignoreCase = true)
    }

}