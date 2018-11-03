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
 * Cell implementation for a [FlatMaze].
 */
class FlatCell(maze: Maze<FlatCell>, x: Int, y: Int) : Cell(maze, x, y) {

    /**
     * Bit field of [Side] values encoding which sides are set.
     */
    var value: Int = Side.NONE.value
        set(value) {
            field = value and Side.ALL.value
        }

    /**
     * Create new cell with initial value of [value].
     */
    constructor(maze: Maze<FlatCell>, x: Int, y: Int, value: Int) : this(maze, x, y) {
        this.value = value
    }

    /**
     * Returns the neighbor cell on the [side] of the cell.
     * If the neighbor doesn't exist, returns null.
     */
    fun getCellOnSide(side: Side): FlatCell? = when (side) {
        Side.NORTH -> if (y == 0) null else maze.grid[x][y - 1] as FlatCell
        Side.EAST -> if (x == maze.width - 1) null else maze.grid[x + 1][y] as FlatCell
        Side.SOUTH -> if (y == maze.height - 1) null else maze.grid[x][y + 1] as FlatCell
        Side.WEST -> if (x == 0) null else maze.grid[x - 1][y] as FlatCell
        else -> null
    }

    /**
     * Get the list of all non-null neighbor cells of this cell.
     */
    fun getNeighbors(): List<FlatCell> = listOfNotNull(
            getCellOnSide(Side.NORTH),
            getCellOnSide(Side.EAST),
            getCellOnSide(Side.SOUTH),
            getCellOnSide(Side.WEST)
    )

    /**
     * Returns true if [side] is set.
     * If [Side.ALL], returns true if all sides are set.
     * if [Side.NONE], returns true if no sides are set.
     */
    fun hasSide(side: Side): Boolean {
        if (side == Side.NONE) return value == Side.NONE.value
        return (value and side.value) == side.value
    }

    /**
     * Opens (removes) [side] of the cell.
     */
    fun openSide(side: Side) {
        changeSide(side) { v, s -> v and s.inv() }
    }

    /**
     * Closes (adds) [side] of the cell.
     */
    fun closeSide(side: Side) {
        changeSide(side, Int::or)
    }

    /**
     * Toggles [side] of the cell.
     */
    fun toggleSide(side: Side) {
        changeSide(side, Int::xor)
    }

    /**
     * Do [operation] on this cell's value and the neighbor on [side]'s value.
     * If side is [Side.ALL], operation is done on all neighbors.
     */
    private fun changeSide(side: Side, operation: (v: Int, s: Int) -> Int) {
        if (side == Side.NONE) {
            return
        } else if (side == Side.ALL) {
            for (s in listOf(Side.NORTH, Side.EAST, Side.SOUTH, Side.WEST)) {
                val cell = getCellOnSide(s)
                if (cell != null) {
                    cell.value = operation.invoke(cell.value, s.opposite().value)
                }
            }
        } else {
            val cell = getCellOnSide(side)
            if (cell != null) {
                cell.value = operation.invoke(cell.value, side.opposite().value)
            }
        }
        value = operation.invoke(value, side.value)
    }

    /**
     * Connect this cell with another cell [cell] if they are neighbors of the same maze.
     * Does nothing otherwise. The common side of both cells is opened (removed).
     */
    fun connectWith(cell: FlatCell) {
        if (cell.maze !== maze) return

        var side: Side? = null
        if (cell.x == x && cell.y == y - 1) {
            side = Side.NORTH
        } else if (cell.x == x + 1 && cell.y == y) {
            side = Side.EAST
        } else if (cell.x == x && cell.y == y + 1) {
            side = Side.SOUTH
        } else if (cell.x == x - 1 && cell.y == y) {
            side = Side.WEST
        }
        if (side != null) {
            cell.value = cell.value and side.opposite().value.inv()
            value = value and side.value.inv()
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(super.toString())
        sb.deleteCharAt(sb.length - 1)
        sb.append(", sides: ")
        when (value) {
            Side.NONE.value -> sb.append("NONE")
            Side.ALL.value -> sb.append("ALL")
            else -> {
                if (hasSide(Side.NORTH)) sb.append("N,")
                if (hasSide(Side.EAST)) sb.append("E,")
                if (hasSide(Side.SOUTH)) sb.append("S,")
                if (hasSide(Side.WEST)) sb.append("W,")
                sb.deleteCharAt(sb.length - 1)
            }
        }
        sb.append("]")
        return sb.toString()
    }

    enum class Side(val value: Int) {
        NONE(0),
        NORTH(1),
        EAST(2),
        SOUTH(4),
        WEST(8),
        ALL(NORTH.value or EAST.value or SOUTH.value or WEST.value);

        /**
         * Returns the opposite side of this side
         */
        fun opposite(): Side = when (this) {
            NONE -> NONE
            NORTH -> SOUTH
            SOUTH -> NORTH
            EAST -> WEST
            WEST -> EAST
            ALL -> ALL
        }
    }
}