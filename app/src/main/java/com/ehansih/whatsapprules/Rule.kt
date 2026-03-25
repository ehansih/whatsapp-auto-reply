package com.ehansih.whatsapprules

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rules")
data class Rule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contactName: String,   // "*" = match all contacts
    val keyword: String,       // "*" = match any message
    val replyMessage: String,
    val isEnabled: Boolean = true
)
