package com.resumemaster.android.data

import com.resumemaster.android.data.db.ResumeDao
import com.resumemaster.android.data.db.toResume
import com.resumemaster.android.data.db.toRows
import com.resumemaster.android.models.Resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * The resume, durable.
 *
 * The builder was IN-MEMORY ONLY: `MutableStateFlow(MockData.defaultResume)` and a `save()` that
 * assigned to it. Process death lost every edit. That was invisible while the data was mock — the
 * app reopened showing the same seeded resume it always showed, which looks identical to a working
 * app — and became real data loss the moment a user typed anything of their own.
 *
 * ── WHY THE UI READS MEMORY AND NOT THE DATABASE ────────────────────────────────────────────────
 *
 * The obvious shape is to expose a Room `Flow` and let the UI observe it. It is wrong here. Every
 * keystroke in the builder is a `save()`, so a DB-backed Flow would re-emit the whole resume on
 * every character and hand Compose a new `TextField` value mid-typing — the classic jumping-cursor
 * and dropped-character bug. So memory is the source of truth for the UI, and the database is a
 * write-through mirror: the state the user sees is never waiting on disk.
 *
 * ── WHY WRITES GO THROUGH A CONFLATED CHANNEL ───────────────────────────────────────────────────
 *
 * Each save persists a COMPLETE snapshot, so two concerns matter and neither is solved by launching
 * a coroutine per save:
 *
 *   ORDER. Concurrent writes could commit a stale snapshot after a fresh one and silently revert
 *   the user's last keystrokes. A single consumer applies them in the order they were sent.
 *
 *   VOLUME. Typing a 40-character line would otherwise queue 40 full rewrites. Conflation keeps
 *   only the newest pending snapshot, which is sound precisely BECAUSE each one is complete — a
 *   dropped intermediate is not a dropped edit.
 *
 * ── WHY `resume` IS NULLABLE ────────────────────────────────────────────────────────────────────
 *
 * null means NOT YET LOADED, and it is a real state rather than a defensive type. Hydration is a
 * disk read and cannot happen on the main thread, so there is a window at startup with no answer
 * yet. Seeding the flow with the mock resume to avoid the null would mean the builder briefly
 * showing a resume that is not the user's, and any edit made in that window would be written on top
 * of the real one. The screens show a loading state instead, and `save()` refuses to write until
 * hydration has finished — so an early edit cannot clobber stored work.
 */
class ResumeRepository(
  private val dao: ResumeDao,
  private val scope: CoroutineScope,
  /** Inserted once, on first run only, when the database is empty. */
  private val seed: () -> Resume,
) {

  private val _resume = MutableStateFlow<Resume?>(null)
  val resume: StateFlow<Resume?> = _resume

  private val _ready = MutableStateFlow(false)
  val ready: StateFlow<Boolean> = _ready

  private val writes = Channel<Resume>(Channel.CONFLATED)

  init {
    scope.launch {
      // ── HYDRATE ──────────────────────────────────────────────────────────────────────────────
      //
      // The count is what makes seeding first-run-only. Testing "is there a resume?" by loading one
      // and checking for null would behave identically today and diverge the moment a user deletes
      // every section: the resume row still exists, so the seed must not come back and overwrite
      // the empty resume they deliberately made.
      val existing = if (dao.countResumes() > 0) dao.loadLatestGraph()?.toResume() else null

      if (existing != null) {
        _resume.value = existing
      } else {
        val seeded = seed()
        _resume.value = seeded
        persist(seeded)
      }
      _ready.value = true

      // ── WRITE LOOP ───────────────────────────────────────────────────────────────────────────
      // Started only after hydration, so a save racing startup cannot commit before the load.
      for (snapshot in writes) persist(snapshot)
    }
  }

  /**
   * Record an edit.
   *
   * Memory updates synchronously — the caller is a UI event handler and must not wait on disk — and
   * the durable write is queued. `lastModified` is stamped here rather than in the ViewModel so
   * every path that mutates a resume is timestamped the same way, including ones added later.
   */
  fun save(resume: Resume) {
    if (!_ready.value) return
    val stamped = resume.copy(lastModified = System.currentTimeMillis())
    _resume.value = stamped
    writes.trySend(stamped)
  }

  private suspend fun persist(resume: Resume) {
    val (resumeRow, sectionRows, fieldRows) = resume.toRows()
    dao.replaceResume(resumeRow, sectionRows, fieldRows)
  }
}
