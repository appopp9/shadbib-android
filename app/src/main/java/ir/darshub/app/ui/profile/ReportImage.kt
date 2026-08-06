package ir.darshub.app.ui.profile

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.res.ResourcesCompat
import ir.darshub.app.R
import ir.darshub.app.core.Fmt
import ir.darshub.app.core.fa
import ir.darshub.app.data.DailyReport
import java.io.File
import java.io.FileOutputStream

/** خروجی تصویری گزارش روزانه: کارت زیبا با فونت وزیرمتن و رنگ تم کاربر،
 *  ذخیره در گالری (Pictures/DarsHub) با MediaStore. */
object ReportImage {

    private fun Int.c() = this or (0xFF shl 24)

    fun save(
        ctx: Context,
        username: String,
        r: DailyReport,
        primary: Int,        // ARGB رنگ اصلی تم فعال
        secondary: Int,
        dark: Boolean,
    ): String? { // null = موفق، در غیر این صورت پیام خطا
        return try {
            val bmp = render(ctx, username, r, primary, secondary, dark)
            val name = "darshub_report_" + System.currentTimeMillis() + ".png"
            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/DarsHub")
                }
                val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return "خطا در ساخت فایل"
                ctx.contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    ?: return "خطا در ذخیره"
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "DarsHub")
                if (!dir.exists()) dir.mkdirs()
                val f = File(dir, name)
                FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                android.media.MediaScannerConnection.scanFile(ctx, arrayOf(f.absolutePath), arrayOf("image/png"), null)
            }
            null
        } catch (e: Exception) { e.message ?: "خطا در ذخیره تصویر" }
    }

    private fun render(ctx: Context, username: String, r: DailyReport, primary: Int, secondary: Int, dark: Boolean): Bitmap {
        val w = 1080
        val pad = 64f
        val rowH = 84f
        val bodyRows = r.sessions.size + r.courses.size
        val h = (760 + bodyRows * rowH + (if (r.sessions.isNotEmpty()) 110 else 0) + (if (r.courses.isNotEmpty()) 110 else 0) + 130).toInt()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)

        val bg = if (dark) 0x0E1412.c() else 0xF4FAF7.c()
        val fg = if (dark) 0xECF2EF.c() else 0x14201A.c()
        val sub = if (dark) 0x9FB2AA.c() else 0x51655B.c()
        val cardBg = if (dark) 0x16211C.c() else 0xFFFFFF.c()

        val bold = ResourcesCompat.getFont(ctx, R.font.vazirmatn_bold) ?: Typeface.DEFAULT_BOLD
        val black = ResourcesCompat.getFont(ctx, R.font.vazirmatn_black) ?: bold
        val regular = ResourcesCompat.getFont(ctx, R.font.vazirmatn_regular) ?: Typeface.DEFAULT

        fun paint(size: Float, color: Int, tf: Typeface, center: Boolean = true) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color; textSize = size; typeface = tf
            textAlign = if (center) Paint.Align.CENTER else Paint.Align.RIGHT
        }

        // پس‌زمینه
        cv.drawColor(bg)
        // هدر گرادیانی با گوشه گرد
        val headerH = 470f
        val hp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, w.toFloat(), headerH,
                intArrayOf(mix(primary, 0xFF000000.toInt(), 0.25f), primary, secondary), null, Shader.TileMode.CLAMP)
        }
        cv.drawRoundRect(RectF(28f, 28f, w - 28f, headerH), 48f, 48f, hp)
        // حباب‌های تزئینی
        val bubble = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x22FFFFFF }
        cv.drawCircle(w - 140f, 110f, 90f, bubble)
        cv.drawCircle(160f, headerH - 90f, 120f, bubble)
        cv.drawCircle(w / 2f + 210f, headerH - 40f, 55f, bubble)

        val cx = w / 2f
        var y = 150f
        cv.drawText("درس هاب 📚", cx, y, paint(64f, 0xFFFFFFFF.toInt(), black)); y += 92f
        cv.drawText(username, cx, y, paint(52f, 0xFFFFFFFF.toInt(), bold)); y += 66f
        cv.drawText(Fmt.todayFull(), cx, y, paint(36f, 0xDDFFFFFF.toInt(), regular)); y += 96f
        cv.drawText("📖 ${Fmt.minutes(r.totalMinutes)}   |   🔥 ${r.streak.fa()} روز استریک", cx, y, paint(42f, 0xFFFFFFFF.toInt(), bold))

        // ردیف چیپ‌های زمان
        y = headerH + 90f
        val chips = buildList {
            add("⏰ بیداری: " + (r.wakeupTime?.let { Fmt.hm(it) } ?: "—"))
            add("🌅 اولی��: " + (r.firstStudy?.let { Fmt.hm(it) } ?: "—"))
            add("🌙 آخرین: " + (r.lastStudy?.let { Fmt.hm(it) } ?: "—"))
        }
        val chipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardBg }
        val chipTxt = paint(32f, fg, regular)
        val chipW = (w - pad * 2 - 40f) / 3f
        chips.forEachIndexed { i, t ->
            val left = pad + i * (chipW + 20f)
            cv.drawRoundRect(RectF(left, y - 52f, left + chipW, y + 24f), 34f, 34f, chipPaint)
            cv.drawText(t, left + chipW / 2f, y - 4f, chipTxt)
        }
        y += 110f

        fun sectionTitle(t: String) {
            cv.drawText(t, w - pad, y, paint(40f, primary, bold, center = false)); y += 64f
        }
        fun row(right: String, left: String) {
            cv.drawRoundRect(RectF(pad, y - 50f, w - pad, y + 22f), 26f, 26f, chipPaint)
            cv.drawText(right, w - pad - 26f, y - 4f, paint(33f, fg, regular, center = false))
            val lp = paint(33f, primary, bold); lp.textAlign = Paint.Align.LEFT
            cv.drawText(left, pad + 26f, y - 4f, lp)
            y += rowH
        }

        if (r.sessions.isNotEmpty()) {
            sectionTitle("📚 پارت‌های امروز")
            r.sessions.forEach { s2 ->
                row("${s2.icon} ${s2.course}", "${Fmt.hm(s2.start)} تا ${Fmt.hm(s2.end)}  ·  ${s2.minutes.fa()} دقیقه")
            }
            y += 30f
        }
        if (r.courses.isNotEmpty()) {
            sectionTitle("📊 تفکیک درس‌ها")
            r.courses.forEach { c2 -> row("${c2.icon} ${c2.name}", Fmt.minutes(c2.minutes)) }
            y += 20f
        }

        cv.drawText("ساخته‌شده با اپلیکیشن درس هاب 🌿", cx, h - 56f, paint(30f, sub, regular))
        return bmp
    }

    private fun mix(a: Int, b: Int, t: Float): Int {
        fun ch(sa: Int, sb: Int) = (sa + ((sb - sa) * t)).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (ch((a shr 16) and 0xFF, (b shr 16) and 0xFF) shl 16) or
                (ch((a shr 8) and 0xFF, (b shr 8) and 0xFF) shl 8) or ch(a and 0xFF, b and 0xFF)
    }
}
