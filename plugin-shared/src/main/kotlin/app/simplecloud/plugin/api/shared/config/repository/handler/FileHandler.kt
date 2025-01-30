package app.simplecloud.plugin.api.shared.config.repository.handler

import java.io.File

interface FileHandler<T : Any> {

    val fileExtension: String

    fun load(file: File): T?

    fun save(file: File, entity: T)

    fun validate(entity: T): Boolean = true

}