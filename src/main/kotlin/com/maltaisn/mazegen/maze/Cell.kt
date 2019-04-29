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


/**
 * Base class for the cell of a flat maze.
 * Each cell has a value, a bit field encoding which sides are set.
 * They also have a reference to the maze containing them as well as their position in that maze.
 * Subclasses have to define sides and their values, in a enum implementing [Side].
 * @property maze The maze containing this cell.
 * @property position The position of the cell in [maze].
 */
abstract class Cell(open val maze: Maze, open val position: Position) {

    /**
     * Cell can be marked as visited by a generator.
     */
    var visited = false

    /**
     * The cell value encoding which sides are set. Bit field of [Side] values.
     */
    open var value = 0

    /**
     * If a distance map was generated, the minimum distance of this cell from the starting cell.
     */
    var distanceMapValue = -1

    /**
     * The list of cells adjacent to this cell, but not necessarily connected.
     * All neighbors must be able to validly connect with this cell.
     */
    open val neighbors: List<Cell> by lazy {
        val list = mutableListOf<Cell>()
        for (side in allSides) {
            val cell = getCellOnSide(side)
            if (cell != null) {
                list.add(cell)
            }
        }
        list
    }

    /**
     * Returns a list of neighbor cells than are accessible from this cell,
     * meaning the wall they share with this cell is not set.
     */
    open fun findAccessibleNeighbors(): MutableList<out Cell> {
        val list = mutableListOf<Cell>()
        for (side in allSides) {
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
     * Returns the neighbor cell on the [side] of the cell.
     * If the neighbor doesn't exist, returns null.
     */
    open fun getCellOnSide(side: Side): Cell? {
        if (side.relativePos == null) return null
        return maze.cellAt(position + side.relativePos!!)
    }

    /**
     * Returns true if [side] is set.
     */
    open fun hasSide(side: Side): Boolean {
        if (side.value == 0) return value == 0
        return (value and side.value) == side.value
    }

    /**
     * Opens [side] of the cell. (Removes a wall)
     */
    fun openSide(side: Side) {
        changeSide(side) { v, s -> v and s.inv() }
    }

    /**
     * Closes [side] of the cell. (Adds a wall)
     */
    fun closeSide(side: Side) {
        changeSide(side, Int::or)
    }

    /**
     * Do [operation] on this cell's [side] wall and the cell on the other side.
     */
    private inline fun changeSide(side: Side, operation: (v: Int, s: Int) -> Int) {
        val cell = getCellOnSide(side)
        if (cell != null) {
            cell.value = operation(cell.value, side.opposite.value)
        }
        value = operation(value, side.value)
    }

    /**
     * Connect this cell with another cell [cell] if they are neighbors of the same maze.
     * Does nothing otherwise. The common wall of both cells is opened (removed).
     */
    open fun connectWith(cell: Cell) {
        val side = findSideOfCell(cell)
        if (side != null) {
            cell.value = cell.value and side.opposite.value.inv()
            value = value and side.value.inv()
        }
    }

    /**
     * Return the side of this cell on which [cell] is placed, if they are
     * neighbors in the same maze. Returns null otherwise.
     */
    open fun findSideOfCell(cell: Cell): Side? {
        if (cell.maze === maze) {
            for (side in allSides) {
                if (getCellOnSide(side) == cell) {
                    return side
                }
            }
        }
        return null
    }

    /**
     * Returns the number of sides set on this cell.
     */
    open val sidesCount: Int
        get() = Integer.bitCount(value)

    /**
     * Returns a list of all possible side values.
     */
    abstract val allSides: List<Side>

    /**
     * Returns the value of a cell with all sides set.
     */
    abstract val allSidesValue: Int


    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("[pos: $position, sides: ")
        if (value == 0) {
            sb.append("NONE")
        } else {
            for (side in allSides) {
                if (hasSide(side)) {
                    sb.append(side.symbol)
                    sb.append(",")
                }
            }
            sb.deleteCharAt(sb.length - 1)
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
        val symbol: String

        /**
         * The side opposite to this side.
         */
        val opposite: Side
    }

}
