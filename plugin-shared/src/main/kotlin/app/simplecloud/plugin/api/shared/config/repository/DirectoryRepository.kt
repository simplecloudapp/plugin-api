package app.simplecloud.plugin.api.shared.config.repository

import app.simplecloud.plugin.api.shared.config.repository.handler.FileHandler
import app.simplecloud.plugin.api.shared.exception.RepositoryException
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.file.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import java.util.jar.JarFile
import kotlin.coroutines.CoroutineContext

/**
 * A directory repository that manages files of a specific type.
 * Features:
 * - Thread-safe entity handling
 * - File watching with automatic reloading
 * - Entity change callbacks
 * - Validation support
 * - Automatic resource cleanup
 *
 * @param I The type of the identifier
 * @param T The type of the entity
 */
class DirectoryRepository<I : Any, T : Any> constructor(
    private val directory: Path,
    private val fileHandler: FileHandler<T>,
    private val coroutineContext: CoroutineContext,
    private val validator: ((T) -> Boolean)? = null
) : AutoCloseable {

    private val logger = LoggerFactory.getLogger(DirectoryRepository::class.java)
    private val entities = ConcurrentHashMap<File, AtomicReference<T>>()
    private val identifiers = ConcurrentHashMap<File, I>()
    private val changeListeners = mutableListOf<suspend (I, T?, T?) -> Unit>()
    private val errorHandlers = mutableListOf<(Exception) -> Unit>()
    private val saveLock = Object()
    private val scope = CoroutineScope(coroutineContext + SupervisorJob())
    private var watchJob: Job? = null

    init {
        if (!directory.toFile().exists()) {
            directory.toFile().mkdirs()
        }
    }

    /**
     * Loads all entities from the directory or creates them using default values.
     * @param defaultEntities Default entities to use if no existing files are found
     * @throws RepositoryException if loading or creation fails
     */
    @Throws(RepositoryException::class)
    fun loadOrCreate(defaultEntities: Map<I, T> = emptyMap()) {
        try {
            if (directory.toFile().list()?.isEmpty() != false) {
                loadDefaultsFromResources(defaultEntities)
            }

            load()
            registerWatcher()
        } catch (e: Exception) {
            handleError(RepositoryException("Failed to load repository", e))
        }
    }

    private fun loadDefaultsFromResources(defaultEntities: Map<I, T>) {
        if (directory.toFile().list()?.isEmpty() == true) {
            val resourceUrl = DirectoryRepository::class.java.getResource("/defaults/") ?: run {
                println("Resource folder '/defaults/' not found.")
                return
            }

            when (resourceUrl.protocol) {
                "file" -> handleFileProtocol(resourceUrl, directory.toFile())
                "jar" -> handleJarProtocol(resourceUrl, directory.toFile())

                else -> println("Unsupported protocol: ${resourceUrl.protocol}")
            }

            defaultEntities.forEach { (id, entity) -> save(id, entity) }
        }
    }

    private fun handleFileProtocol(resourceUrl: URL, targetDirectory: File) {
        val resourceDir = File(resourceUrl.toURI())

        if (resourceDir.exists()) {
            resourceDir.copyRecursively(targetDirectory, overwrite = true)
        } else {
            println("Resource directory does not exist: ${resourceUrl.path}")
        }
    }

    private fun handleJarProtocol(resourceUrl: URL, targetDirectory: File) {
        val jarPath = resourceUrl.path.substringBefore("!").removePrefix("file:")

        try {
            JarFile(jarPath).use { jarFile ->
                jarFile.entries().asSequence()
                    .filter { it.name.startsWith("defaults/") && !it.isDirectory }
                    .forEach { entry ->
                        val targetFile = File(targetDirectory, entry.name.removePrefix("defaults/"))
                        targetFile.parentFile.mkdirs()
                        try {
                            jarFile.getInputStream(entry).use { inputStream ->
                                FileOutputStream(targetFile).use { fos ->
                                    fos.write(0xEF)
                                    fos.write(0xBB)
                                    fos.write(0xBF)
                                    inputStream.copyTo(fos)
                                }
                            }
                        } catch (e: Exception) {
                            println("Error copying file ${entry.name}: ${e.message}")
                        }
                    }
            }
        } catch (e: Exception) {
            println("Error processing JAR file: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun load() {
        Files.walk(directory)
            .filter { !it.toFile().isDirectory && it.toString().endsWith(fileHandler.fileExtension) }
            .forEach { loadFile(it.toFile()) }
    }

    private fun loadFile(file: File) {
        try {
            fileHandler.load(file)?.let { entity ->
                if (validateEntity(entity)) {
                    entities[file] = AtomicReference(entity)
                }
            }
        } catch (e: Exception) {
            handleError(RepositoryException("Error loading file ${file.name}", e))
        }
    }

    /**
     * Saves an entity to the repository
     * @throws RepositoryException if saving fails or validation fails
     */
    @Throws(RepositoryException::class)
    fun save(identifier: I, entity: T) {
        if (!validateEntity(entity)) {
            throw RepositoryException("Entity validation failed")
        }

        synchronized(saveLock) {
            try {
                val file = getFile(identifier)
                file.parentFile?.mkdirs()

                fileHandler.save(file, entity)
                val oldEntity = entities[file]?.get()
                entities[file] = AtomicReference(entity)
                identifiers[file] = identifier

                scope.launch {
                    notifyChangeListeners(identifier, oldEntity, entity)
                }
            } catch (e: Exception) {
                handleError(RepositoryException("Failed to save entity", e))
                throw e
            }
        }
    }

    /**
     * Deletes an entity from the repository
     */
    fun delete(identifier: I): Boolean {
        val file = getFile(identifier)
        if (!file.exists()) return false

        synchronized(saveLock) {
            return try {
                val deleted = file.delete()

                if (deleted) {
                    val oldEntity = entities.remove(file)?.get()
                    identifiers.remove(file)
                    if (oldEntity != null) {
                        scope.launch {
                            notifyChangeListeners(identifier, oldEntity, null)
                        }
                    }
                }

                deleted
            } catch (e: Exception) {
                handleError(RepositoryException("Failed to delete entity", e))
                false
            }
        }
    }

    /**
     * Finds an entity by identifier
     */
    fun find(identifier: I): T? =
        entities[getFile(identifier)]?.get()


    /**
     * Gets all entities in the repository
     */
    fun getAll(): List<T> = entities.values.mapNotNull { it.get() }

    /**
     * Gets all identifiers in the repository
     */
    fun getAllIdentifiers(): Set<I> = identifiers.values.toSet()

    /**
     * Adds a listener for entity changes
     */
    fun onEntityChanged(listener: suspend (I, T?, T?) -> Unit) {
        synchronized(changeListeners) {
            changeListeners.add(listener)
        }
    }

    /**
     * Adds an error handler
     */
    fun onError(handler: (Exception) -> Unit) {
        synchronized(errorHandlers) {
            errorHandlers.add(handler)
        }
    }

    private fun validateEntity(entity: T): Boolean {
        return validator?.invoke(entity) ?: fileHandler.validate(entity)
    }

    private fun handleError(error: Exception) {
        logger.error(error.message, error)
        synchronized(errorHandlers) {
            errorHandlers.forEach { it(error) }
        }
    }

    private suspend fun notifyChangeListeners(identifier: I, oldEntity: T?, newEntity: T?) {
        val listeners = synchronized(changeListeners) { changeListeners.toList() }

        listeners.forEach { listener ->
            try {
                withContext(coroutineContext) {
                    if (oldEntity != newEntity) {
                        listener(identifier, oldEntity, newEntity)
                    }
                }
            } catch (e: Exception) {
                handleError(RepositoryException("Error in change listener", e))
            }
        }
    }

    private fun getFile(identifier: I): File =
        directory.resolve("$identifier${fileHandler.fileExtension}").toFile()


    private fun registerWatcher(): Job {
        val watchService = FileSystems.getDefault().newWatchService()
        directory.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE
        )

        return scope.launch {
            watchService.use { service ->
                while (isActive) {
                    val key = service.take()

                    key.pollEvents().forEach { event ->
                        handleWatchEvent(event)
                    }

                    if (!key.reset()) break
                }
            }
        }.also { watchJob = it }
    }

    private suspend fun handleWatchEvent(event: WatchEvent<*>) {
        val path = event.context() as? Path ?: return
        if (!path.toString().endsWith(fileHandler.fileExtension)) return

        val resolvedPath = directory.resolve(path)
        if (Files.isDirectory(resolvedPath)) return

        when (event.kind()) {
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY -> {
                delay(100)
                loadFile(resolvedPath.toFile())
            }

            StandardWatchEventKinds.ENTRY_DELETE -> {
                val file = resolvedPath.toFile()
                val identifier = identifiers[file]
                val oldEntity = entities.remove(file)?.get()

                if (identifier != null && oldEntity != null) {
                    notifyChangeListeners(identifier, oldEntity, null)
                }
            }

            else -> {}
        }
    }

    override fun close() {
        scope.cancel()
        watchJob?.cancel()
    }

    companion object {
        /**
         * Creates a new DirectoryRepository instance
         */
        inline fun <reified I : Any, reified T : Any> create(
            directory: Path,
            fileHandler: FileHandler<T>,
            noinline validator: ((T) -> Boolean)? = null,
            coroutineContext: CoroutineContext = Dispatchers.IO
        ): DirectoryRepository<I, T> = DirectoryRepository(
            directory = directory,
            fileHandler = fileHandler,
            validator = validator,
            coroutineContext = coroutineContext
        )
    }
}
