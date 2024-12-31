package app.simplecloud.plugin.api.shared.matcher.operation

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
object ContainsOperationMatcher : OperationMatcher {

    override fun matches(name: String, value: String): Boolean {
        return name.contains(value, ignoreCase = true)
    }

}