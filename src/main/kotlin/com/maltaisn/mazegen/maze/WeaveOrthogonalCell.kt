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

import kotlin.math.absoluteValue


/**
 * A octogon-like cell for [WeaveOrthogonalMaze].
 * Has north, east, south, west sides.
 */
class WeaveOrthogonalCell(override val maze: WeaveOrthogonalMaze,
                          override val position: Position2D) : Cell(maze, position) {

    /**
     * If a color map was generated, the minimum distance of this cell from
     * the starting cell for the tunnel on this cell if it has one.
     */
    var colorMapDistanceTunnel = -1

    /**
     * Returns a list of immediate neighbors as well as neighbors connectable with a tunnel.
     * All neighbors returned can make a valid connection with the cell.
     */
    override val neighbors = mutableListOf<WeaveOrthogonalCell>()
        get() {
            // If weaving is disabled, neighbors are always the same.
            if (maze.maxWeave == 0 && field.isNotEmpty()) return field

            // Neighbors must be found on every call, they depend on other cells connections.
            field.clear()
            for (side in allSides) {
                var pos = position
                for (i in 0..maze.maxWeave) {
                    pos += side.relativePos
                    val cell = maze.cellAt(pos)
                    if (cell != null) {
                        if (i == 0 || !cell.hasTunnel) {
                            field.add(cell)
                            if (cell.isPassageParallelTo(side)) {
                                continue
                            }
                        }
                    }
                    break
                }
            }
            return field
        }

    override fun findAccessibleNeighbors(): MutableList<WeaveOrthogonalCell> {
        val list = mutableListOf<WeaveOrthogonalCell>()
        for (side in allSides) {
            var pos = position
            for (i in 0..maze.maxWeave) {
                pos += side.relativePos
                val cell = maze.cellAt(pos)
                if (cell == null || i == 0 && hasSide(side)) {
                    // Reached maze border or this cell has a wall: there's no neighbors on this side.
                    break
                } else if (!cell.hasTunnel || !cell.hasSide(side)) {
                    // First cell that is not a tunnel, this is the neighbor on this side.
                    list.add(cell)
                    break
                }
            }
        }
        return list
    }

    override fun connectWith(cell: Cell) {
        cell as WeaveOrthogonalCell
        if (cell.maze === maze) {
            // Find the difference in horizontal or vertical position between the two cells
            // Also find on which side of this cell the cell to connect to is.
            val diff: Int
            val side: Side
            val dx = cell.position.x - position.x
            val dy = cell.position.y - position.y
            if (dx != 0 && dy == 0) {
                diff = dx.absoluteValue
                side = if (dx > 0) Side.EAST else Side.WEST
            } else if (dy != 0 && dx == 0) {
                diff = dy.absoluteValue
                side = if (dy > 0) Side.SOUTH else Side.NORTH
            } else {
                // Cell is not in the same row or column, can't connect them.
                throw IllegalArgumentException("Cells can't be connected.")
            }

            value = value and side.value.inv()

            // Make every cell between the two cells tunnels
            var pos = position
            for (i in 0 until diff - 1) {
                pos += side.relativePos
                maze.cellAt(pos)?.createTunnel()
            }

            cell.value = cell.value and side.opposite.value.inv()
        }
    }

    /**
     * Returns true if this cell is a straight passage parallel to a [side],
     * meaning it only has two sides set, both parallel to [side].
     */
    private fun isPassageParallelTo(side: Side): Boolean = when (side) {
        Side.NORTH, Side.SOUTH -> value == Side.HORIZONTAL_PASSAGE_VALUE
        Side.WEST, Side.EAST -> value == Side.VERTICAL_PASSAGE_VALUE
    }

    /**
     * Returns true if this cell has a tunnel under it.
     */
    val hasTunnel: Boolean
        get() = value and Side.TUNNEL == Side.TUNNEL

    /**
     * Make this cell a tunnel.
     */
    private fun createTunnel() {
        value = value or Side.TUNNEL
    }

    override val allSides = Side.ALL

    override val allSidesValue = Side.ALL_VALUE

    /**
     * Enum class for the sides of a weave square cell.
     */
    enum class Side(override val value: Int,
                    override val relativePos: Position2D,
                    override val symbol: String) : Cell.Side {

        NORTH(1, Position2D(0, -1), "N"),
        EAST(2, Position2D(1, 0), "E"),
        SOUTH(4, Position2D(0, 1), "S"),
        WEST(8, Position2D(-1, 0), "W");

        override val opposite: Cell.Side
            get() = when (this) {
                NORTH -> SOUTH
                SOUTH -> NORTH
                EAST -> WEST
                WEST -> EAST
            }

        companion object {
            /**
             * A flag indicating that there is a perpendiculer passage under this cell.
             */
            const val TUNNEL = 16

            val ALL = listOf(NORTH, EAST, SOUTH, WEST)
            const val ALL_VALUE = 15

            val VERTICAL_PASSAGE_VALUE = WEST.value or EAST.value
            val HORIZONTAL_PASSAGE_VALUE = NORTH.value or SOUTH.value
        }

    }

}
