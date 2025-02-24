package org.game2048

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.unit.DpSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedVisibility
import org.game2048.engine.GameEngine
import org.game2048.engine.GameEngineImpl
import org.game2048.model.Board
import org.game2048.model.GameState
import org.game2048.model.Move

fun main() = application {
    val windowState = remember {
        WindowState(
            size = DpSize(550.dp, 700.dp)
        )
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "2048 Game",
        state = windowState,
        resizable = true,
        transparent = false
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .sizeIn(minWidth = 400.dp, minHeight = 500.dp)
        ) {
            Game()
        }
    }
}

@Composable
@Preview
fun Game() {
    var gameEngine by remember { mutableStateOf<GameEngine>(GameEngineImpl()) }
    var gameState by remember { mutableStateOf<GameState>(gameEngine.newGame()) }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFFAF8EF)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Game title and instructions
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "2048",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF776E65)
                    )
                    Text(
                        text = "Join the tiles, get to 2048!",
                        fontSize = 16.sp,
                        color = Color(0xFF776E65)
                    )
                    Text(
                        text = "Use arrow keys to move",
                        fontSize = 14.sp,
                        color = Color(0xFF776E65).copy(alpha = 0.7f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Score and status display
                    Column {
                        Text(
                            text = "SCORE",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF776E65)
                        )
                        Text(
                            text = "${gameState.board.score}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF776E65)
                        )
                        // Game status
                        val status = when (val state = gameState) {
                            is GameState.Won -> if (state.continueGame) {
                                StatusText("Keep going!", Color(0xFFF9BE02))
                            } else {
                                StatusText("You won!", Color(0xFFF9BE02))
                            }
                            is GameState.Lost -> StatusText("Game Over!", Color(0xFFE74C3C))
                            else -> null
                        }
                        status?.let { (text, color) ->
                            Text(
                                text = text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                    }

                    // Game controls
                    GameControls(
                        onNewGame = {
                            gameState = gameEngine.newGame()
                        },
                        onUndo = {
                            gameEngine.undo()?.let { gameState = it }
                        },
                        onContinue = {
                            gameState = gameEngine.continueGame()
                        },
                        gameState = gameState,
                        canUndo = gameState is GameState.Playing && (gameState as GameState.Playing).canUndo
                    )
                }

                // Game board
                GameBoard(
                    gameState = gameState,
                    onMove = { move ->
                        gameState = gameEngine.makeMove(move)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GameBoard(
    gameState: GameState,
    onMove: (Move) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var lastMove by remember { mutableStateOf<Move?>(null) }
    var showArrow by remember { mutableStateOf(false) }
    var lastKeyPressed by remember { mutableStateOf<String?>(null) }
    var showKeyOverlay by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    var isFocused by remember { mutableStateOf(false) }

    val keyOverlayAlpha by animateFloatAsState(
        targetValue = if (showKeyOverlay) 1f else 0f,
        animationSpec = tween(300),
        finishedListener = { if (!showKeyOverlay) lastKeyPressed = null }
    )

    val borderAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(
            durationMillis = 200,
            easing = LinearEasing
        )
    )

    val borderColor = Color(0xFF8F7A66).copy(alpha = borderAlpha)

    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .padding(8.dp)
            .aspectRatio(1f)
            .background(Color(0xFFBBADA0), RoundedCornerShape(6.dp))
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(4.dp)
            .focusRequester(focusRequester)
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    val move = when (event.key) {
                        Key.DirectionLeft, Key.A -> Move.LEFT
                        Key.DirectionRight, Key.D -> Move.RIGHT
                        Key.DirectionUp, Key.W -> Move.UP
                        Key.DirectionDown, Key.S -> Move.DOWN
                        else -> null
                    }

                    if (move != null) {
                        lastMove = move
                        showArrow = true
                        // Update last pressed key
                        lastKeyPressed = when (event.key) {
                            Key.DirectionLeft, Key.A -> "←"
                            Key.DirectionRight, Key.D -> "→"
                            Key.DirectionUp, Key.W -> "↑"
                            Key.DirectionDown, Key.S -> "↓"
                            else -> null
                        }
                        showKeyOverlay = true
                        scope.launch {
                            onMove(move)
                            kotlinx.coroutines.delay(200)
                            showArrow = false
                            showKeyOverlay = false
                        }
                        true
                    } else false
                } else false
            },
        contentAlignment = Alignment.Center
    ) {
        // Key overlay
        if (lastKeyPressed != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF776E65).copy(alpha = keyOverlayAlpha * 0.3f))
                        .border(
                            width = 2.dp,
                            color = Color(0xFF776E65).copy(alpha = keyOverlayAlpha * 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = lastKeyPressed!!,
                        fontSize = 40.sp,
                        color = Color.White.copy(alpha = keyOverlayAlpha),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val board = gameState.board
            for (row in 0 until board.size) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (col in 0 until board.size) {
                        GameTile(
                            value = board.cells[row][col],
                            row = row,
                            col = col,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameTile(
    value: Int,
    row: Int,
    col: Int,
    modifier: Modifier = Modifier
) {
    var isNew by remember { mutableStateOf(true) }
    var isMerged by remember { mutableStateOf(false) }
    var previousRow by remember(row) { mutableStateOf(row) }
    var previousCol by remember(col) { mutableStateOf(col) }
    val scope = rememberCoroutineScope()

    val tileSize = 96.dp // Tile size including padding
    // Animation configuration
    val moveAnimDuration = 45 // Base duration for movement animation (super quick)
    val newTileAnimDuration = 100 // Duration for new tile appearance
    val mergeAnimDuration = 60 // Duration for merge animation
    val density = LocalDensity.current
    val tileSizePx = with(density) { tileSize.toPx() }

    var targetOffsetX by remember { mutableStateOf(0f) }
    var targetOffsetY by remember { mutableStateOf(0f) }

    val offsetX by animateFloatAsState(
        targetValue = targetOffsetX,
        animationSpec = tween(
            durationMillis = moveAnimDuration,
            easing = LinearEasing // Pure linear for super quick movement
        )
    )

    val offsetY by animateFloatAsState(
        targetValue = targetOffsetY,
        animationSpec = tween(
            durationMillis = moveAnimDuration,
            easing = LinearEasing // Pure linear for super quick movement
        )
    )

    LaunchedEffect(row, col) {
        if (row != previousRow || col != previousCol) {
            // Calculate the offset based on position change in pixels
            targetOffsetX = (previousCol - col) * tileSizePx
            targetOffsetY = (previousRow - row) * tileSizePx

            // Trigger animation by resetting the offset
            kotlinx.coroutines.delay(4) // Minimal delay for smoother transition
            targetOffsetX = 0f
            targetOffsetY = 0f

            previousRow = row
            previousCol = col
        }
    }

    LaunchedEffect(value) {
        if (value > 0) {
            isNew = true
            isMerged = true
            scope.launch {
                kotlinx.coroutines.delay((moveAnimDuration + 5).toLong()) // Wait for move to complete
                isNew = false
                kotlinx.coroutines.delay(10) // Minimal pause before merge animation
                isMerged = false
            }
        }
    }

    val scale by animateFloatAsState(
        targetValue = when {
            isMerged -> 1.08f // Very subtle merge animation
            isNew && value > 0 -> 0.5f // Less dramatic appearance
            else -> 1f
        },
        animationSpec = tween(
            durationMillis = when {
                isNew -> moveAnimDuration + 20 // Slightly longer than move
                isMerged -> mergeAnimDuration
                else -> moveAnimDuration
            },
            easing = when {
                isNew -> FastOutSlowInEasing // Smoother appearance
                isMerged -> FastOutLinearInEasing // Quick merge
                else -> LinearEasing
            }
        )
    )

    val alpha by animateFloatAsState(
        targetValue = if (value > 0) 1f else 0f,
        animationSpec = tween(
            durationMillis = moveAnimDuration + 10,
            easing = FastOutLinearInEasing
        )
    )

    Box {
        Box(
            modifier = modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(4.dp))
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                    translationX = offsetX
                    translationY = offsetY
                }
                .background(getTileColor(value))
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            if (value > 0) {
                val fontSize = when {
                    value >= 1024 -> 20.sp
                    value >= 100 -> 24.sp
                    else -> 28.sp
                }
                Text(
                    text = value.toString(),
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = if (value > 4) Color.White else Color(0xFF776E65)
                )
            }
        }

        if (value == Board.WINNING_VALUE) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0x88F9BE02), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "2048!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun GameControls(
    onNewGame: () -> Unit,
    onUndo: () -> Unit,
    onContinue: () -> Unit,
    gameState: GameState,
    canUndo: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onNewGame,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF8F7A66),
                contentColor = Color.White
            )
        ) {
            Text(
                text = "New Game",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (gameState is GameState.Won && !gameState.continueGame) {
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF9BE02),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Continue",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Button(
            onClick = onUndo,
            enabled = canUndo,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF8F7A66),
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Undo",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MoveArrow(move: Move, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val arrowLength = size.minDimension * 0.4f
        val headLength = arrowLength * 0.3f
        val headAngle = 35f
        val strokeWidth = size.minDimension * 0.05f

        rotate(when(move) {
            Move.UP -> 270f
            Move.DOWN -> 90f
            Move.LEFT -> 180f
            Move.RIGHT -> 0f
        }) {
            // Arrow shaft
            drawLine(
                color = Color.White.copy(alpha = 0.6f),
                start = Offset(size.width * 0.3f, size.height * 0.5f),
                end = Offset(size.width * 0.7f, size.height * 0.5f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Arrow head
            drawLine(
                color = Color.White.copy(alpha = 0.6f),
                start = Offset(size.width * 0.7f, size.height * 0.5f),
                end = Offset(
                    size.width * 0.7f - headLength * kotlin.math.cos((180 - headAngle) * Math.PI / 180f).toFloat(),
                    size.height * 0.5f - headLength * kotlin.math.sin((180 - headAngle) * Math.PI / 180f).toFloat()
                ),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            drawLine(
                color = Color.White.copy(alpha = 0.6f),
                start = Offset(size.width * 0.7f, size.height * 0.5f),
                end = Offset(
                    size.width * 0.7f - headLength * kotlin.math.cos((180 + headAngle) * Math.PI / 180f).toFloat(),
                    size.height * 0.5f - headLength * kotlin.math.sin((180 + headAngle) * Math.PI / 180f).toFloat()
                ),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

private data class StatusText(val text: String, val color: Color)

private fun getTileColor(value: Int): Color = when (value) {
    0 -> Color(0xFFCDC1B4)
    2 -> Color(0xFFEEE4DA)
    4 -> Color(0xFFEDE0C8)
    8 -> Color(0xFFF2B179)
    16 -> Color(0xFFF59563)
    32 -> Color(0xFFF67C5F)
    64 -> Color(0xFFF65E3B)
    128 -> Color(0xFFEDCF72)
    256 -> Color(0xFFEDCC61)
    512 -> Color(0xFFEDC850)
    1024 -> Color(0xFFEDC53F)
    2048 -> Color(0xFFEDC22E)
    else -> Color(0xFF3C3A32)
}
