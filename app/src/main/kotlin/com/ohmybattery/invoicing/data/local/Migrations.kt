package com.ohmybattery.invoicing.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE invoices ADD COLUMN vehicleModel TEXT")
        db.execSQL("ALTER TABLE invoices ADD COLUMN vehicleRegistration TEXT")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE invoices ADD COLUMN type TEXT NOT NULL DEFAULT 'INVOICE'")
        db.execSQL("ALTER TABLE invoices ADD COLUMN linkedInvoiceId INTEGER")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE invoices ADD COLUMN comment TEXT")
    }
}
