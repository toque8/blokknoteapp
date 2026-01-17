package space.blokknote.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao
) {
    fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()

    suspend fun getNoteById(id: String): Note? = noteDao.getNoteById(id)

    suspend fun saveNote(note: Note) {
        if (note.id.isBlank()) {
            noteDao.insertNote(note)
        } else {
            noteDao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    suspend fun deleteAll() = noteDao.deleteAll()
}