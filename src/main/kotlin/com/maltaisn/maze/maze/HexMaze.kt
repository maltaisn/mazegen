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

import com.maltaisn.maze.maze.HexCell.Side
import org.jfree.graphics2d.svg.SVGGraphics2D
import java.awt.geom.GeneralPath
import java.util.concurrent.ThreadLocalRandom


/**
 * Class for a hexagon-tiled maze represented by 2D grid of [HexCell].
 */
class HexMaze : Maze {

    val width: Int
    val height: Int
    val arrangement: Arrangement

    /**
     * Hexagonal maze grid. There number of columns is the same as the maze width, except for
     * hexagonal shaped mazes where the number of columns is equal to `width * 2 - 1`.
     * The number of rows varies for each column depending on the arrangement of the maze.
     */
    private val grid: Array<Array<HexCell>>

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
        var gridWith = width
        val rowsForColumn: (column: Int) -> Int
        val rowOffset: (column: Int) -> Int
        when (arrangement) {
            Arrangement.RECTANGLE -> {
                rowsForColumn = { height }
                rowOffset = { it / 2 }
            }
            Arrangement.HEXAGON -> {
                gridWith = 2 * width - 1
                rowsForColumn = { gridWith - Math.abs(it - width + 1) }
                rowOffset = { if (it < width) 0 else it - width + 1 }
            }
            Arrangement.TRIANGLE -> {
                rowsForColumn = { it + 1 }
                rowOffset = { 0 }
            }
            Arrangement.RHOMBUS -> {
                rowsForColumn = { height }
                rowOffset = { 0 }
            }
        }

        rowOffsets = IntArray(gridWith)
        grid = Array(gridWith) { x ->
            rowOffsets[x] = rowOffset(x)
            Array(rowsForColumn(x)) { y ->
                HexCell(this, PositionXY(x, y + rowOffsets[x]), Side.NONE.value)
            }
        }
    }

    /**
     * Create a new hexagonal maze with the same width and height, equal to [dimension]
     * Hexagon and triangle mazes should be created with this constructor.
     */
    constructor(dimension: Int, arrangement: Arrangement) :
            this(dimension, dimension, arrangement)

    private constructor(maze: HexMaze) {
        width = maze.width
        height = maze.height
        arrangement = maze.arrangement
        grid = Array(maze.grid.size) { maze.grid.get(it).clone() }
        rowOffsets = maze.rowOffsets.clone()
    }

    override fun cellAt(pos: Position): HexCell = if (pos is PositionXY) {
        grid[pos.x][pos.y - rowOffsets[pos.x]]
    } else {
        throw IllegalArgumentException("Position has wrong type.")
    }

    override fun optionalCellAt(pos: Position): HexCell? {
        if (pos is PositionXY) {
            if (pos.x < 0 || pos.x >= grid.size) return null
            val actualY = pos.y - rowOffsets[pos.x]
            if (actualY < 0 || actualY >= grid[pos.x].size) return null
            return grid[pos.x][actualY]
        }
        throw IllegalArgumentException("Position has wrong type")
    }

    override fun getRandomCell(): HexCell {
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

    override fun getAllCells(): LinkedHashSet<HexCell> {
        val set = LinkedHashSet<HexCell>(getCellCount())
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

    override fun copy(): Maze = HexMaze(this)

    /**
     * Render the maze to SVG.
     * Without going into details, only half the sides are drawn for each cell except
     * the bottommost and rightmost cells.
     */
    override fun renderToSvg(): String {
        // Find the empty top padding (minTop) and maximum column rows
        var maxRow = 0.0
        var minTop = Double.MAX_VALUE
        for (x in 0 until grid.size) {
            val top = rowOffsets[x] + (grid.size - x - 1) / 2.0
            if (top < minTop) minTop = top
            val row = grid[x].size + top
            if (row > maxRow) maxRow = row
        }
        maxRow -= minTop

        val cellHeight = Math.sqrt(3.0) * SVG_CELL_SIDE
        val width = Math.ceil((1.5 * (grid.size - 1) + 2) * SVG_CELL_SIDE).toInt()
        val height = Math.ceil(cellHeight * maxRow).toInt()
        val canvas = SVGGraphics2D(width, height)
        canvas.stroke = Maze.SVG_STROKE_STYLE
        canvas.color = Maze.SVG_STROKE_COLOR

        val path = GeneralPath()
        for (x in 0 until grid.size) {
            val cx = (1.5 * x + 1) * SVG_CELL_SIDE
            for (y in 0 until grid[x].size) {
                val cy = (y + rowOffsets[x] - minTop + (grid.size - x - 1) / 2.0 + 0.5) * cellHeight
                val cell = grid[x][y]

                // Draw north, northwest and southwest for every cell
                if (cell.hasSide(Side.NORTH)) {
                    path.moveTo(cx + SVG_CELL_SIDE / 2, cy - cellHeight / 2)
                    path.lineTo(cx - SVG_CELL_SIDE / 2, cy - cellHeight / 2)
                }
                if (cell.hasSide(Side.NORTHWEST)) {
                    path.moveTo(cx - SVG_CELL_SIDE / 2, cy - cellHeight / 2)
                    path.lineTo(cx - SVG_CELL_SIDE, cy)
                }
                if (cell.hasSide(Side.SOUTHWEST)) {
                    path.moveTo(cx - SVG_CELL_SIDE, cy)
                    path.lineTo(cx - SVG_CELL_SIDE / 2, cy + cellHeight / 2)
                }

                // Only draw the remaining sides if there's no cell on the side
                if (cell.hasSide(Side.SOUTH)
                        && cell.getCellOnSide(Side.SOUTH) == null) {
                    path.moveTo(cx - SVG_CELL_SIDE / 2, cy + cellHeight / 2)
                    path.lineTo(cx + SVG_CELL_SIDE / 2, cy + cellHeight / 2)
                }
                if (cell.hasSide(Side.SOUTHEAST)
                        && cell.getCellOnSide(Side.SOUTHEAST) == null) {
                    path.moveTo(cx + SVG_CELL_SIDE / 2, cy + cellHeight / 2)
                    path.lineTo(cx + SVG_CELL_SIDE, cy)
                }
                if (cell.hasSide(Side.NORTHEAST)
                        && cell.getCellOnSide(Side.NORTHEAST) == null) {
                    path.moveTo(cx + SVG_CELL_SIDE, cy)
                    path.lineTo(cx + SVG_CELL_SIDE / 2, cy - cellHeight / 2)
                }
            }
        }

        if (path.currentPoint != null) {
            path.closePath()
            canvas.draw(path)
        }

        return canvas.svgDocument
    }

    override fun toString(): String {
        return "[arrangement: $arrangement, ${if (arrangement == Arrangement.TRIANGLE
                || arrangement == Arrangement.HEXAGON)
            "dimension : $width" else "width: $width, height: $height"}"
    }

    companion object {
        private const val SVG_CELL_SIDE = 10.0
    }

}