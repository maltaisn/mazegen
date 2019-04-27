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

import com.maltaisn.mazegen.Configuration
import com.maltaisn.mazegen.maze.DeltaCell.Side
import com.maltaisn.mazegen.render.Canvas
import com.maltaisn.mazegen.render.Point
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.sqrt


/**
 * Class for a triangle-tiled maze of a [shape] with [width] rows and [height] columns.
 */
class DeltaMaze(width: Int, height: Int, shape: Shape) :
        BaseShapedMaze<DeltaCell>(width, height, shape) {

    override val grid: Array<Array<DeltaCell>>

    /**
     * The offset of the actual Y coordinate of a cell in the grid array, for each column
     * The cell at ```grid[x][y]```'s actual coordinates are `(x ; y + rowOffsets[x])`.
     */
    override val rowOffsets: IntArray


    init {
        var gridWith = width * 2 - 1
        val rowsForColumn: (column: Int) -> Int
        val rowOffset: (column: Int) -> Int
        when (shape) {
            Shape.RECTANGLE -> {
                rowsForColumn = { height }
                rowOffset = { 0 }
            }
            Shape.HEXAGON -> {
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
            Shape.TRIANGLE -> {
                rowOffset = { 0 }
                rowsForColumn = { width - (it - gridWith / 2).absoluteValue }
            }
            Shape.RHOMBUS -> {
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
                rowOffset = { max(0, height - gridWith + it) }
            }
        }
        rowOffsets = IntArray(gridWith)
        grid = Array(gridWith) { x ->
            rowOffsets[x] = rowOffset(x)
            Array(rowsForColumn(x)) { y ->
                DeltaCell(this, Position2D(x, y + rowOffsets[x]))
            }
        }
    }

    override fun drawTo(canvas: Canvas, style: Configuration.Style) {
        var maxHeight = 0
        for (x in 0 until grid.size) {
            val height = grid[x].size + rowOffsets[x]
            if (height > maxHeight) maxHeight = height
        }

        val csive = style.cellSize
        val cheight = sqrt(3.0) / 2 * csive
        canvas.init((grid.size / 2.0 + 0.5) * csive + style.stroke.lineWidth,
                maxHeight * cheight + style.stroke.lineWidth)

        // Draw the background
        if (style.backgroundColor != null) {
            canvas.color = style.backgroundColor
            canvas.drawRect(0.0, 0.0, canvas.width, canvas.height, true)
        }

        val offset = style.stroke.lineWidth / 2.0
        canvas.translate = Point(offset, offset)

        // Draw the color map
        if (hasColorMap) {
            val colorMapColors = style.generateColorMapColors(this)
            drawForEachCell { cell, x, y, flatTopped ->
                canvas.color = colorMapColors[cell.colorMapDistance]
                val vertices = if (flatTopped) {
                    listOf(Point(x * csive / 2, y * cheight),
                            Point((x + 2) * csive / 2, y * cheight),
                            Point((x + 1) * csive / 2, (y + 1) * cheight))
                } else {
                    listOf(Point(x * csive / 2, (y + 1) * cheight),
                            Point((x + 2) * csive / 2, (y + 1) * cheight),
                            Point((x + 1) * csive / 2, y * cheight))
                }
                canvas.drawPolygon(vertices, true)
            }
        }

        // Draw the maze
        canvas.color = style.color
        canvas.stroke = style.stroke
        drawForEachCell { cell, x, y, flatTopped ->
            if (cell.hasSide(Side.BASE)) {
                if (flatTopped) {
                    canvas.drawLine(x * csive / 2, y * cheight,
                            (x + 2) * csive / 2, y * cheight)
                } else if (cell.getCellOnSide(Side.BASE) == null) {
                    canvas.drawLine(x * csive / 2, (y + 1) * cheight,
                            (x + 2) * csive / 2, (y + 1) * cheight)
                }
            }
            if (cell.hasSide(Side.EAST)) {
                if (flatTopped) {
                    canvas.drawLine((x + 2) * csive / 2, y * cheight,
                            (x + 1) * csive / 2, (y + 1) * cheight)
                } else if (cell.getCellOnSide(Side.EAST) == null) {
                    canvas.drawLine((x + 1) * csive / 2, y * cheight,
                            (x + 2) * csive / 2, (y + 1) * cheight)
                }
            }
            if (cell.hasSide(Side.WEST)) {
                if (flatTopped) {
                    canvas.drawLine(x * csive / 2, y * cheight,
                            (x + 1) * csive / 2, (y + 1) * cheight)
                } else if (cell.getCellOnSide(Side.WEST) == null) {
                    canvas.drawLine((x + 1) * csive / 2, y * cheight,
                            x * csive / 2, (y + 1) * cheight)
                }
            }
        }

        // Draw the solution
        if (solution != null) {
            canvas.color = style.solutionColor
            canvas.stroke = style.solutionStroke

            val points = LinkedList<Point>()
            for (cell in solution!!) {
                val pos = cell.position as Position2D
                val flatTopped = (pos.x + pos.y) % 2 == 0
                val px = (pos.x + 1.0) * csive / 2.0
                val py = (pos.y + (if (flatTopped) 1 else 2) / 3.0) * cheight
                points.add(Point(px, py))
            }
            canvas.drawPolyline(points)
        }
    }

    /**
     * For each cell, call [draw] with the cell and some parameters.
     */
    private inline fun drawForEachCell(draw: (cell: DeltaCell, x: Int, y: Int, flatTopped: Boolean) -> Unit) {
        for (x in 0 until grid.size) {
            for (y in 0 until grid[x].size) {
                val actualY = y + rowOffsets[x]
                draw(grid[x][y], x, actualY, (x + actualY) % 2 == 0)
            }
        }
    }

}
