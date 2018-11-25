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

import com.maltaisn.maze.maze.DeltaCell.Side
import com.maltaisn.maze.render.Canvas
import com.maltaisn.maze.render.Point
import java.awt.BasicStroke
import java.awt.Color
import java.util.*
import java.util.concurrent.ThreadLocalRandom


/**
 * Class for a triangle-tiled maze represented by 2D grid of [DeltaCell].
 * @param[width] number of rows
 * @param[height] number of columns
 * @param[arrangement] cell arrangement
 */
class DeltaMaze(val width: Int, height: Int,
                private val arrangement: Arrangement) : Maze() {

    val height: Int

    private val grid: Array<Array<DeltaCell>>

    /**
     * The offset of the actual Y coordinate of a cell in the grid array, for each column
     * The cell at ```grid[x][y]```'s actual coordinates are `(x ; y + rowOffsets[x])`.
     */
    private val rowOffsets: IntArray

    init {
        if (width < 1 || height < 1) {
            throw IllegalArgumentException("Dimensions must be at least 1.")
        }
        if (arrangement == Arrangement.TRIANGLE
                || arrangement == Arrangement.HEXAGON) {
            // Hexagon and triangle mazes have only one dimension parameter.
            this.height = width
        } else {
            this.height = height
        }
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
                DeltaCell(this, PositionXY(x, y + rowOffsets[x]))
            }
        }
    }

    /**
     * Create a new delta maze with the same width and height, equal to [dimension]
     * Hexagon and triangle mazes should be created with this constructor.
     */
    constructor(dimension: Int, arrangement: Arrangement) :
            this(dimension, dimension, arrangement)


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

    override fun forEachCell(action: (Cell) -> Unit) {
        for (x in 0 until grid.size) {
            for (y in 0 until grid[x].size) {
                action(grid[x][y])
            }
        }
    }

    override fun getOpeningCell(opening: Opening): Cell? {
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

        return optionalCellAt(PositionXY(x, y))
    }

    override fun drawTo(canvas: Canvas,
                        cellSize: Double, backgroundColor: Color?,
                        color: Color, stroke: BasicStroke,
                        solutionColor: Color, solutionStroke: BasicStroke) {
        var maxHeight = 0
        for (x in 0 until grid.size) {
            val height = grid[x].size + rowOffsets[x]
            if (height > maxHeight) maxHeight = height
        }

        val cellHeight = Math.sqrt(3.0) / 2 * cellSize
        canvas.init((grid.size / 2.0 + 0.5) * cellSize + stroke.lineWidth,
                maxHeight * cellHeight + stroke.lineWidth)

        // Draw the background
        if (backgroundColor != null) {
            canvas.color = backgroundColor
            canvas.drawRect(0.0, 0.0, canvas.width, canvas.height, true)
        }

        // Draw the maze
        val offset = stroke.lineWidth / 2.0
        canvas.translate(offset, offset)
        canvas.color = color
        canvas.stroke = stroke
        for (x in 0 until grid.size) {
            for (y in 0 until grid[x].size) {
                val cell = grid[x][y]
                val actualY = y + rowOffsets[x]
                val flatTopped = (x + actualY) % 2 == 0
                if (cell.hasSide(Side.BASE)) {
                    if (flatTopped) {
                        canvas.drawLine(x * cellSize / 2.0, actualY * cellHeight,
                                (x + 2) * cellSize / 2.0, actualY * cellHeight)
                    } else if (cell.getCellOnSide(Side.BASE) == null) {
                        canvas.drawLine(x * cellSize / 2.0, (actualY + 1) * cellHeight,
                                (x + 2) * cellSize / 2.0, (actualY + 1) * cellHeight)
                    }
                }
                if (cell.hasSide(Side.EAST)) {
                    if (flatTopped) {
                        canvas.drawLine((x + 2) * cellSize / 2.0, actualY * cellHeight,
                                (x + 1) * cellSize / 2.0, (actualY + 1) * cellHeight)
                    } else if (cell.getCellOnSide(Side.EAST) == null) {
                        canvas.drawLine((x + 1) * cellSize / 2.0, actualY * cellHeight,
                                (x + 2) * cellSize / 2.0, (actualY + 1) * cellHeight)
                    }
                }
                if (cell.hasSide(Side.WEST)) {
                    if (flatTopped) {
                        canvas.drawLine(x * cellSize / 2.0, actualY * cellHeight,
                                (x + 1) * cellSize / 2.0, (actualY + 1) * cellHeight)
                    } else if (cell.getCellOnSide(Side.WEST) == null) {
                        canvas.drawLine((x + 1) * cellSize / 2.0, actualY * cellHeight,
                                x * cellSize / 2.0, (actualY + 1) * cellHeight)
                    }
                }
            }
        }

        // Draw the solution
        if (solution != null) {
            canvas.color = solutionColor
            canvas.stroke = solutionStroke

            val points = LinkedList<Point>()
            for (cell in solution!!) {
                val pos = cell.position as PositionXY
                val flatTopped = (pos.x + pos.y) % 2 == 0
                val px = (pos.x + 1.0) * cellSize / 2.0
                val py = (pos.y + (if (flatTopped) 1 else 2) / 3.0) * cellHeight
                points.add(Point(px, py))
            }
            canvas.drawPolyline(points)
        }
    }


    override fun toString(): String {
        return "[arrangement: $arrangement, ${if (arrangement == Arrangement.TRIANGLE
                || arrangement == Arrangement.HEXAGON)
            "dimension : $width" else "width: $width, height: $height"}"
    }

}