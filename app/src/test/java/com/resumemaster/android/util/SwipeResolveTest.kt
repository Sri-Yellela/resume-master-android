package com.resumemaster.android.util

import com.resumemaster.android.models.SwipeAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The drag-to-action mapping that both the card overlay and the release handler read.
 *
 * Pins BEHAVIOUR at the thresholds, not the constants' values: what matters is which action a given
 * displacement produces, including the boundaries a refactor is most likely to move by one.
 */
class SwipeResolveTest {

  private val soft = SwipeThresholds.SOFT_OFFSET
  private val hard = SwipeThresholds.HARD_OFFSET
  private val up = 60f   // a firm upward component; it no longer means anything

  @Test
  fun `a drag short of the soft threshold does nothing, in either direction`() {
    assertNull(SwipeThresholds.resolve(0f, 0f))
    assertNull(SwipeThresholds.resolve(soft - 1f, 0f))
    assertNull(SwipeThresholds.resolve(-(soft - 1f), 0f))
    // A strong upward drag alone is not a star: horizontal travel gates everything.
    assertNull(SwipeThresholds.resolve(soft - 1f, -(up * 5)))
  }

  @Test
  fun `right drafts however far and whatever the vertical travel - it never submits`() {
    // The deck (09-30): one meaning per direction. A hard right used to be "Queue first" and an
    // up-and-right a Star — two accident-prone motions bound to two different intents.
    assertEquals(SwipeAction.Queue, SwipeThresholds.resolve(soft, 0f))
    assertEquals(SwipeAction.Queue, SwipeThresholds.resolve(hard + 1f, 0f))
    assertEquals(SwipeAction.Queue, SwipeThresholds.resolve(soft, -(up * 5)))
    assertEquals(SwipeAction.Queue, SwipeThresholds.resolve(hard + 1f, up * 5))
  }

  @Test
  fun `left is skip regardless of vertical travel`() {
    // Left is the SAVED pass (owner, 09-29) — it was a local Skip, and the card came back.
    assertEquals(SwipeAction.Dislike, SwipeThresholds.resolve(-soft, 0f))
    assertEquals(SwipeAction.Dislike, SwipeThresholds.resolve(-(hard + 1f), -(up * 5)))
  }
}
