package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: Int,
    val bookTitle: String,
    val bookAuthor: String,
    val borrowerName: String,
    val borrowerClass: String,
    val loanDate: Long, // Start timestamp
    val dueDate: Long,  // Expected return timestamp (1 week/7 days after loanDate by default)
    val returnDate: Long? = null, // Actual return timestamp (null if still borrowed)
    val quantity: Int = 1 // Borrowed book quantity
) {
    fun calculatedIsOverdue(): Boolean {
        return returnDate == null && System.currentTimeMillis() > dueDate
    }
}
