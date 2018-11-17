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

import com.maltaisn.maze.SVGRenderer
import com.maltaisn.maze.maze.DeltaCell.Side
import java.util.concurrent.ThreadLocalRandom


/**
 * Class for a triangle-tiled maze represented by 2D grid of [DeltaCell].
 */
class DeltaMaze : Maze {

    val width: Int
    val height: Int
    val arrangement: Arrangement

    private val grid: Array<Array<DeltaCell>>

    /**
     * The offset of the actual Y coordinate of a cell in the grid array, for each column
     * The cell at ```grid[x][y]```'s actual coordinates are `(x ; y + rowOffsets[x])`.
     */
    private val rowOffsets: IntArray

    /**
     * Create an empty delta maze with [width] columns and [height] rows shaped in [arrangement].
     */
    constructor(width: Int, height: Int, arrangement: Arrangement) {
        if (width < 1 || height < 1) {
            throw IllegalArgumentException("Dimensions must be at least 1.")
        }

        this.width = width
        this.arrangement = arrangement

        if (arrangement == Arrangement.TRIANGLE
                || arrangement == Arrangement.HEXAGON) {
            // Hexagon and triangle mazes have only one dimension parameter.
            this.height = width
        } else {
            this.height = height
        }

        // Create maze grid
        var gridWith = width * 2 - 1
        val rowsForColumn: (column: Int) -> Int
        val rowOffset: (column: Int) -> Int
        when (arrangement) {
            Arrangement.RECTANGLE -> {
                rowsForColumn = { height }
                rowOffset = { 0 }
            }
            Arrangement.HEXAGON -> {
                gridWith = 4 * width - 1
                rowsForColumn = {
                    2 * when {
                        it < width -> it + 1
                        it >= gridWith - width -> gridWith - it
                        else -> width
                    }
                }
                rowOffset = {
                    when {
                        it < width -> width - it - 1
                        it >= gridWith - width -> width + it - gridWith
                        else -> 0
                    } + width % 2
                }
            }
            Arrangement.TRIANGLE -> {
                rowOffset = { 0 }
                rowsForColumn = { width - Math.abs(it - gridWith / 2) }
            }
            Arrangement.RHOMBUS -> {
                gridWith = 2 * width + height - 1
                rowsForColumn = {
                    var rows = height
                    if (it < height) {
                        rows -= height - it - 1
                    }
                    if (it >= gridWith - height) {
                        rows -= it - (gridWith - height)
                    }
                    rows
                }
                rowOffset = { Math.max(0, height - gridWith + it) }
            }
        }

        rowOffsets = IntArray(gridWith)
        grid = Array(gridWith) { x ->
            rowOffsets[x] = rowOffset(x)
            Array(rowsForColumn(x)) { y ->
                DeltaCell(this, PositionXY(x, y + rowOffsets[x]), Side.NONE.value)
            }
        }
    }

    /**
     * Create a new delta maze with the same width and height, equal to [dimension]
     * Hexagon and triangle mazes should be created with this constructor.
     */
    constructor(dimension: Int, arrangement: Arrangement) :
            this(dimension, dimension, arrangement)

    private constructor(maze: DeltaMaze) {
        width = maze.width
        height = maze.height
        arrangement = maze.arrangement
        grid = Array(maze.grid.size) { maze.grid.get(it).clone() }
        rowOffsets = maze.rowOffsets.clone()
    }


    override fun cellAt(pos: Position): DeltaCell = if (pos is PositionXY) {
        cellAt(pos.x, pos.y)
    } else {
        throw IllegalArgumentException("Position has wrong type.")
    }

    private fun cellAt(x: Int, y: Int) = grid[x][y - rowOffsets[x]]

    override fun optionalCellAt(pos: Position): DeltaCell? {
        if (pos is PositionXY) {
            if (pos.x < 0 || pos.x >= grid.size) return null
            val actualY = pos.y - rowOffsets[pos.x]
            if (actualY < 0 || actualY >= grid[pos.x].size) return null
            return grid[pos.x][actualY]
        }
        throw IllegalArgumentException("Position has wrong type")
    }

    override fun getRandomCell(): DeltaCell {
        val random = ThreadLocalRandom.current()
        val x = random.nextInt(grid.size)
        return grid[x][random.nextInt(grid[x].size)]
    }

    override fun getCellCount(): Int {
        var count = 0
        for (x in 0 until grid.size) {
            count += grid[x].size
        }
        return count
    }

    override fun getAllCells(): LinkedHashSet<DeltaCell> {
        val set = LinkedHashSet<DeltaCell>(getCellCount())
        for (x in 0 until grid.size) {
            for (y in 0 until grid[x].size) {
                set.add(grid[x][y])
            }
        }
        return set
    }

    override fun createOpenings(vararg openings: Opening) {
        for (opening in openings) {
            val x = when (val pos = opening.position[0]) {
                Opening.POS_START -> 0
                Opening.POS_CENTER -> grid.size / 2
                Opening.POS_END -> grid.size - 1
                else -> pos
            }
            val y = when (val pos = opening.position[1]) {
                Opening.POS_START -> 0
                Opening.POS_CENTER -> grid[x].size / 2
                Opening.POS_END -> grid[x].size - 1
                else -> pos
            } + rowOffsets[x]

            val cell = optionalCellAt(PositionXY(x, y))
            if (cell != null) {
                for (side in cell.getAllSides()) {
                    if (cell.getCellOnSide(side) == null) {
                        cell.openSide(side)
                        break
                    }
                }
            } else {
                throw IllegalArgumentException("Opening describes no cell in the maze.")
            }
        }
    }

    override fun reset(empty: Boolean) {
        val value = if (empty) Side.NONE.value else Side.ALL.value
        for (x in 0 until grid.size) {
            for (y in 0 until grid[x].size) {
                val cell = grid[x][y]
                cell.visited = false
                cell.value = value
            }
        }
    }

    override fun copy(): Maze = DeltaMaze(this)

    override fun renderToSvg(): SVGRenderer {
        var maxHeight = 0
        for (x in 0 until grid.size) {
            val height = grid[x].size + rowOffsets[x]
            if (height > maxHeight) maxHeight = height
        }

        val cellHeight = Math.sqrt(3.0) / 2 * SVG_CELL_SIZE
        val width = Math.ceil((grid.size / 2.0 + 0.5) * SVG_CELL_SIZE)
        val height = Math.ceil(maxHeight * cellHeight)
        val svg = SVGRenderer(width.toInt(), height.toInt())
        svg.strokeColor = Maze.SVG_STROKE_COLOR
        svg.strokeWidth = Maze.SVG_STROKE_WIDTH

        for (x in 0 until grid.size) {
            for (y in 0 until grid[x].size) {
                val cell = grid[x][y]
                val actualY = y + rowOffsets[x]
                val flatTopped = (x + actualY) % 2 == 0
                if (cell.hasSide(Side.BASE)) {
                    if (flatTopped) {
                        svg.drawLine(x * SVG_CELL_SIZE / 2.0, actualY * cellHeight,
                                (x + 2) * SVG_CELL_SIZE / 2.0, actualY * cellHeight)
                    } else if (cell.getCellOnSide(Side.BASE) == null) {
                        svg.drawLine(x * SVG_CELL_SIZE / 2.0, (actualY + 1) * cellHeight,
                                (x + 2) * SVG_CELL_SIZE / 2.0, (actualY + 1) * cellHeight)
                    }
                }
                if (cell.hasSide(Side.EAST)) {
                    if (flatTopped) {
                        svg.drawLine((x + 2) * SVG_CELL_SIZE / 2.0, actualY * cellHeight,
                                (x + 1) * SVG_CELL_SIZE / 2.0, (actualY + 1) * cellHeight)
                    } else if (cell.getCellOnSide(Side.EAST) == null) {
                        svg.drawLine((x + 1) * SVG_CELL_SIZE / 2.0, actualY * cellHeight,
                                (x + 2) * SVG_CELL_SIZE / 2.0, (actualY + 1) * cellHeight)
                    }
                }
                if (cell.hasSide(Side.WEST)) {
                    if (flatTopped) {
                        svg.drawLine(x * SVG_CELL_SIZE / 2.0, actualY * cellHeight,
                                (x + 1) * SVG_CELL_SIZE / 2.0, (actualY + 1) * cellHeight)
                    } else if (cell.getCellOnSide(Side.WEST) == null) {
                        svg.drawLine((x + 1) * SVG_CELL_SIZE / 2.0, actualY * cellHeight,
                                x * SVG_CELL_SIZE / 2.0, (actualY + 1) * cellHeight)
                    }
                }
            }
        }

        return svg
    }

    override fun toString(): String {
        return "[arrangement: $arrangement, ${if (arrangement == Arrangement.TRIANGLE
                || arrangement == Arrangement.HEXAGON)
            "dimension : $width" else "width: $width, height: $height"}"
    }

    companion object {
        private const val SVG_CELL_SIZE = 10.0
    }

}