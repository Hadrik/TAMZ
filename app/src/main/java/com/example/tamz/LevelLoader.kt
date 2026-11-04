package com.example.tamz

import android.content.Context
import kotlin.collections.map

data class SavedLevelState(
    val moves: Int,
    val level: Array<TileType>?
)

fun loadLevels(
    path: String,
    context: Context,
    savedState: (String, Array<TileType>) -> SavedLevelState = { _, _ ->
        SavedLevelState(0, null)
    }
): List<LevelData> {
    val text = context.assets.open(path)
        .bufferedReader()
        .use { it.readText() }

    return text.split("\r\n\r\n").map { levelText ->
        val lines = levelText.lines().filter { it.isNotBlank() }
        val name = lines[1].removeSurrounding("\'")
        val level = lines.drop(2)
        val width = level.maxOfOrNull { it.length } ?: 0
        val height = level.size
        val levelTiles = Array(width * height) { TileType.SPACE }

        for (y in level.indices) {
            val line = level[y]
            for (x in line.indices) {
                levelTiles[y * width + x] = TileType.entries.first() { it.tile == line[x] }
            }
        }

        val saved = savedState(name, levelTiles)
        LevelData(
            levelTiles,
            width,
            height,
            name,
            initialMoves = saved.moves,
            initialLevel = saved.level
        )
    }
}