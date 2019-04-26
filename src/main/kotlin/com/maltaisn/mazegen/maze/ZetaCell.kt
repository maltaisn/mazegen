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

package com.maltaisn.mazegen.maze


/**
 * A octogon-like cell for [ZetaMaze].
 * Has north, northeast, east, southeast, south, southwest, west and northwest sides.
 */
class ZetaCell(override val maze: ZetaMaze,
               override val position: Position2D) : Cell(maze, position) {

    override val neighbors = mutableListOf<Cell>()
        get() {
            // Neighbors must be found on every call, they depend on other cells connections.
            field.clear()
            for (side in allSides) {
                val cell = getCellOnSide(side)
                if (cell != null) {
                    if (!side.isDiagonal || !hasDiagonalPassageOnSide(side)) {
                        // Cells on diagonal sides are only neighbors
                        // if there's no diagonal passage blocking it.
                        // North, east, south and west neighbors are always neighbors.
                        field.add(cell)
                    }
                }
            }
            return field
        }

    override val accessibleNeighbors: MutableList<ZetaCell>
        get() {
            val list = mutableListOf<ZetaCell>()
            for (side in allSides) {
                val cell = getCellOnSide(side) as ZetaCell?
                if (cell != null) {
                    if (!hasSide(side) && (!side.isDiagonal || !hasDiagonalPassageOnSide(side))) {
                        // To be accessible, a cell must be a neighbor and not have the wall on this side.
                        list.add(cell)
                    }
                }
            }
            return list
        }

    override val allSides = Side.ALL

    override val allSidesValue = Side.ALL_VALUE

    /**
     * Returns true if this cell has a diagonal passage on one of its diagonal [side].
     * For example, if [side] is northeast, the north and east cells must be connected.
     */
    fun hasDiagonalPassageOnSide(side: Side): Boolean {
        if (!side.isDiagonal) return false
        val cell1 = maze.cellAt(position + Position2D(side.relativePos.x, 0)) ?: return false
        val cell2 = maze.cellAt(position + Position2D(0, side.relativePos.y)) ?: return false
        return !cell1.hasSide(cell1.findSideOfCell(cell2)!!)
    }

    /**
     * Enum class for the sides of an octogon-like zeta cell.
     */
    enum class Side(override val value: Int,
                    override val relativePos: Position2D,
                    val isDiagonal: Boolean,
                    override val symbol: String) : Cell.Side {

        NORTH(1, Position2D(0, -1), false, "N"),
        NORTHEAST(2, Position2D(1, -1), true, "NE"),
        EAST(4, Position2D(1, 0), false, "E"),
        SOUTHEAST(8, Position2D(1, 1), true, "SE"),
        SOUTH(16, Position2D(0, 1), false, "S"),
        SOUTHWEST(32, Position2D(-1, 1), true, "SW"),
        WEST(64, Position2D(-1, 0), false, "W"),
        NORTHWEST(128, Position2D(-1, -1), true, "NW");

        override val opposite: Cell.Side
            get() = when (this) {
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
            val ALL = listOf(NORTH, SOUTH, EAST, WEST, NORTHEAST, SOUTHWEST, SOUTHEAST, NORTHWEST)
            const val ALL_VALUE = 255
        }
    }

}
