package ir.shadbib.app.ui.messages

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.shadbib.app.core.Api
import ir.shadbib.app.data.DmMessage
import ir.shadbib.app.data.DmPartner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

class DmThreadViewModel : ViewModel() {
    data class State(
        val loading: Boolean = true,
        val partner: DmPartner? = null,
        val messages: List<DmMessage> = emptyList(),
        val uploadPercent: Int? = null,
        val partnerTyping: Boolean = false,
    )

    val state = MutableStateFlow(State())
    private var username: String = ""
    private fun target() = mapOf("to" to username)

    fun bind(user: String) {
        if (username == user) return
        username = user
        state.value = State()
    }

    fun poll() {
        if (username.isBlank()) return
        viewModelScope.launch {
            runCatching {
                val o = Api.obj(Api.get("dm_thread", "username" to username))
                val partner = o.optJSONObject("partner")?.let { DmPartner.from(it) }
                val msgs = o.optJSONArray("messages")?.let { DmMessage.list(it) } ?: emptyList()
                Triple(partner, msgs, o.optInt("partner_typing", 0) == 1)
            }.onSuccess { (p, m, t) -> state.update { it.copy(loading = false, partner = p, messages = m, partnerTyping = t) } }
                .onFailure { state.update { it.copy(loading = false) } }
        }
    }

    private var lastTypingPing = 0L
    /** اعلام «در حال نوشتن» — حداکثر هر ۳ ثانیه یک‌بار. */
    fun notifyTyping() {
        val now = System.currentTimeMillis()
        if (now - lastTypingPing < 3000 || username.isBlank()) return
        lastTypingPing = now
        viewModelScope.launch { runCatching { Api.post("dm_typing", JSONObject().put("to", username)) } }
    }

    /** ری‌اکشن (تلگرامی) — سرور toggle می‌کند. */
    fun react(msgId: Int, emoji: String) {
        viewModelScope.launch {
            runCatching { Api.post("react", JSONObject().put("scope", "dm").put("id", msgId).put("emoji", emoji)) }
            poll()
        }
    }

    /** هدایت پیام به یک گفتگوی دیگر. */
    fun forward(msgId: Int, toUser: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching { Api.post("forward", JSONObject().put("source_scope", "dm").put("id", msgId).put("to", toUser)) }
                .onSuccess { onDone(null) }.onFailure { onDone(it.message) }
        }
    }

    fun sendText(text: String, replyTo: Int?, onErr: (String) -> Unit) {
        val t = text.trim(); if (t.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                val b = JSONObject().put("to", username).put("message", t)
                if (replyTo != null) b.put("reply_to", replyTo)
                Api.post("dm_send", b)
            }.onFailure { onErr(it.message ?: "خطا در ارسال") }
            poll()
        }
    }

    private fun sendMediaUri(ctx: Context, type: String, uri: Uri, duration: Int?, onErr: (String) -> Unit) {
        viewModelScope.launch {
            state.update { it.copy(uploadPercent = 0) }
            val err = ir.shadbib.app.ui.messages.ChatMedia.sendMedia(ctx, "dm", target(), type, uri, duration) { pct ->
                state.update { it.copy(uploadPercent = pct) }
            }
            state.update { it.copy(uploadPercent = null) }
            if (err != null) onErr(err)
            poll()
        }
    }

    fun sendImage(ctx: Context, uri: Uri, onErr: (String) -> Unit) = sendMediaUri(ctx, "image", uri, null, onErr)
    fun sendFile(ctx: Context, uri: Uri, onErr: (String) -> Unit) = sendMediaUri(ctx, "file", uri, null, onErr)
    fun sendMusicFile(ctx: Context, uri: Uri, onErr: (String) -> Unit) = sendMediaUri(ctx, "music", uri, null, onErr)

    fun sendVoice(file: File, durationSec: Int, onErr: (String) -> Unit) {
        viewModelScope.launch {
            state.update { it.copy(uploadPercent = 0) }
            val err = ir.shadbib.app.ui.messages.ChatMedia.sendMedia(App_context(), "dm", target(), "voice", Uri.fromFile(file), durationSec) { pct ->
                state.update { it.copy(uploadPercent = pct) }
            }
            state.update { it.copy(uploadPercent = null) }
            runCatching { file.delete() }
            if (err != null) onErr(err)
            poll()
        }
    }

    private fun App_context(): Context = ir.shadbib.app.App.instance

    fun sendMusic(musicId: Int, onErr: (String) -> Unit) {
        viewModelScope.launch {
            val err = ir.shadbib.app.ui.messages.ChatMedia.sendRef("dm", target(), "music", musicId = musicId)
            if (err != null) onErr(err); poll()
        }
    }

    fun sendSticker(stickerId: Int, onErr: (String) -> Unit) {
        viewModelScope.launch {
            val err = ir.shadbib.app.ui.messages.ChatMedia.sendRef("dm", target(), "sticker", stickerId = stickerId)
            if (err != null) onErr(err); poll()
        }
    }

    fun edit(id: Int, text: String, onErr: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { Api.put("dm_edit", JSONObject().put("id", id).put("message", text.trim())) }.onFailure { onErr(it.message ?: "خطا") }
            poll()
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch { runCatching { Api.delete("dm_delete", "id" to id.toString()) }; poll() }
    }
}
