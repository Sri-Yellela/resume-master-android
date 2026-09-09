package com.resumemaster.android.data.contract

/**
 * What an ATS score is allowed to say on a phone.
 *
 * A MIRROR of shared/atsBands.js in the desktop repo, which is the single definition. The cutpoints
 * are duplicated here because a native client cannot import JavaScript, and they are pinned so the
 * two cannot drift silently: ContractJobDecodeTest asserts the cutpoints, the gate's independence
 * from the Strong band, and that no label renders the number; ContractDriftTest asserts the
 * contract still marks matchScore internal, which is the reason the band exists at all.
 *
 * There is no AtsBandsContractTest. This comment named one until the mobile corruption sweep went
 * looking for it — the behaviour was covered the whole time, by the two classes above, but a name
 * that resolves to nothing reads as a missing guard to the next person who greps for it, and this
 * project has already shipped four guards that were inert for real.
 *
 * ── WHY THE NUMBER IS NOT ON SCREEN ─────────────────────────────────────────────────────────────
 *
 * Job.matchScore is marked INTERNAL in the contract, in these words: "DO NOT DISPLAY THIS NUMBER".
 * The engine runs Spearman rho 0.746 against the owner's human-graded 30 with 12.2% of pairs still
 * mis-ordered. That is a genuinely useful ordering and it is nowhere near "this job is a 43".
 *
 * The number stays in the payload for exactly one sanctioned use: the auto-apply gate is a numeric
 * threshold (30) and a client may show remaining capacity against it. Rendering it as a score, a
 * percentage, a progress ring or a colour ramp is not that use.
 *
 * ── A NULL IS NOT A ZERO ────────────────────────────────────────────────────────────────────────
 *
 * null means the scorer DECLINED for want of signal. Declining took the false-match rate from 22.8%
 * to 0.8%, so it is the engine's most honest answer, and coercing it to 0 would render it as the
 * worst grade available. NOT_ENOUGH_SIGNAL is its own band, deliberately off the green-amber-red
 * axis, and its copy does not describe a degree of fit.
 */
enum class AtsBand {
  STRONG,
  MODERATE,
  WEAK,
  NOT_ENOUGH_SIGNAL,
}

object AtsBands {

  /** INCLUSIVE lower bounds on the internal score. From the owner's graded 30, not percentiles. */
  const val STRONG_CUTPOINT = 44
  const val MODERATE_CUTPOINT = 26

  /**
   * The auto-apply gate, which is NOT a band and must never be coupled to one.
   *
   * Kept here beside the cutpoints so the distinction is visible at the point of temptation. Gating
   * on STRONG would cut auto-apply volume from ~36% of the board to ~6% as a side effect of a copy
   * decision; moving STRONG to 30 would answer the gate's question ("is this safe to submit
   * unattended") with the band's answer ("what should this person be told"). 44 and 30 being
   * different is not a near-miss to be tidied up.
   */
  const val AUTO_APPLY_GATE_THRESHOLD = 30

  fun bandFor(score: Int?): AtsBand = when {
    score == null -> AtsBand.NOT_ENOUGH_SIGNAL
    score >= STRONG_CUTPOINT -> AtsBand.STRONG
    score >= MODERATE_CUTPOINT -> AtsBand.MODERATE
    else -> AtsBand.WEAK
  }

  /** The short chip label. Matches shared/atsBands.js `short`. */
  fun shortLabel(band: AtsBand): String = when (band) {
    AtsBand.STRONG -> "Strong"
    AtsBand.MODERATE -> "Moderate"
    AtsBand.WEAK -> "Weak"
    AtsBand.NOT_ENOUGH_SIGNAL -> "No signal"
  }

  fun label(band: AtsBand): String = when (band) {
    AtsBand.STRONG -> "Strong match"
    AtsBand.MODERATE -> "Moderate match"
    AtsBand.WEAK -> "Weak match"
    AtsBand.NOT_ENOUGH_SIGNAL -> "Not enough signal"
  }

  fun blurb(band: AtsBand): String = when (band) {
    AtsBand.STRONG -> "Your resume covers most of what this posting asks for."
    AtsBand.MODERATE -> "Some of what this posting asks for is covered, and some is missing."
    AtsBand.WEAK -> "Little of what this posting asks for appears in your resume."
    AtsBand.NOT_ENOUGH_SIGNAL ->
      "This posting does not say enough for a fit to be judged. It is not a poor match — it is an unknown one."
  }

  /**
   * Background / foreground, as hex, matching shared/atsBands.js exactly.
   *
   * NOT_ENOUGH_SIGNAL is grey on purpose and must stay off the green-amber-red scale: it is the
   * absence of a judgement, so it must not read as a position on the axis the other three share.
   */
  fun colors(band: AtsBand): Pair<String, String> = when (band) {
    AtsBand.STRONG -> "#dcfce7" to "#166534"
    AtsBand.MODERATE -> "#fef9c3" to "#854d0e"
    AtsBand.WEAK -> "#fee2e2" to "#991b1b"
    AtsBand.NOT_ENOUGH_SIGNAL -> "#e5e7eb" to "#4b5563"
  }
}
