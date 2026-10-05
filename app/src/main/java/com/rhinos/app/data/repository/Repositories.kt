package com.rhinos.app.data.repository

import com.rhinos.app.data.db.DailyPnlDao
import com.rhinos.app.data.db.NoteDao
import com.rhinos.app.data.model.DailyPnl
import com.rhinos.app.data.model.Note
import kotlinx.coroutines.flow.Flow

class DailyPnlRepository(private val dao: DailyPnlDao) {

    fun observeAll(): Flow<List<DailyPnl>> = dao.observeAll()

    suspend fun get(id: Long): DailyPnl? = dao.getById(id)

    suspend fun getAllOnce(): List<DailyPnl> = dao.getAllOnce()

    suspend fun add(entry: DailyPnl): Long {
        require(!entry.amount.isNaN() && !entry.amount.isInfinite()) { "Amount must be a valid number." }
        return dao.insert(entry)
    }

    suspend fun update(entry: DailyPnl) {
        require(!entry.amount.isNaN() && !entry.amount.isInfinite()) { "Amount must be a valid number." }
        dao.update(entry)
    }

    suspend fun delete(entry: DailyPnl) = dao.delete(entry)

    suspend fun deleteAll() = dao.deleteAll()

    suspend fun insertAllUnchecked(entries: List<DailyPnl>) = dao.insertAll(entries)
}

class NoteRepository(private val dao: NoteDao) {

    fun observeAll(): Flow<List<Note>> = dao.observeAll()

    suspend fun get(id: Long): Note? = dao.getById(id)

    suspend fun getAllOnce(): List<Note> = dao.getAllOnce()

    suspend fun add(note: Note): Long {
        require(note.title.isNotBlank()) { "Title is required." }
        return dao.insert(note)
    }

    suspend fun update(note: Note) {
        require(note.title.isNotBlank()) { "Title is required." }
        dao.update(note)
    }

    suspend fun delete(note: Note) = dao.delete(note)

    suspend fun deleteAll() = dao.deleteAll()

    suspend fun insertAllUnchecked(notes: List<Note>) = dao.insertAll(notes)
}
