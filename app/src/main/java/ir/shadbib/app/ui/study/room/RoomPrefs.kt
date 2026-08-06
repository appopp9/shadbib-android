package ir.shadbib.app.ui.study.room

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A desk/wall item unlocked with study coins.
 *
 * [group] is the item family (clock / plant / mug / lamp). Only one model per
 * family is shown on the desk at a time; the rest stay in storage.
 */
data class DeskItem(
    val key: String,
    val fa: String,
    val cost: Int,
    val group: String,
)

data class DeskGroup(
    val key: String,
    val fa: String,
)

object RoomPrefs {

    val groups: List<DeskGroup> = listOf(
        DeskGroup("clock", "ساعت مطالعه"),
        DeskGroup("plant", "گلدان"),
        DeskGroup("mug", "ماگ"),
        DeskGroup("lamp", "چراغ"),
    )

    /** One studied minute = one coin. */
    val catalogue: List<DeskItem> = listOf(
        DeskItem("lamp", "چراغ مطالعه کلاسیک", 60, "lamp"),
        DeskItem("lamp_neon", "چراغ نئونی", 900, "lamp"),
        DeskItem("clock_round", "ساعت رومیزی گرد", 120, "clock"),
        DeskItem("clock_hourglass", "ساعت شنی", 480, "clock"),
        DeskItem("clock_digital", "ساعت دیجیتال", 1200, "clock"),
        DeskItem("clock_pendulum", "ساعت پاندولی", 2400, "clock"),
        DeskItem("plant", "گلدان ساده", 180, "plant"),
        DeskItem("plant_cactus", "کاکتوس", 540, "plant"),
        DeskItem("plant_monstera", "مونسترا", 1100, "plant"),
        DeskItem("plant_bonsai", "بونسای", 2000, "plant"),
        DeskItem("mug_coffee", "ماگ قهوه", 360, "mug"),
        DeskItem("mug_tea", "استکان چای", 700, "mug"),
        DeskItem("mug_matcha", "ماچای سبز", 1400, "mug"),
        DeskItem("mug_cocoa", "کاکائوی داغ", 2200, "mug"),
    )

    fun itemsOf(group: String): List<DeskItem> = catalogue.filter { it.group == group }

    fun item(key: String): DeskItem? = catalogue.find { it.key == key }

    val goalChoices = listOf(60, 90, 120, 180, 240)

    private const val FILE = "shadbib_room"
    private const val K_GOAL = "goal_minutes"
    private const val K_OWNED = "owned_items"
    private const val K_SPENT = "coins_spent"
    private const val K_ACTIVE = "active_items"

    private var prefs: SharedPreferences? = null

    private val _goal = MutableStateFlow(120)
    val goal: StateFlow<Int> get() = _goal

    private val _owned = MutableStateFlow<Set<String>>(emptySet())
    val owned: StateFlow<Set<String>> get() = _owned

    private val _spent = MutableStateFlow(0)
    val spent: StateFlow<Int> get() = _spent

    /** group key -> item key currently placed on the desk. */
    private val _active = MutableStateFlow<Map<String, String>>(emptyMap())
    val active: StateFlow<Map<String, String>> get() = _active

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        prefs = p
        _goal.value = p.getInt(K_GOAL, 120)
        val ownedRaw = p.getStringSet(K_OWNED, emptySet())?.toSet() ?: emptySet()
        // migration from 2.3.1: the old "coffee" key is now "mug_coffee"
        _owned.value = if (ownedRaw.contains("coffee")) ownedRaw - "coffee" + "mug_coffee" else ownedRaw
        _spent.value = p.getInt(K_SPENT, 0)
        _active.value = decodeActive(p.getStringSet(K_ACTIVE, emptySet()))
    }

    private fun decodeActive(raw: Set<String>?): Map<String, String> {
        val out = HashMap<String, String>()
        raw?.forEach { entry ->
            val i = entry.indexOf('=')
            if (i > 0) out[entry.substring(0, i)] = entry.substring(i + 1)
        }
        return out
    }

    private fun persistActive(map: Map<String, String>) {
        val encoded = map.map { e -> e.key + "=" + e.value }.toSet()
        prefs?.edit()?.putStringSet(K_ACTIVE, encoded)?.apply()
    }

    /** Which model should be drawn for this family, or null if the user owns none. */
    fun activeOf(group: String): String? {
        val chosen = _active.value[group]
        if (chosen != null && _owned.value.contains(chosen)) return chosen
        return itemsOf(group).firstOrNull { _owned.value.contains(it.key) }?.key
    }

    fun setActive(group: String, key: String) {
        if (!_owned.value.contains(key)) return
        val m = _active.value + (group to key)
        _active.value = m
        persistActive(m)
    }

    /** Set the daily goal directly, from the goal sheet. */
    fun setGoal(minutes: Int) {
        _goal.value = minutes
        prefs?.edit()?.putInt(K_GOAL, minutes)?.apply()
    }

    fun cycleGoal() {
        val i = goalChoices.indexOf(_goal.value)
        val next = goalChoices[if (i < 0) 0 else (i + 1) % goalChoices.size]
        _goal.value = next
        prefs?.edit()?.putInt(K_GOAL, next)?.apply()
    }

    fun coins(totalMinutes: Int): Int = (totalMinutes - _spent.value).coerceAtLeast(0)

    fun isOwned(key: String): Boolean = _owned.value.contains(key)

    /** Buy an item. Returns false when already owned or coins are insufficient. */
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
        val m = _active.value + (item.group to item.key)
        _active.value = m
        persistActive(m)
        return true
    }
}
