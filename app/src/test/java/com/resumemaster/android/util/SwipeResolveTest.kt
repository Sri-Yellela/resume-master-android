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
  private val up = SwipeThresholds.DIAGONAL_OFFSET

  @Test
  fun `a drag short of the soft threshold does nothing, in either direction`() {
    assertNull(SwipeThresholds.resolve(0f, 0f))
    assertNull(SwipeThresholds.resolve(soft - 1f, 0f))
    assertNull(SwipeThresholds.resolve(-(soft - 1f), 0f))
    // A strong upward drag alone is not a star: horizontal travel gates everything.
    assertNull(SwipeThresholds.resolve(soft - 1f, -(up * 5)))
  }

  @Test
  fun `right queues, and a firm right queues first - neither submits`() {
    assertEquals(SwipeAction.Queue, SwipeThresholds.resolve(soft, 0f))
    assertEquals(SwipeAction.Queue, SwipeThresholds.resolve(hard, 0f))
    assertEquals(SwipeAction.QueuePriority, SwipeThresholds.resolve(hard + 1f, 0f))
  }

  @Test
  fun `up-and-right past the diagonal stars, and beats queue-first`() {
    assertEquals(SwipeAction.Star, SwipeThresholds.resolve(soft, -(up + 1f)))
    assertEquals(SwipeAction.Star, SwipeThresholds.resolve(hard + 1f, -(up + 1f)))
    // Exactly at the diagonal is not past it.
    assertEquals(SwipeAction.Queue, SwipeThresholds.resolve(soft, -up))
    // Downward travel never stars.
    assertEquals(SwipeAction.Queue, SwipeThresholds.resolve(soft, up * 5))
  }

  @Test
  fun `left is skip regardless of vertical travel`() {
    assertEquals(SwipeAction.Skip, SwipeThresholds.resolve(-soft, 0f))
    assertEquals(SwipeAction.Skip, SwipeThresholds.resolve(-(hard + 1f), -(up * 5)))
  }
}
