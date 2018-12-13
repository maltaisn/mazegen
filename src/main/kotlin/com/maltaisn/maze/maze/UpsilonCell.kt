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
 * A square or octogon cell for [UpsilonMaze].
 * Has 4 or 8 sides depending on its type.
 */
class UpsilonCell(maze: UpsilonMaze, position: Position2D) : Cell(maze, position) {

    override var value: Int = 0
        set(value) {
            // Diagonal sides can't be set on square cells, prevent it.
            field = value and getAllSidesValue()
        }

    /**
     * True if the cell is a square, false if the cell is an octogon.
     */
    private val isSquare: Boolean = (position.x + position.y) % 2 != 0


    override fun getCellOnSide(side: Cell.Side): Cell? {
        if (isSquare && (side as Side).isDiagonal) {
            // Square cells have no diagonal neighbors
            return null
        }
        return super.getCellOnSide(side)
    }

    override fun getAllSides(): List<Side> = if (isSquare) {
        Side.ALL_SQUARE
    } else {
        Side.ALL_OCTOGON
    }

    override fun getAllSidesValue(): Int = if (isSquare) {
        Side.ALL_SQUARE_VALUE
    } else {
        Side.ALL_OCTOGON_VALUE
    }

    /**
     * Enum class for the side of both octogonal and square cells.
     */
    enum class Side(override val value: Int,
                    override val relativePos: Position2D,
                    val isDiagonal: Boolean,
                    override val symbol: String) : Cell.Side {
        NORTH(1, Position2D(0, -1), false, "N"),
        EAST(2, Position2D(1, 0), false, "E"),
        SOUTH(4, Position2D(0, 1), false, "S"),
        WEST(8, Position2D(-1, 0), false, "W"),
        NORTHEAST(16, Position2D(1, -1), true, "NE"),
        SOUTHEAST(32, Position2D(1, 1), true, "SE"),
        SOUTHWEST(64, Position2D(-1, 1), true, "SW"),
        NORTHWEST(128, Position2D(-1, -1), true, "NW");

        override fun opposite(): Side = when (this) {
            NORTH -> SOUTH
            SOUTH -> NORTH
            EAST -> WEST
            WEST -> EAST
            NORTHEAST -> SOUTHWEST
            SOUTHWEST -> NORTHEAST
            SOUTHEAST -> NORTHWEST
            NORTHWEST -> SOUTHEAST
        }

        companion object {
            val ALL_SQUARE = listOf(NORTH, SOUTH, EAST, WEST)
            val ALL_OCTOGON = listOf(NORTH, SOUTH, EAST, WEST,
                    NORTHEAST, SOUTHWEST, SOUTHEAST, NORTHWEST)
            const val ALL_SQUARE_VALUE = 15
            const val ALL_OCTOGON_VALUE = 255
        }

    }

}