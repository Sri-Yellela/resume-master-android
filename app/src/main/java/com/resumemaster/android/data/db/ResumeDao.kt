package com.resumemaster.android.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface ResumeDao {

  @Query("SELECT COUNT(*) FROM resumes")
  suspend fun countResumes(): Int

  @Query("SELECT * FROM resumes ORDER BY last_modified DESC LIMIT 1")
  suspend fun latestResume(): ResumeEntity?

  /**
   * ORDER BY is not decoration here.
   *
   * Room's `@Relation` cannot express one, which is why this DAO uses plain queries and assembles
   * the graph itself: a resume whose sections came back in storage order would render with
   * Experience above Contact and nothing would look broken enough to report.
   */
  @Query("SELECT * FROM resume_sections WHERE resume_id = :resumeId ORDER BY section_order ASC")
  suspend fun sectionsOf(resumeId: String): List<SectionEntity>

  @Query("SELECT * FROM resume_fields WHERE section_id IN (:sectionIds) ORDER BY position ASC")
  suspend fun fieldsOf(sectionIds: List<String>): List<FieldEntity>

  @Insert
  suspend fun insertResume(resume: ResumeEntity)

  @Insert
  suspend fun insertSections(sections: List<SectionEntity>)

  @Insert
  suspend fun insertFields(fields: List<FieldEntity>)

  @Query("DELETE FROM resume_sections WHERE resume_id = :resumeId")
  suspend fun deleteSectionsOf(resumeId: String)

  @Query("DELETE FROM resumes WHERE id = :resumeId")
  suspend fun deleteResume(resumeId: String)

  /**
   * Replace one resume, whole, in a single transaction.
   *
   * WHY DELETE-AND-INSERT RATHER THAN A DIFF. Every edit in the builder produces a complete new
   * `Resume` object — the ViewModel's mutations are all `copy()` — so there is no delta to apply,
   * and computing one would mean re-deriving what changed from two snapshots that already agree.
   *
   * WHY IT MUST BE @Transaction. Between the delete and the insert the resume has NO sections. A
   * process death in that window, or a concurrent read, would see an empty resume — which is
   * precisely the data loss this whole change exists to prevent. The transaction makes the window
   * unobservable: either the old rows or the new ones, never neither.
   *
   * Deleting the sections is enough to clear the fields: the foreign key cascades. That relies on
   * foreign keys being ENFORCED, which Room enables by default and `ResumeDatabaseTest` pins,
   * because a silently-disabled cascade would leak every field row of every edit.
   */
  @Transaction
  suspend fun replaceResume(
    resume: ResumeEntity,
    sections: List<SectionEntity>,
    fields: List<FieldEntity>,
  ) {
    deleteSectionsOf(resume.id)
    deleteResume(resume.id)
    insertResume(resume)
    insertSections(sections)
    insertFields(fields)
  }

  /** Load the whole graph, ordered. Null when nothing has been saved yet. */
  @Transaction
  suspend fun loadLatestGraph(): ResumeGraph? {
    val resume = latestResume() ?: return null
    val sections = sectionsOf(resume.id)
    val fields = if (sections.isEmpty()) emptyList() else fieldsOf(sections.map { it.id })
    return ResumeGraph(resume, sections, fields)
  }
}

/** A resume and its descendants, already ordered by the queries that produced them. */
data class ResumeGraph(
  val resume: ResumeEntity,
  val sections: List<SectionEntity>,
  val fields: List<FieldEntity>,
)
