package ir.shadbib.app.ui.study.room

/**
 * دیالوگ خودکار شخصیت‌ها.
 *
 * جمله فقط از روی وضعیت و یک عدد ثابت (شناسهٔ صندلی + شمارندهٔ چرخش)
 * انتخاب می‌شود؛ پس در هر بازترکیب عوض نمی‌شود و به چشم پریدن نمی‌افتد.
 */
object RoomDialog {

    private val studying = listOf(
        "تمرکز! 📚",
        "یه صفحهٔ دیگه…",
        "دارم پیش می‌رم!",
        "هیس، درس دارم",
        "این فصل تمومه",
        "حواسم جمعه ✨",
    )

    private val idle = listOf(
        "درس بخون دیگه!",
        "شروع کنیم؟",
        "بزن بریم 💪",
        "کی استارت می‌زنی؟",
        "یه نفس بکشم…",
        "تایمر رو بزن!",
    )

    private val sleeping = listOf(
        "زیزیزی…",
        "پنج دقیقهٔ دیگه…",
        "خوابم برد 😴",
    )

    fun line(state: String, seed: Int): String {
        val pool = when (state) {
            RoomState.STUDYING -> studying
            RoomState.SLEEPING -> sleeping
            else -> idle
        }
        val i = ((seed % pool.size) + pool.size) % pool.size
        return pool[i]
    }
}
