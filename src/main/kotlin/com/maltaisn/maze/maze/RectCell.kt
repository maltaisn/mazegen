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


class RectCell(override val maze: RectMaze, override val position: PositionXY) : Cell {

    override var visited = false

    override var value: Int = Side.NONE.value
        set(value) {
            field = value and Side.ALL.value
        }

    override var neighborList: List<Cell>? = null

    /**
     * Create new cell with initial value of [value].
     */
    constructor(maze: RectMaze, position: PositionXY, value: Int) : this(maze, position) {
        this.value = value
    }

    override fun getAllSides(): List<Side> = ALL_SIDES

    override fun getCellOnSide(side: Cell.Side): RectCell? {
        return super.getCellOnSide(side) as RectCell?
    }

    @Suppress("UNCHECKED_CAST")
    override fun getNeighbors(): List<RectCell> {
        return super.getNeighbors() as List<RectCell>
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("[pos: $position, sides: ")
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
        sb.append(", ")
        sb.append(if (visited) "visited" else "unvisited")
        sb.append("]")
        return sb.toString()
    }

    /**
     * Enum class for the side a rectangular cell
     */
    enum class Side(override val value: Int,
                    override val relativePos: PositionXY?) : Cell.Side {
        NONE(0, null),
        NORTH(1, PositionXY(0, -1)),
        EAST(2, PositionXY(1, 0)),
        SOUTH(4, PositionXY(0, 1)),
        WEST(8, PositionXY(-1, 0)),
        ALL(15, null);

        /**
         * Returns the opposite side of this side
         */
        override fun opposite(): Side = when (this) {
            NONE -> NONE
            NORTH -> SOUTH
            SOUTH -> NORTH
            EAST -> WEST
            WEST -> EAST
            ALL -> ALL
        }

        override fun isAll(): Boolean = (this == ALL)
    }

    companion object {
        private val ALL_SIDES = listOf(Side.NORTH, Side.EAST, Side.SOUTH, Side.WEST)
    }

}