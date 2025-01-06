package app.simplecloud.plugin.api.shared.matcher.operation

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.util.regex.Pattern

@ConfigSerializable
object PatternOperationMatcher : OperationMatcher {

    override fun matches(name: String, value: String): Boolean {
        return Pattern.matches(name, value)
    }

}