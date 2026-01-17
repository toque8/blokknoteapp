package space.blokknote.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val content: String = "",
    val htmlContent: String = "",
    val fontFamily: String = "",
    val fontSize: Int = 17,
    val language: String = "ru",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isShared: Boolean = false,
    val shareId: String? = null
)