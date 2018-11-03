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


/**
 * Base class for a maze of width [width] and height [height].
 * A maze is a simple wrapper around a 2D cell array.
 * @param[T] type of the cells in the maze
 */
abstract class Maze<T : Cell>(val width: Int, val height: Int) {

    lateinit var grid: Array<Array<T>>
        protected set

    /**
     * Returns the cell at coordinates ([x]; [y]).
     */
    fun cellAt(x: Int, y: Int): T = grid[x][y]

    /**
     * Returns the cell at coordinates ([x]; [y]) if valid.
     * Otherwise returns null.
     */
    fun optionalCellAt(x: Int, y: Int): T? {
        if (x < 0 || x >= width || y < 0 || y >= height) return null
        return grid[x][y]
    }

    /**
     * Returns a string representation of the maze
     */
    abstract fun format(): String

    override fun toString(): String = "[width: $width, height: $height]"

}