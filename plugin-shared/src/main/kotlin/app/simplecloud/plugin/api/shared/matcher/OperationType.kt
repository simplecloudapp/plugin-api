package app.simplecloud.plugin.api.shared.matcher

import app.simplecloud.plugin.api.shared.matcher.operation.*

enum class OperationType(
    private val matcher: OperationMatcher
) {

    REGEX(RegexOperationMatcher),
    PATTERN(PatternOperationMatcher),
    EQUALS(EqualsOperationMatcher),
    NOT_EQUALS(NotEqualsOperationMatcher),
    CONTAINS(ContainsOperationMatcher),
    STARTS_WITH(StartsWithOperationMatcher),
    ENDS_WITH(EndsWithOperationMatcher);

    fun matches(name: String, value: String, negate: Boolean): Boolean {
        val matches = this.matcher.matches(name, value)
        if (negate) {
            return matches.not()
        }
        return matches
    }

    fun anyMatches(names: List<String>, value: String, negate: Boolean): Boolean {
        return names.any { matches(it, value, negate) }
    }

    fun allMatches(names: List<String>, value: String, negate: Boolean): Boolean {
        return names.all { matches(it, value, negate) }
    }

}