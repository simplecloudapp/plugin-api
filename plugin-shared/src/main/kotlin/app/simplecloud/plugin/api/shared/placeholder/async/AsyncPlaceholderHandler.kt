package app.simplecloud.plugin.api.shared.placeholder.async

fun interface AsyncPlaceholderHandler<T> {

    /**
     * Gets the return value for a placeholder
     *
     * @param value of the placeholder key method
     */
    suspend fun handle(value: T): Any?
}
