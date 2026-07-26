package com.voicetasker.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `notes_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `transcription` TEXT NOT NULL,
                `audioFilePath` TEXT NOT NULL,
                `categoryId` INTEGER NOT NULL,
                `scheduledDate` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `durationMs` INTEGER NOT NULL,
                `isPinned` INTEGER NOT NULL,
                `isCompleted` INTEGER NOT NULL,
                `location` TEXT NOT NULL,
                `noteTime` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `notes_new` (
                `id`, `title`, `transcription`, `audioFilePath`, `categoryId`,
                `scheduledDate`, `createdAt`, `updatedAt`, `durationMs`,
                `isPinned`, `isCompleted`, `location`, `noteTime`
            )
            SELECT
                `id`, `title`, `transcription`, `audioFilePath`, `categoryId`,
                `scheduledDate`, `createdAt`, `updatedAt`, `durationMs`,
                `isPinned`, `isCompleted`, '', ''
            FROM `notes`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `notes`")
        db.execSQL("ALTER TABLE `notes_new` RENAME TO `notes`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_categoryId` ON `notes` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_scheduledDate` ON `notes` (`scheduledDate`)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `categories` ADD COLUMN `canonicalKey` TEXT")
        assignCanonicalKey(db, iconName = "Work", canonicalKey = "work")
        assignCanonicalKey(db, iconName = "Person", canonicalKey = "family")
        assignCanonicalKey(db, iconName = "Favorite", canonicalKey = "health")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_canonicalKey` " +
                "ON `categories` (`canonicalKey`)"
        )
    }

    private fun assignCanonicalKey(
        db: SupportSQLiteDatabase,
        iconName: String,
        canonicalKey: String
    ) {
        db.execSQL(
            """
            UPDATE `categories`
            SET `canonicalKey` = ?
            WHERE `isDefault` = 1
              AND `iconName` = ?
              AND (SELECT COUNT(*) FROM `categories`
                   WHERE `isDefault` = 1 AND `iconName` = ?) = 1
            """.trimIndent(),
            arrayOf(canonicalKey, iconName, iconName)
        )
    }
}
