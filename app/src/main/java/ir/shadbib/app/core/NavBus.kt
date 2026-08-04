package ir.shadbib.app.core

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

    private val _openUser = MutableStateFlow<String?>(null)
    val openUser: StateFlow<String?> get() = _openUser

    /** درخواست پرش به یک تب دلخواه (خانه، کتابخانه، اجتماع و …) از میانبرهای صفحهٔ اصلی. */
    private val _openTab = MutableStateFlow<String?>(null)
    val openTab: StateFlow<String?> get() = _openTab

    private val _openChannel = MutableStateFlow<Triple<String, String, String>?>(null)
    val openChannel: StateFlow<Triple<String, String, String>?> get() = _openChannel

    fun requestDm(username: String) { _openDm.value = username }
    fun consumeDm() { _openDm.value = null }

    fun requestStudy() { _openStudy.value = true }
    fun consumeStudy() { _openStudy.value = false }

    fun requestRoom() { _openRoom.value = true }
    fun consumeRoom() { _openRoom.value = false }

    fun requestUser(username: String) { _openUser.value = username }
    fun consumeUser() { _openUser.value = null }

    fun requestTab(route: String) { _openTab.value = route }
    fun consumeTab() { _openTab.value = null }

    fun requestChannel(key: String, title: String, emoji: String) { _openChannel.value = Triple(key, title, emoji) }
    fun consumeChannel() { _openChannel.value = null }
}
