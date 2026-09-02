package com.resumemaster.android.data.contract

/**
 * What the candidate will face at the apply destination, known at BROWSE time.
 *
 * ── WHY A PHONE HAS TO CARE ─────────────────────────────────────────────────────────────────────
 *
 * The desktop gated handoff works because a desktop browser is already holding the user's
 * authenticated portal session, and the extension borrows it for exactly one gesture under an
 * `activeTab` grant. That IS the security property. There is no extension on Android, so there is
 * no analogue — a `held_gate` row queued from a phone is unresolvable, forever, by design.
 *
 * So a gated job must be shown as desktop-only or excluded. There is no third option that keeps the
 * property, and "queue it anyway and hope" produces a queue the user cannot empty.
 *
 * ── UNKNOWN IS NOT COMPLETABLE, AND THAT IS BROADER THAN "GATED" ────────────────────────────────
 *
 * `null` means the row predates migration 078 and has not been recomputed. The contract is explicit
 * that it must be read exactly as UNKNOWN and NEVER as `direct`. Reading a null as direct is the
 * dangerous direction: it would queue a job whose destination nobody has classified.
 */
enum class AutomationTier(val wire: String, val completableOnMobile: Boolean) {
  DIRECT("direct", true),
  GUEST("guest", true),
  ACCOUNT("account", false),
  GATED("gated", false),
  UNKNOWN("unknown", false);

  companion object {
    /** Anything unrecognised, and null, become UNKNOWN. Never DIRECT. */
    fun from(wire: String?): AutomationTier =
      entries.firstOrNull { it.wire == wire } ?: UNKNOWN

    /**
     * The tiers a mobile feed may complete, for `tiers_include`.
     *
     * FILTER SERVER-SIDE, NEVER CLIENT-SIDE. The server pages before the client would filter, so
     * dropping rows after the fact yields short pages and a `total` that disagrees with the list —
     * and with a cursor feed it also means the cursor advances past rows the user never saw.
     */
    fun completableWireValues(): List<String> =
      entries.filter { it.completableOnMobile }.map { it.wire }
  }

  /** Copy for a job the phone can see but cannot finish. */
  val desktopOnlyReason: String?
    get() = when (this) {
      GATED -> "Needs a desktop: this employer requires an account plus a CAPTCHA or identity check."
      ACCOUNT -> "Needs a desktop: this employer requires you to create an account first."
      UNKNOWN -> "Needs a desktop: we have not yet checked what this employer's application requires."
      DIRECT, GUEST -> null
    }
}
