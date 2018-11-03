package com.maltaisn.maze.maze


abstract class Maze<T : Cell>(val width: Int, val height: Int) {

    lateinit var grid: Array<Array<T>>

    fun cellAt(x: Int, y: Int): T = grid[x][y]

    fun optionalCellAt(x: Int, y: Int): T? {
        if (x < 0 || x >= width || y < 0 || y >= height) return null
        return grid[x][y]
    }

    abstract fun format(): String

    override fun toString(): String = "[width: $width, height: $height]"

}