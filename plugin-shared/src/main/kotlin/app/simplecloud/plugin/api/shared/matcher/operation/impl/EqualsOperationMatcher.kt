package app.simplecloud.plugin.api.shared.matcher.operation.impl

import app.simplecloud.plugin.api.shared.matcher.operation.StringOperationMatcher
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
object EqualsOperationMatcher : StringOperationMatcher {

    override fun matches(key: String, value: String): Boolean = key.equals(value, ignoreCase = true)
}
