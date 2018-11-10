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

import java.awt.BasicStroke
import java.awt.Color


/**
 * Interface for a maze.
 */
interface Maze {

    /**
     * Returns the cell at [pos].
     */
    fun cellAt(pos: Position): Cell

    /**
     * Returns the cell at [pos] if it exists, otherwise returns null.
     */
    fun optionalCellAt(pos: Position): Cell?

    /**
     * Returns a random cell in the maze.
     */
    fun getRandomCell(): Cell

    /**
     * Get the total number of cells in this maze.
     */
    fun getCellCount(): Int

    /**
     * Returns a set containing all the cells in this maze.
     */
    fun getAllCells(): LinkedHashSet<Cell>

    /**
     * Clears all the sides of all cells in the maze if [empty] is true,
     * otherwise sets all sides on all the cells.
     */
    fun reset(empty: Boolean)

    /**
     * Render the maze to a SVG format and returns it.
     */
    fun renderToSvg(): String


    companion object {
        val SVG_STROKE_STYLE = BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        val SVG_STROKE_COLOR: Color = Color.BLACK
    }

}