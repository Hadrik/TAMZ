package com.example.tamz

enum class TileType(val tile: Char) {
    SPACE(' '),
    WALL('#'),
    TARGET('.'),
    BOX('$'),
    BOX_ON_TARGET('*'),
    PLAYER('@'),
    PLAYER_ON_TARGET('+')
}
