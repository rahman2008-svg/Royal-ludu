package com.example.ludo.model

import androidx.compose.ui.graphics.Color

enum class LudoColor(val displayName: String, val hex: String, val secondaryHex: String) {
    RED("Red", "#E53935", "#FF8A80"),
    GREEN("Green", "#43A047", "#B9F6CA"),
    YELLOW("Yellow", "#FDD835", "#FFFF8D"),
    BLUE("Blue", "#1E88E5", "#82B1FF");

    fun getColor(): Color = Color(android.graphics.Color.parseColor(hex))
    fun getSecondaryColor(): Color = Color(android.graphics.Color.parseColor(secondaryHex))
}

enum class BoardTheme(
    val id: String,
    val displayName: String,
    val bgGradientStart: String,
    val bgGradientEnd: String,
    val boardBg: String,
    val cardBg: String,
    val primaryText: String,
    val secondaryText: String
) {
    CLASSIC(
        "classic", "Classic Board",
        "#ECEFF1", "#CFD8DC", "#FFFFFF", "#F8F9FA", "#212121", "#757575"
    ),
    ROYAL_GOLD(
        "royal_gold", "Royal Gold",
        "#1A1A1A", "#332200", "#FFF8E1", "#2A1F0D", "#FFD700", "#B8860B"
    ),
    NEON_CYBER(
        "neon_cyber", "Cyber Neon",
        "#0D0E15", "#1B0B2E", "#120B20", "#1A102E", "#00FFFF", "#FF00FF"
    ),
    COSMIC_DARK(
        "cosmic_dark", "Cosmic Slate",
        "#0B132B", "#1C2541", "#1C2541", "#3A506B", "#6FFFE9", "#5BC0BE"
    ),
    PLAYFUL_PASTEL(
        "playful", "Sweet Pastel",
        "#FDF6E2", "#F0DFDD", "#FFFFFF", "#FFF3E0", "#5D4037", "#8D6E63"
    );

    fun getBgGradientStartColor() = Color(android.graphics.Color.parseColor(bgGradientStart))
    fun getBgGradientEndColor() = Color(android.graphics.Color.parseColor(bgGradientEnd))
    fun getBoardBgColor() = Color(android.graphics.Color.parseColor(boardBg))
    fun getCardBgColor() = Color(android.graphics.Color.parseColor(cardBg))
    fun getPrimaryTextColor() = Color(android.graphics.Color.parseColor(primaryText))
    fun getSecondaryTextColor() = Color(android.graphics.Color.parseColor(secondaryText))
}

data class Player(
    val index: Int = 0,
    val name: String = "",
    val color: LudoColor = LudoColor.RED,
    val isComputer: Boolean = false,
    val avatarId: Int = 0, // Reference to pre-defined avatars 0..7
    val isActive: Boolean = true
)

data class Token(
    val playerColor: LudoColor,
    val tokenIndex: Int, // 0..3
    val position: Int = 0 // 0 = In Base, 1..51 = Outer Path, 52..56 = Home Column, 57 = Home Triangle
)

enum class TurnState {
    ROLLING,
    MOVING,
    EXTRA_ROLL_EARNED,
    TURNS_PASSED
}

data class SimplePoint(val x: Int, val y: Int)

object BoardPath {
    // Standard 52 outer cells track loop
    val outerTrack = listOf(
        SimplePoint(0, 6), SimplePoint(1, 6), SimplePoint(2, 6), SimplePoint(3, 6), SimplePoint(4, 6), SimplePoint(5, 6),
        SimplePoint(6, 5), SimplePoint(6, 4), SimplePoint(6, 3), SimplePoint(6, 2), SimplePoint(6, 1), SimplePoint(6, 0),
        SimplePoint(7, 0),
        SimplePoint(8, 0), SimplePoint(8, 1), SimplePoint(8, 2), SimplePoint(8, 3), SimplePoint(8, 4), SimplePoint(8, 5),
        SimplePoint(9, 6), SimplePoint(10, 6), SimplePoint(11, 6), SimplePoint(12, 6), SimplePoint(13, 6), SimplePoint(14, 6),
        SimplePoint(14, 7),
        SimplePoint(14, 8), SimplePoint(13, 8), SimplePoint(12, 8), SimplePoint(11, 8), SimplePoint(10, 8), SimplePoint(9, 8),
        SimplePoint(8, 9), SimplePoint(8, 10), SimplePoint(8, 11), SimplePoint(8, 12), SimplePoint(8, 13), SimplePoint(8, 14),
        SimplePoint(7, 14),
        SimplePoint(6, 14), SimplePoint(6, 13), SimplePoint(6, 12), SimplePoint(6, 11), SimplePoint(6, 10), SimplePoint(6, 9),
        SimplePoint(5, 8), SimplePoint(4, 8), SimplePoint(3, 8), SimplePoint(2, 8), SimplePoint(1, 8), SimplePoint(0, 8),
        SimplePoint(0, 7)
    )

    // Base start indexes on outer loop
    fun getStartLoopIndex(color: LudoColor): Int = when (color) {
        LudoColor.RED -> 1      // (1,6)
        LudoColor.GREEN -> 14   // (8,1)
        LudoColor.YELLOW -> 27  // (13,8)
        LudoColor.BLUE -> 40    // (6,13)
    }

    // Base end indexes before entering Home path
    fun getEndLoopIndex(color: LudoColor): Int = when (color) {
        LudoColor.RED -> 0      // (0,6)
        LudoColor.GREEN -> 13   // (8,0)
        LudoColor.YELLOW -> 26  // (14,8)
        LudoColor.BLUE -> 39    // (6,14)
    }

    // Stars/Safe points
    val safeStarIndices = setOf(8, 21, 34, 47)

    fun isSafeLoopIndex(index: Int): Boolean {
        // Safe loops: starting cells + star cells
        val startCells = setOf(1, 14, 27, 40)
        return index in startCells || index in safeStarIndices
    }

    // Home Column Tracks (5 cells each)
    val redHomeColumn = listOf(
        SimplePoint(1, 7), SimplePoint(2, 7), SimplePoint(3, 7), SimplePoint(4, 7), SimplePoint(5, 7)
    )
    val greenHomeColumn = listOf(
        SimplePoint(7, 1), SimplePoint(7, 2), SimplePoint(7, 3), SimplePoint(7, 4), SimplePoint(7, 5)
    )
    val yellowHomeColumn = listOf(
        SimplePoint(13, 7), SimplePoint(12, 7), SimplePoint(11, 7), SimplePoint(10, 7), SimplePoint(9, 7)
    )
    val blueHomeColumn = listOf(
        SimplePoint(7, 13), SimplePoint(7, 12), SimplePoint(7, 11), SimplePoint(7, 10), SimplePoint(7, 9)
    )

    fun getHomeColumn(color: LudoColor): List<SimplePoint> = when (color) {
        LudoColor.RED -> redHomeColumn
        LudoColor.GREEN -> greenHomeColumn
        LudoColor.YELLOW -> yellowHomeColumn
        LudoColor.BLUE -> blueHomeColumn
    }

    // Preassigned base coordinates for Yard slots (4 slots for 4 tokens)
    val redYardSlots = listOf(
        SimplePoint(1, 1), SimplePoint(3, 1),
        SimplePoint(1, 3), SimplePoint(3, 3)
    )
    val greenYardSlots = listOf(
        SimplePoint(10, 1), SimplePoint(12, 1),
        SimplePoint(10, 3), SimplePoint(12, 3)
    )
    val yellowYardSlots = listOf(
        SimplePoint(10, 10), SimplePoint(12, 10),
        SimplePoint(10, 12), SimplePoint(12, 12)
    )
    val blueYardSlots = listOf(
        SimplePoint(1, 10), SimplePoint(3, 10),
        SimplePoint(1, 12), SimplePoint(3, 12)
    )

    fun getYardSlots(color: LudoColor): List<SimplePoint> = when (color) {
        LudoColor.RED -> redYardSlots
        LudoColor.GREEN -> greenYardSlots
        LudoColor.YELLOW -> yellowYardSlots
        LudoColor.BLUE -> blueYardSlots
    }

    // Home center positions
    fun getHomeCenter(color: LudoColor): SimplePoint = when (color) {
        LudoColor.RED -> SimplePoint(6, 7)
        LudoColor.GREEN -> SimplePoint(7, 6)
        LudoColor.YELLOW -> SimplePoint(8, 7)
        LudoColor.BLUE -> SimplePoint(7, 8)
    }

    // Dynamic grid mapping for any token's path position (0..57)
    fun getGridCoordinates(color: LudoColor, tokenIndex: Int, pos: Int): SimplePoint {
        return when (pos) {
            0 -> getYardSlots(color)[tokenIndex] // Base Yard slot
            in 1..51 -> {
                val startIdx = getStartLoopIndex(color)
                val trackIdx = (startIdx + (pos - 1)) % 52
                outerTrack[trackIdx]
            }
            in 52..56 -> {
                val colIdx = pos - 52
                getHomeColumn(color)[colIdx]
            }
            else -> {
                // Completed! Move to designated central home quadrant triangle
                getHomeCenter(color)
            }
        }
    }
}
