package ir.shadbib.app.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.shadbib.app.core.Api
import ir.shadbib.app.data.Conversation
import ir.shadbib.app.data.UserResult
import ir.shadbib.app.data.StudyGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MessagesViewModel : ViewModel() {
    data class State(
        val loading: Boolean = true,
        val conversations: List<Conversation> = emptyList(),
        val query: String = "",
        val searchResults: List<UserResult>? = null,
        val searching: Boolean = false,
        val groups: List<StudyGroup> = emptyList(),
    )

    val state = MutableStateFlow(State())

    fun poll() {
        viewModelScope.launch {
            runCatching {
                val meta = Api.obj(Api.get("inbox_meta"))
                val chs = meta.optJSONArray("channels")
                if (chs != null) {
                    val m = mutableMapOf<String, Triple<String?, String?, Int>>()
                    for (i in 0 until chs.length()) {
                        val c = chs.getJSONObject(i)
                        m[c.optString("channel")] = Triple(c.optString("last_message").takeIf { it.isNotBlank() && it != "null" }, c.optString("last_sender").takeIf { it.isNotBlank() && it != "null" }, c.optInt("unread", 0))
                    }
                    channelMeta.value = m
                }
                val grs = meta.optJSONArray("groups")
                if (grs != null) {
                    val gm = mutableMapOf<Int, GroupMeta>()
                    for (i in 0 until grs.length()) {
                        val g = grs.getJSONObject(i)
                        gm[g.optInt("group_id")] = GroupMeta(
                            g.optString("last_message").takeIf { it.isNotBlank() && it != "null" },
                            g.optString("last_sender").takeIf { it.isNotBlank() && it != "null" },
                            g.optString("last_time").takeIf { it.isNotBlank() && it != "null" },
                            g.optInt("unread", 0), g.optInt("mention", 0) == 1)
                    }
                    groupMeta.value = gm
                }
            }
            runCatching { Conversation.list(Api.arr(Api.get("dm_conversations"))) }
                .onSuccess { l -> state.update { it.copy(loading = false, conversations = l) } }
                .onFailure { state.update { it.copy(loading = false) } }
            runCatching { StudyGroup.list(Api.arr(Api.get("group_list"))) }
                .onSuccess { l -> state.update { it.copy(groups = l) } }
        }
    }

    fun setQuery(q: String) {
        state.update { it.copy(query = q, searchResults = if (q.isBlank()) null else it.searchResults) }
    }

    fun search() {
        val q = state.value.query.trim()
        if (q.isEmpty()) { state.update { it.copy(searchResults = null) }; return }
        viewModelScope.launch {
            state.update { it.copy(searching = true) }
            runCatching { UserResult.list(Api.arr(Api.get("user_search", "q" to q))) }
                .onSuccess { l -> state.update { it.copy(searchResults = l, searching = false) } }
                .onFailure { state.update { it.copy(searching = false) } }
        }
    }

    fun clearSearch() {
        state.update { it.copy(query = "", searchResults = null) }
    }
}
