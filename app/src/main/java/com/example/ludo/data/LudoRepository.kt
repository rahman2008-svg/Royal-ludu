package com.example.ludo.data

import kotlinx.coroutines.flow.Flow

class LudoRepository(private val dao: LudoDao) {

    val profile: Flow<ProfileEntity?> = dao.getProfile()

    val matchHistories: Flow<List<MatchHistoryEntity>> = dao.getAllMatchHistories()

    val savedGame: Flow<SavedGameEntity?> = dao.getSavedGame()

    suspend fun getProfileDirect(): ProfileEntity {
        return dao.getProfileDirect() ?: ProfileEntity().also {
            dao.insertProfile(it)
        }
    }

    suspend fun saveProfile(profile: ProfileEntity) {
        dao.insertProfile(profile)
    }

    suspend fun saveMatchHistory(match: MatchHistoryEntity) {
        dao.insertMatchHistory(match)
        // Also update the profile stats when a match completes
        val currentProfile = getProfileDirect()
        val isUserWin = match.winnerName == currentProfile.name
        val updated = currentProfile.copy(
            matchesPlayed = currentProfile.matchesPlayed + 1,
            matchesWon = currentProfile.matchesWon + if (isUserWin) 1 else 0
        )
        dao.insertProfile(updated)
    }

    suspend fun getSavedGameDirect(): SavedGameEntity? {
        return dao.getSavedGameDirect()
    }

    suspend fun saveGame(stateJson: String) {
        dao.insertSavedGame(SavedGameEntity(stateJson = stateJson))
    }

    suspend fun clearSavedGame() {
        dao.deleteSavedGame()
    }
}
