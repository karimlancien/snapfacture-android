package com.ohmybattery.invoicing.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE invoices ADD COLUMN vehicleModel TEXT")
        db.execSQL("ALTER TABLE invoices ADD COLUMN vehicleRegistration TEXT")
    }
}
