package app.simplecloud.plugin.api.shared.pattern

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.server.Server

/**
 * @author Niklas Nieberler
 */

class ServerPatternIdentifier(
    private val pattern: String = "<group_name>-<numerical_id>",
    regexPattern: String = pattern
        .replace("<group_name>", "(?<groupName>[a-zA-Z]+)")
        .replace("<numerical_id>", "(?<numericalId>\\d+)"),
    private val controllerApi: ControllerApi.Coroutine = ControllerApi.createCoroutineApi()
) {

    private val regex = Regex(regexPattern)

    /**
     * Gets the group and numerical id matching the [pattern] in a [Pair]
     * @param name the server name
     * @param customRegex custom regex pattern
     */
    fun parse(name: String, customRegex: Regex? = null): Pair<String, Int> {
        val matchResult = customRegex?.matchEntire(name) ?: this.regex.matchEntire(name)
        if (matchResult == null)
            throw IllegalArgumentException("$name does not match the pattern")

        val groupName = matchResult.groups["groupName"]?.value
            ?: throw IllegalArgumentException("Group name not found")
        val numericalId = matchResult.groups["numericalId"]?.value?.toInt()
            ?: throw IllegalArgumentException("Numerical ID not found")
        return Pair(groupName, numericalId)
    }

    /**
     * Gets a server string as a set in the pattern
     * @param server replaces it to pattern
     */
    fun parseServerToPattern(server: Server): String {
        return this.pattern
            .replace("<group_name>", server.group)
            .replace("<numerical_id>", server.numericalId.toString())
    }

    /**
     * Gets the [Group] by the matching [pattern]
     * @param name the server name
     */
    suspend fun getGroup(name: String): Group? {
        val groupName = parse(name).first
        return this.controllerApi.getGroups().getGroupByName(groupName)
    }

    /**
     * Gets the numerical id by the matching [pattern]
     * @param name the server name
     */
    fun getNumericalId(name: String): Int {
        return parse(name).second
    }

}