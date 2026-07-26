package com.voicetasker.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VoiceTaskerMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        VoiceTaskerDatabase::class.java
    )

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val databaseNames = mutableSetOf<String>()

    @After
    fun deleteTestDatabases() {
        databaseNames.forEach(context::deleteDatabase)
    }

    @Test
    fun migrate1To3_preservesNotesAndAssociationsAndAssignsCanonicalKeys() {
        val name = testDatabaseName("v1-to-v3")
        helper.createDatabase(name, 1).use { db ->
            insertCategory(db, 41, "Lavoro", "Work", isDefault = true)
            insertCategory(db, 73, "Personale", "Person", isDefault = true)
            insertCategory(db, 105, "Salute", "Favorite", isDefault = true)
            insertCategory(db, 222, "Lavoro", "Label", isDefault = false)
            insertV1Note(db, 501, 41, "work note")
            insertV1Note(db, 502, 73, "family note")
            insertV1Note(db, 503, 105, "health note")
            insertV1Note(db, 504, 222, "custom note")
        }

        helper.runMigrationsAndValidate(name, 3, true, MIGRATION_1_2, MIGRATION_2_3).use { db ->
            assertEquals(4, db.longQuery("SELECT COUNT(*) FROM notes"))
            assertEquals(
                listOf(501L to 41L, 502L to 73L, 503L to 105L, 504L to 222L),
                db.idAssociations()
            )
            assertEquals(4, db.longQuery("SELECT COUNT(*) FROM notes WHERE location = ''"))
            assertEquals(4, db.longQuery("SELECT COUNT(*) FROM notes WHERE noteTime = ''"))
            db.assertLegacyNoteFields(
                id = 501,
                expectedTitle = "work note",
                expectedCategoryId = 41,
                expectedIsPinned = 0
            )
            assertEquals("work", db.stringQuery("SELECT canonicalKey FROM categories WHERE id = 41"))
            assertEquals("family", db.stringQuery("SELECT canonicalKey FROM categories WHERE id = 73"))
            assertEquals("health", db.stringQuery("SELECT canonicalKey FROM categories WHERE id = 105"))
            assertNull(db.nullableStringQuery("SELECT canonicalKey FROM categories WHERE id = 222"))
            assertEquals("ok", db.stringQuery("PRAGMA integrity_check"))
        }
    }

    @Test
    fun migrate2To3_preservesExistingNoteFieldsAndHandlesSameNameCustomCategory() {
        val name = testDatabaseName("v2-to-v3")
        helper.createDatabase(name, 2).use { db ->
            insertCategory(db, 81, "Lavoro", "Work", isDefault = true)
            insertCategory(db, 96, "Lavoro", "Label", isDefault = false)
            insertCategory(db, 117, "Personale", "Person", isDefault = true)
            insertCategory(db, 144, "Salute", "Favorite", isDefault = true)
            insertV2Note(db, 601, 81, "Office", "09:30")
            insertV2Note(db, 602, 96, "Home", "20:15")
        }

        helper.runMigrationsAndValidate(name, 3, true, MIGRATION_2_3).use { db ->
            assertEquals(
                listOf(601L to 81L, 602L to 96L),
                db.idAssociations()
            )
            assertEquals("Office", db.stringQuery("SELECT location FROM notes WHERE id = 601"))
            assertEquals("09:30", db.stringQuery("SELECT noteTime FROM notes WHERE id = 601"))
            assertEquals("Home", db.stringQuery("SELECT location FROM notes WHERE id = 602"))
            assertEquals("20:15", db.stringQuery("SELECT noteTime FROM notes WHERE id = 602"))
            db.assertLegacyNoteFields(
                id = 601,
                expectedTitle = "title",
                expectedCategoryId = 81,
                expectedIsPinned = 1
            )
            assertEquals("work", db.stringQuery("SELECT canonicalKey FROM categories WHERE id = 81"))
            assertNull(db.nullableStringQuery("SELECT canonicalKey FROM categories WHERE id = 96"))
            assertEquals("family", db.stringQuery("SELECT canonicalKey FROM categories WHERE id = 117"))
            assertEquals("health", db.stringQuery("SELECT canonicalKey FROM categories WHERE id = 144"))
            assertEquals("ok", db.stringQuery("PRAGMA integrity_check"))
        }
    }

    @Test
    fun migrate2To3_recognizesRenamedDefaultWithoutUsingIdsOrInsertingMissingDefaults() {
        val name = testDatabaseName("renamed-and-missing")
        helper.createDatabase(name, 2).use { db ->
            insertCategory(db, 700, "Progetti", "Work", isDefault = true)
            insertCategory(db, 900, "Casa", "Label", isDefault = false)
        }

        helper.runMigrationsAndValidate(name, 3, true, MIGRATION_2_3).use { db ->
            assertEquals(2, db.longQuery("SELECT COUNT(*) FROM categories"))
            assertEquals("work", db.stringQuery("SELECT canonicalKey FROM categories WHERE id = 700"))
            assertEquals("Progetti", db.stringQuery("SELECT name FROM categories WHERE id = 700"))
            assertNull(db.nullableStringQuery("SELECT canonicalKey FROM categories WHERE id = 900"))
            assertEquals(0, db.longQuery("SELECT COUNT(*) FROM categories WHERE canonicalKey = 'family'"))
            assertEquals(0, db.longQuery("SELECT COUNT(*) FROM categories WHERE canonicalKey = 'health'"))
            assertEquals("ok", db.stringQuery("PRAGMA integrity_check"))
        }
    }

    @Test
    fun migrate2To3_leavesDuplicateDefaultCandidatesUnassigned() {
        val name = testDatabaseName("duplicates")
        helper.createDatabase(name, 2).use { db ->
            insertCategory(db, 310, "Famiglia", "Person", isDefault = true)
            insertCategory(db, 311, "Parenti", "Person", isDefault = true)
            insertCategory(db, 470, "Altro", "Label", isDefault = false)
        }

        helper.runMigrationsAndValidate(name, 3, true, MIGRATION_2_3).use { db ->
            assertEquals(3, db.longQuery("SELECT COUNT(*) FROM categories"))
            assertEquals(0, db.longQuery("SELECT COUNT(*) FROM categories WHERE canonicalKey IS NOT NULL"))
            assertEquals(listOf(310L, 311L, 470L), db.ids("SELECT id FROM categories ORDER BY id"))
            assertEquals("ok", db.stringQuery("PRAGMA integrity_check"))
        }
    }

    @Test
    fun migratedDatabase_reopensTwiceThroughRoomWithoutChangingData() {
        val name = testDatabaseName("room-reopen")
        helper.createDatabase(name, 2).use { db ->
            insertCategory(db, 55, "Lavoro rinominato", "Work", isDefault = true)
            insertV2Note(db, 808, 55, "Saved", "12:45")
        }
        helper.runMigrationsAndValidate(name, 3, true, MIGRATION_2_3).close()

        repeat(2) {
            val roomDatabase = Room.databaseBuilder(context, VoiceTaskerDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
            try {
                val db = roomDatabase.openHelper.writableDatabase
                assertEquals(1, db.longQuery("SELECT COUNT(*) FROM categories"))
                assertEquals(1, db.longQuery("SELECT COUNT(*) FROM notes"))
                assertEquals(55, db.longQuery("SELECT categoryId FROM notes WHERE id = 808"))
                assertEquals("Saved", db.stringQuery("SELECT location FROM notes WHERE id = 808"))
                assertEquals("12:45", db.stringQuery("SELECT noteTime FROM notes WHERE id = 808"))
                assertEquals("work", db.stringQuery("SELECT canonicalKey FROM categories WHERE id = 55"))
                assertEquals("ok", db.stringQuery("PRAGMA integrity_check"))
            } finally {
                roomDatabase.close()
            }
        }
    }

    private fun testDatabaseName(suffix: String): String =
        "voicetasker-migration-$suffix.db".also(databaseNames::add)

    private fun insertCategory(
        db: SupportSQLiteDatabase,
        id: Long,
        name: String,
        iconName: String,
        isDefault: Boolean
    ) {
        db.execSQL(
            """
            INSERT INTO categories (id, name, colorHex, iconName, isDefault, createdAt)
            VALUES (?, ?, '#123456', ?, ?, 1700000000000)
            """.trimIndent(),
            arrayOf(id, name, iconName, if (isDefault) 1 else 0)
        )
    }

    private fun insertV1Note(
        db: SupportSQLiteDatabase,
        id: Long,
        categoryId: Long,
        title: String
    ) {
        db.execSQL(
            """
            INSERT INTO notes (
                id, title, transcription, audioFilePath, categoryId, scheduledDate,
                createdAt, updatedAt, durationMs, isPinned, isCompleted
            ) VALUES (?, ?, 'transcription', '/audio.m4a', ?, 1700000100000,
                      1700000000000, 1700000200000, 1500, 0, 0)
            """.trimIndent(),
            arrayOf(id, title, categoryId)
        )
    }

    private fun insertV2Note(
        db: SupportSQLiteDatabase,
        id: Long,
        categoryId: Long,
        location: String,
        noteTime: String
    ) {
        db.execSQL(
            """
            INSERT INTO notes (
                id, title, transcription, audioFilePath, categoryId, scheduledDate,
                createdAt, updatedAt, durationMs, isPinned, isCompleted, location, noteTime
            ) VALUES (?, 'title', 'transcription', '/audio.m4a', ?, 1700000100000,
                      1700000000000, 1700000200000, 1500, 1, 0, ?, ?)
            """.trimIndent(),
            arrayOf(id, categoryId, location, noteTime)
        )
    }

    private fun SupportSQLiteDatabase.longQuery(sql: String): Long =
        query(sql).use { cursor ->
            cursor.moveToFirst()
            cursor.getLong(0)
        }

    private fun SupportSQLiteDatabase.stringQuery(sql: String): String =
        query(sql).use { cursor ->
            cursor.moveToFirst()
            cursor.getString(0)
        }

    private fun SupportSQLiteDatabase.nullableStringQuery(sql: String): String? =
        query(sql).use { cursor ->
            cursor.moveToFirst()
            if (cursor.isNull(0)) null else cursor.getString(0)
        }

    private fun SupportSQLiteDatabase.idAssociations(): List<Pair<Long, Long>> =
        query("SELECT id, categoryId FROM notes ORDER BY id").use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getLong(0) to cursor.getLong(1))
                }
            }
        }

    private fun SupportSQLiteDatabase.assertLegacyNoteFields(
        id: Long,
        expectedTitle: String,
        expectedCategoryId: Long,
        expectedIsPinned: Long
    ) {
        query(
            """
            SELECT title, transcription, audioFilePath, categoryId, scheduledDate,
                   createdAt, updatedAt, durationMs, isPinned, isCompleted
            FROM notes WHERE id = ?
            """.trimIndent(),
            arrayOf(id)
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals(expectedTitle, cursor.getString(0))
            assertEquals("transcription", cursor.getString(1))
            assertEquals("/audio.m4a", cursor.getString(2))
            assertEquals(expectedCategoryId, cursor.getLong(3))
            assertEquals(1700000100000, cursor.getLong(4))
            assertEquals(1700000000000, cursor.getLong(5))
            assertEquals(1700000200000, cursor.getLong(6))
            assertEquals(1500, cursor.getLong(7))
            assertEquals(expectedIsPinned, cursor.getLong(8))
            assertEquals(0, cursor.getLong(9))
        }
    }

    private fun SupportSQLiteDatabase.ids(sql: String): List<Long> =
        query(sql).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getLong(0))
                }
            }
        }
}
