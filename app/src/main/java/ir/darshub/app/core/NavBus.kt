package ir.darshub.app.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Lightweight cross-tab navigation events (e.g. open a DM from Home/Friends). */
object NavBus {
    private val _openDm = MutableStateFlow<String?>(null)
    val openDm: StateFlow<String?> get() = _openDm

    private val _openStudy = MutableStateFlow(false)
    val openStudy: StateFlow<Boolean> get() = _openStudy

    private val _openRoom = MutableStateFlow(false)
    val openRoom: StateFlow<Boolean> get() = _openRoom

    private val _roomSheet = MutableStateFlow<String?>(null)
    val roomSheet: StateFlow<String?> get() = _roomSheet

    private val _openUser = MutableStateFlow<String?>(null)
    val openUser: StateFlow<String?> get() = _openUser

    private val _openChannel = MutableStateFlow<Triple<String, String, String>?>(null)
    val openChannel: StateFlow<Triple<String, String, String>?> get() = _openChannel

    /*
     * Generic tab jump.
     *
     * The bottom bar now only carries the four destinations people use every
     * session. Tasks, library and community moved into the Home shortcut grid,
     * and those tiles need a way to reach a route they can no longer tap in the
     * bar, so they publish the route here and MainScaffold navigates.
     */
    private val _openRoute = MutableStateFlow<String?>(null)
    val openRoute: StateFlow<String?> get() = _openRoute

    fun requestDm(username: String) { _openDm.value = username }
    fun consumeDm() { _openDm.value = null }

    fun requestStudy() { _openStudy.value = true }
    fun consumeStudy() { _openStudy.value = false }

    /**
     * Open the study room. [sheet] optionally names an overlay the room
     * should present as soon as it is on screen, e.g. "top" for the full
     * leaderboard opened from the Home card.
     */
    fun requestRoom(sheet: String? = null) {
        _roomSheet.value = sheet
        _openRoom.value = true
    }
    fun consumeRoomSheet() { _roomSheet.value = null }
    fun consumeRoom() { _openRoom.value = false }

    fun requestUser(username: String) { _openUser.value = username }
    fun consumeUser() { _openUser.value = null }

    fun requestChannel(key: String, title: String, emoji: String) { _openChannel.value = Triple(key, title, emoji) }
    fun consumeChannel() { _openChannel.value = null }

    fun requestRoute(route: String) { _openRoute.value = route }
    fun consumeRoute() { _openRoute.value = null }
}
