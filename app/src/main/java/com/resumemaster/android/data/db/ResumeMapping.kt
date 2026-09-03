package com.resumemaster.android.data.db

import com.resumemaster.android.models.Resume
import com.resumemaster.android.models.ResumeField
import com.resumemaster.android.models.ResumeSection

/**
 * Domain <-> rows.
 *
 * Kept as free functions with no Android or Room dependency so they are testable on the JVM. The
 * ordering guarantee this layer is responsible for is the part most likely to break and the part a
 * device test would be slowest to catch.
 */

/** Flatten a resume into the three row sets, assigning the order columns from list POSITION. */
fun Resume.toRows(): Triple<ResumeEntity, List<SectionEntity>, List<FieldEntity>> {
  val resumeRow = ResumeEntity(
    id = id,
    name = name,
    templateId = templateID,
    lastModified = lastModified,
  )

  // section_order comes from the LIST INDEX, not from section.order.
  //
  // Those two disagree in practice: reorderSections() and deleteSection() renumber `order` from the
  // list, but addField/updateField/toggleVisible do not touch it, and onLinkedInImport inserts at
  // index 0 before renumbering. Trusting the field would persist whatever it happened to hold;
  // trusting the index persists what the user actually sees. The index is the truth on screen, so
  // it is the truth on disk.
  val sectionRows = sections.mapIndexed { index, section ->
    SectionEntity(
      id = section.id,
      resumeId = id,
      title = section.title,
      isVisible = section.isVisible,
      sectionOrder = index,
    )
  }

  val fieldRows = sections.flatMap { section ->
    section.fields.mapIndexed { index, field ->
      FieldEntity(
        id = field.id,
        sectionId = section.id,
        label = field.label,
        value = field.value,
        isBold = field.isBold,
        position = index,
      )
    }
  }

  return Triple(resumeRow, sectionRows, fieldRows)
}

/**
 * Rebuild a resume from rows.
 *
 * Assumes the rows arrive ordered, because the DAO's queries order them. It does NOT re-sort:
 * re-sorting here would hide a missing ORDER BY in SQL, and then the ordering would break the first
 * time anything read those tables without going through this function.
 *
 * `order` on the rebuilt section is set from the row's `section_order` so the domain object is
 * self-consistent with its own position, rather than carrying whatever stale number was saved.
 */
fun ResumeGraph.toResume(): Resume {
  val fieldsBySection = fields.groupBy { it.sectionId }

  return Resume(
    id = resume.id,
    name = resume.name,
    templateID = resume.templateId,
    lastModified = resume.lastModified,
    sections = sections.map { section ->
      ResumeSection(
        id = section.id,
        title = section.title,
        isVisible = section.isVisible,
        order = section.sectionOrder,
        fields = fieldsBySection[section.id].orEmpty().map { field ->
          ResumeField(
            id = field.id,
            label = field.label,
            value = field.value,
            isBold = field.isBold,
          )
        },
      )
    },
  )
}
