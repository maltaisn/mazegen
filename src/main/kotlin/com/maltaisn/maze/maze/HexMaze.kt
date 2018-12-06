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

import com.maltaisn.maze.ParameterException
import com.maltaisn.maze.maze.HexCell.Side
import com.maltaisn.maze.render.Canvas
import com.maltaisn.maze.render.Point
import java.awt.BasicStroke
import java.awt.Color
import java.util.*
import kotlin.random.Random


/**
 * Class for a hexagon-tiled maze represented by 2D grid of [HexCell].
 * @param[width] number of rows
 * @param[height] number of columns
 * @param[arrangement] cell arrangement
 */
class HexMaze(val width: Int, height: Int,
              private val arrangement: Arrangement) : Maze() {

    val height: Int

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

    init {
        if (width < 1 || height < 1) {
            throw ParameterException("Dimensions must be at least 1.")
        }
        if (arrangement == Arrangement.TRIANGLE
                || arrangement == Arrangement.HEXAGON) {
            // Hexagon and triangle mazes have only one size parameter.
            this.height = width
        } else {
            this.height = height
        }
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
                HexCell(this, Position2D(x, y + rowOffsets[x]))
            }
        }
    }

    /**
     * Create a new maze with the same width and height, equal to [size]
     * Hexagon and triangle mazes should be created with this constructor.
     */
    constructor(size: Int, arrangement: Arrangement) :
            this(size, size, arrangement)


    override fun cellAt(pos: Position): HexCell? {
        val pos2d = pos as Position2D
        return cellAt(pos2d.x, pos2d.y)
    }

    fun cellAt(x: Int, y: Int): HexCell? {
        if (x < 0 || x >= grid.size) return null
        val actualY = y - rowOffsets[x]
        if (actualY < 0 || actualY >= grid[x].size) return null
        return grid[x][actualY]
    }

    override fun getRandomCell(): HexCell {
        val x = Random.nextInt(grid.size)
        return grid[x][Random.nextInt(grid[x].size)]
    }

    override fun getCellCount(): Int {
        var count = 0
        for (x in 0 until grid.size) {
            count += grid[x].size
        }
        return count
    }

    override fun getAllCells(): MutableList<HexCell> {
        val list = ArrayList<HexCell>(getCellCount())
        for (x in 0 until grid.size) {
            for (y in 0 until grid[x].size) {
                list.add(grid[x][y])
            }
        }
        return list
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
        return cellAt(x, y)
    }

    override fun drawTo(canvas: Canvas,
                        cellSize: Float, backgroundColor: Color?,
                        color: Color, stroke: BasicStroke,
                        solutionColor: Color, solutionStroke: BasicStroke) {
        // Find the empty top padding (minTop) and maximum column rows
        var maxRow = 0f
        var minTop = Float.MAX_VALUE
        for (x in 0 until grid.size) {
            val top = rowOffsets[x] + (grid.size - x - 1) / 2f
            if (top < minTop) minTop = top
            val row = grid[x].size + top
            if (row > maxRow) maxRow = row
        }
        maxRow -= minTop

        val cellHeight = (Math.sqrt(3.0) * cellSize).toFloat()
        canvas.init((1.5f * (grid.size - 1) + 2) * cellSize + stroke.lineWidth,
                cellHeight * maxRow + stroke.lineWidth)

        // Draw the background
        if (backgroundColor != null) {
            canvas.color = backgroundColor
            canvas.drawRect(0f, 0f, canvas.width, canvas.height, true)
        }

        // Draw the maze
        // Without going into details, only half the sides are drawn
        // for each cell except the bottommost and rightmost cells.
        val offset = stroke.lineWidth / 2
        canvas.translate = Point(offset, offset)
        canvas.color = color
        canvas.stroke = stroke
        var cx = cellSize
        for (x in 0 until grid.size) {
            var cy = (rowOffsets[x] - minTop + (grid.size - x - 1) / 2f + 0.5f) * cellHeight
            for (y in 0 until grid[x].size) {
                val cell = grid[x][y]

                // Draw north, northwest and southwest for every cell
                if (cell.hasSide(Side.NORTH)) {
                    canvas.drawLine(cx + cellSize / 2, cy - cellHeight / 2,
                            cx - cellSize / 2, cy - cellHeight / 2)
                }
                if (cell.hasSide(Side.NORTHWEST)) {
                    canvas.drawLine(cx - cellSize / 2, cy - cellHeight / 2,
                            cx - cellSize, cy)
                }
                if (cell.hasSide(Side.SOUTHWEST)) {
                    canvas.drawLine(cx - cellSize, cy,
                            cx - cellSize / 2, cy + cellHeight / 2)
                }

                // Only draw the remaining sides if there's no cell on the side
                if (cell.hasSide(Side.SOUTH)
                        && cell.getCellOnSide(Side.SOUTH) == null) {
                    canvas.drawLine(cx - cellSize / 2, cy + cellHeight / 2,
                            cx + cellSize / 2, cy + cellHeight / 2)
                }
                if (cell.hasSide(Side.SOUTHEAST)
                        && cell.getCellOnSide(Side.SOUTHEAST) == null) {
                    canvas.drawLine(cx + cellSize / 2, cy + cellHeight / 2,
                            cx + cellSize, cy)
                }
                if (cell.hasSide(Side.NORTHEAST)
                        && cell.getCellOnSide(Side.NORTHEAST) == null) {
                    canvas.drawLine(cx + cellSize, cy,
                            cx + cellSize / 2, cy - cellHeight / 2)
                }

                cy += cellHeight
            }
            cx += 1.5f * cellSize
        }

        // Draw the solution
        if (solution != null) {
            canvas.color = solutionColor
            canvas.stroke = solutionStroke

            val points = LinkedList<Point>()
            for (cell in solution!!) {
                val pos = cell.position as Position2D
                val px = (1.5f * pos.x + 1f) * cellSize
                val py = (pos.y - minTop + (grid.size - pos.x - 1) / 2f + 0.5f) * cellHeight
                points.add(Point(px, py))
            }
            canvas.drawPolyline(points)
        }
    }


    override fun toString(): String {
        return "[arrangement: $arrangement, ${if (arrangement == Arrangement.TRIANGLE
                || arrangement == Arrangement.HEXAGON)
            "size : $width" else "width: $width, height: $height"}]"
    }

}