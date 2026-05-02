package com.exchenged.client.dao

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.exchenged.client.dto.Node
import com.exchenged.client.dto.Subscription


@Database(entities = [Subscription::class, Node::class], version = 4)
abstract class ExchengedClientDatabase: RoomDatabase() {


    abstract fun NodeDao(): NodeDao

    abstract fun SubscriptionDao(): SubscriptionDao

    companion object {

        @Volatile
        var INSTANCE: ExchengedClientDatabase? = null

        fun getXrayDatabase(context: Context): ExchengedClientDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExchengedClientDatabase::class.java,
                    "exchenged_client_database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
val MIGRATION_1_2 = object: Migration(1,2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS Link")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS Node (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                protocolPrefix TEXT NOT NULL,
                address TEXT NOT NULL,
                port INTEGER NOT NULL,
                selected INTEGER NOT NULL,
                remark TEXT,
                subscriptionId INTEGER NOT NULL,
                url TEXT NOT NULL,
                countryISO TEXT NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE Subscription ADD COLUMN updateInterval INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE Subscription ADD COLUMN userInfo TEXT")
        db.execSQL("ALTER TABLE Subscription ADD COLUMN supportUrl TEXT")
        db.execSQL("ALTER TABLE Subscription ADD COLUMN webPageUrl TEXT")
        db.execSQL("ALTER TABLE Subscription ADD COLUMN isLocked INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE Subscription ADD COLUMN announce TEXT")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add missing isAutoUpdate column
        db.execSQL("ALTER TABLE Subscription ADD COLUMN isAutoUpdate INTEGER NOT NULL DEFAULT 0")
    }
}
