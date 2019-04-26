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

package com.maltaisn.mazegen.maze

import com.maltaisn.mazegen.ParameterException
import com.maltaisn.mazegen.maze.BaseShapedMaze.Shape
import kotlin.random.Random


/**
 * Base class for a maze represented with a 2D grid of cells that forms a [Shape].
 * @property width number of rows
 * @property height number of columns
 * @property shape the shape of the maze.
 */
abstract class BaseShapedMaze<T : Cell>(val width: Int, height: Int,
                                        private val shape: Shape) : Maze() {

    val height: Int

    /**
     * The maze grid. There number of columns is the same as the maze width, except for
     * hexagon-shaped mazes where the number of columns is equal to `width * 2 - 1`.
     * The number of rows varies for each column depending on the shape of the maze.
     */
    protected abstract val grid: Array<Array<T>>

    /**
     * The offset of the actual Y coordinate of a cell in the grid array, for each column
     * The cell at ```grid[x][y]```'s actual coordinates are `(x ; y + rowOffsets[x])`.
     */
    protected abstract val rowOffsets: IntArray

    init {
        if (width < 1 || height < 1) {
            throw ParameterException("Dimensions must be at least 1.")
        }
        if (shape == Shape.TRIANGLE
                || shape == Shape.HEXAGON) {
            // Hexagon and triangle mazes have only one size parameter.
            this.height = width
        } else {
            this.height = height
        }
    }

    override val randomCell: T
        get() {
            val x = Random.nextInt(grid.size)
            return grid[x][Random.nextInt(grid[x].size)]
        }

    override val cellCount: Int
        get() {
            var count = 0
            for (x in 0 until grid.size) {
                count += grid[x].size
            }
            return count
        }

    override val cellList: MutableList<T>
        get() {
            val list = mutableListOf<T>()
            for (x in 0 until grid.size) {
                for (y in 0 until grid[x].size) {
                    list.add(grid[x][y])
                }
            }
            return list
        }

    override fun cellAt(pos: Position) =
            cellAt((pos as Position2D).x, pos.y)

    fun cellAt(x: Int, y: Int): T? {
        if (x < 0 || x >= grid.size) return null
        val actualY = y - rowOffsets[x]
        if (actualY < 0 || actualY >= grid[x].size) return null
        return grid[x][actualY]
    }

    override fun forEachCell(action: (Cell) -> Unit) {
        for (x in 0 until grid.size) {
            for (y in 0 until grid[x].size) {
                action(grid[x][y])
            }
        }
    }

    override fun getOpeningCell(opening: Position): T? {
        val x = when (val pos = (opening as Position2D).x) {
            OPENING_POS_START -> 0
            OPENING_POS_CENTER -> grid.size / 2
            OPENING_POS_END -> grid.size - 1
            else -> pos
        }
        val y = when (val pos = opening.y) {
            OPENING_POS_START -> 0
            OPENING_POS_CENTER -> grid[x].size / 2
            OPENING_POS_END -> grid[x].size - 1
            else -> pos
        } + rowOffsets[x]
        return cellAt(x, y)
    }


    override fun toString() = "[shape: $shape, ${if (shape == Shape.TRIANGLE || shape == Shape.HEXAGON)
        "size : $width" else "width: $width, height: $height"}]"


    /**
     * Possible shapes for a shaped maze.
     */
    enum class Shape {
        RECTANGLE,
        TRIANGLE,
        HEXAGON,
        RHOMBUS
    }

}
