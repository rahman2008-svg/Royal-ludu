package com.example.ludo.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ludo.model.*
import com.example.ludo.viewmodel.LudoViewModel

// Stylized Avatars list
val LUDO_AVATARS = listOf(
    "👑" to "Emperor",
    "🛡️" to "Warlord",
    "🧙" to "Mage",
    "🦄" to "Pegasus",
    "🐯" to "Tiger",
    "🐼" to "Panda",
    "👽" to "Alien",
    "🤖" to "Android"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainLudoScreen(
    viewModel: LudoViewModel,
    modifier: Modifier = Modifier
) {
    val players by viewModel.players.collectAsStateWithLifecycle()
    val tokens by viewModel.tokens.collectAsStateWithLifecycle()
    val activeIdx by viewModel.activePlayerIndex.collectAsStateWithLifecycle()
    val diceVal by viewModel.diceValue.collectAsStateWithLifecycle()
    val turnState by viewModel.turnState.collectAsStateWithLifecycle()
    val isOver by viewModel.isOver.collectAsStateWithLifecycle()
    val winner by viewModel.winner.collectAsStateWithLifecycle()
    val theme by viewModel.selectedTheme.collectAsStateWithLifecycle()
    val logMessages by viewModel.logMessages.collectAsStateWithLifecycle()

    val profileEntity by viewModel.userProfile.collectAsStateWithLifecycle(initialValue = null)
    val savedGameEntity by viewModel.savedGame.collectAsStateWithLifecycle(initialValue = null)
    val history by viewModel.matchHistory.collectAsStateWithLifecycle(initialValue = emptyList())

    // UI control states inside Lobby
    var lobbyName by remember { mutableStateOf("") }
    var lobbyAvatar by remember { mutableIntStateOf(0) }
    var showProfileEditor by remember { mutableStateOf(false) }

    // Synchronize lobby editor once profile loads
    LaunchedEffect(profileEntity) {
        profileEntity?.let {
            lobbyName = it.name
            lobbyAvatar = it.avatarId
        }
    }

    // Default Starting Configuration Setup for Lobby
    var matchPlayerCount by remember { mutableIntStateOf(4) }
    var p1Config by remember { mutableStateOf(Pair("Royal Emperor (You)", false)) }
    var p2Config by remember { mutableStateOf(Pair("Mega Bot", true)) }
    var p3Config by remember { mutableStateOf(Pair("Giga Bot", true)) }
    var p4Config by remember { mutableStateOf(Pair("Ultra Bot", true)) }

    // Dynamic color gradient following selected theme
    val windowGradient = Brush.verticalGradient(
        colors = listOf(theme.getBgGradientStartColor(), theme.getBgGradientEndColor())
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(windowGradient),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (players.isEmpty()) {
                // RENDER LOBBY DASHBOARD SCREEN
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        // Majestic game banner header
                        Text(
                            text = "ROYAL LUDO 👑",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = theme.getPrimaryTextColor(),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Gamer-grade local offline matches & premium themes",
                            fontSize = 14.sp,
                            color = theme.getSecondaryTextColor(),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    // RESTORABLE RESUME INTERACTIVE SLATE
                    if (savedGameEntity != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = theme.getPrimaryTextColor().copy(alpha = 0.12f)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(2.dp, theme.getPrimaryTextColor().copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Unfinished Match Found ⚔",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = theme.getPrimaryTextColor()
                                        )
                                        Text(
                                            text = "You have an active match saved on the board. Resume now!",
                                            fontSize = 12.sp,
                                            color = theme.getSecondaryTextColor()
                                        )
                                    }
                                    Button(
                                        onClick = { viewModel.resumeLastSavedGame() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                                        modifier = Modifier.testTag("resume_match_button")
                                    ) {
                                        Text(
                                            text = "Resume",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // PROFILE MANAGER HEADER CARD
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = theme.getCardBgColor()),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .background(theme.getPrimaryTextColor().copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = LUDO_AVATARS.getOrNull(lobbyAvatar)?.first ?: "👑",
                                                fontSize = 28.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = lobbyName.ifBlank { "Royal Emperor" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = theme.getPrimaryTextColor()
                                            )
                                            Text(
                                                text = "Honor Tier Slot 1 • Titleholder",
                                                fontSize = 11.sp,
                                                color = theme.getSecondaryTextColor()
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { showProfileEditor = !showProfileEditor },
                                        modifier = Modifier.testTag("edit_profile_button")
                                    ) {
                                        Icon(
                                            imageVector = if (showProfileEditor) Icons.Default.Close else Icons.Default.Edit,
                                            contentDescription = "Edit Profile",
                                            tint = theme.getPrimaryTextColor()
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = showProfileEditor) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "Customize Profile Settings",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = theme.getPrimaryTextColor()
                                        )

                                        OutlinedTextField(
                                            value = lobbyName,
                                            onValueChange = { lobbyName = it },
                                            label = { Text("Display Name") },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("profile_name_input"),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = theme.getPrimaryTextColor(),
                                                unfocusedTextColor = theme.getPrimaryTextColor()
                                            )
                                        )

                                        Text(
                                            text = "Select Avatar Royal Crest:",
                                            fontSize = 12.sp,
                                            color = theme.getSecondaryTextColor()
                                        )
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(LUDO_AVATARS.size) { avIdx ->
                                                val isSel = lobbyAvatar == avIdx
                                                Box(
                                                    modifier = Modifier
                                                        .size(46.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isSel) theme.getPrimaryTextColor() else theme
                                                                .getPrimaryTextColor()
                                                                .copy(alpha = 0.08f)
                                                        )
                                                        .clickable { lobbyAvatar = avIdx }
                                                        .border(
                                                            width = if (isSel) 2.dp else 0.dp,
                                                            color = Color(0xFFFFD700),
                                                            shape = CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(text = LUDO_AVATARS[avIdx].first, fontSize = 22.sp)
                                                }
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.saveUserProfile(lobbyName, lobbyAvatar)
                                                p1Config = Pair(lobbyName, false)
                                                showProfileEditor = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = theme.getPrimaryTextColor()),
                                            modifier = Modifier
                                                .align(Alignment.End)
                                                .testTag("save_profile_button")
                                        ) {
                                            Text(
                                                "Save Stats",
                                                color = theme.getBgGradientStartColor(),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // THEMATIC GRAPHIC DESIGN SLIDER
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = theme.getCardBgColor()),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Royal Board Skins 🎨",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = theme.getPrimaryTextColor(),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(BoardTheme.entries) { itemTheme ->
                                        val isSelected = theme == itemTheme
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) itemTheme.getBgGradientStartColor() else itemTheme.getCardBgColor()
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .width(135.dp)
                                                .border(
                                                    width = if (isSelected) 3.dp else 1.dp,
                                                    color = if (isSelected) Color(0xFFFFD700) else itemTheme
                                                        .getPrimaryTextColor()
                                                        .copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable { viewModel.updateTheme(itemTheme) }
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .padding(12.dp)
                                                    .fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = itemTheme.displayName,
                                                    color = if (isSelected) Color.White else itemTheme.getPrimaryTextColor(),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    textAlign = TextAlign.Center
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                // Mini Color Palettes
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(12.dp)
                                                            .clip(CircleShape)
                                                            .background(LudoColor.RED.getColor())
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .size(12.dp)
                                                            .clip(CircleShape)
                                                            .background(LudoColor.GREEN.getColor())
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .size(12.dp)
                                                            .clip(CircleShape)
                                                            .background(LudoColor.YELLOW.getColor())
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .size(12.dp)
                                                            .clip(CircleShape)
                                                            .background(LudoColor.BLUE.getColor())
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // LOCAL MULTIPLAYER MATCH SETTINGS
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = theme.getCardBgColor()),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Configure Competitors 🎮",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = theme.getPrimaryTextColor(),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    (2..4).forEach { count ->
                                        val isSel = matchPlayerCount == count
                                        Button(
                                            onClick = { matchPlayerCount = count },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSel) theme.getPrimaryTextColor() else theme.getPrimaryTextColor().copy(alpha = 0.1f)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "$count Players",
                                                color = if (isSel) theme.getBgGradientStartColor() else theme.getPrimaryTextColor(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Players Config slot list
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Player 1 (User ALWAYS human)
                                    PlayerSlotConfigRow("Slot I (Red)", p1Config, theme) {
                                        // P1 stays as human/User
                                    }

                                    // Player 2
                                    PlayerSlotConfigRow("Slot II (Green)", p2Config, theme) { isComp ->
                                        p2Config = Pair(if (isComp) "Classic Bot" else "Royal Guard", isComp)
                                    }

                                    // Player 3 (visible if capacity >= 3)
                                    if (matchPlayerCount >= 3) {
                                        PlayerSlotConfigRow("Slot III (Yellow)", p3Config, theme) { isComp ->
                                            p3Config = Pair(if (isComp) "Tactical Bot" else "Champion", isComp)
                                        }
                                    }

                                    // Player 4 (visible if capacity == 4)
                                    if (matchPlayerCount == 4) {
                                        PlayerSlotConfigRow("Slot IV (Blue)", p4Config, theme) { isComp ->
                                            p4Config = Pair(if (isComp) "Apex CPU" else "Elite Player", isComp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // LUDO HONORS STATISTICS STATS
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = theme.getCardBgColor()),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Honors Hall Statistics 🏆",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = theme.getPrimaryTextColor(),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                val totalMatches = profileEntity?.matchesPlayed ?: 0
                                val totalWins = profileEntity?.matchesWon ?: 0
                                val winRate = if (totalMatches > 0) (totalWins.toFloat() / totalMatches * 100).toInt() else 0

                                Row(
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    StatItem("Played", totalMatches.toString(), theme)
                                    StatItem("Crowns", totalWins.toString(), theme)
                                    StatItem("Win Rate", "$winRate%", theme)
                                }

                                if (history.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Past Battles Log:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = theme.getSecondaryTextColor(),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )

                                    history.take(3).forEach { hist ->
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${hist.dateString} • Won by ${hist.winnerName}",
                                                fontSize = 11.sp,
                                                color = theme.getPrimaryTextColor()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(android.graphics.Color.parseColor(hist.winnerColorHex)))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // MASSIVE PLAY LAUNCH ACTION BUTTON
                    item {
                        Button(
                            onClick = {
                                val launchConfigs = mutableListOf<Pair<String, Boolean>>()
                                launchConfigs.add(Pair(lobbyName.ifBlank { "Royal Emperor" }, false)) // User
                                launchConfigs.add(p2Config)
                                if (matchPlayerCount >= 3) launchConfigs.add(p3Config)
                                if (matchPlayerCount == 4) launchConfigs.add(p4Config)

                                viewModel.startNewGame(launchConfigs)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("start_game_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "START OFFLINE MATCH",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.Black
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }
            } else {
                // RENDER ACTIVE GAME SCREEN
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Match top bar header info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Exit Match button
                        Button(
                            onClick = { viewModel.exitGameAndClear() },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.getPrimaryTextColor().copy(alpha = 0.2f)),
                            modifier = Modifier.testTag("exit_match_button")
                        ) {
                            Text("< Exit Lobby", color = theme.getPrimaryTextColor(), fontWeight = FontWeight.Bold)
                        }

                        // Theme visual identifier badge
                        Text(
                            text = theme.displayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.getPrimaryTextColor(),
                            modifier = Modifier
                                .background(theme.getPrimaryTextColor().copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    // Players current progress indicators (Avatars with turn colors)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        players.forEachIndexed { num, pl ->
                            val isActivePlayer = activeIdx == num
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isActivePlayer) pl.color.getColor() else theme.getCardBgColor()
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        width = if (isActivePlayer) 3.dp else 0.dp,
                                        color = if (isActivePlayer) Color(0xFFFFD700) else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = pl.name.split(" ").firstOrNull() ?: pl.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isActivePlayer) Color.Black else theme.getPrimaryTextColor(),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = LUDO_AVATARS.getOrNull(pl.avatarId)?.first ?: "👑",
                                        fontSize = 16.sp
                                    )
                                    if (pl.isComputer) {
                                        Text(
                                            text = "BOT",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isActivePlayer) Color.Black.copy(alpha = 0.6f) else theme.getSecondaryTextColor()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Core Interactive Ludo Grid Canvas
                    LudoBoard(
                        tokens = tokens,
                        players = players,
                        activePlayerIdx = activeIdx,
                        movableTokens = viewModel.movableTokenIndices,
                        animatingToken = viewModel.animatingTokenIndex,
                        theme = theme,
                        onTokenClick = { tokenIndex -> viewModel.moveToken(tokenIndex) },
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth(0.95f)
                    )

                    // Bottom Panel containing Roller and Live Message Logs
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Current match player banner
                        val currentPl = viewModel.activePlayer()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(currentPl.color.getColor().copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(currentPl.color.getColor())
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Turn Master: ${currentPl.name}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.getPrimaryTextColor()
                            )
                        }

                        // DICE STATE CONTROLLER SLATE
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(theme.getCardBgColor(), RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val actionTip = when {
                                    isOver -> "Tournament is complete!"
                                    viewModel.isRollingAnim -> "Dice are tumbling..."
                                    turnState == TurnState.ROLLING -> {
                                        if (currentPl.isComputer) "Bot is strategizing roll..."
                                        else "TAP DICE BOX TO ROLL!"
                                    }
                                    turnState == TurnState.MOVING -> {
                                        if (currentPl.isComputer) "Bot is selecting best path..."
                                        else "Tap wiggling piece to move!"
                                    }
                                    else -> "Progressing matching loop"
                                }

                                Text(
                                    text = "Command:",
                                    fontSize = 11.sp,
                                    color = theme.getSecondaryTextColor(),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = actionTip,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = theme.getPrimaryTextColor()
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Interactive rollable dice cube design
                            val rollRotation by animateFloatAsState(
                                targetValue = if (viewModel.isRollingAnim) 360f else 0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(400, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "roller"
                            )

                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .rotate(if (viewModel.isRollingAnim) rollRotation else 0f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .border(4.dp, currentPl.color.getColor(), RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (!currentPl.isComputer && turnState == TurnState.ROLLING && !isOver) {
                                            viewModel.rollDice()
                                        }
                                    }
                                    .testTag("dice_roller_cube"),
                                contentAlignment = Alignment.Center
                            ) {
                                // Draw Dots corresponding to standard 1..6 configurations
                                DiceDotsGraphic(diceVal, currentPl.color.getColor())
                            }
                        }

                        // REAL-TIME MATCH MESSAGE LOG PANELS
                        Card(
                            colors = CardDefaults.cardColors(containerColor = theme.getCardBgColor().copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Battle Log Console:",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.getSecondaryTextColor()
                                )
                                AnimatedContent(
                                    targetState = logMessages.firstOrNull() ?: "",
                                    transitionSpec = {
                                        slideInVertically { height -> height } + fadeIn() togetherWith
                                                slideOutVertically { height -> -height } + fadeOut()
                                    },
                                    label = "console"
                                ) { text ->
                                    Text(
                                        text = text,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = theme.getPrimaryTextColor(),
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // OVERLAY VICTORY CROWNING MODAL DIALOG
            if (isOver) {
                AlertDialog(
                    onDismissRequest = { viewModel.exitGameAndClear() },
                    title = {
                        Text(
                            text = "VICTORY DECREED! 👑",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Ludo Crowns Awarded to:",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(winner?.color?.getColor() ?: Color.Gray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = LUDO_AVATARS.getOrNull(winner?.avatarId ?: 0)?.first ?: "👑",
                                    fontSize = 32.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = winner?.name ?: "Unknown Champion",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                color = winner?.color?.getColor() ?: Color.Gray
                            )
                            Text(
                                text = "All tokens completed their loops!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.exitGameAndClear() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                        ) {
                            Text("Confirm & Claim", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.testTag("victory_dialog")
                )
            }
        }
    }
}

// Draw detailed Dice roll pips
@Composable
fun DiceDotsGraphic(value: Int, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        when (value) {
            1 -> Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
            2 -> {
                Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxHeight()) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                    }
                }
            }
            3 -> {
                Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxHeight()) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color).align(Alignment.Start))
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color).align(Alignment.CenterHorizontally))
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color).align(Alignment.End))
                }
            }
            4 -> {
                Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxHeight()) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                    }
                }
            }
            5 -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color).align(Alignment.TopStart))
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color).align(Alignment.TopEnd))
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color).align(Alignment.Center))
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color).align(Alignment.BottomStart))
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color).align(Alignment.BottomEnd))
                }
            }
            6 -> {
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxHeight()) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color))
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color))
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color))
                    }
                    Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxHeight()) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color))
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color))
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color))
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerSlotConfigRow(
    slotLabel: String,
    config: Pair<String, Boolean>,
    theme: BoardTheme,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = slotLabel,
                fontSize = 11.sp,
                color = theme.getSecondaryTextColor(),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = config.first,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = theme.getPrimaryTextColor()
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = { onToggle(false) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!config.second) theme.getPrimaryTextColor() else theme.getPrimaryTextColor().copy(alpha = 0.1f)
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text(
                    "Local",
                    fontSize = 11.sp,
                    color = if (!config.second) theme.getBgGradientStartColor() else theme.getPrimaryTextColor(),
                    fontWeight = FontWeight.Bold
                )
            }
            Button(
                onClick = { onToggle(true) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (config.second) theme.getPrimaryTextColor() else theme.getPrimaryTextColor().copy(alpha = 0.1f)
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text(
                    "AI Bot",
                    fontSize = 11.sp,
                    color = if (config.second) theme.getBgGradientStartColor() else theme.getPrimaryTextColor(),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, theme: BoardTheme) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 12.dp)
    ) {
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = theme.getPrimaryTextColor())
        Text(text = label, fontSize = 11.sp, color = theme.getSecondaryTextColor())
    }
}
