package com.example.tamz

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun GameScreen(
    levelData: LevelData,
    tileBitmap: TileBitmap,
    preview: Boolean,
    onGameFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val level = remember { levelData }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .then(if (!preview) Modifier.pointerInput(level.currentLevel) {
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

                        if (abs(totalDragX) > abs(totalDragY)) {
                            dx = if (totalDragX > 0) 1 else -1
                        } else {
                            dy = if (totalDragY > 0) 1 else -1
                        }

                        level.move(dx, dy).let {
                            level.currentLevel
                            if (level.isLevelWon()) onGameFinished()
                        }
                        totalDragX = 0f
                        totalDragY = 0f
                    }
                ) { change, dragAmount ->
                    change.consume()
                    totalDragX += dragAmount.x
                    totalDragY += dragAmount.y
                }
        } else Modifier)
    ) {
        val tileWidthPx = size.width / level.width
        val tileHeightPx = size.height / level.height
        val tileSize = minOf(tileWidthPx, tileHeightPx)

        for (y in 0 until level.height) {
            for (x in 0 until level.width) {
                val img: ImageBitmap = tileBitmap.get(level.at(x, y))

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
}