package com.voicetasker.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.voicetasker.app.data.local.dao.CategoryDao
import com.voicetasker.app.data.local.dao.NoteDao
import com.voicetasker.app.data.local.dao.ReminderDao
import com.voicetasker.app.data.local.entity.CategoryEntity
import com.voicetasker.app.data.local.entity.NoteEntity
import com.voicetasker.app.data.local.entity.ReminderEntity

@Database(entities = [NoteEntity::class, CategoryEntity::class, ReminderEntity::class], version = 2, exportSchema = false)
abstract class VoiceTaskerDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun categoryDao(): CategoryDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        const val DATABASE_NAME = "voicetasker_db"
        fun getCallback(): Callback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val now = System.currentTimeMillis()
                db.execSQL("INSERT INTO categories (name, colorHex, iconName, isDefault, createdAt) VALUES ('Lavoro', '#6C63FF', 'Work', 1, $now)")
                db.execSQL("INSERT INTO categories (name, colorHex, iconName, isDefault, createdAt) VALUES ('Famiglia', '#FF6584', 'Person', 1, $now)")
                db.execSQL("INSERT INTO categories (name, colorHex, iconName, isDefault, createdAt) VALUES ('Salute', '#00D9A6', 'Favorite', 1, $now)")
            }
        }
    }
}
