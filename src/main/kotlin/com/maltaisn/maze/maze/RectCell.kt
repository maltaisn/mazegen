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
 * A square cell for [RectCell].
 * Has north, east, south and east sides.
 */
class RectCell : Cell {

    constructor(maze: RectMaze, position: PositionXY) : super(maze, position)

    constructor(maze: RectMaze, position: PositionXY, value: Int) : super(maze, position, value)

    constructor(maze: Maze, cell: Cell) : super(maze, cell)


    override fun getAllSides(): List<Side> = ALL_SIDES

    override fun getAllSideValue(): Side = Side.ALL

    override fun copy(maze: Maze) = RectCell(maze, this)

    /**
     * Enum class for the side a rectangular cell
     */
    enum class Side(override val value: Int,
                    override val relativePos: PositionXY?,
                    override val symbol: String?) : Cell.Side {
        NONE(0, null, null),
        NORTH(1, PositionXY(0, -1), "N"),
        EAST(2, PositionXY(1, 0), "E"),
        SOUTH(4, PositionXY(0, 1), "S"),
        WEST(8, PositionXY(-1, 0), "W"),
        ALL(15, null, null);

        override fun opposite(): Side = when (this) {
            NONE -> NONE
            NORTH -> SOUTH
            SOUTH -> NORTH
            EAST -> WEST
            WEST -> EAST
            ALL -> ALL
        }

    }

    companion object {
        private val ALL_SIDES = listOf(Side.NORTH, Side.SOUTH, Side.WEST, Side.EAST)
    }

}