package app.simplecloud.plugin.api.shared.repository

interface Repository<I, E> {

    fun delete(element: E): Boolean

    fun save(element: E)

    fun find(identifier: I): E?

    fun getAll(): List<E>

}