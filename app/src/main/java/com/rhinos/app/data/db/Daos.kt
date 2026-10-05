package com.rhinos.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.rhinos.app.data.model.DailyPnl
import com.rhinos.app.data.model.Note
import com.rhinos.app.data.model.Trade
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeDao {

    @Query("SELECT * FROM trades ORDER BY tradeDate DESC, createdAt DESC, id DESC")
    fun observeAll(): Flow<List<Trade>>

    @Query("SELECT * FROM trades")
    suspend fun getAllOnce(): List<Trade>

    @Query("SELECT * FROM trades WHERE id = :id")
    suspend fun getById(id: Long): Trade?

    @Insert
    suspend fun insert(trade: Trade): Long

    @Insert
    suspend fun insertAll(trades: List<Trade>)

    @Update
    suspend fun update(trade: Trade)

    @Delete
    suspend fun delete(trade: Trade)

    @Query("DELETE FROM trades")
    suspend fun deleteAll()
}

@Dao
interface DailyPnlDao {

    @Query("SELECT * FROM daily_pnl ORDER BY date DESC, createdAt DESC")
    fun observeAll(): Flow<List<DailyPnl>>

    @Query("SELECT * FROM daily_pnl")
    suspend fun getAllOnce(): List<DailyPnl>

    @Query("SELECT * FROM daily_pnl WHERE id = :id")
    suspend fun getById(id: Long): DailyPnl?

    @Insert
    suspend fun insert(entry: DailyPnl): Long

    @Insert
    suspend fun insertAll(entries: List<DailyPnl>)

    @Update
    suspend fun update(entry: DailyPnl)

    @Delete
    suspend fun delete(entry: DailyPnl)

    @Query("DELETE FROM daily_pnl")
    suspend fun deleteAll()
}

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<Note>>

    @Query("SELECT * FROM notes")
    suspend fun getAllOnce(): List<Note>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): Note?

    @Insert
    suspend fun insert(note: Note): Long

    @Insert
    suspend fun insertAll(notes: List<Note>)

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("DELETE FROM notes")
    suspend fun deleteAll()
}
