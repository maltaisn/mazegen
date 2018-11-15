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

import org.jfree.graphics2d.svg.SVGGraphics2D
import java.awt.geom.GeneralPath
import java.util.concurrent.ThreadLocalRandom


/**
 * Class for a triangle-tiled maze represented by 2D grid of [DeltaCell].
 * @param[width] the number of columns
 * @param[height] the number of rows
 */
class DeltaMaze(val width: Int, height: Int, val arrangement: Arrangement) : Maze {

    val height: Int

    private val grid: Array<Array<DeltaCell>>

    /**
     * The offset of the actual Y coordinate of a cell in the grid array, for each column
     * The cell at ```grid[x][y]```'s actual coordinates are `(x ; y + rowOffsets[x])`.
     */
    private val rowOffsets: IntArray

    /**
     * Create a new delta maze with the same width and height, equal to [dimension]
     * Hexagon and triangle mazes should be created with this constructor.
     */
    constructor(dimension: Int, arrangement: Arrangement) :
            this(dimension, dimension, arrangement)

    init {
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
                    }
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
                DeltaCell(this, PositionXY(x, y + rowOffsets[x]), DeltaCell.Side.NONE.value)
            }
        }
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

    override fun reset(empty: Boolean) {
        val value = if (empty) DeltaCell.Side.NONE.value else DeltaCell.Side.ALL.value
        for (x in 0 until grid.size) {
            for (y in 0 until grid[x].size) {
                val cell = grid[x][y]
                cell.visited = false
                cell.value = value
            }
        }
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

    override fun renderToSvg(): String {
        var maxHeight = 0
        for (x in 0 until grid.size) {
            val height = grid[x].size + rowOffsets[x]
            if (height > maxHeight) maxHeight = height
        }

        val cellHeight = Math.sqrt(3.0) / 2 * SVG_CELL_SIZE
        val width = Math.ceil((grid.size / 2.0 + 0.5) * SVG_CELL_SIZE)
        val height = Math.ceil(maxHeight * cellHeight)
        val canvas = SVGGraphics2D(width.toInt(), height.toInt())
        canvas.stroke = Maze.SVG_STROKE_STYLE
        canvas.color = Maze.SVG_STROKE_COLOR

        val path = GeneralPath()

        for (x in 0 until grid.size) {
            for (y in 0 until grid[x].size) {
                val cell = grid[x][y]
                val actualY = y + rowOffsets[x]
                val flatTopped = (x + actualY) % 2 == 0
                if (cell.hasSide(DeltaCell.Side.BASE)) {
                    if (flatTopped) {
                        path.moveTo(x * SVG_CELL_SIZE / 2.0, actualY * cellHeight)
                        path.lineTo((x + 2) * SVG_CELL_SIZE / 2.0, actualY * cellHeight)
                    } else if (cell.getCellOnSide(DeltaCell.Side.BASE) == null) {
                        path.moveTo(x * SVG_CELL_SIZE / 2.0, (actualY + 1) * cellHeight)
                        path.lineTo((x + 2) * SVG_CELL_SIZE / 2.0, (actualY + 1) * cellHeight)
                    }
                }
                if (cell.hasSide(DeltaCell.Side.EAST)) {
                    if (flatTopped) {
                        path.moveTo((x + 2) * SVG_CELL_SIZE / 2.0, actualY * cellHeight)
                        path.lineTo((x + 1) * SVG_CELL_SIZE / 2.0, (actualY + 1) * cellHeight)
                    } else if (cell.getCellOnSide(DeltaCell.Side.EAST) == null) {
                        path.moveTo((x + 1) * SVG_CELL_SIZE / 2.0, actualY * cellHeight)
                        path.lineTo((x + 2) * SVG_CELL_SIZE / 2.0, (actualY + 1) * cellHeight)
                    }
                }
                if (cell.hasSide(DeltaCell.Side.WEST)) {
                    if (flatTopped) {
                        path.moveTo(x * SVG_CELL_SIZE / 2.0, actualY * cellHeight)
                        path.lineTo((x + 1) * SVG_CELL_SIZE / 2.0, (actualY + 1) * cellHeight)
                    } else if (cell.getCellOnSide(DeltaCell.Side.WEST) == null) {
                        path.moveTo((x + 1) * SVG_CELL_SIZE / 2.0, actualY * cellHeight)
                        path.lineTo(x * SVG_CELL_SIZE / 2.0, (actualY + 1) * cellHeight)
                    }
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
        private const val SVG_CELL_SIZE = 10.0
    }

}