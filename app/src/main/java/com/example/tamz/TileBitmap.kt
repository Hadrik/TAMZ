package com.example.tamz

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

class TileBitmap {
    private val tiles: Array<ImageBitmap>

    constructor(context: Context) {
        tiles = arrayOf(
            BitmapFactory.decodeResource(context.resources, R.drawable.empty).asImageBitmap(), // 0
            BitmapFactory.decodeResource(context.resources, R.drawable.wall).asImageBitmap(),  // 1
            BitmapFactory.decodeResource(context.resources, R.drawable.box).asImageBitmap(),   // 2
            BitmapFactory.decodeResource(context.resources, R.drawable.goal).asImageBitmap(),  // 3
            BitmapFactory.decodeResource(context.resources, R.drawable.hero).asImageBitmap(),  // 4
            BitmapFactory.decodeResource(context.resources, R.drawable.boxok).asImageBitmap()  // 5
        )
    }

    fun get(tile: TileType): ImageBitmap {
        return when (tile) {
            TileType.SPACE -> tiles[0]
            TileType.WALL -> tiles[1]
            TileType.TARGET -> tiles[3]
            TileType.BOX -> tiles[2]
            TileType.BOX_ON_TARGET -> tiles[5]
            TileType.PLAYER -> tiles[4]
            TileType.PLAYER_ON_TARGET -> tiles[4]
        }
    }
}