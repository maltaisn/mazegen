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

import com.maltaisn.maze.Configuration
import com.maltaisn.maze.MazeType
import com.maltaisn.maze.ParameterException
import com.maltaisn.maze.maze.ZetaCell.Side
import com.maltaisn.maze.render.Canvas
import com.maltaisn.maze.render.Point
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.sqrt
import kotlin.random.Random


/**
 * Class for a square-tiled maze represented by 2D grid of [ZetaCell].
 * Like a [OrthogonalMaze] but allows passage at 45 degrees.
 * Create an empty maze with [width] columns and [height] rows.
 */
class ZetaMaze(val width: Int, val height: Int) : Maze(MazeType.ZETA) {

    private val grid: Array<Array<ZetaCell>>

    init {
        if (width < 1 || height < 1) {
            throw ParameterException("Dimensions must be at least 1.")
        }
        grid = Array(width) { x ->
            Array(height) { y ->
                ZetaCell(this, Position2D(x, y))
            }
        }
    }


    override fun cellAt(pos: Position) =
            cellAt((pos as Position2D).x, pos.y)

    fun cellAt(x: Int, y: Int): ZetaCell? {
        if (x < 0 || x >= width || y < 0 || y >= height) return null
        return grid[x][y]
    }

    override fun getRandomCell(): ZetaCell {
        return grid[Random.nextInt(width)][Random.nextInt(height)]
    }

    override fun getCellCount(): Int = width * height

    override fun getAllCells(): MutableList<ZetaCell> {
        val set = ArrayList<ZetaCell>(width * height)
        for (x in 0 until width) {
            for (y in 0 until height) {
                set.add(grid[x][y])
            }
        }
        return set
    }

    override fun forEachCell(action: (Cell) -> Unit) {
        for (x in 0 until width) {
            for (y in 0 until height) {
                action(grid[x][y])
            }
        }
    }

    override fun getOpeningCell(opening: Opening): Cell? {
        val x = when (val pos = opening.position[0]) {
            Opening.POS_START -> 0
            Opening.POS_CENTER -> width / 2
            Opening.POS_END -> width - 1
            else -> pos
        }
        val y = when (val pos = opening.position[1]) {
            Opening.POS_START -> 0
            Opening.POS_CENTER -> height / 2
            Opening.POS_END -> height - 1
            else -> pos
        }
        return cellAt(x, y)
    }

    override fun drawTo(canvas: Canvas, style: Configuration.Style) {
        val sideSize = style.cellSize  // Size of an octogon wall
        val diagSize = sqrt(2f) / 2 * sideSize  // Diagonal wall size
        val cellSize = 2 * diagSize + sideSize  // Full cell size

        canvas.init(width * cellSize + style.stroke.lineWidth,
                height * cellSize + style.stroke.lineWidth)

        // Draw the background
        if (style.backgroundColor != null) {
            canvas.color = style.backgroundColor
            canvas.drawRect(0f, 0f, canvas.width, canvas.height, true)
        }

        // Draw the maze
        // For each cell, draw west and north walls, as well as all 45 degrees lines.
        // On the last row and column, the east and south walls are also drawn.
        val offset = style.stroke.lineWidth / 2
        canvas.translate = Point(offset, offset)
        canvas.color = style.color
        canvas.stroke = style.stroke
        for (x in 0 until width) {
            val px = x * cellSize
            val cx = px + cellSize / 2
            for (y in 0 until height) {
                val py = y * cellSize
                val cy = py + cellSize / 2
                val cell = cellAt(x, y)!!

                val sides = HashMap<Side, Boolean>()
                val passages = HashMap<Side, Boolean>()
                for (side in cell.getAllSides()) {
                    if (side.isDiagonal) {
                        val hasSide = cell.hasSide(side)
                        sides[side] = hasSide
                        val hasPassage = cell.hasDiagonalPassageOnSide(side)
                        passages[side] = hasPassage

                        if (hasSide && hasPassage) {
                            // Draw a diagonal line if there's a diagonal passage on this side
                            // of the cell and this cell has also a wall.
                            val pos = side.relativePos
                            canvas.drawLine(cx + pos.x * sideSize / 2, cy + pos.y * cellSize / 2,
                                    cx + pos.x * cellSize / 2, cy + pos.y * sideSize / 2)
                        }
                    }
                }

                // Draw the west and north walls
                // They must take account of the presence of diagonal passages.
                if (cell.hasSide(Side.WEST)) {
                    val start = if (!sides[Side.NORTHWEST]!!
                            || passages[Side.NORTHWEST]!!) diagSize else 0f
                    val end = cellSize - if (!sides[Side.SOUTHWEST]!!
                            || passages[Side.SOUTHWEST]!!) diagSize else 0f
                    canvas.drawLine(px, py + start, px, py + end)
                }
                if (cell.hasSide(Side.NORTH)) {
                    val start = if (!sides[Side.NORTHWEST]!!
                            || passages[Side.NORTHWEST]!!) diagSize else 0f
                    val end = cellSize - if (!sides[Side.NORTHEAST]!!
                            || passages[Side.NORTHEAST]!!) diagSize else 0f
                    canvas.drawLine(px + start, py, px + end, py)
                }

                // On the last row and column, also draw east and north walls
                if (x == width - 1 && cell.hasSide(Side.EAST)) {
                    canvas.drawLine(px + cellSize, py, px + cellSize, py + cellSize)
                }
                if (y == height - 1 && cell.hasSide(Side.SOUTH)) {
                    canvas.drawLine(px, py + cellSize, px + cellSize, py + cellSize)
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
                val px = (pos.x + 0.5f) * cellSize
                val py = (pos.y + 0.5f) * cellSize
                points.add(Point(px, py))
            }
            canvas.drawPolyline(points)
        }
    }


    override fun toString(): String {
        return "[width: $width, height: $height]"
    }

}