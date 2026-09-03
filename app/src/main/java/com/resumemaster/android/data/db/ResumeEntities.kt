package com.resumemaster.android.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The resume, at rest.
 *
 * ── THIS SCHEMA MODELS NOTHING THE SERVER OWNS ──────────────────────────────────────────────────
 *
 * Deliberately. PHASE_2A's warning is that a local schema modelling jobs or applications and
 * disagreeing with the contract is Shape 1 and fails silently. So this stores only the resume the
 * builder edits, which is local-only in Phase 2a — no job rows, no application rows, no `Job`
 * mirror. Nothing here can drift from `contract/mobile-api.v1.json` because nothing here is
 * described by it. Resume SYNC is a later phase and will need a contract of its own.
 *
 * ── TWO COLUMNS THAT EXIST ONLY BECAUSE SQL HAS NO LIST TYPE ────────────────────────────────────
 *
 * `section_order` and `position`. A SELECT without ORDER BY returns rows in whatever order the
 * engine finds convenient, so a resume round-tripped through this schema without them would come
 * back with its sections and fields SHUFFLED. That is the failure this pair prevents, and it is a
 * quiet one: the data is all present and all correct, just in an order the user did not choose.
 *
 * `ResumeSection` already carries `order` in the domain model, so `section_order` is the same value
 * under a different column name — `order` is a reserved word in SQL and Room would emit it
 * unquoted. `ResumeField` carries NO order at all; its order is its position in a Kotlin list, and
 * `position` is where that goes. The domain model is left untouched: list position is the right
 * representation in memory, and an index column is the right representation on disk.
 */
@Entity(tableName = "resumes")
data class ResumeEntity(
  @PrimaryKey val id: String,
  val name: String,
  @ColumnInfo(name = "template_id") val templateId: String?,
  @ColumnInfo(name = "last_modified") val lastModified: Long,
)

/**
 * CASCADE, not SET NULL and not RESTRICT. A section belongs to exactly one resume and is meaningless
 * without it, so deleting the resume must take its sections with it — leaving orphans would grow the
 * file forever and, worse, a later resume reusing an id would inherit them.
 */
@Entity(
  tableName = "resume_sections",
  foreignKeys = [
    ForeignKey(
      entity = ResumeEntity::class,
      parentColumns = ["id"],
      childColumns = ["resume_id"],
      onDelete = ForeignKey.CASCADE,
    )
  ],
  indices = [Index("resume_id")],
)
data class SectionEntity(
  @PrimaryKey val id: String,
  @ColumnInfo(name = "resume_id") val resumeId: String,
  val title: String,
  @ColumnInfo(name = "is_visible") val isVisible: Boolean,
  @ColumnInfo(name = "section_order") val sectionOrder: Int,
)

@Entity(
  tableName = "resume_fields",
  foreignKeys = [
    ForeignKey(
      entity = SectionEntity::class,
      parentColumns = ["id"],
      childColumns = ["section_id"],
      onDelete = ForeignKey.CASCADE,
    )
  ],
  indices = [Index("section_id")],
)
data class FieldEntity(
  @PrimaryKey val id: String,
  @ColumnInfo(name = "section_id") val sectionId: String,
  val label: String,
  val value: String,
  @ColumnInfo(name = "is_bold") val isBold: Boolean,
  /** Index within the section. See the note on ResumeEntity for why this column has to exist. */
  val position: Int,
)
