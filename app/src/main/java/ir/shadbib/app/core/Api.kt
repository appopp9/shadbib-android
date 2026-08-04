package ir.shadbib.app.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.buffer
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ApiException(message: String, val code: Int = 0) : Exception(message)

/** خطاهای فنی/شبکه را به پیام فارسی تمیز تبدیل می‌کند — هرگز نام هاست یا جزئیات فنی لو نمی‌رود. */
fun humanizeError(e: Throwable): String = when {
    e is ApiException -> e.message ?: "خطایی پیش آمد"
    e is java.net.UnknownHostException -> "اینترنت در دسترس نیست 🌐"
    e is java.net.SocketTimeoutException -> "سرور دیر جواب داد؛ دوباره تلاش کن ⏳"
    e is java.net.ConnectException -> "ارتباط برقرار نشد؛ اتصالت رو چک کن 📡"
    e is javax.net.ssl.SSLException -> "ارتباط امن برقرار نشد؛ دوباره تلاش کن 🔒"
    e is java.io.IOException -> "مشکل در اتصال — اینترنتت رو بررسی کن 🌐"
    else -> "خطایی پیش آمد؛ دوباره تلاش کن"
}

object Api {
    // NEW SERVER (studying.ir). Old host was beautifulrabor.ir/shadbib/
    const val HOST = "studying.ir"
    const val BASE = "https://studying.ir/api.php"
    const val MEDIA_BASE = "https://studying.ir/"
    // Media gateway: once media.php is live, signed links come from the server,
    // so mediaUrl() below keeps working for both plain and signed paths.
    const val MEDIA_GATEWAY = "https://studying.ir/media.php"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    fun mediaUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        return if (path.startsWith("http")) path else MEDIA_BASE + path.trimStart('/')
    }

    private fun buildUrl(action: String, query: Map<String, String>): String {
        val sb = StringBuilder("$BASE?action=$action")
        for ((k, v) in query) {
            sb.append('&').append(k).append('=').append(URLEncoder.encode(v, "UTF-8"))
        }
        return sb.toString()
    }

    private fun execute(action: String, method: String, body: JSONObject?, query: Map<String, String>): String {
        val builder = Request.Builder().url(buildUrl(action, query))
        Store.token?.let { builder.header("Authorization", "Bearer $it") }
        builder.header("Accept", "application/json")
        when (method) {
            "GET" -> builder.get()
            "DELETE" -> builder.delete()
            else -> {
                val json = (body ?: JSONObject()).toString()
                builder.method(method, json.toRequestBody("application/json; charset=utf-8".toMediaType()))
            }
        }
        val call = try { client.newCall(builder.build()).execute() } catch (e: Exception) { throw ApiException(humanizeError(e)) }
        call.use { resp ->
            val text = resp.body?.string()?.trim() ?: ""
            if (text.startsWith("{")) {
                val obj = JSONObject(text)
                if (obj.has("error")) {
                    if (resp.code == 401 && action != "login" && action != "register") Store.clearSession()
                    throw ApiException(obj.str("error"), resp.code)
                }
            }
            if (!resp.isSuccessful) throw ApiException("سرور در دسترس نیست (${resp.code}) 🛠", resp.code)
            return text
        }
    }

    suspend fun get(action: String, vararg query: Pair<String, String>): String =
        withContext(Dispatchers.IO) { execute(action, "GET", null, query.toMap()) }

    suspend fun post(action: String, body: JSONObject = JSONObject(), vararg query: Pair<String, String>): String =
        withContext(Dispatchers.IO) { execute(action, "POST", body, query.toMap()) }

    suspend fun put(action: String, body: JSONObject = JSONObject(), vararg query: Pair<String, String>): String =
        withContext(Dispatchers.IO) { execute(action, "PUT", body, query.toMap()) }

    suspend fun delete(action: String, vararg query: Pair<String, String>): String =
        withContext(Dispatchers.IO) { execute(action, "DELETE", null, query.toMap()) }

    class FilePart(val field: String, val fileName: String, val bytes: ByteArray, val mime: String)

    suspend fun upload(action: String, fields: Map<String, String>, files: List<FilePart>, onProgress: ((Int) -> Unit)? = null): String =
        withContext(Dispatchers.IO) {
            val mp = MultipartBody.Builder().setType(MultipartBody.FORM)
            for ((k, v) in fields) mp.addFormDataPart(k, v)
            for (f in files) {
                mp.addFormDataPart(f.field, f.fileName, f.bytes.toRequestBody(f.mime.toMediaType()))
            }
            val body = mp.build()
            val reqBody = if (onProgress != null) ProgressRequestBody(body, onProgress) else body
            val builder = Request.Builder().url(buildUrl(action, emptyMap())).post(reqBody)
            Store.token?.let { builder.header("Authorization", "Bearer $it") }
            val call = try { client.newCall(builder.build()).execute() } catch (e: Exception) { throw ApiException(humanizeError(e)) }
            call.use { resp ->
                val text = resp.body?.string()?.trim() ?: ""
                if (text.startsWith("{")) {
                    val obj = JSONObject(text)
                    if (obj.has("error")) throw ApiException(obj.str("error"), resp.code)
                }
                if (!resp.isSuccessful) throw ApiException("سرور در دسترس نیست (${resp.code}) 🛠", resp.code)
                text
            }
        }

    private class ProgressRequestBody(private val delegate: RequestBody, private val onProgress: (Int) -> Unit) : RequestBody() {
        override fun contentType() = delegate.contentType()
        override fun contentLength() = runCatching { delegate.contentLength() }.getOrDefault(-1L)
        override fun writeTo(sink: BufferedSink) {
            val total = contentLength()
            var written = 0L
            val counting = object : okio.ForwardingSink(sink) {
                override fun write(source: okio.Buffer, byteCount: Long) {
                    super.write(source, byteCount)
                    written += byteCount
                    if (total > 0) onProgress(((written * 100) / total).toInt().coerceIn(0, 100))
                }
            }
            val buffered = counting.buffer()
            delegate.writeTo(buffered)
            buffered.flush()
        }
    }

    fun obj(text: String): JSONObject = JSONObject(text)
    fun arr(text: String): JSONArray = JSONArray(text)
}
