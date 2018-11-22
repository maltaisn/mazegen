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

import com.maltaisn.maze.render.Canvas


/**
 * Interface for a maze.
 */
abstract class Maze {

    var name: String? = null

    constructor()

    constructor(maze: Maze) {
        name = maze.name
    }

    /**
     * Returns the cell at [pos].
     */
    abstract fun cellAt(pos: Position): Cell

    /**
     * Returns the cell at [pos] if it exists, otherwise returns null.
     */
    abstract fun optionalCellAt(pos: Position): Cell?

    /**
     * Returns a random cell in the maze.
     */
    abstract fun getRandomCell(): Cell

    /**
     * Get the total number of cells in this maze.
     */
    abstract fun getCellCount(): Int

    /**
     * Returns a set containing all the cells in this maze.
     */
    abstract fun getAllCells(): LinkedHashSet<out Cell>

    /**
     * Create openings [openings] in the maze.
     * An exception is thrown if the opening position doesn't match any cell.
     */
    abstract fun createOpenings(vararg openings: Opening)

    /**
     * Clears all the sides of all cells in the maze if [empty] is true,
     * otherwise sets all sides on all the cells. All cells are set as unvisited.
     */
    abstract fun reset(empty: Boolean)

    /**
     * Creates a deep copy of this maze.
     */
    abstract fun copy(): Maze

    /**
     * Draw the maze to a [canvas], with an arbitrarey cell size parameter of [cellSize].
     */
    abstract fun drawTo(canvas: Canvas, cellSize: Double)

}