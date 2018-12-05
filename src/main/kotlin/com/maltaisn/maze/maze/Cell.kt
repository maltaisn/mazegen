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
 * Subclasses have to define sides and their values, in a enum implementing [Side].
 * @property maze The maze containing this cell.
 * @property position The position of the cell in [maze].
 */
abstract class Cell(val maze: Maze, val position: Position) {

    /**
     * Cell can be marked as visited by a generator.
     */
    var visited: Boolean = false

    /**
     * The cell value encoding which sides are set. Bit field of [Side] values.
     */
    var value: Int = 0

    /**
     * The list of cells adjacent to this cell, but not necessarily connected.
     */
    open val neighbors: List<Cell> by lazy {
        val list = mutableListOf<Cell>()
        for (side in getAllSides()) {
            val cell = getCellOnSide(side)
            if (cell != null) {
                list.add(cell)
            }
        }
        list.toList()
    }

    /**
     * Returns the neighbor cell on the [side] of the cell.
     * If the neighbor doesn't exist, returns null.
     */
    open fun getCellOnSide(side: Side): Cell? {
        if (side.relativePos == null) return null
        return maze.cellAt(position + side.relativePos!!)
    }

    /**
     * Returns a list of neighbor cells than are accessible from this cell,
     * meaning the side they share with this cell is not set.
     */
    open fun getAccessibleNeighbors(): MutableList<Cell> {
        val list = ArrayList<Cell>()
        for (side in getAllSides()) {
            if (!hasSide(side)) {
                val neighbor = getCellOnSide(side)
                if (neighbor != null) {
                    list.add(neighbor)
                }
            }
        }
        return list
    }

    /**
     * Returns true if [side] is set.
     */
    open fun hasSide(side: Side): Boolean {
        if (side.value == 0) return value == 0
        return (value and side.value) == side.value
    }

    /**
     * Opens (removes) [side] of the cell.
     */
    fun openSide(side: Side) {
        val cell = getCellOnSide(side)
        if (cell != null) {
            cell.value = cell.value and side.opposite().value.inv()
        }
        value = value and side.value.inv()
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
     * Return the side of this cell on which [cell] is placed, if they are
     * neighbors in the same maze. Returns null otherwise.
     */
    open fun findSideOfCell(cell: Cell): Side? {
        if (cell.maze === maze) {
            for (side in getAllSides()) {
                if (getCellOnSide(side) == cell) {
                    return side
                }
            }
        }
        return null
    }

    /**
     * Return the number of sides set on this cell.
     */
    open fun countSides(): Int = Integer.bitCount(value)

    /**
     * Returns the enum value representing all sides.
     */
    abstract fun getAllSideValue(): Side

    /**
     * Returns a list of all possible side values.
     */
    abstract fun getAllSides(): List<Side>


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
        // A cell cannot be equal to another, there is exactly
        // one cell for each position of every maze.
        return other === this
    }

    /**
     * Interface for a cell side enum.
     */
    interface Side {
        /**
         * Side value, a single bit different for each side.
         */
        val value: Int

        /**
         * Relative position of a cell on this side.
         * Can be null if not always the same or not applicable.
         */
        val relativePos: Position?

        /**
         * The symbol for this side, used by [Cell.toString].
         */
        val symbol: String?

        /**
         * Get the side opposite to this side.
         */
        fun opposite(): Side
    }

}