package app.simplecloud.plugin.api.shared.matcher.operation.impl

import app.simplecloud.plugin.api.shared.matcher.operation.StringOperationMatcher
import java.util.regex.Pattern
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
object PatternOperationMatcher : StringOperationMatcher {

    override fun matches(key: String, value: String): Boolean {
        return Pattern.matches(key, value)
    }
}
