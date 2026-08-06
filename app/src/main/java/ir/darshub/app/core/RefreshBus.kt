package ir.darshub.app.core

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/** Global refresh events so every screen updates instantly after any change.
 *  Tags: "study", "tasks", "home", "dm", "groups", "notes", "friends", "all" */
object RefreshBus {
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val events: SharedFlow<String> get() = _events
    fun emit(tag: String) { _events.tryEmit(tag) }
}
