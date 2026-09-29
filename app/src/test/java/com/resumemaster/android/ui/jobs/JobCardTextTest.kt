package com.resumemaster.android.ui.jobs

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * What the card says about a job is what the EMPLOYER said, and nothing else.
 */
class JobCardTextTest {

  private val posting = "Own the Android payments SDK."

  @Test
  fun `expanding a card shows the posting's own description and invents nothing`() {
    // The expanded card appended a hardcoded sentence -- "Requirements include strong Kotlin,
    // product judgment, testing discipline..." -- to EVERY job's description. That was harmless
    // over MockData's ten Android roles. Over the live board it put invented requirements under a
    // real employer's name on every card, whatever the role.
    assertEquals(posting, cardDescription(posting, open = true))
    assertEquals(posting, cardDescription(posting, open = false))
  }
}
