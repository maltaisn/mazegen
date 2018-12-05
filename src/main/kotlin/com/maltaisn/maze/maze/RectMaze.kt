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

import com.maltaisn.maze.maze.RectCell.Side
import com.maltaisn.maze.render.Canvas
import com.maltaisn.maze.render.Point
import java.awt.BasicStroke
import java.awt.Color
import java.util.*
import kotlin.random.Random


/**
 * Class for a square-tiled maze represented by 2D grid of [RectCell].
 * Create an empty maze with [width] columns and [height] rows.
 */
class RectMaze(val width: Int, val height: Int) : Maze() {

    private val grid: Array<Array<RectCell>>

    init {
        if (width < 1 || height < 1) {
            throw IllegalArgumentException("Dimensions must be at least 1.")
        }
        grid = Array(width) { x ->
            Array(height) { y ->
                RectCell(this, Position2D(x, y))
            }
        }
    }


    override fun cellAt(pos: Position): RectCell? {
        if (pos is Position2D) {
            if (pos.x < 0 || pos.x >= width || pos.y < 0 || pos.y >= height) return null
            return grid[pos.x][pos.y]
        }
        return null
    }

    override fun getRandomCell(): RectCell {
        return grid[Random.nextInt(width)][Random.nextInt(height)]
    }

    override fun getCellCount(): Int = width * height

    override fun getAllCells(): MutableList<RectCell> {
        val set = ArrayList<RectCell>(width * height)
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
        return grid[x][y]
    }

    override fun drawTo(canvas: Canvas,
                        cellSize: Float, backgroundColor: Color?,
                        color: Color, stroke: BasicStroke,
                        solutionColor: Color, solutionStroke: BasicStroke) {
        canvas.init(width * cellSize + stroke.lineWidth,
                height * cellSize + stroke.lineWidth)

        // Draw the background
        if (backgroundColor != null) {
            canvas.color = backgroundColor
            canvas.drawRect(0f, 0f, canvas.width, canvas.height, true)
        }

        // Draw the maze
        // For each cell, only the north and east sides are drawn if they are set,
        // except for the last row and column where to south and east side are also drawn.
        val offset = stroke.lineWidth / 2
        canvas.translate = Point(offset, offset)
        canvas.color = color
        canvas.stroke = stroke
        for (x in 0..width) {
            val px = x * cellSize
            for (y in 0..height) {
                val py = y * cellSize
                val cell = cellAt(Position2D(x, y))
                if (cell != null && cell.hasSide(Side.NORTH) || cell == null
                        && cellAt(Position2D(x, y - 1))?.hasSide(Side.SOUTH) == true) {
                    canvas.drawLine(px, py, px + cellSize, py)
                }
                if (cell != null && cell.hasSide(Side.WEST) || cell == null
                        && cellAt(Position2D(x - 1, y))?.hasSide(Side.EAST) == true) {
                    canvas.drawLine(px, py, px, py + cellSize)
                }
            }
        }

        // Draw the solution
        if (solution != null) {
            canvas.color = solutionColor
            canvas.stroke = solutionStroke

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