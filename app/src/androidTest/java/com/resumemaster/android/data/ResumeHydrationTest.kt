package com.resumemaster.android.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.resumemaster.android.data.db.AppDatabase
import com.resumemaster.android.data.db.toRows
import com.resumemaster.android.models.Resume
import com.resumemaster.android.models.ResumeField
import com.resumemaster.android.models.ResumeSection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * SURVIVING PROCESS DEATH, as a test rather than as a screenshot.
 *
 * This is the claim step 4 exists to make, and the interesting half of it is not the write — that is
 * covered by ResumeDatabaseTest — but the READ BACK BY A PROCESS THAT DID NOT DO THE WRITING. A new
 * ResumeRepository built over a database some earlier run populated has to arrive at that resume and
 * not at the seed.
 *
 * It uses a REAL FILE-BACKED database, deleted and recreated per test, rather than an in-memory one:
 * an in-memory database cannot outlive the process that made it, so it cannot express the thing
 * being asserted.
 */
@RunWith(AndroidJUnit4::class)
class ResumeHydrationTest {

  private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
  private val dbName = "hydration_test.db"

  private fun openDb() =
    Room.databaseBuilder(context, AppDatabase::class.java, dbName).build()

  @Before
  fun wipe() {
    context.deleteDatabase(dbName)
  }

  @After
  fun cleanUp() {
    context.deleteDatabase(dbName)
  }

  private fun seed() = Resume(
    id = "seed",
    name = "Seed Person",
    sections = listOf(
      ResumeSection(
        id = "seed-s", title = "Summary", order = 0, isVisible = true,
        fields = listOf(ResumeField(id = "seed-f", label = "Profile", value = "seeded")),
      ),
    ),
  )

  private fun edited() = Resume(
    id = "stored",
    name = "Sri Yellela",
    templateID = "modern",
    sections = listOf(
      ResumeSection(
        id = "s-1", title = "PERSISTED", order = 0, isVisible = true,
        fields = listOf(
          ResumeField(id = "f-1", label = "Profile", value = "typed by hand"),
          ResumeField(id = "f-2", label = "Second", value = "and this"),
        ),
      ),
      ResumeSection(
        id = "s-2", title = "Experience", order = 1, isVisible = false,
        fields = listOf(ResumeField(id = "f-3", label = "Role", value = "SWE")),
      ),
    ),
  )

  /** Waits for hydration the way the UI does: by observing `ready`. */
  private suspend fun awaitReady(repo: ResumeRepository) =
    withTimeout(10_000) {
      while (!repo.ready.value) kotlinx.coroutines.delay(20)
      repo.resume.value!!
    }

  @Test
  fun aRepositoryOverAPopulatedDatabaseHydratesToTheStoredResumeAndNotTheSeed() = runBlocking {
    // ── The "previous process": write, then close the database entirely. ─────────────────────
    val writer = openDb()
    val (r, s, f) = edited().toRows()
    writer.resumeDao().replaceResume(r, s, f)
    writer.close()

    // ── The "new process": a fresh database handle and a fresh repository. ───────────────────
    val reader = openDb()
    val repo = ResumeRepository(
      reader.resumeDao(),
      CoroutineScope(SupervisorJob() + Dispatchers.IO),
    ) { seed() }

    val hydrated = awaitReady(repo)
    reader.close()

    assertEquals("stored", hydrated.id)
    assertEquals("Sri Yellela", hydrated.name)
    // The edit, read back by something that never saw it written.
    assertEquals("PERSISTED", hydrated.sections[0].title)
    assertEquals(listOf("PERSISTED", "Experience"), hydrated.sections.map { it.title })
    assertEquals(listOf("Profile", "Second"), hydrated.sections[0].fields.map { it.label })
    assertEquals(false, hydrated.sections[1].isVisible)
    // The seed must not have run, and must not have overwritten anything.
    assertFalse("the seed replaced stored work", hydrated.name == "Seed Person")
  }

  @Test
  fun anEmptyDatabaseIsSeededOnceAndThatSeedIsItselfPersisted() = runBlocking {
    val first = openDb()
    val repo = ResumeRepository(
      first.resumeDao(),
      CoroutineScope(SupervisorJob() + Dispatchers.IO),
    ) { seed() }
    val hydrated = awaitReady(repo)
    assertEquals("Seed Person", hydrated.name)
    // Seeding that only set memory would lose the resume on the next launch and silently re-seed
    // forever, which looks like it works and never accumulates an edit.
    assertEquals(1, first.resumeDao().countResumes())
    first.close()

    // Second run: the seed must NOT run again, and must not be re-inserted.
    val second = openDb()
    val repo2 = ResumeRepository(
      second.resumeDao(),
      CoroutineScope(SupervisorJob() + Dispatchers.IO),
    ) { throw AssertionError("seed ran a second time over a populated database") }
    assertEquals("Seed Person", awaitReady(repo2).name)
    assertEquals(1, second.resumeDao().countResumes())
    second.close()
  }

  @Test
  fun anEditIsDurableAcrossAFreshRepositoryOverTheSameFile() = runBlocking {
    val first = openDb()
    val repo = ResumeRepository(
      first.resumeDao(),
      CoroutineScope(SupervisorJob() + Dispatchers.IO),
    ) { seed() }
    val hydrated = awaitReady(repo)

    // Edit exactly as the ViewModel does: whole-object copy through save().
    repo.save(
      hydrated.copy(
        sections = hydrated.sections.map { it.copy(title = "EDITED-BY-USER") },
      )
    )
    // save() is fire-and-forget onto a conflated channel, so wait for the write to land.
    withTimeout(10_000) {
      while (first.resumeDao().loadLatestGraph()
          ?.sections?.firstOrNull()?.title != "EDITED-BY-USER"
      ) kotlinx.coroutines.delay(20)
    }
    first.close()

    val second = openDb()
    val repo2 = ResumeRepository(
      second.resumeDao(),
      CoroutineScope(SupervisorJob() + Dispatchers.IO),
    ) { throw AssertionError("seed ran over an edited resume") }
    assertEquals("EDITED-BY-USER", awaitReady(repo2).sections.first().title)
    second.close()
  }

  @Test
  fun saveIsRefusedBeforeHydrationSoAnEarlyEditCannotClobberStoredWork() = runBlocking {
    val writer = openDb()
    val (r, s, f) = edited().toRows()
    writer.resumeDao().replaceResume(r, s, f)
    writer.close()

    val reader = openDb()
    val repo = ResumeRepository(
      reader.resumeDao(),
      CoroutineScope(SupervisorJob() + Dispatchers.IO),
    ) { seed() }

    // Before hydration there is nothing to read and nothing may be written.
    assertNull(repo.resume.value)
    assertFalse(repo.ready.value)
    repo.save(seed().copy(name = "SHOULD NOT LAND"))

    val hydrated = awaitReady(repo)
    assertEquals("Sri Yellela", hydrated.name)

    val onDisk = reader.resumeDao().loadLatestGraph()!!
    reader.close()
    assertTrue("a pre-hydration save reached the database", onDisk.resume.name == "Sri Yellela")
  }
}
