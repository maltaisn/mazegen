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
 * A cell for [PolarMaze], different than other cells because it can
 * sometimes have more than one neighbor on the outward side.
 * Has outward, inward, clockwise and counterclockwise sides.
 */
class PolarCell(maze: PolarMaze, position: PositionPolar) : Cell(maze, position) {

    override val neighbors: List<Cell> by lazy {
        val list = mutableListOf<Cell>()
        for (side in getAllSides()) {
            if (side == Side.OUT) {
                list.addAll(outwardCells)
            } else {
                val cell = getCellOnSide(side)
                if (cell != null) {
                    list.add(cell)
                }
            }
        }
        list.toList()
    }

    private val outwardCells: List<PolarCell> by lazy {
        maze.getOutwardCellsOf(this)
    }

    override fun getCellOnSide(side: Cell.Side): PolarCell? = when (side) {
        Side.OUT -> (maze as PolarMaze).getOutwardCellOf(this)
        Side.IN -> (maze as PolarMaze).getInwardCellOf(this)
        else -> {
            val cell = super.getCellOnSide(side)
            if (cell !== this) {
                cell as PolarCell?
            } else {
                // Can happens if trying to get clockwise neighbor of center cell for example.
                // Super method will try to get cellAt(PositionPolar(1, 0)) and the circular wrapping
                // will make it return the center cell again.
                null
            }
        }
    }

    override fun getAccessibleNeighbors(): MutableList<Cell> {
        val list = ArrayList<Cell>()
        for (side in getAllSides()) {
            if (side == Side.OUT) {
                // There can be many cells in this direction, check them all.
                for (neighbor in outwardCells) {
                    if (!neighbor.hasSide(Side.IN)) {
                        list.add(neighbor)
                    }
                }
            } else {
                if (!hasSide(side)) {
                    val neighbor = getCellOnSide(side)
                    if (neighbor != null) {
                        list.add(neighbor)
                    }
                }
            }
        }
        return list
    }

    override fun hasSide(side: Cell.Side): Boolean {
        if (side == Side.OUT && outwardCells.isNotEmpty()) {
            // Outward side value is unreliable if not on last row, manually check each outward cell.
            for (cell in outwardCells) {
                if (!cell.hasSide(Side.IN)) {
                    // One of the outward cells is connected to this cell, don't count as set.
                    return false
                }
            }
            return true
        }
        return super.hasSide(side)
    }

    override fun findSideOfCell(cell: Cell): Cell.Side? {
        if (cell.maze === maze) {
            for (side in getAllSides()) {
                if (side == Side.OUT) {
                    // There can be many cells in this direction, check them all.
                    for (outCell in outwardCells) {
                        if (outCell == cell) {
                            return Side.OUT
                        }
                    }
                } else if (getCellOnSide(side) == cell) {
                    return side
                }
            }
        }
        return null
    }

    override fun countSides(): Int {
        var count = Integer.bitCount(value and Side.OUT.value.inv())
        if (hasSide(Side.OUT)) count++
        return count
    }

    override fun getAllSides(): List<Side> = ALL_SIDES

    override fun getAllSideValue(): Side = Side.ALL

    /**
     * Enum class for the side a polar cell.
     */
    enum class Side(override val value: Int,
                    override val relativePos: PositionPolar? = null,
                    override val symbol: String?) : Cell.Side {

        /**
         * The outward side of a cell. This side has no meaning if there's more than
         * one cell on the side. [IN] side should be used instead.
         */
        OUT(1, null, "OUT"),
        IN(2, null, "IN"),
        CW(4, PositionPolar(-1, 0), "CW"),
        CCW(8, PositionPolar(1, 0), "CCW"),
        ALL(15, null, null);

        override fun opposite(): Side = when (this) {
            OUT -> IN
            IN -> OUT
            CW -> CCW
            CCW -> CW
            ALL -> ALL
        }

    }

    companion object {
        private val ALL_SIDES = listOf(Side.OUT, Side.IN, Side.CW, Side.CCW)
    }

}