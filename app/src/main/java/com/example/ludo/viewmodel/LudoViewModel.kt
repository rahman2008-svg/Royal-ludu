package com.example.ludo.viewmodel

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ludo.data.LudoDatabase
import com.example.ludo.data.LudoRepository
import com.example.ludo.data.MatchHistoryEntity
import com.example.ludo.data.ProfileEntity
import com.example.ludo.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class LudoViewModel(application: Application) : AndroidViewModel(application) {

    private val db = LudoDatabase.getDatabase(application)
    private val repository = LudoRepository(db.ludoDao())

    // Database states
    val userProfile = repository.profile
    val matchHistory = repository.matchHistories
    val savedGame = repository.savedGame

    // Live Game state
    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players.asStateFlow()

    private val _tokens = MutableStateFlow<List<Token>>(emptyList())
    val tokens: StateFlow<List<Token>> = _tokens.asStateFlow()

    private val _activePlayerIndex = MutableStateFlow(0)
    val activePlayerIndex: StateFlow<Int> = _activePlayerIndex.asStateFlow()

    private val _diceValue = MutableStateFlow(1)
    val diceValue: StateFlow<Int> = _diceValue.asStateFlow()

    private val _turnState = MutableStateFlow(TurnState.ROLLING)
    val turnState: StateFlow<TurnState> = _turnState.asStateFlow()

    private val _isOver = MutableStateFlow(false)
    val isOver: StateFlow<Boolean> = _isOver.asStateFlow()

    private val _winner = MutableStateFlow<Player?>(null)
    val winner: StateFlow<Player?> = _winner.asStateFlow()

    private val _selectedTheme = MutableStateFlow(BoardTheme.COSMIC_DARK)
    val selectedTheme: StateFlow<BoardTheme> = _selectedTheme.asStateFlow()

    private val _logMessages = MutableStateFlow<List<String>>(listOf("Welcome to Royal Ludo! Select players & start match."))
    val logMessages: StateFlow<List<String>> = _logMessages.asStateFlow()

    // UI state states
    var isRollingAnim by mutableStateOf(false)
        private set
    var animatingTokenIndex by mutableStateOf<Pair<LudoColor, Int>?>(null) // (PlayerColor, tokenIndex)
        private set
    var movableTokenIndices by mutableStateOf<Set<Int>>(emptySet())
        private set

    private var consecutiveSixCount = 0
    private var isComputerMovingJob: Job? = null
    private var ludoToneGenerator: ToneGenerator? = null

    init {
        try {
            ludoToneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        } catch (e: Exception) {
            // Fallback if audio system not supported in test framework environment
        }

        // Load theme selection from profile
        viewModelScope.launch {
            repository.profile.collect { profile ->
                if (profile != null) {
                    val savedTheme = BoardTheme.entries.find { it.id == profile.selectedTheme }
                    if (savedTheme != null) {
                        _selectedTheme.value = savedTheme
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ludoToneGenerator?.release()
    }

    private fun playSound(type: String) {
        viewModelScope.launch {
            try {
                when (type) {
                    "roll" -> ludoToneGenerator?.startTone(ToneGenerator.TONE_CDMA_PIP, 80)
                    "move" -> ludoToneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 50)
                    "capture" -> {
                        ludoToneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 150)
                        delay(120)
                        ludoToneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 150)
                    }
                    "home" -> {
                        ludoToneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 350)
                    }
                    "win" -> {
                        ludoToneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
                        delay(250)
                        ludoToneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
                    }
                }
            } catch (e: Exception) {
                // Safely catch all exceptions in test frameworks
            }
        }
    }

    fun addLog(msg: String) {
        val currentList = _logMessages.value.toMutableList()
        currentList.add(0, msg)
        if (currentList.size > 20) {
            currentList.removeAt(currentList.lastIndex)
        }
        _logMessages.value = currentList
    }

    fun updateTheme(theme: BoardTheme) {
        _selectedTheme.value = theme
        viewModelScope.launch {
            val prof = repository.getProfileDirect()
            repository.saveProfile(prof.copy(selectedTheme = theme.id))
        }
    }

    fun saveUserProfile(name: String, avatarId: Int) {
        viewModelScope.launch {
            val current = repository.getProfileDirect()
            repository.saveProfile(current.copy(name = name, avatarId = avatarId))
            addLog("Profile updated: $name")
        }
    }

    // Configure and start a new game
    fun startNewGame(playerConfig: List<Pair<String, Boolean>>) { // Pair(Name, isComputer)
        // Set up 2-4 players with respective Ludo Colors
        val colors = LudoColor.entries
        val setupPlayers = mutableListOf<Player>()

        for (i in playerConfig.indices) {
            if (i >= 4) break
            val (name, isComp) = playerConfig[i]
            setupPlayers.add(
                Player(
                    index = i,
                    name = name,
                    color = colors[i],
                    isComputer = isComp,
                    avatarId = if (isComp) i + 4 else i, // Different mock avatar icons
                    isActive = true
                )
            )
        }

        _players.value = setupPlayers

        // Set up 16 tokens (4 tokens for each of the 4 players)
        val setupTokens = mutableListOf<Token>()
        for (player in setupPlayers) {
            for (t in 0..3) {
                setupTokens.add(Token(playerColor = player.color, tokenIndex = t, position = 0))
            }
        }
        _tokens.value = setupTokens

        _activePlayerIndex.value = 0
        _diceValue.value = 1
        _turnState.value = TurnState.ROLLING
        _isOver.value = false
        _winner.value = null
        consecutiveSixCount = 0
        movableTokenIndices = emptySet()

        _logMessages.value = listOf("Match started! Roll dice for ${activePlayer().name}")
        playSound("roll")
        saveCurrentGameState()

        // If first player is Computer, trigger auto-play
        checkAndTriggerComputerTurn()
    }

    fun activePlayer(): Player = _players.value.getOrNull(_activePlayerIndex.value) ?: Player()

    // Handle dice roll
    fun rollDice() {
        if (_turnState.value != TurnState.ROLLING || _isOver.value || isRollingAnim) return

        viewModelScope.launch {
            isRollingAnim = true
            playSound("roll")

            // Visual roll animation simulation
            repeat(8) {
                _diceValue.value = Random.nextInt(1, 7)
                delay(60)
            }

            val finalRoll = Random.nextInt(1, 7)
            _diceValue.value = finalRoll
            isRollingAnim = false

            addLog("${activePlayer().name} rolled a $finalRoll 🎲")

            // Rules management
            if (finalRoll == 6) {
                consecutiveSixCount++
                if (consecutiveSixCount == 3) {
                    addLog("Oops! Three 6s in a row for ${activePlayer().name}. Turn passed.")
                    consecutiveSixCount = 0
                    passTurn()
                    saveCurrentGameState()
                    return@launch
                }
            } else {
                consecutiveSixCount = 0
            }

            // Calculate movements possible
            calculateMovableTokens(finalRoll)

            if (movableTokenIndices.isEmpty()) {
                addLog("No moves possible for ${activePlayer().name} with roll $finalRoll.")
                delay(1000)
                if (finalRoll == 6) {
                    // Grants another roller even if couldn't move anything! Standard rule option OR turn passes.
                    // Usually in Ludo King, you roll again only if you actually move. Let's make it traditional:
                    // Under standard rules, getting 6 enables another roll, so keep turn!
                    _turnState.value = TurnState.ROLLING
                    addLog("${activePlayer().name} gets to roll again due to a 6!")
                } else {
                    passTurn()
                }
            } else {
                _turnState.value = TurnState.MOVING

                // Smart QoL: Auto-move if human player has ONLY 1 possible piece to move!
                if (!activePlayer().isComputer && movableTokenIndices.size == 1) {
                    val singleTokenIdx = movableTokenIndices.first()
                    addLog("Auto-moving sole movable piece...")
                    delay(500)
                    moveToken(singleTokenIdx)
                } else if (activePlayer().isComputer) {
                    // Computer turn auto play
                    triggerComputerMovement()
                }
            }
            saveCurrentGameState()
        }
    }

    private fun calculateMovableTokens(roll: Int) {
        val color = activePlayer().color
        val playerTokens = _tokens.value.filter { it.playerColor == color }
        val indices = mutableSetOf<Int>()

        playerTokens.forEachIndexed { idx, token ->
            val pos = token.position
            if (pos == 0 && roll == 6) {
                // Can deploy token from BASE to START index
                indices.add(idx)
            } else if (pos > 0 && pos + roll <= 57) {
                // Normal track / home path movement
                indices.add(idx)
            }
        }
        movableTokenIndices = indices
    }

    // Perform movement on selected active token index (0..3)
    fun moveToken(tokenIndex: Int) {
        if (_turnState.value != TurnState.MOVING || _isOver.value) return
        if (!movableTokenIndices.contains(tokenIndex)) return

        val pColor = activePlayer().color
        movableTokenIndices = emptySet()

        viewModelScope.launch {
            val roll = _diceValue.value
            val tokenList = _tokens.value.toMutableList()
            val tokenGlobalIdx = tokenList.indexOfFirst { it.playerColor == pColor && it.tokenIndex == tokenIndex }

            if (tokenGlobalIdx == -1) return@launch

            val initialPos = tokenList[tokenGlobalIdx].position

            // Animate moving one step at a time
            if (initialPos == 0 && roll == 6) {
                // Deploy directly to Track Position 1
                animatingTokenIndex = Pair(pColor, tokenIndex)
                playSound("move")
                tokenList[tokenGlobalIdx] = Token(pColor, tokenIndex, 1)
                _tokens.value = tokenList
                delay(200)
                animatingTokenIndex = null
            } else {
                // Step-by-step loop
                var currentPos = initialPos
                val targetPos = initialPos + roll

                animatingTokenIndex = Pair(pColor, tokenIndex)
                while (currentPos < targetPos) {
                    currentPos++
                    tokenList[tokenGlobalIdx] = Token(pColor, tokenIndex, currentPos)
                    _tokens.value = tokenList
                    playSound("move")
                    delay(120)
                }
                animatingTokenIndex = null
            }

            // After step-by-step moves, execute post-move logic: captures or home completed!
            val finalToken = _tokens.value[tokenGlobalIdx]
            val landingPosition = finalToken.position

            var earnedExtraTurn = false

            // 1. Capture/Knockout Check
            if (landingPosition in 1..51) {
                val startIdx = BoardPath.getStartLoopIndex(pColor)
                val absoluteLoopIdx = (startIdx + (landingPosition - 1)) % 52

                if (!BoardPath.isSafeLoopIndex(absoluteLoopIdx)) {
                    // Check if there are opponent characters residing on this exact cell
                    val opposingTokensToKick = _tokens.value.filter {
                        if (it.playerColor == pColor || it.position == 0 || it.position > 51) {
                            false
                        } else {
                            val oppStart = BoardPath.getStartLoopIndex(it.playerColor)
                            val oppLoopIdx = (oppStart + (it.position - 1)) % 52
                            oppLoopIdx == absoluteLoopIdx
                        }
                    }

                    if (opposingTokensToKick.isNotEmpty()) {
                        playSound("capture")
                        earnedExtraTurn = true
                        addLog("Kicked! ${activePlayer().name} caught competitor token in safe break-out!")

                        val updatedTokens = _tokens.value.map { tk ->
                            if (opposingTokensToKick.contains(tk)) {
                                addLog("Competitor ${tk.playerColor.displayName} returned to Yard Base 0!")
                                Token(tk.playerColor, tk.tokenIndex, 0)
                            } else {
                                tk
                            }
                        }
                        _tokens.value = updatedTokens
                    }
                }
            }

            // 2. Reached Home Completed Check
            if (landingPosition == 57) {
                playSound("home")
                earnedExtraTurn = true
                addLog("Success! ${pColor.displayName} piece reached HOME center! 🏆")
            }

            // 3. Victory Check
            val allHomeCompleted = _tokens.value
                .filter { it.playerColor == pColor }
                .all { it.position == 57 }

            if (allHomeCompleted) {
                playSound("win")
                _isOver.value = true
                _winner.value = activePlayer()
                addLog("VICTORY! ${activePlayer().name} (${pColor.displayName}) won the Ludo Match! 👑")

                // Save game history to database
                saveMatchHistoryToDb()
                repository.clearSavedGame() // Game ended, clear resumes
                return@launch
            }

            // Turn progression
            if (roll == 6 || earnedExtraTurn) {
                _turnState.value = TurnState.ROLLING
                addLog("${activePlayer().name} gets an extra roll!")
            } else {
                passTurn()
            }

            saveCurrentGameState()
            checkAndTriggerComputerTurn()
        }
    }

    private fun passTurn() {
        val totalPlayersCount = _players.value.size
        _activePlayerIndex.value = (_activePlayerIndex.value + 1) % totalPlayersCount
        _turnState.value = TurnState.ROLLING
        consecutiveSixCount = 0
        movableTokenIndices = emptySet()
        addLog("Next player Turn: ${activePlayer().name} (${activePlayer().color.displayName})")
    }

    private fun checkAndTriggerComputerTurn() {
        if (_isOver.value) return
        val player = activePlayer()
        if (player.isComputer && _turnState.value == TurnState.ROLLING) {
            isComputerMovingJob = viewModelScope.launch {
                delay(1200) // Realistic breathing delay for bot thinking
                rollDice()
            }
        }
    }

    private fun triggerComputerMovement() {
        val activeBot = activePlayer()
        if (!activeBot.isComputer || movableTokenIndices.isEmpty() || _isOver.value) return

        viewModelScope.launch {
            delay(1000) // Delay representation for decision taking
            val tokenToMove = selectBestBotToken(activeBot.color, movableTokenIndices)
            moveToken(tokenToMove)
        }
    }

    // Robust Rule-based Heuristic AI Engine for computer decisions
    private fun selectBestBotToken(color: LudoColor, movableIndices: Set<Int>): Int {
        val playerTokens = _tokens.value.filter { it.playerColor == color }
        val roll = _diceValue.value

        var bestTokenIdx = movableIndices.first()
        var maxPriority = -1

        for (tokenIdx in movableIndices) {
            val token = playerTokens[tokenIdx]
            val currentPos = token.position
            val targetPos = currentPos + roll
            var priority = 0

            // Priority 1: Capturing Opponent Piece
            if (targetPos in 1..51) {
                val startIdx = BoardPath.getStartLoopIndex(color)
                val targetLoopIdx = (startIdx + (targetPos - 1)) % 52
                if (!BoardPath.isSafeLoopIndex(targetLoopIdx)) {
                    val hasOpponent = _tokens.value.any {
                        it.playerColor != color && it.position in 1..51 &&
                            ((BoardPath.getStartLoopIndex(it.playerColor) + (it.position - 1)) % 52) == targetLoopIdx
                    }
                    if (hasOpponent) {
                        priority += 100 // Highest priority: kick opponent!
                    }
                }
            }

            // Priority 2: Safe Landing
            if (targetPos in 1..51) {
                val startIdx = BoardPath.getStartLoopIndex(color)
                val targetLoopIdx = (startIdx + (targetPos - 1)) % 52
                if (BoardPath.isSafeLoopIndex(targetLoopIdx)) {
                    priority += 15
                }
            }

            // Priority 3: Enter Home path or reach Home center
            if (targetPos == 57) {
                priority += 80 // Complete piece!
            } else if (targetPos in 52..56) {
                priority += 40 // Move piece into safe zone home columns
            }

            // Priority 4: Releasing from Base (Deploy piece)
            if (currentPos == 0 && roll == 6) {
                priority += 50 // Release piece is very valuable
            }

            // Priority 5: Default progressing furthest piece
            priority += currentPos / 2

            if (priority > maxPriority) {
                maxPriority = priority
                bestTokenIdx = tokenIdx
            }
        }

        return bestTokenIdx
    }

    // Persistence: Serialization and Saving
    private fun saveCurrentGameState() {
        viewModelScope.launch {
            if (_players.value.isEmpty() || _isOver.value) return@launch

            // Custom secure, fast state serialization representation
            val playerStrings = _players.value.joinToString(";") {
                "${it.index},${it.name},${it.color.name},${it.isComputer},${it.avatarId}"
            }

            val tokenStrings = _tokens.value.joinToString(";") {
                "${it.playerColor.name},${it.tokenIndex},${it.position}"
            }

            val serialized = buildString {
                append("version=1\n")
                append("theme=${_selectedTheme.value.id}\n")
                append("activeIdx=${_activePlayerIndex.value}\n")
                append("diceVal=${_diceValue.value}\n")
                append("turnState=${_turnState.value.name}\n")
                append("consecutiveSixes=$consecutiveSixCount\n")
                append("isOver=${_isOver.value}\n")
                append("logMessages=${_logMessages.value.joinToString("~")}\n")
                append("##PLAYERS##\n")
                append(playerStrings)
                append("\n##TOKENS##\n")
                append(tokenStrings)
            }

            repository.saveGame(serialized)
        }
    }

    // Try parsing and resuming saved game
    fun resumeLastSavedGame() {
        viewModelScope.launch {
            val saved = repository.getSavedGameDirect() ?: return@launch
            try {
                val lines = saved.stateJson.split("\n")
                var activeIdx = 0
                var dVal = 1
                var tState = TurnState.ROLLING
                var themeObj = BoardTheme.COSMIC_DARK
                var conSixes = 0
                var parsedPlayers = mutableListOf<Player>()
                var parsedTokens = mutableListOf<Token>()
                var logs = listOf<String>()

                var section = ""

                for (line in lines) {
                    if (line.startsWith("##PLAYERS##")) {
                        section = "PLAYERS"
                        continue
                    }
                    if (line.startsWith("##TOKENS##")) {
                        section = "TOKENS"
                        continue
                    }

                    if (section == "") {
                        val parts = line.split("=", limit = 2)
                        if (parts.size == 2) {
                            val (key, value) = parts
                            when (key) {
                                "activeIdx" -> activeIdx = value.toInt()
                                "diceVal" -> dVal = value.toInt()
                                "theme" -> {
                                    BoardTheme.entries.find { it.id == value }?.let { themeObj = it }
                                }
                                "turnState" -> tState = TurnState.valueOf(value)
                                "consecutiveSixes" -> conSixes = value.toInt()
                                "logMessages" -> logs = value.split("~")
                            }
                        }
                    } else if (section == "PLAYERS") {
                        val playerRows = line.split(";")
                        for (row in playerRows) {
                            if (row.isBlank()) continue
                            val p = row.split(",")
                            parsedPlayers.add(
                                Player(
                                    index = p[0].toInt(),
                                    name = p[1],
                                    color = LudoColor.valueOf(p[2]),
                                    isComputer = p[3].toBoolean(),
                                    avatarId = p[4].toInt()
                                )
                            )
                        }
                    } else if (section == "TOKENS") {
                        val tokenRows = line.split(";")
                        for (row in tokenRows) {
                            if (row.isBlank()) continue
                            val t = row.split(",")
                            parsedTokens.add(
                                Token(
                                    playerColor = LudoColor.valueOf(t[0]),
                                    tokenIndex = t[1].toInt(),
                                    position = t[2].toInt()
                                )
                            )
                        }
                    }
                }

                _players.value = parsedPlayers
                _tokens.value = parsedTokens
                _activePlayerIndex.value = activeIdx
                _diceValue.value = dVal
                _turnState.value = tState
                _selectedTheme.value = themeObj
                consecutiveSixCount = conSixes
                _isOver.value = false
                _winner.value = null
                if (logs.isNotEmpty()) {
                    _logMessages.value = logs
                }

                addLog("Game successfully resumed! Turn: ${activePlayer().name}")
                checkAndTriggerComputerTurn()

            } catch (e: Exception) {
                addLog("Failed serialization resume: Starting classic fallback.")
            }
        }
    }

    private suspend fun saveMatchHistoryToDb() {
        val opponentNames = _players.value.map { it.name }.joinToString(", ")
        val simpleDateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val dateString = simpleDateFormat.format(Date())

        _winner.value?.let { wm ->
            val match = MatchHistoryEntity(
                winnerName = wm.name,
                winnerColorHex = wm.color.hex,
                playerNamesList = opponentNames,
                dateString = dateString
            )
            repository.saveMatchHistory(match)
        }
    }

    fun exitGameAndClear() {
        _players.value = emptyList()
        _tokens.value = emptyList()
        _isOver.value = false
        _winner.value = null
        isComputerMovingJob?.cancel()
        movableTokenIndices = emptySet()
    }
}
