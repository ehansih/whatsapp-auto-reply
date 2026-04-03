package com.ehansih.whatsapprules

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rules")
data class Rule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contactName: String,   // "*" = match all contacts
    val keyword: String,       // "*" = match any message
    val replyMessage: String,  // supports {name} and {message} placeholders; "AI" = use Claude
    val useAI: Boolean = false,
    val aiProvider: String = AiProvider.GROQ.name, // which AI provider to use
    val isEnabled: Boolean = true
)
