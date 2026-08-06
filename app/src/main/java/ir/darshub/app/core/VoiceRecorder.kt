package ir.darshub.app.core

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/** Simple voice-note recorder → AAC/m4a in cache. */
class VoiceRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startMs: Long = 0

    val isRecording: Boolean get() = recorder != null

    fun start(): Boolean {
        stopInternal(delete = true)
        val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        return try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(64000)
            r.setAudioSamplingRate(44100)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            outputFile = file
            startMs = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            runCatching { r.release() }
            outputFile = null
            false
        }
    }

    /** Returns (file, durationSeconds) or null if too short/failed. */
    fun stop(): Pair<File, Int>? {
        val file = outputFile
        val dur = ((System.currentTimeMillis() - startMs) / 1000).toInt()
        val ok = runCatching { recorder?.stop() }.isSuccess
        runCatching { recorder?.release() }
        recorder = null
        return if (ok && file != null && file.exists() && dur >= 1) {
            file to dur
        } else {
            file?.delete()
            outputFile = null
            null
        }
    }

    fun cancel() = stopInternal(delete = true)

    fun elapsedSec(): Int = if (isRecording) ((System.currentTimeMillis() - startMs) / 1000).toInt() else 0

    private fun stopInternal(delete: Boolean) {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        if (delete) { outputFile?.delete(); outputFile = null }
    }
}
