package app.simplecloud.plugin.api.shared.matcher.operation

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
object NotEqualsOperationMatcher : OperationMatcher {

    override fun matches(name: String, value: String): Boolean {
        return !name.equals(value, ignoreCase = true)
    }

}