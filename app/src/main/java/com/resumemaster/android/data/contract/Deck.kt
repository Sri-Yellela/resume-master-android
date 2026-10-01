package com.resumemaster.android.data.contract

import com.resumemaster.android.data.toUiJob
import com.resumemaster.android.models.Job
import org.json.JSONArray
import org.json.JSONObject

/**
 * Today's deck — GET /api/draft/today (contract 1.2.0, DeckResponse / DeckCard / DeckWhy).
 *
 * The same frozen deal the web shows: a card drafted here reads as decided there. Dealt from the top
 * of the fit-ranked board and NEVER padded — `size` may be below 10 (reason "thin") or 0 ("empty").
 *
 * ⛔ The card's job carries matchScore NULL by design (G5: a deck card is a display surface). The band
 * the server dealt it under is carried onto the UI Job as `band`, so the chip shows Strong/Moderate —
 * deriving it from the null score would show "Not enough signal" on every card.
 *
 * Hand-written, not generated: the generator emits the $Job model only. Small, and pinned by
 * DeckViewModelTest against the server's real field names.
 */
data class DeckWhy(val matched: List<String> = emptyList(), val missing: List<String> = emptyList(), val matchedTotal: Int = 0)

data class DeckCard(val position: Int, val band: AtsBand, val decision: String?, val job: Job, val why: DeckWhy)

data class Deck(val date: String, val size: Int, val thin: Boolean, val reason: String?, val cards: List<DeckCard>) {
  /** What is still to decide, in deal order — position IS the fit signal. */
  val undecided: List<DeckCard> get() = cards.filter { it.decision == null }
}

private fun strings(a: JSONArray?): List<String> =
  if (a == null) emptyList() else (0 until a.length()).mapNotNull { i -> a.optString(i, "").ifBlank { null } }

private fun bandOf(s: String): AtsBand = when (s) {
  "strong" -> AtsBand.STRONG
  "moderate" -> AtsBand.MODERATE
  "weak" -> AtsBand.WEAK
  else -> AtsBand.NOT_ENOUGH_SIGNAL
}

fun parseDeck(body: String): Deck {
  val o = JSONObject(body)
  val arr = o.optJSONArray("cards") ?: JSONArray()
  val cards = (0 until arr.length()).mapNotNull { i ->
    val c = arr.optJSONObject(i) ?: return@mapNotNull null
    val job = c.optJSONObject("job")?.let { ContractJob.fromJson(it) } ?: return@mapNotNull null
    val band = bandOf(c.optString("band", ""))
    val why = c.optJSONObject("why")
    DeckCard(
      position = c.optInt("position", i),
      band = band,
      decision = if (c.isNull("decision")) null else c.optString("decision").ifBlank { null },
      job = job.toUiJob().copy(band = band),
      why = DeckWhy(strings(why?.optJSONArray("matched")), strings(why?.optJSONArray("missing")), why?.optInt("matchedTotal", 0) ?: 0),
    )
  }
  return Deck(
    date = o.optString("date", ""),
    size = o.optInt("size", cards.size),
    thin = o.optBoolean("thin", false),
    reason = if (o.isNull("reason")) null else o.optString("reason").ifBlank { null },
    cards = cards,
  )
}
