package ir.shadbib.app.ui.auth

/**
 * Iranian mobile numbers only. The server does the same in otp.php
 * (otp_normalize_phone), but validating in the app too means we never burn
 * an sms on a bad number and the user gets instant feedback.
 */
object IranPhone {

    /** Three digit operator prefixes, including the leading zero. */
    private val VALID_PREFIXES = listOf("090", "091", "092", "093", "094", "099")

    private const val FA_ZERO = 0x06F0
    private const val AR_ZERO = 0x0660

    /** Converts Persian and Arabic digits to plain latin digits. */
    fun toLatinDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val c = ch.code
            val out = when {
                c in FA_ZERO..(FA_ZERO + 9) -> '0' + (c - FA_ZERO)
                c in AR_ZERO..(AR_ZERO + 9) -> '0' + (c - AR_ZERO)
                else -> ch
            }
            sb.append(out)
        }
        return sb.toString()
    }

    fun digitsOnly(input: String): String = toLatinDigits(input).filter { it.isDigit() }

    /**
     * Brings any accepted shape to the 11 digit 09xxxxxxxxx form.
     * Accepts 09123456789, 9123456789, +989123456789, 00989123456789 and
     * 989123456789, written with latin or Persian digits.
     *
     * Returns null when the input is not an Iranian mobile number.
     */
    fun normalize(raw: String): String? {
        var d = digitsOnly(raw)

        if (d.startsWith("0098")) d = d.substring(4)
        else if (d.startsWith("098")) d = d.substring(3)
        else if (d.startsWith("98") && d.length >= 12) d = d.substring(2)

        if (d.length == 10 && d.startsWith("9")) d = "0" + d

        if (d.length != 11) return null
        if (!d.startsWith("09")) return null
        if (VALID_PREFIXES.none { d.startsWith(it) }) return null

        return d
    }

    fun isValid(raw: String): Boolean = normalize(raw) != null

    /**
     * The user is still typing, so we must not complain too early.
     * True while the input can still grow into a valid number.
     */
    fun couldBecomeValid(raw: String): Boolean {
        val d = digitsOnly(raw)
        if (d.isEmpty()) return true
        if (d.length > 11) return false
        if (d.length == 1) return d == "0" || d == "9"
        if (!d.startsWith("09")) return false
        if (d.length >= 3 && VALID_PREFIXES.none { it == d.substring(0, 3) }) return false
        return true
    }

    /** 09123456789 becomes 0912 345 6789, easier to read while typing. */
    fun pretty(raw: String): String {
        val d = digitsOnly(raw).take(11)
        return when {
            d.length <= 4 -> d
            d.length <= 7 -> d.substring(0, 4) + " " + d.substring(4)
            else -> d.substring(0, 4) + " " + d.substring(4, 7) + " " + d.substring(7)
        }
    }

    /** 0912***6789, for the "we sent the code to" line. */
    fun mask(raw: String): String {
        val d = normalize(raw) ?: digitsOnly(raw)
        if (d.length != 11) return d
        return d.substring(0, 4) + "***" + d.substring(7)
    }

    /*
     * Bidi isolation.
     *
     * The app runs right to left, so a bare latin number glued to Persian text
     * gets reordered by the unicode bidi algorithm and "0912***6789 فرستادیم"
     * can render with the digits in the wrong place. Wrapping the number in an
     * isolate tells the layout engine to treat it as one opaque left to right
     * chunk, whatever sits around it.
     *
     * U+2066 LEFT-TO-RIGHT ISOLATE ... U+2069 POP DIRECTIONAL ISOLATE
     */
    fun ltr(text: String): String = "\u2066" + text + "\u2069"

    /** The masked number, already safe to drop inside a Persian sentence. */
    fun maskLtr(raw: String): String = ltr(mask(raw))

    /**
     * Digit grouping used by the input field, kept here so the visual
     * transformation and the offset mapping can never drift apart.
     *
     * A space is inserted after original index 4 and after original index 7.
     */
    const val GROUP_1 = 4
    const val GROUP_2 = 7

    /** Placeholder for the input, written in the same 4 3 4 shape. */
    const val HINT = "0913 222 2222"
}
