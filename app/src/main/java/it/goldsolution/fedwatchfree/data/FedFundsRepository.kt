package it.goldsolution.fedwatchfree.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.floor

/**
 * Free provider chain:
 * 1) attempts to read the public CME/QuikStrike FedWatch HTML with the CME page as referrer;
 * 2) if CME denies/changes the page, falls back to an independent calculation from ZQ futures.
 *
 * No paid CME API key is used.
 */
object FedFundsRepository {
    private const val CME_PAGE = "https://www.cmegroup.com/markets/interest-rates/cme-fedwatch-tool.html"
    private const val QUIKSTRIKE = "https://cmegroup-tools.quikstrike.net/User/QuikStrikeView.aspx?viewitemid=IntegratedFedWatchTool"
    private const val TV_CONTRACTS = "https://www.tradingview.com/symbols/CBOT-ZQ1%21/contracts/"

    enum class DataKind { CME_PUBLIC, CALCULATED_FALLBACK }

    data class Result(
        val meetingDate: LocalDate,
        val outcomeA: String,
        val probabilityA: Double,
        val outcomeB: String,
        val probabilityB: Double,
        val meetingFuture: Double? = null,
        val anchorFuture: Double? = null,
        val expectedChangeBps: Double? = null,
        val source: String,
        val dataKind: DataKind
    )

    fun loadNextMeeting(): Result {
        val meeting = LocalDate.of(2026, 9, 16)

        // Prefer the exact probabilities shown by the public CME FedWatch component.
        try {
            loadCmePublic(meeting)?.let { return it }
        } catch (_: Exception) {
            // Deliberately continue to the transparent calculated fallback.
        }

        return loadCalculatedFallback(meeting)
    }

    private fun loadCmePublic(meeting: LocalDate): Result? {
        val doc = Jsoup.connect(QUIKSTRIKE)
            .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124 Mobile Safari/537.36")
            .referrer(CME_PAGE)
            .header("Accept-Language", "en-US,en;q=0.9")
            .timeout(18000)
            .followRedirects(true)
            .get()

        if (doc.title().contains("Error", ignoreCase = true) ||
            doc.text().contains("Access to QuikStrike has been denied", ignoreCase = true)) {
            return null
        }

        // FedWatch's Current table contains target-rate ranges + probabilities.
        // We intentionally locate it semantically instead of relying on one brittle table index.
        val candidates = mutableListOf<Pair<String, Double>>()
        val rateRegex = Regex("\\b(\\d+(?:\\.\\d+)?\\s*[-–]\\s*\\d+(?:\\.\\d+)?)\\s*%?\\b")
        val pctRegex = Regex("(?<![0-9])(100(?:\\.0+)?|\\d{1,2}(?:\\.\\d+)?)\\s*%")

        for (table in doc.select("table")) {
            val text = table.text()
            if (!text.contains("Probability", true) && !text.contains("Target Rate", true) && !text.contains("Current", true)) continue
            for (row in table.select("tr")) {
                val rowText = row.text()
                val rate = rateRegex.find(rowText)?.groupValues?.get(1)?.replace(" ", "") ?: continue
                val probs = pctRegex.findAll(rowText).mapNotNull { it.groupValues[1].toDoubleOrNull() }.toList()
                if (probs.isNotEmpty()) candidates += rate to probs.first()
            }
        }

        val nonZero = candidates
            .filter { it.second > 0.0 }
            .distinctBy { it.first }
            .sortedByDescending { it.second }

        if (nonZero.size < 2) return null

        val a = nonZero[0]
        val b = nonZero[1]
        // Guard against grabbing an unrelated table: next-meeting probabilities should approximately sum to 100.
        if (abs((a.second + b.second) - 100.0) > 2.0) return null

        return Result(
            meetingDate = meeting,
            outcomeA = "TARGET ${a.first}%",
            probabilityA = a.second,
            outcomeB = "TARGET ${b.first}%",
            probabilityB = b.second,
            source = "CME FedWatch • pagina pubblica",
            dataKind = DataKind.CME_PUBLIC
        )
    }

    private fun loadCalculatedFallback(meeting: LocalDate): Result {
        val meetingSymbol = "ZQU2026"
        val anchorSymbol = "ZQV2026"

        val html = Jsoup.connect(TV_CONTRACTS)
            .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36")
            .referrer("https://www.google.com/")
            .timeout(15000)
            .get()
            .html()

        val meetingPx = extractPrice(html, meetingSymbol)
        val anchorPx = extractPrice(html, anchorSymbol)
        val avgMeeting = 100.0 - meetingPx
        val endMeeting = 100.0 - anchorPx

        val ym = YearMonth.from(meeting)
        val totalDays = ym.lengthOfMonth().toDouble()
        val nBefore = meeting.dayOfMonth.toDouble()
        val mAfter = totalDays - nBefore
        val startMeeting = (avgMeeting - (mAfter / totalDays) * endMeeting) / (nBefore / totalDays)
        val delta = endMeeting - startMeeting
        val units = delta / 0.25

        val sign = if (units >= 0) 1 else -1
        val magnitude = abs(units)
        val whole = floor(magnitude).toInt()
        val rem = magnitude - whole
        val bpsA = whole * 25 * sign
        val bpsB = (whole + 1) * 25 * sign

        return Result(
            meetingDate = meeting,
            outcomeA = describe(bpsA),
            probabilityA = (1.0 - rem).coerceIn(0.0, 1.0) * 100.0,
            outcomeB = describe(bpsB),
            probabilityB = rem.coerceIn(0.0, 1.0) * 100.0,
            meetingFuture = meetingPx,
            anchorFuture = anchorPx,
            expectedChangeBps = delta * 100.0,
            source = "STIMA • ZQ futures / formula FedWatch",
            dataKind = DataKind.CALCULATED_FALLBACK
        )
    }

    private fun describe(bps: Int): String = when {
        bps > 0 -> "RIALZO +${bps} bp"
        bps < 0 -> "TAGLIO ${bps} bp"
        else -> "INVARIATO"
    }

    private fun extractPrice(html: String, symbol: String): Double {
        val idx = html.indexOf(symbol)
        require(idx >= 0) { "Contratto $symbol non trovato nella sorgente gratuita" }
        val chunk = html.substring(idx, minOf(html.length, idx + 12000))
        val regex = Regex("(?<![0-9])9[0-9](?:[\\.,][0-9]{2,4})(?![0-9])")
        return regex.findAll(chunk)
            .mapNotNull { it.value.replace(',', '.').toDoubleOrNull() }
            .firstOrNull { it in 90.0..100.0 }
            ?: error("Prezzo $symbol non disponibile. Tocca ↻ più tardi.")
    }
}
