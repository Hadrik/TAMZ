package com.example.tamz

import kotlin.math.max
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class LevelData {
    val name: String
    var moves by mutableStateOf(0)
    var onStateChanged: (() -> Unit)? = null

    val baseLevel: Array<TileType>
    var currentLevel by mutableStateOf<Array<TileType>>(emptyArray())
    val width: Int
    val height: Int

    constructor(
        level: Array<TileType>,
        width: Int,
        height: Int,
        name: String,
        initialMoves: Int = 0,
        initialLevel: Array<TileType>? = null
    ) {
        baseLevel = level.copyOf()
        currentLevel = initialLevel?.copyOf() ?: level.copyOf()
        this.width = width
        this.height = height
        this.name = name
        moves = initialMoves
    }

    fun at(x: Int, y: Int): TileType {
        return currentLevel[y * width + x]
    }

    fun move(dx: Int, dy: Int) {
        val playerIndex = max(
            currentLevel.indexOf(TileType.PLAYER),
            currentLevel.indexOf(TileType.PLAYER_ON_TARGET)
        )
        if (playerIndex == -1) return

        val playerX = playerIndex % width
        val playerY = playerIndex / width
        val targetX = playerX + dx
        val targetY = playerY + dy
        if (targetX !in 0 until width || targetY !in 0 until height) return

        val playerTarget = targetY * width + targetX
        val target = currentLevel[playerTarget]
        if (target == TileType.WALL) return

        val pushesBox = target == TileType.BOX || target == TileType.BOX_ON_TARGET
        if (pushesBox) {
            val boxX = targetX + dx
            val boxY = targetY + dy
            if (boxX !in 0 until width || boxY !in 0 until height) return

            val boxTarget = boxY * width + boxX
            if (currentLevel[boxTarget] != TileType.SPACE && currentLevel[boxTarget] != TileType.TARGET) return

            val movedLevel = currentLevel.copyOf()
            movedLevel[boxTarget] =
                if (baseLevel[boxTarget] == TileType.TARGET) TileType.BOX_ON_TARGET else TileType.BOX
            movedLevel[playerTarget] =
                if (baseLevel[playerTarget] == TileType.TARGET) TileType.PLAYER_ON_TARGET else TileType.PLAYER
            movedLevel[playerIndex] =
                if (baseLevel[playerIndex] == TileType.TARGET || baseLevel[playerIndex] == TileType.PLAYER_ON_TARGET)
                    TileType.TARGET else TileType.SPACE
            currentLevel = movedLevel
            moves++
            onStateChanged?.invoke()
            return
        }

        val movedLevel = currentLevel.copyOf()
        movedLevel[playerTarget] =
            if (baseLevel[playerTarget] == TileType.TARGET) TileType.PLAYER_ON_TARGET else TileType.PLAYER
        movedLevel[playerIndex] =
            if (baseLevel[playerIndex] == TileType.TARGET || baseLevel[playerIndex] == TileType.PLAYER_ON_TARGET)
                TileType.TARGET else TileType.SPACE
        currentLevel = movedLevel
        moves++
        onStateChanged?.invoke()
        return
    }

    fun isLevelWon(): Boolean {
        return currentLevel.none { it == TileType.BOX }
    }

    fun reset() {
        currentLevel = baseLevel.copyOf()
        moves = 0
        onStateChanged?.invoke()
    }
}