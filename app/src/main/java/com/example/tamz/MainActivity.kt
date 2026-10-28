package com.example.tamz

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.tamz.ui.theme.TAMZTheme
import kotlin.math.abs
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TAMZTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                ) { innerPadding ->
                    GameScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

private const val LEVEL_WIDTH = 10
private const val LEVEL_HEIGHT = 10

private val levelData = intArrayOf(
    1, 1, 1, 1, 1, 1, 1, 1, 1, 0,
    1, 0, 0, 0, 0, 0, 0, 0, 1, 0,
    1, 0, 2, 3, 3, 2, 1, 0, 1, 0,
    1, 0, 1, 3, 2, 3, 2, 0, 1, 0,
    1, 0, 2, 3, 3, 2, 4, 0, 1, 0,
    1, 0, 1, 3, 2, 3, 2, 0, 1, 0,
    1, 0, 2, 3, 3, 2, 1, 0, 1, 0,
    1, 0, 0, 0, 0, 0, 0, 0, 1, 0,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0
)

private fun isLevelWon(level: IntArray): Boolean {
    return level.none { it == 2 }
}

private fun movePlayer(level: IntArray, baseLevel: IntArray, dx: Int, dy: Int): IntArray? {
    val playerIndex = level.indexOf(4)
    if (playerIndex == -1) return null

    val playerX = playerIndex % LEVEL_WIDTH
    val playerY = playerIndex / LEVEL_WIDTH
    val targetX = playerX + dx
    val targetY = playerY + dy
    if (targetX !in 0 until LEVEL_WIDTH || targetY !in 0 until LEVEL_HEIGHT) return null

    val targetIndex = targetY * LEVEL_WIDTH + targetX
    val target = level[targetIndex]
    if (target == 1) return null

    val pushesBox = target == 2 || target == 5
    if (pushesBox) {
        val boxX = targetX + dx
        val boxY = targetY + dy
        if (boxX !in 0 until LEVEL_WIDTH || boxY !in 0 until LEVEL_HEIGHT) return null

        val boxIndex = boxY * LEVEL_WIDTH + boxX
        if (level[boxIndex] != 0 && level[boxIndex] != 3) return null

        val movedLevel = level.copyOf()
        movedLevel[boxIndex] = if (baseLevel[boxIndex] == 3) 5 else 2
        movedLevel[targetIndex] = 4
        movedLevel[playerIndex] = if (baseLevel[playerIndex] == 3) 3 else 0
        return movedLevel
    }

    val movedLevel = level.copyOf()
    movedLevel[targetIndex] = 4
    movedLevel[playerIndex] = if (baseLevel[playerIndex] == 3) 3 else 0
    return movedLevel
}

@Composable
fun GameScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val baseLevel = remember { levelData.copyOf() }
    var currentLevel by remember { mutableStateOf(baseLevel.copyOf()) }
    var showWinDialog by remember { mutableStateOf(false) }
    val tiles = remember {
        arrayOf(
            BitmapFactory.decodeResource(context.resources, R.drawable.empty).asImageBitmap(), // 0
            BitmapFactory.decodeResource(context.resources, R.drawable.wall).asImageBitmap(),  // 1
            BitmapFactory.decodeResource(context.resources, R.drawable.box).asImageBitmap(),   // 2
            BitmapFactory.decodeResource(context.resources, R.drawable.goal).asImageBitmap(),  // 3
            BitmapFactory.decodeResource(context.resources, R.drawable.hero).asImageBitmap(),  // 4
            BitmapFactory.decodeResource(context.resources, R.drawable.boxok).asImageBitmap()  // 5
        )
    }

    Column(modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(currentLevel) {
                    var totalDragX = 0f
                    var totalDragY = 0f

                    detectDragGestures(
                        onDragCancel = {
                            totalDragX = 0f
                            totalDragY = 0f
                        },
                        onDragEnd = {
                            var dx = 0
                            var dy = 0

                            if (abs(totalDragX) > abs(totalDragY)){
                                dx = if (totalDragX > 0) 1 else -1
                            } else {
                                dy = if (totalDragY > 0) 1 else -1
                            }

                            movePlayer(currentLevel, baseLevel, dx, dy)?.let {
                                currentLevel = it
                                showWinDialog = isLevelWon(it)
                            }
                            totalDragX = 0f
                            totalDragY = 0f
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y
                    }
                }
        ) {
            val tileWidthPx = size.width / LEVEL_WIDTH
            val tileHeightPx = size.height / LEVEL_HEIGHT
            val tileSize = minOf(tileWidthPx, tileHeightPx)

            for (y in 0 until LEVEL_HEIGHT) {
                for (x in 0 until LEVEL_WIDTH) {
                    val tileIndex = currentLevel[y * LEVEL_WIDTH + x]
                    val img: ImageBitmap = tiles[tileIndex.coerceIn(0, tiles.lastIndex)]

                    drawImage(
                        image = img,
                        dstOffset = IntOffset(
                            x = (x * tileSize).roundToInt(),
                            y = (y * tileSize).roundToInt()
                        ),
                        dstSize = IntSize(
                            width = tileSize.roundToInt(),
                            height = tileSize.roundToInt()
                        )
                    )
                }
            }
        }

        Button(
            onClick = {
                currentLevel = baseLevel.copyOf()
                showWinDialog = false
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Reset")
        }
    }

    if (showWinDialog) {
        AlertDialog(
            onDismissRequest = { showWinDialog = false },
            title = { Text("Výhra!") },
            text = { Text("Dobrá práce chlapáku. Vyhrál jsi!") },
            confirmButton = {
                Button(
                    onClick = {
                        currentLevel = baseLevel.copyOf()
                        showWinDialog = false
                    }
                ) {
                    Text("Reset")
                }
            }
        )
    }
}