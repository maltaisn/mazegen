/*
 * Copyright (c) 2019 Nicolas Maltais
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

import com.maltaisn.mazegen.paramError
import kotlin.random.Random


/**
 * Base class for a maze represented by 2D orthogonal grid of cells.
 * @property width number of rows
 * @property height number of columns
 */
abstract class BaseGridMaze<T : Cell>(val width: Int, val height: Int) : Maze() {

    protected abstract val grid: Array<Array<T>>

    init {
        if (width < 1 || height < 1) {
            paramError("Dimensions must be at least 1.")
        }
    }


    override val cellCount: Int
        get() = grid.size * grid[0].size

    override fun cellAt(pos: Position) = cellAt((pos as Position2D).x, pos.y)

    fun cellAt(x: Int, y: Int): T? = grid.getOrNull(x)?.getOrNull(y)

    override fun getRandomCell() = grid[Random.nextInt(grid.size)][Random.nextInt(grid[0].size)]

    override fun getAllCells(): MutableList<T> {
        val list = mutableListOf<T>()
        for (x in 0 until grid.size) {
            for (y in 0 until grid[x].size) {
                list.add(grid[x][y])
            }
        }
        return list
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
            OPENING_POS_CENTER -> grid[0].size / 2
            OPENING_POS_END -> grid[0].size - 1
            else -> pos
        }
        return cellAt(x, y)
    }

    override fun toString() = "[width: $width, height: $height]"

}
