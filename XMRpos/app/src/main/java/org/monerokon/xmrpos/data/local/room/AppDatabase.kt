package org.monerokon.xmrpos.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import org.monerokon.xmrpos.data.local.room.model.Transaction

@Database(entities = [Transaction::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}