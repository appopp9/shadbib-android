package ir.shadbib.app.ui.study.room

import androidx.annotation.DrawableRes
import ir.shadbib.app.R

/**
 * رجیستری شخصیت‌های اتاق مطالعه.
 *
 * همهٔ اسپرایت‌ها روی یک بوم مربعی ۴۸۰×۴۸۰ با لنگر «پایین-وسط» رندر شده‌اند،
 * پس نسبت ابعاد همهٔ فریم‌ها دقیقاً یکسان است و برای جاگذاری فقط کافی است
 * ارتفاع بوم را بدهیم؛ عرض خودکار حساب می‌شود.
 *
 * حجم ظاهری همهٔ شخصیت‌ها برابر پاندا نرمال شده و فریم پلک دقیقاً روی
 * کادر فریم بیکار قفل شده، پس موقع پلک‌زدن هیچ جابه‌جایی‌ای رخ نمی‌دهد.
 */
data class RoomCharacter(
    val key: String,
    val fa: String,
    @DrawableRes val studying: Int,
    @DrawableRes val idle: Int,
    @DrawableRes val blink: Int,
    @DrawableRes val sleeping: Int,
)

object RoomChars {

    /** نسبت عرض به ارتفاع بوم اسپرایت‌ها (۴۸۰ ÷ ۴۸۰ = مربع). */
    const val ASPECT = 1.0f

    /**
     * تقریباً چه کسری از ارتفاع بوم را خود بدن پر می‌کند.
     * برای پیدا کردن جای بالای سر (حباب دیالوگ) لازم می‌شود.
     */
    const val BODY_FRACTION = 0.80f

    val all: List<RoomCharacter> = listOf(
        RoomCharacter(
            "cat", "گربه",
            R.drawable.char_cat_studying, R.drawable.char_cat_idle,
            R.drawable.char_cat_blink, R.drawable.char_cat_sleeping,
        ),
        RoomCharacter(
            "dog", "سگ",
            R.drawable.char_dog_studying, R.drawable.char_dog_idle,
            R.drawable.char_dog_blink, R.drawable.char_dog_sleeping,
        ),
        RoomCharacter(
            "owl", "جغد",
            R.drawable.char_owl_studying, R.drawable.char_owl_idle,
            R.drawable.char_owl_blink, R.drawable.char_owl_sleeping,
        ),
        RoomCharacter(
            "rabbit", "خرگوش",
            R.drawable.char_rabbit_studying, R.drawable.char_rabbit_idle,
            R.drawable.char_rabbit_blink, R.drawable.char_rabbit_sleeping,
        ),
        RoomCharacter(
            "fox", "روباه",
            R.drawable.char_fox_studying, R.drawable.char_fox_idle,
            R.drawable.char_fox_blink, R.drawable.char_fox_sleeping,
        ),
        RoomCharacter(
            "panda", "پاندا",
            R.drawable.char_panda_studying, R.drawable.char_panda_idle,
            R.drawable.char_panda_blink, R.drawable.char_panda_sleeping,
        ),
        RoomCharacter(
            "bear", "خرس",
            R.drawable.char_bear_studying, R.drawable.char_bear_idle,
            R.drawable.char_bear_blink, R.drawable.char_bear_sleeping,
        ),
    )

    fun of(key: String?): RoomCharacter = all.firstOrNull { it.key == key } ?: all.first()

    /** انتخاب فریم بر اساس وضعیت. پلک‌زدن فقط روی حالت بیکار معنی دارد. */
    @DrawableRes
    fun frame(c: RoomCharacter, state: String, blinking: Boolean): Int = when (state) {
        RoomState.STUDYING -> c.studying
        RoomState.SLEEPING -> c.sleeping
        else -> if (blinking) c.blink else c.idle
    }
}

object RoomState {
    const val STUDYING = "studying"
    const val IDLE = "idle"
    const val SLEEPING = "sleeping"
}
