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
 * A triangular cell in [DeltaMaze].
 * Has west, east and base (north or south) sides.
 */
class DeltaCell : Cell {

    constructor(maze: DeltaMaze, position: PositionXY) : super(maze, position)

    constructor(maze: DeltaMaze, position: PositionXY, value: Int) : super(maze, position, value)

    override fun getCellOnSide(side: Cell.Side): Cell? {
        if (side == Side.BASE) {
            // The cell on the base side can be either up or down, depending on the X position.
            val pos = position as PositionXY
            val flatTopped = (pos.x + pos.y) % 2 == 0
            return maze.optionalCellAt(position
                    + PositionXY(0, if (flatTopped) -1 else 1))
        }
        return super.getCellOnSide(side)
    }

    override fun getAllSides(): List<Side> = ALL_SIDES

    override fun getAllSideValue(): Side = Side.ALL

    /**
     * Enum class for the side a delta cell.
     */
    enum class Side(override val value: Int,
                    override val relativePos: PositionXY?,
                    override val symbol: String?) : Cell.Side {
        NONE(0, null, null),
        BASE(1, null, "B"),
        EAST(2, PositionXY(1, 0), "E"),
        WEST(4, PositionXY(-1, 0), "W"),
        ALL(7, null, null);

        override fun opposite(): Side = when (this) {
            NONE -> NONE
            BASE -> BASE
            EAST -> WEST
            WEST -> EAST
            ALL -> ALL
        }

    }

    companion object {
        private val ALL_SIDES = listOf(Side.BASE, Side.EAST, Side.WEST)
    }

}