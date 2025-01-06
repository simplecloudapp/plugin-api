package app.simplecloud.plugin.api.shared.repository

interface LoadableRepository<I, E> : Repository<I, E> {

    fun load(): List<E>

}