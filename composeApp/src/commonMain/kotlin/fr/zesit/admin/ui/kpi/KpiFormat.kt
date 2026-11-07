package fr.zesit.admin.ui.kpi

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt

// Formats français — miroir de zesit-kpi/apps/web/src/lib/format.ts.

/** Groupe un entier avec des espaces (équivalent toLocaleString("fr-FR")). */
fun fmtInt(v: Double): String {
    val n = v.toLong()
    val sign = if (n < 0) "-" else ""
    val digits = abs(n).toString()
    return sign + digits.reversed().chunked(3).joinToString(" ").reversed()
}

/** Montant en euros sans décimales : "12 345 €". */
fun eur(v: Double): String = "${fmtInt(v)} €"

/** Montant en euros avec 2 décimales : "12 345,68 €". */
fun eur2(v: Double): String {
    val cents = (v * 100).let { if (it >= 0) it + 0.5 else it - 0.5 }.toLong()
    val sign = if (cents < 0) "-" else ""
    val c = abs(cents)
    val euros = c / 100
    val centimes = (c % 100).toString().padStart(2, '0')
    return "$sign${fmtInt(euros.toDouble())},$centimes €"
}

/** Prix de la pâte : "1,50 €/kg". */
fun fmtPate(p: Double): String = fmtDec2(p) + " €/kg"

/** "60/40" (CHR/GMS, partGMS = 0.4). */
fun fmtMix(partGMS: Double): String =
    "${((1 - partGMS) * 100).roundToInt()}/${(partGMS * 100).roundToInt()}"

/** Décimales à la française : "1,5" / "2,05". */
fun fmtDec(v: Double, decimals: Int): String {
    val factor = 10.0.pow(decimals)
    val scaled = round(v * factor).toLong()
    val sign = if (scaled < 0) "-" else ""
    val absValue = abs(scaled)
    val intPart = absValue / factor.toLong()
    val decPart = (absValue % factor.toLong()).toString().padStart(decimals, '0')
    val text = if (decimals == 0) intPart.toString() else "$intPart,$decPart"
    return "$sign$text"
}

fun fmtDec1(v: Double): String = fmtDec(v, 1)

fun fmtDec2(v: Double): String = fmtDec(v, 2)

/** Pourcentage : "2,0 %". */
fun fmtPct(v: Double, decimals: Int = 1): String = "${fmtDec(v, decimals)} %"

/** Axe Y compact : 1234 -> "1,2 k€", -500 -> "-500", 3 200 000 -> "3,2 M€". */
fun fmtAxisEuro(v: Double): String = when {
    abs(v) >= 1_000_000 -> "${fmtDec(v / 1_000_000, 1)} M€"
    abs(v) >= 1_000 -> "${fmtDec(v / 1_000, 1)} k€"
    else -> fmtInt(v)
}

/** Entier groupé sans unité (slider quantités) : "4 000". */
fun fmtQty(v: Double): String = fmtInt(v)

/** "Nice number" pour graduer les axes : 1 / 2 / 5 / 10 × 10^k. */
fun niceStep(span: Double): Double {
    if (span <= 0) return 1.0
    val exponent = floor(log10(span))
    val fraction = span / 10.0.pow(exponent)
    val nice = when {
        fraction <= 1.0 -> 1.0
        fraction <= 2.0 -> 2.0
        fraction <= 5.0 -> 5.0
        else -> 10.0
    }
    return nice * 10.0.pow(exponent)
}
