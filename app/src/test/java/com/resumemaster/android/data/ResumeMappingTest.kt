package com.resumemaster.android.data

import com.resumemaster.android.data.db.ResumeGraph
import com.resumemaster.android.data.db.toResume
import com.resumemaster.android.data.db.toRows
import com.resumemaster.android.models.Resume
import com.resumemaster.android.models.ResumeField
import com.resumemaster.android.models.ResumeSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The resume, through the schema and back.
 *
 * ORDER IS THE POINT OF THESE TESTS. Every other property of a resume survives a round trip because
 * it is a column; order survives only because two synthetic columns carry it, and a SELECT that
 * forgot its ORDER BY would return every field of every section, all correct, in the wrong
 * sequence. That is a defect a user notices immediately and a test suite notices never — unless the
 * test builds a resume whose stored order and natural order DISAGREE, which is what `resume()`
 * below does deliberately.
 */
class ResumeMappingTest {

  /**
   * A resume whose section list order does NOT match the `order` field any section carries, and
   * whose fields are in a sequence no sort would reproduce by accident.
   *
   * The stale `order` values (9, 7, 3) are realistic, not contrived: the ViewModel renumbers `order`
   * on reorder and delete but not on any other mutation, so a resume that has been edited for a
   * while genuinely holds numbers that no longer describe its layout.
   */
  private fun resume() = Resume(
    id = "r1",
    name = "Sri Yellela",
    templateID = "modern",
    lastModified = 1_756_000_000_000L,
    sections = listOf(
      ResumeSection(
        id = "s-contact", title = "Contact", order = 9, isVisible = true,
        fields = listOf(
          ResumeField(id = "f-email", label = "Email", value = "a@b.c"),
          ResumeField(id = "f-phone", label = "Phone", value = "+1", isBold = true),
          ResumeField(id = "f-site", label = "Site", value = ""),
        ),
      ),
      ResumeSection(
        id = "s-exp", title = "Experience", order = 7, isVisible = false,
        fields = listOf(
          ResumeField(id = "f-role", label = "Role", value = "SWE"),
          ResumeField(id = "f-dates", label = "Dates", value = "2022-2026"),
        ),
      ),
      ResumeSection(
        id = "s-edu", title = "Education", order = 3, isVisible = true,
        fields = emptyList(),
      ),
    ),
  )

  /** What the DAO does: hand back rows ordered by the two order columns. */
  private fun roundTrip(source: Resume): Resume {
    val (resumeRow, sectionRows, fieldRows) = source.toRows()
    return ResumeGraph(
      resume = resumeRow,
      sections = sectionRows.sortedBy { it.sectionOrder },
      fields = fieldRows.sortedBy { it.position },
    ).toResume()
  }

  @Test
  fun `section order survives, and comes from list position rather than the stale order field`() {
    val out = roundTrip(resume())

    assertEquals(listOf("Contact", "Experience", "Education"), out.sections.map { it.title })
    // The saved numbers were 9, 7, 3 — descending, so a sort by the stale field would have
    // REVERSED the resume. They are renumbered from position on the way out.
    assertEquals(listOf(0, 1, 2), out.sections.map { it.order })
  }

  @Test
  fun `field order survives inside every section`() {
    val out = roundTrip(resume())

    assertEquals(
      listOf("Email", "Phone", "Site"),
      out.sections.first { it.id == "s-contact" }.fields.map { it.label },
    )
    assertEquals(
      listOf("Role", "Dates"),
      out.sections.first { it.id == "s-exp" }.fields.map { it.label },
    )
  }

  @Test
  fun `fields of different sections do not leak into each other`() {
    // Every field row is ordered by `position` in ONE query across all sections, so positions
    // collide across sections by design — three sections all have a field at position 0. The
    // section_id is what keeps them apart, and a regrouping that ignored it would put Contact's
    // Email inside Experience.
    val out = roundTrip(resume())
    assertEquals(3, out.sections.first { it.id == "s-contact" }.fields.size)
    assertEquals(2, out.sections.first { it.id == "s-exp" }.fields.size)
    assertEquals(0, out.sections.first { it.id == "s-edu" }.fields.size)
  }

  @Test
  fun `every scalar survives, including the ones that are easy to drop`() {
    val out = roundTrip(resume())
    val source = resume()

    assertEquals(source.id, out.id)
    assertEquals(source.name, out.name)
    assertEquals(source.templateID, out.templateID)
    assertEquals(source.lastModified, out.lastModified)

    // isVisible=false is a user decision to hide a section from the PDF; defaulting it to true on
    // read would silently put it back.
    assertEquals(false, out.sections.first { it.id == "s-exp" }.isVisible)
    assertEquals(true, out.sections.first { it.id == "s-contact" }.isVisible)

    // isBold on one field only, so a blanket true or false would fail here.
    val contact = out.sections.first { it.id == "s-contact" }.fields
    assertEquals(false, contact.first { it.id == "f-email" }.isBold)
    assertEquals(true, contact.first { it.id == "f-phone" }.isBold)

    // An empty value is a real value — a field the user cleared, not a field to drop.
    assertEquals("", contact.first { it.id == "f-site" }.value)
  }

  @Test
  fun `ids are preserved, because the UI addresses sections and fields by id`() {
    // Every mutation in ResumeViewModel looks up by id. Regenerating ids on load would make a
    // reopened resume uneditable in a way that looks like the taps are being ignored.
    val out = roundTrip(resume())
    assertEquals(listOf("s-contact", "s-exp", "s-edu"), out.sections.map { it.id })
    assertEquals(
      listOf("f-email", "f-phone", "f-site"),
      out.sections.first { it.id == "s-contact" }.fields.map { it.id },
    )
  }

  @Test
  fun `a null templateID stays null`() {
    val out = roundTrip(resume().copy(templateID = null))
    assertNull(out.templateID)
  }

  @Test
  fun `a resume with no sections round-trips as empty rather than reverting to a seed`() {
    // Reachable: deleteSection can remove the last one. The repository must persist THIS, and the
    // seed must not come back on next launch — which is why hydration counts resumes rather than
    // testing whether a resume has content.
    val out = roundTrip(resume().copy(sections = emptyList()))
    assertTrue(out.sections.isEmpty())
    assertEquals("Sri Yellela", out.name)
  }

  @Test
  fun `every field row is stamped with its own section, and positions restart per section`() {
    val (_, sections, fields) = resume().toRows()

    assertEquals(listOf(0, 1, 2), sections.map { it.sectionOrder })
    assertEquals("r1", sections.map { it.resumeId }.distinct().single())

    val contact = fields.filter { it.sectionId == "s-contact" }
    val exp = fields.filter { it.sectionId == "s-exp" }
    assertEquals(listOf(0, 1, 2), contact.map { it.position })
    assertEquals(listOf(0, 1), exp.map { it.position })
  }
}
