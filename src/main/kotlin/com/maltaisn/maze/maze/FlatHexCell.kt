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


class FlatHexCell(override val maze: FlatHexMaze,
                  override val position: PositionXY) : HexCell, FlatCell {

    override var visited = false

    override var value: Int = Side.NONE.value
        set(value) {
            field = value and Side.ALL.value
        }

    override var neighborList: List<FlatCell>? = null

    /**
     * Create new cell with initial value of [value].
     */
    constructor(maze: FlatHexMaze, position: PositionXY, value: Int) : this(maze, position) {
        this.value = value
    }

    override fun getAllSides(): List<Side> = ALL_SIDES

    override fun getCellOnSide(side: FlatCell.Side): FlatHexCell? {
        return super.getCellOnSide(side) as FlatHexCell?
    }

    @Suppress("UNCHECKED_CAST")
    override fun getNeighbors(): List<FlatHexCell> {
        return super.getNeighbors() as List<FlatHexCell>
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("[pos: $position, sides, ")
        when (value) {
            Side.NONE.value -> sb.append("NONE")
            Side.ALL.value -> sb.append("ALL")
            else -> {
                if (hasSide(Side.NORTH)) sb.append("N,")
                if (hasSide(Side.NORTHEAST)) sb.append("NE,")
                if (hasSide(Side.SOUTHEAST)) sb.append("SE,")
                if (hasSide(Side.SOUTH)) sb.append("S,")
                if (hasSide(Side.SOUTHWEST)) sb.append("SW,")
                if (hasSide(Side.NORTHWEST)) sb.append("NW,")
                sb.deleteCharAt(sb.length - 1)
            }
        }
        sb.append(", ")
        sb.append(if (visited) "visited" else "unvisited")
        sb.append("]")
        return sb.toString()
    }

    /**
     * Enum class for the side a hexagonal cell
     * ```
     *     N -> __
     *   NW -> /  \  <- NE
     *   SW -> \__/  <- SE
     *      S ->
     * ```
     */
    enum class Side(override val value: Int, override val relativePos: Position?) : FlatCell.Side {
        NONE(0, null),
        NORTH(1, PositionXY(0, -1)),
        NORTHEAST(2, PositionXY(1, 0)),
        SOUTHEAST(4, PositionXY(1, 1)),
        SOUTH(8, PositionXY(0, 1)),
        SOUTHWEST(16, PositionXY(-1, 1)),
        NORTHWEST(32, PositionXY(-1, -1)),
        ALL(63, null);

        /**
         * Returns the opposite side of this side
         */
        override fun opposite(): Side = when (this) {
            NONE -> NONE
            NORTH -> SOUTH
            NORTHEAST -> SOUTHWEST
            SOUTHEAST -> NORTHWEST
            SOUTH -> NORTH
            SOUTHWEST -> NORTHEAST
            NORTHWEST -> SOUTHEAST
            ALL -> ALL
        }

        override fun isAll(): Boolean = (this == ALL)
    }

    companion object {
        private val ALL_SIDES = listOf(Side.NORTH, Side.NORTHEAST, Side.SOUTHEAST,
                Side.SOUTH, Side.SOUTHWEST, Side.NORTHWEST)
    }

}