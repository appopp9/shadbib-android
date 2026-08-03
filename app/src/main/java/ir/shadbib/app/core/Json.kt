package ir.shadbib.app.core

import org.json.JSONArray
import org.json.JSONObject

/** Robust JSON helpers — PHP/PDO often returns numbers as strings. */

fun JSONObject.str(key: String): String = if (isNull(key)) "" else optString(key, "")

fun JSONObject.strOrNull(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }

fun JSONObject.int(key: String): Int = when (val v = opt(key)) {
    is Number -> v.toInt()
    is String -> v.trim().toDoubleOrNull()?.toInt() ?: 0
    else -> 0
}

fun JSONObject.intOrNull(key: String): Int? = when (val v = opt(key)) {
    is Number -> v.toInt()
    is String -> v.trim().toDoubleOrNull()?.toInt()
    else -> null
}

fun JSONObject.boolish(key: String): Boolean = when (val v = opt(key)) {
    is Boolean -> v
    is Number -> v.toInt() == 1
    is String -> v == "1" || v.equals("true", true)
    else -> false
}

fun JSONArray.objects(): List<JSONObject> =
    (0 until length()).mapNotNull { optJSONObject(it) }

fun JSONArray.strings(): List<String> =
    (0 until length()).mapNotNull { if (isNull(it)) null else optString(it) }
