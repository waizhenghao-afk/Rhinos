package com.rhinos.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rhinos.app.data.model.DailyPnl
import com.rhinos.app.data.model.Note
import com.rhinos.app.data.model.Trade

@Database(
    entities = [Trade::class, DailyPnl::class, Note::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RhinosDatabase : RoomDatabase() {
    abstract fun tradeDao(): TradeDao
    abstract fun dailyPnlDao(): DailyPnlDao
    abstract fun noteDao(): NoteDao
}
