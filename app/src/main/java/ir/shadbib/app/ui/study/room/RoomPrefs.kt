package ir.shadbib.app.ui.study.room

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** یک وسیلهٔ روی میز که با سکهٔ مطالعه باز می‌شود. */
data class DeskItem(
    val key: String,
    val fa: String,
    val cost: Int,
)

/**
 * حافظهٔ محلی اتاق مطالعه.
 *
 * عمداً روی دستگاه ذخیره می‌شود و نه روی سرور، چون برای این قابلیت‌ها
 * لازم نیست کاربر دوباره دستی SQL بزند. سکه‌ها هم از روی مجموع دقیقه‌های
 * مطالعه‌ای که سرور می‌دهد حساب می‌شوند، پس جایی جداگانه ذخیره نمی‌شوند؛
 * فقط مقدار خرج‌شده نگه داشته می‌شود.
 */
object RoomPrefs {

    /** یک دقیقه مطالعه = یک سکه. */
    val catalogue: List<DeskItem> = listOf(
        DeskItem("lamp", "چراغ مطالعه", 60),
        DeskItem("plant", "گلدان", 180),
        DeskItem("coffee", "فنجان قهوه", 360),
    )

    val goalChoices = listOf(60, 90, 120, 180, 240)

    private const val FILE = "shadbib_room"
    private const val K_GOAL = "goal_minutes"
    private const val K_OWNED = "owned_items"
    private const val K_SPENT = "coins_spent"

    private var prefs: SharedPreferences? = null

    private val _goal = MutableStateFlow(120)
    val goal: StateFlow<Int> get() = _goal

    private val _owned = MutableStateFlow<Set<String>>(emptySet())
    val owned: StateFlow<Set<String>> get() = _owned

    private val _spent = MutableStateFlow(0)
    val spent: StateFlow<Int> get() = _spent

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        prefs = p
        _goal.value = p.getInt(K_GOAL, 120)
        _owned.value = p.getStringSet(K_OWNED, emptySet())?.toSet() ?: emptySet()
        _spent.value = p.getInt(K_SPENT, 0)
    }

    /** هدف روزانه را به گزینهٔ بعدی می‌برد. */
    fun cycleGoal() {
        val i = goalChoices.indexOf(_goal.value)
        val next = goalChoices[if (i < 0) 0 else (i + 1) % goalChoices.size]
        _goal.value = next
        prefs?.edit()?.putInt(K_GOAL, next)?.apply()
    }

    fun coins(totalMinutes: Int): Int = (totalMinutes - _spent.value).coerceAtLeast(0)

    fun isOwned(key: String): Boolean = _owned.value.contains(key)

    /** خرید. اگر سکه کم باشد یا قبلاً خریده باشد، false برمی‌گرداند. */
    fun buy(item: DeskItem, totalMinutes: Int): Boolean {
        if (isOwned(item.key)) return false
        if (coins(totalMinutes) < item.cost) return false
        val newOwned = _owned.value + item.key
        val newSpent = _spent.value + item.cost
        _owned.value = newOwned
        _spent.value = newSpent
        prefs?.edit()
            ?.putStringSet(K_OWNED, newOwned)
            ?.putInt(K_SPENT, newSpent)
            ?.apply()
        return true
    }
}
