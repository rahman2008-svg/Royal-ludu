package com.example.ludo.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LudoDao {
    // Profile DB APIs
    @Query("SELECT * FROM profiles WHERE id = 1 LIMIT 1")
    fun getProfile(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE id = 1 LIMIT 1")
    suspend fun getProfileDirect(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    // History DB APIs
    @Query("SELECT * FROM match_history ORDER BY timestamp DESC")
    fun getAllMatchHistories(): Flow<List<MatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatchHistory(match: MatchHistoryEntity)

    // Saved Game DB APIs for game pausing/resuming
    @Query("SELECT * FROM saved_game WHERE id = 1 LIMIT 1")
    fun getSavedGame(): Flow<SavedGameEntity?>

    @Query("SELECT * FROM saved_game WHERE id = 1 LIMIT 1")
    suspend fun getSavedGameDirect(): SavedGameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedGame(game: SavedGameEntity)

    @Query("DELETE FROM saved_game WHERE id = 1")
    suspend fun deleteSavedGame()
}
