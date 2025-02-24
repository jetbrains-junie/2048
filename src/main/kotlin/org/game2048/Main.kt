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
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.game2048.engine.GameEngine
import org.game2048.engine.GameEngineImpl
import org.game2048.model.Board
import org.game2048.model.GameState
import org.game2048.model.Move

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "2048 Game",
        resizable = false,
        transparent = false
    ) {
        Game()
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

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    var isFocused by remember { mutableStateOf(false) }

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
            .width(400.dp)
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
                    when (event.key) {
                        Key.DirectionLeft, Key.A -> {
                            onMove(Move.LEFT)
                            true
                        }
                        Key.DirectionRight, Key.D -> {
                            onMove(Move.RIGHT)
                            true
                        }
                        Key.DirectionUp, Key.W -> {
                            onMove(Move.UP)
                            true
                        }
                        Key.DirectionDown, Key.S -> {
                            onMove(Move.DOWN)
                            true
                        }
                        else -> false
                    }
                } else false
            },
        contentAlignment = Alignment.Center
    ) {
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
    modifier: Modifier = Modifier
) {
    var isNew by remember { mutableStateOf(true) }
    var isMerged by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(value) {
        if (value > 0) {
            isNew = true
            isMerged = true
            scope.launch {
                kotlinx.coroutines.delay(50)
                isNew = false
                kotlinx.coroutines.delay(150)
                isMerged = false
            }
        }
    }

    val scale by animateFloatAsState(
        targetValue = when {
            isMerged -> 1.2f
            isNew && value > 0 -> 0.8f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val rotation by animateFloatAsState(
        targetValue = if (isMerged) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val alpha by animateFloatAsState(
        targetValue = if (value > 0) 1f else 0f,
        animationSpec = tween(
            durationMillis = 200,
            easing = LinearEasing
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
                    rotationZ = rotation
                    this.alpha = alpha
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
