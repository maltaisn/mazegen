/*
 * Copyright (c) 2018 Nicolas Maltais
 *
 * Permission is hereby granted, free of charge,
 * to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to
 * deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom
 * the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice
 * shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.maltaisn.maze.maze

import com.maltaisn.maze.MazeType
import com.maltaisn.maze.ParameterException
import java.util.*
import kotlin.random.Random


/**
 * Base class for an orthogonal maze represented by 2D grid of cells.
 * @property width number of rows
 * @property height number of columns
 */
abstract class BaseOrthogonalMaze<T : Cell>(val width: Int, val height: Int,
                                            type: MazeType) : Maze(type) {

    private val grid: Array<Array<T>>

    init {
        if (width < 1 || height < 1) {
            throw ParameterException("Dimensions must be at least 1.")
        }
        @Suppress("UNCHECKED_CAST")
        grid = Array(width) { x ->
            Array<Cell>(height) { y ->
                createCell(Position2D(x, y))
            }
        } as Array<Array<T>>
    }

    /**
     * Create a cell for this maze at [pos].
     */
    protected abstract fun createCell(pos: Position2D): T


    override fun cellAt(pos: Position) =
            cellAt((pos as Position2D).x, pos.y)

    fun cellAt(x: Int, y: Int): T? {
        if (x < 0 || x >= width || y < 0 || y >= height) return null
        return grid[x][y]
    }

    override fun getRandomCell(): T {
        return grid[Random.nextInt(width)][Random.nextInt(height)]
    }

    override fun getCellCount(): Int = width * height

    override fun getAllCells(): MutableList<T> {
        val set = ArrayList<T>()
        for (x in 0 until width) {
            for (y in 0 until height) {
                set.add(grid[x][y])
            }
        }
        return set
    }

    override fun forEachCell(action: (Cell) -> Unit) {
        for (x in 0 until width) {
            for (y in 0 until height) {
                action(grid[x][y])
            }
        }
    }

    override fun getOpeningCell(opening: Opening): T? {
        val x = when (val pos = opening.position[0]) {
            Opening.POS_START -> 0
            Opening.POS_CENTER -> width / 2
            Opening.POS_END -> width - 1
            else -> pos
        }
        val y = when (val pos = opening.position[1]) {
            Opening.POS_START -> 0
            Opening.POS_CENTER -> height / 2
            Opening.POS_END -> height - 1
            else -> pos
        }
        return cellAt(x, y)
    }

    override fun toString(): String {
        return "[width: $width, height: $height]"
    }

}