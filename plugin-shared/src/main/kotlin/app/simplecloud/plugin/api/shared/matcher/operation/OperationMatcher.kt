package app.simplecloud.plugin.api.shared.matcher.operation

interface OperationMatcher {

    fun matches(name: String, value: String): Boolean

}