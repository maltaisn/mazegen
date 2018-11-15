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
 * Base class for the cell of a flat maze.
 * Each cell has a value, a bit field encoding which sides are set.
 * They also have a reference to the maze containing them as well as their position in that maze.
 * Subclasses have define sides and their values, in a enum implementing [Side].
 */
abstract class Cell {

    /**
     * The maze containing this cell.
     */
    val maze: Maze

    /**
     * The position of the cell in [maze].
     */
    val position: Position

    /**
     * Cell can be marked as visited by a generator.
     */
    var visited: Boolean = false

    var value: Int = 0

    private var neighborList: List<Cell>? = null

    /**
     * Create new empty cell at [position] in [maze].
     */
    constructor(maze: Maze, position: Position) {
        this.maze = maze
        this.position = position
    }

    /**
     * Create new cell at [position] in [maze] with [value].
     */
    constructor(maze: Maze, position: Position, value: Int) : this(maze, position) {
        this.value = value
    }

    /**
     * Returns the neighbor cell on the [side] of the cell.
     * If the neighbor doesn't exist, returns null.
     */
    open fun getCellOnSide(side: Side): Cell? {
        if (side.relativePos == null) return null
        return maze.optionalCellAt(position + side.relativePos!!)
    }

    /**
     * Get the list of all non-null neighbor cells of this cell.
     */
    open fun getNeighbors(): List<Cell> {
        if (neighborList == null) {
            val list = mutableListOf<Cell>()
            for (side in getAllSides()) {
                val cell = getCellOnSide(side)
                if (cell != null) {
                    list.add(cell)
                }
            }
            neighborList = list.toList()
        }
        return neighborList!!.toList()
    }

    /**
     * Returns true if [side] is set.
     */
    fun hasSide(side: Side): Boolean {
        if (side.value == 0) return value == 0
        return (value and side.value) == side.value
    }

    /**
     * Connect this cell with another cell [cell] if they are neighbors of the same maze.
     * Does nothing otherwise. The common side of both cells is opened (removed).
     */
    fun connectWith(cell: Cell) {
        val side = findSideOfCell(cell)
        if (side != null) {
            cell.value = cell.value and side.opposite().value.inv()
            value = value and side.value.inv()
        }
    }

    /**
     * Returns a list of all possible side values.
     */
    abstract fun getAllSides(): List<Side>

    /**
     * Return the side of this cell on which [cell] is placed, if they are
     * neighbors in the same maze. Returns null otherwise.
     */
    fun findSideOfCell(cell: Cell): Side? {
        if (cell.maze === maze) {
            for (side in getAllSides()) {
                if (getCellOnSide(side) == cell) {
                    return side
                }
            }
        }
        return null
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("[pos: $position, sides: ")
        when (value) {
            0 -> sb.append("NONE")
            else -> {
                for (side in getAllSides()) {
                    if (hasSide(side)) {
                        sb.append(side.symbol)
                        sb.append(",")
                    }
                }
                sb.deleteCharAt(sb.length - 1)
            }
        }
        sb.append(", ")
        sb.append(if (visited) "visited" else "unvisited")
        sb.append("]")
        return sb.toString()
    }

    override fun hashCode(): Int = position.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Cell) return false
        return maze == other.maze && position == other.position
    }

    /**
     * Interface for a cell side enum.
     */
    interface Side {

        val value: Int

        val relativePos: Position?

        val symbol: String?

        /**
         * Get the side opposite to this side.
         */
        fun opposite(): Side

    }

}