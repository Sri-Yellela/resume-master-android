package com.resumemaster.android.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * ⛔ NO fallbackToDestructiveMigration(), EVER, ON THIS DATABASE.
 *
 * That call is the usual way to make a schema change compile, and here it would mean: on the first
 * app update that changes this schema, silently delete the user's resume. A resume is hand-typed
 * work that exists nowhere else in Phase 2a — there is no server copy to restore from, and the
 * token store is excluded from backup while this file is not, so cloud backup is the only other
 * copy and it is a day old at best.
 *
 * Omitting it means an unmigrated schema change throws IllegalStateException at startup instead.
 * That is a loud, obvious, developer-facing failure during development, which is exactly the right
 * place for it — as opposed to a quiet, user-facing one after shipping.
 *
 * The schema is exported to app/schemas/ so a future migration can be written against a real
 * baseline rather than a reconstruction.
 */
@Database(
  entities = [ResumeEntity::class, SectionEntity::class, FieldEntity::class],
  version = 1,
  exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

  abstract fun resumeDao(): ResumeDao

  companion object {
    /**
     * Not "resume_master.db".
     *
     * The file name is referenced from `backup_rules.xml` and `data_extraction_rules.xml`, which
     * exclude the token store by path. Naming it here, once, keeps those two files reviewable
     * against something rather than against a string nobody can find.
     */
    const val DATABASE_NAME = "resume_master.db"

    fun build(context: Context): AppDatabase =
      Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
        // Room enables foreign key enforcement itself; the CASCADE from section to field depends on
        // it, so ResumeDatabaseTest asserts a delete actually cascades rather than trusting it.
        .build()
  }
}
