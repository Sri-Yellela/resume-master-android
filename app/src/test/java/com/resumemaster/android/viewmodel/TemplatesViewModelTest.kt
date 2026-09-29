package com.resumemaster.android.viewmodel

import com.resumemaster.android.data.MockData
import com.resumemaster.android.data.ResumeRepository
import com.resumemaster.android.data.db.FieldEntity
import com.resumemaster.android.data.db.ResumeDao
import com.resumemaster.android.data.db.ResumeEntity
import com.resumemaster.android.data.db.SectionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Picking a template has to change the resume the preview and the PDF export read.
 *
 * Over an in-memory ResumeDao: the interface is what ResumeRepository depends on, and the Room
 * implementation of it is covered on a device by ResumeDatabaseTest / ResumeHydrationTest.
 */
class TemplatesViewModelTest {

  /** Holds whatever was last written; empty at start, so the repository seeds. */
  private class MemoryDao : ResumeDao {
    var resume: ResumeEntity? = null
    var sections: List<SectionEntity> = emptyList()
    var fields: List<FieldEntity> = emptyList()
    override suspend fun countResumes() = if (resume == null) 0 else 1
    override suspend fun latestResume() = resume
    override suspend fun sectionsOf(resumeId: String) = sections.filter { it.resumeId == resumeId }
    override suspend fun fieldsOf(sectionIds: List<String>) = fields.filter { it.sectionId in sectionIds }
    override suspend fun insertResume(resume: ResumeEntity) { this.resume = resume }
    override suspend fun insertSections(sections: List<SectionEntity>) { this.sections = sections }
    override suspend fun insertFields(fields: List<FieldEntity>) { this.fields = fields }
    override suspend fun deleteSectionsOf(resumeId: String) { sections = emptyList(); fields = emptyList() }
    override suspend fun deleteResume(resumeId: String) { resume = null }
  }

  @Test
  fun `selecting a template changes the resume that the preview and export read`() {
    val dao = MemoryDao()
    // Unconfined + a DAO that never suspends: hydration and every queued write run inline.
    val repo = ResumeRepository(dao, CoroutineScope(Dispatchers.Unconfined)) { MockData.defaultResume }
    assertEquals(MockData.DEFAULT_TEMPLATE_ID, repo.resume.value?.templateID)

    TemplatesViewModel(repo).select("classic")

    assertEquals("classic", repo.resume.value?.templateID)
    assertEquals("the choice is persisted, not only held in memory", "classic", dao.resume?.templateId)
    assertEquals("Classic", MockData.templateFor(repo.resume.value?.templateID).name)
  }

  @Test
  fun `an unknown or missing templateID falls back to the default rather than throwing`() {
    // ResumePreviewScreen did MockData.templates.first { it.id == ... }, which throws
    // NoSuchElementException for an id not in the list -- a crash on "Export as PDF".
    assertEquals(MockData.DEFAULT_TEMPLATE_ID, MockData.templateFor("no-such-template").id)
    assertEquals(MockData.DEFAULT_TEMPLATE_ID, MockData.templateFor(null).id)
    assertEquals("technical", MockData.templateFor("technical").id)
  }
}
