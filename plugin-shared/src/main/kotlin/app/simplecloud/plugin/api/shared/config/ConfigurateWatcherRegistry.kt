package app.simplecloud.plugin.api.shared.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.WatchEvent

/**
 * @author Niklas Nieberler
 */

class ConfigurateWatcherRegistry {

    private val events = hashMapOf<WatchEvent.Kind<*>, (File) -> Unit>()
    private var watcherRequirements: (Path) -> Boolean = { false }

    /**
     * Adds an event for the watcher
     * @param event to register
     * @param function to handle
     */
    fun withEvent(vararg event: WatchEvent.Kind<*>, function: (File) -> Unit): ConfigurateWatcherRegistry {
        event.forEach { this.events[it] = function }
        return this
    }

    /**
     * Adds a requirement for the event watcher
     * @param function the requirement
     */
    fun withWatcherRequirements(function: (Path) -> Boolean): ConfigurateWatcherRegistry {
        this.watcherRequirements = function
        return this
    }

    /**
     * Registers a new [java.nio.file.WatchService] for a path
     * @param path to register the watcher
     */
    fun register(path: Path): Job {
        val watchService = FileSystems.getDefault().newWatchService()

        path.parent.register(
            watchService,
            *this.events.keys.toTypedArray()
        )

        return CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val watchKey = watchService.take()
                watchKey.pollEvents().forEach { watchEvent(path, it) }
                watchKey.reset()
            }
        }
    }

    private fun watchEvent(directoryPath: Path, event: WatchEvent<*>) {
        val path = event.context() as? Path ?: return
        if (!path.toString().endsWith(".yml"))
            return

        val resolvedPath = directoryPath.resolve(path)
        if (this.watcherRequirements(resolvedPath))
            return

        val kind = event.kind()
        this.events[kind]?.invoke(resolvedPath.toFile())
    }

}