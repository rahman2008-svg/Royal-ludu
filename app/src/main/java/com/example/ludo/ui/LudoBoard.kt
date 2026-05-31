package com.example.ludo.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.ludo.model.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LudoBoard(
    tokens: List<Token>,
    players: List<Player>,
    activePlayerIdx: Int,
    movableTokens: Set<Int>,
    animatingToken: Pair<LudoColor, Int>?,
    theme: BoardTheme,
    onTokenClick: (tokenIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Pulse animation for movable tokens
    val infiniteTransition = rememberInfiniteTransition(label = "halo")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val bounceAnim by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    val activePlayer = players.getOrNull(activePlayerIdx) ?: Player()

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .testTag("ludo_game_board")
    ) {
        val width = constraints.maxWidth.toFloat()
        val cellSize = width / 15f

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(tokens, players, activePlayer, movableTokens) {
                    detectTapGestures { offset ->
                        // Reverse engineer grid coords (col, row)
                        val col = (offset.x / cellSize).toInt().coerceIn(0, 14)
                        val row = (offset.y / cellSize).toInt().coerceIn(0, 14)

                        // If it is our turn and we have movable tokens
                        if (movableTokens.isNotEmpty() && !activePlayer.isComputer) {
                            // Find if any of the movable tokens is at (col, row) or in Yard slot
                            val matchIndex = movableTokens.find { tIdx ->
                                val token = tokens.firstOrNull { it.playerColor == activePlayer.color && it.tokenIndex == tIdx }
                                if (token != null) {
                                    val coords = BoardPath.getGridCoordinates(token.playerColor, token.tokenIndex, token.position)
                                    coords.x == col && coords.y == row
                                } else false
                            }

                            if (matchIndex != null) {
                                onTokenClick(matchIndex)
                            }
                        }
                    }
                }
        ) {
            // 1. Draw Thematic Board Background Canvas
            drawRect(color = theme.getBoardBgColor())

            // 2. Draw 4 Yard Quadrants (Bases)
            drawYardBase(LudoColor.RED, theme, cellSize)
            drawYardBase(LudoColor.GREEN, theme, cellSize)
            drawYardBase(LudoColor.YELLOW, theme, cellSize)
            drawYardBase(LudoColor.BLUE, theme, cellSize)

            // 3. Draw All 52 Loop Track Squares
            for (idx in BoardPath.outerTrack.indices) {
                val pt = BoardPath.outerTrack[idx]
                val rectOffset = Offset(pt.x * cellSize, pt.y * cellSize)

                // Grid background & borders
                drawRect(
                    color = theme.getCardBgColor(),
                    topLeft = rectOffset,
                    size = Size(cellSize, cellSize)
                )
                drawRect(
                    color = theme.getPrimaryTextColor().copy(alpha = 0.15f),
                    topLeft = rectOffset,
                    size = Size(cellSize, cellSize),
                    style = Stroke(width = 1f)
                )

                // Coloring Start cells
                when (pt) {
                    SimplePoint(1, 6) -> drawCellHighlightAndArrow(LudoColor.RED, rectOffset, cellSize, "right")
                    SimplePoint(8, 1) -> drawCellHighlightAndArrow(LudoColor.GREEN, rectOffset, cellSize, "down")
                    SimplePoint(13, 8) -> drawCellHighlightAndArrow(LudoColor.YELLOW, rectOffset, cellSize, "left")
                    SimplePoint(6, 13) -> drawCellHighlightAndArrow(LudoColor.BLUE, rectOffset, cellSize, "up")
                }

                // Draw Star / Safe marks on standard secure index hubs
                if (BoardPath.isSafeLoopIndex(idx)) {
                    val starCenter = Offset(rectOffset.x + cellSize / 2f, rectOffset.y + cellSize / 2f)
                    drawLudoStar(starCenter, cellSize * 0.35f, theme.getPrimaryTextColor().copy(alpha = 0.45f))
                    // Draw mini start tags or star marks
                    if (idx == 8) drawLudoStar(starCenter, cellSize * 0.35f, LudoColor.GREEN.getColor())
                    if (idx == 21) drawLudoStar(starCenter, cellSize * 0.35f, LudoColor.YELLOW.getColor())
                    if (idx == 34) drawLudoStar(starCenter, cellSize * 0.35f, LudoColor.BLUE.getColor())
                    if (idx == 47) drawLudoStar(starCenter, cellSize * 0.35f, LudoColor.RED.getColor())
                }
            }

            // 4. Draw Home Column Tracks
            drawHomeColumnPath(LudoColor.RED, cellSize)
            drawHomeColumnPath(LudoColor.GREEN, cellSize)
            drawHomeColumnPath(LudoColor.YELLOW, cellSize)
            drawHomeColumnPath(LudoColor.BLUE, cellSize)

            // 5. Draw Home Triangle Center Hub
            drawHomeCenterTriangles(cellSize)

            // 6. Draw Tokens & Handle overlapping clustering
            // Group active tokens by their computed coordinates to detect collisions and draw beautifully grouped clusters
            val tokenPositionsMap = mutableMapOf<SimplePoint, MutableList<Token>>()
            for (tk in tokens) {
                val pt = BoardPath.getGridCoordinates(tk.playerColor, tk.tokenIndex, tk.position)
                if (tokenPositionsMap.containsKey(pt)) {
                    tokenPositionsMap[pt]?.add(tk)
                } else {
                    tokenPositionsMap[pt] = mutableListOf(tk)
                }
            }

            // Iterate over each cell and draw its tokens with clever cluster spacing
            for ((gridPt, cellTokens) in tokenPositionsMap) {
                val count = cellTokens.size
                val baseCenterX = gridPt.x * cellSize + cellSize / 2f
                val baseCenterY = gridPt.y * cellSize + cellSize / 2f

                cellTokens.forEachIndexed { sliceIndex, tk ->
                    // Calculate token specific offset if multiple pieces share the same house
                    // Clustering algorithm spreads tokens in circular form inside cell boundaries
                    val offsetDistance = if (count > 1) cellSize * 0.22f else 0f
                    val angle = if (count > 1) (sliceIndex * (360f / count) * Math.PI / 180f) else 0.0

                    val finalCenterX = baseCenterX + (offsetDistance * cos(angle)).toFloat()
                    var finalCenterY = baseCenterY + (offsetDistance * sin(angle)).toFloat()

                    // Apply wiggle bounce animation to current active player's movable selection
                    val isMovable = movableTokens.contains(tk.tokenIndex) && tk.playerColor == activePlayer.color
                    if (isMovable) {
                        finalCenterY += bounceAnim // Bouncers feel satisfying to tap!
                    }

                    // Apply smooth visual zoom on active step-by-step moving tokens
                    val isAnimating = animatingToken?.first == tk.playerColor && animatingToken.second == tk.tokenIndex
                    val tokenRadius = if (isAnimating) cellSize * 0.42f else cellSize * 0.33f

                    // Render Token Graphics
                    drawTokenGraphics(
                        center = Offset(finalCenterX, finalCenterY),
                        radius = tokenRadius,
                        playerColor = tk.playerColor,
                        isMovable = isMovable,
                        pulseScale = pulseScale,
                        theme = theme,
                        tokenIndex = tk.tokenIndex,
                        cellSize = cellSize
                    )
                }
            }
        }
    }
}

// Draw specialized arrow guides for player starting points
private fun DrawScope.drawCellHighlightAndArrow(
    color: LudoColor,
    topLeft: Offset,
    cellSize: Float,
    direction: String
) {
    // Fill background cells with color tint
    drawRect(
        color = color.getColor().copy(alpha = 0.25f),
        topLeft = topLeft,
        size = Size(cellSize, cellSize)
    )

    // Draw stylized arrows
    val arrowPath = Path()
    val padding = cellSize * 0.2f
    when (direction) {
        "right" -> {
            arrowPath.moveTo(topLeft.x + padding, topLeft.y + cellSize / 2f)
            arrowPath.lineTo(topLeft.x + cellSize - padding, topLeft.y + cellSize / 2f)
            arrowPath.lineTo(topLeft.x + cellSize / 2f, topLeft.y + padding)
            arrowPath.moveTo(topLeft.x + cellSize - padding, topLeft.y + cellSize / 2f)
            arrowPath.lineTo(topLeft.x + cellSize / 2f, topLeft.y + cellSize - padding)
        }
        "down" -> {
            arrowPath.moveTo(topLeft.x + cellSize / 2f, topLeft.y + padding)
            arrowPath.lineTo(topLeft.x + cellSize / 2f, topLeft.y + cellSize - padding)
            arrowPath.lineTo(topLeft.x + padding, topLeft.y + cellSize / 2f)
            arrowPath.moveTo(topLeft.x + cellSize / 2f, topLeft.y + cellSize - padding)
            arrowPath.lineTo(topLeft.x + cellSize - padding, topLeft.y + cellSize / 2f)
        }
        "left" -> {
            arrowPath.moveTo(topLeft.x + cellSize - padding, topLeft.y + cellSize / 2f)
            arrowPath.lineTo(topLeft.x + padding, topLeft.y + cellSize / 2f)
            arrowPath.lineTo(topLeft.x + cellSize / 2f, topLeft.y + padding)
            arrowPath.moveTo(topLeft.x + padding, topLeft.y + cellSize / 2f)
            arrowPath.lineTo(topLeft.x + cellSize / 2f, topLeft.y + cellSize - padding)
        }
        "up" -> {
            arrowPath.moveTo(topLeft.x + cellSize / 2f, topLeft.y + cellSize - padding)
            arrowPath.lineTo(topLeft.x + cellSize / 2f, topLeft.y + padding)
            arrowPath.lineTo(topLeft.x + padding, topLeft.y + cellSize / 2f)
            arrowPath.moveTo(topLeft.x + cellSize / 2f, topLeft.y + padding)
            arrowPath.lineTo(topLeft.x + cellSize - padding, topLeft.y + cellSize / 2f)
        }
    }

    drawPath(
        path = arrowPath,
        color = color.getColor(),
        style = Stroke(width = cellSize * 0.12f)
    )
}

// Core drawing helper for standard yard boxes representing houses
private fun DrawScope.drawYardBase(color: LudoColor, theme: BoardTheme, cellSize: Float) {
    val left = if (color == LudoColor.RED || color == LudoColor.BLUE) 0f else 9 * cellSize
    val top = if (color == LudoColor.RED || color == LudoColor.GREEN) 0f else 9 * cellSize
    val yardSize = 6 * cellSize

    // Base color gradient fill
    val baseBrush = Brush.linearGradient(
        colors = listOf(color.getColor(), color.getColor().copy(alpha = 0.75f)),
        start = Offset(left, top),
        end = Offset(left + yardSize, top + yardSize)
    )

    drawRect(
        brush = baseBrush,
        topLeft = Offset(left, top),
        size = Size(yardSize, yardSize)
    )

    // Inner White Square Box
    val whitePadding = cellSize * 0.85f
    val innerLeft = left + whitePadding
    val innerTop = top + whitePadding
    val innerSize = yardSize - (whitePadding * 2)

    drawRect(
        color = Color.White,
        topLeft = Offset(innerLeft, innerTop),
        size = Size(innerSize, innerSize)
    )

    // Gold/Textured Border for luxury theme
    if (theme == BoardTheme.ROYAL_GOLD) {
        drawRect(
            color = Color(0xFFFFD700),
            topLeft = Offset(left, top),
            size = Size(yardSize, yardSize),
            style = Stroke(width = 6f)
        )
    }

    // Yard circular token parking slots (4 slots)
    val slots = BoardPath.getYardSlots(color)
    slots.forEach { pt ->
        val centerX = pt.x * cellSize + cellSize / 2f
        val centerY = pt.y * cellSize + cellSize / 2f

        // Parking slot ring style
        drawCircle(
            color = color.getColor().copy(alpha = 0.10f),
            radius = cellSize * 0.5f,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = color.getColor(),
            radius = cellSize * 0.38f,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = Color.White,
            radius = cellSize * 0.28f,
            center = Offset(centerX, centerY)
        )
    }
}

// Draw the colored home paths
private fun DrawScope.drawHomeColumnPath(color: LudoColor, cellSize: Float) {
    val pathPoints = BoardPath.getHomeColumn(color)
    pathPoints.forEach { pt ->
        val topLeft = Offset(pt.x * cellSize, pt.y * cellSize)
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(color.getColor(), color.getColor().copy(alpha = 0.8f)),
                start = topLeft,
                end = Offset(topLeft.x + cellSize, topLeft.y + cellSize)
            ),
            topLeft = topLeft,
            size = Size(cellSize, cellSize)
        )
        // Accent border
        drawRect(
            color = Color.White.copy(alpha = 0.3f),
            topLeft = topLeft,
            size = Size(cellSize, cellSize),
            style = Stroke(width = 1f)
        )
    }
}

// Render diagonal triangular wedges meeting in home center
private fun DrawScope.drawHomeCenterTriangles(cellSize: Float) {
    val centerLeft = 6 * cellSize
    val centerTop = 6 * cellSize
    val hubSize = 3 * cellSize

    val exactCenter = Offset(centerLeft + hubSize / 2f, centerTop + hubSize / 2f)

    // 1. Red (Left triangle)
    val redPath = Path().apply {
        moveTo(centerLeft, centerTop)
        lineTo(exactCenter.x, exactCenter.y)
        lineTo(centerLeft, centerTop + hubSize)
        close()
    }
    drawPath(path = redPath, color = LudoColor.RED.getColor())

    // 2. Green (Top triangle)
    val greenPath = Path().apply {
        moveTo(centerLeft, centerTop)
        lineTo(exactCenter.x, exactCenter.y)
        lineTo(centerLeft + hubSize, centerTop)
        close()
    }
    drawPath(path = greenPath, color = LudoColor.GREEN.getColor())

    // 3. Yellow (Right triangle)
    val yellowPath = Path().apply {
        moveTo(centerLeft + hubSize, centerTop)
        lineTo(exactCenter.x, exactCenter.y)
        lineTo(centerLeft + hubSize, centerTop + hubSize)
        close()
    }
    drawPath(path = yellowPath, color = LudoColor.YELLOW.getColor())

    // 4. Blue (Bottom triangle)
    val bluePath = Path().apply {
        moveTo(centerLeft, centerTop + hubSize)
        lineTo(exactCenter.x, exactCenter.y)
        lineTo(centerLeft + hubSize, centerTop + hubSize)
        close()
    }
    drawPath(path = bluePath, color = LudoColor.BLUE.getColor())

    // Thick center accent ring separating zones
    drawCircle(
        color = Color.White,
        radius = cellSize * 0.45f,
        center = exactCenter,
        style = Stroke(width = 3f)
    )
}

// Advanced 10-point mathematical coordinates vector star drawer
private fun DrawScope.drawLudoStar(center: Offset, radius: Float, color: Color) {
    val path = Path()
    val points = 5
    var angle = -Math.PI / 2.0
    val dAngle = Math.PI / points

    for (i in 0 until (points * 2)) {
        val r = if (i % 2 == 0) radius else radius * 0.4f
        val x = (center.x + r * cos(angle)).toFloat()
        val y = (center.y + r * sin(angle)).toFloat()

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
        angle += dAngle
    }
    path.close()
    drawPath(path = path, color = color)
}

// Gorgeous detailed Token Graphic drawing blocks
private fun DrawScope.drawTokenGraphics(
    center: Offset,
    radius: Float,
    playerColor: LudoColor,
    isMovable: Boolean,
    pulseScale: Float,
    theme: BoardTheme,
    tokenIndex: Int,
    cellSize: Float
) {
    val color = playerColor.getColor()
    val secondary = playerColor.getSecondaryColor()

    // 1. Draw glowing golden halo or orbit ring if piece is movable
    if (isMovable) {
        val haloRadius = radius * pulseScale * 1.35f
        drawCircle(
            color = Color(0xFFFFD700).copy(alpha = 0.35f),
            radius = haloRadius,
            center = center,
            style = Stroke(width = cellSize * 0.12f)
        )
    }

    // 2. Clear token shadow below
    drawCircle(
        color = Color.Black.copy(alpha = 0.3f),
        radius = radius,
        center = center + Offset(3f, 5f)
    )

    // 3. Main token gradient fill
    val tokenBrush = Brush.radialGradient(
        colors = listOf(secondary, color, color.copy(alpha = 0.9f)),
        center = center - Offset(radius * 0.25f, radius * 0.25f),
        radius = radius
    )
    drawCircle(
        brush = tokenBrush,
        radius = radius,
        center = center
    )

    // 4. Draw modern white container ring
    drawCircle(
        color = Color.White,
        radius = radius * 0.85f,
        center = center,
        style = Stroke(width = cellSize * 0.08f)
    )

    // 5. Draw matching inner core slot
    val fillResizer = when (tokenIndex) {
        0 -> 0.45f
        1 -> 0.50f
        2 -> 0.55f
        else -> 0.40f
    }
    drawCircle(
        color = color,
        radius = radius * fillResizer,
        center = center
    )

    // 6. Theme specific overlay ornaments (e.g., Gold crown star highlight or cyber neon cores)
    if (theme == BoardTheme.ROYAL_GOLD) {
        drawCircle(
            color = Color(0xFFFFD700),
            radius = radius * 0.2f,
            center = center
        )
    } else {
        drawCircle(
            color = Color.White,
            radius = radius * 0.22f,
            center = center - Offset(radius * 0.22f, radius * 0.22f) // Gloss flare reflecting
        )
    }
}
