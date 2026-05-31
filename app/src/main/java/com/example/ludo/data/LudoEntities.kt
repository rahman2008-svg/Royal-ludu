package com.example.ludo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1, // Single player user profile
    val name: String = "Royal Emperor",
    val avatarId: Int = 0, // Avatars: list of 0-7 images/icons
    val matchesPlayed: Int = 0,
    val matchesWon: Int = 0,
    val totalRolls: Int = 0,
    val selectedTheme: String = "classic" // Default theme ID
)

@Entity(tableName = "match_history")
data class MatchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val winnerName: String,
    val winnerColorHex: String,
    val playerNamesList: String, // Comma-separated or serialized players
    val dateString: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_game")
data class SavedGameEntity(
    @PrimaryKey val id: Int = 1, // Store one active match state to resume
    val stateJson: String,      // Serialized game state
    val timestamp: Long = System.currentTimeMillis()
)
