package app.simplecloud.plugin.api.shared.matcher.operation

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
object RegexOperationMatcher : OperationMatcher {

    override fun matches(name: String, value: String): Boolean {
        return Regex(value).matches(name)
    }

}