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
class DeltaCell(maze: DeltaMaze, position: Position2D) : Cell(maze, position) {

    override fun getCellOnSide(side: Cell.Side): Cell? {
        if (side == Side.BASE) {
            // The cell on the base side can be either up or down, depending on the X position.
            val pos = position as Position2D
            val flatTopped = (pos.x + pos.y) % 2 == 0
            return maze.cellAt(position
                    + Position2D(0, if (flatTopped) -1 else 1))
        }
        return super.getCellOnSide(side)
    }

    override fun getAllSides(): List<Side> = Side.ALL

    override fun getAllSidesValue(): Int = Side.ALL_VALUE

    /**
     * Enum class for the side a delta cell.
     */
    enum class Side(override val value: Int,
                    override val relativePos: Position2D?,
                    override val symbol: String?) : Cell.Side {
        BASE(1, null, "B"),
        EAST(2, Position2D(1, 0), "E"),
        WEST(4, Position2D(-1, 0), "W");

        override fun opposite(): Side = when (this) {
            BASE -> BASE
            EAST -> WEST
            WEST -> EAST
        }

        companion object {
            val ALL = listOf(Side.BASE, Side.EAST, Side.WEST)
            const val ALL_VALUE = 7
        }

    }

}