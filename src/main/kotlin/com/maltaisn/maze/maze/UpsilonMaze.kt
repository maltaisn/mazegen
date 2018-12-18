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
import com.maltaisn.maze.maze.UpsilonCell.Side
import com.maltaisn.maze.render.Canvas
import com.maltaisn.maze.render.Point
import java.util.*
import kotlin.math.sqrt


/**
 * Class for a square and octogon-tiled orthogonal maze with [width] columns and [height] rows.
 */
class UpsilonMaze(width: Int, height: Int) :
        BaseGridMaze<UpsilonCell>(width, height) {

    override val grid: Array<Array<UpsilonCell>>

    init {
        grid = Array(width) { x ->
            Array(height) { y ->
                UpsilonCell(this, Position2D(x, y))
            }
        }
    }

    override fun drawTo(canvas: Canvas, style: Configuration.Style) {
        val csive = style.cellSize
        val dsize = sqrt(2f) / 2 * csive // Diagonal wall size
        val centerDistance = dsize + csive

        canvas.init(width * centerDistance + dsize + style.stroke.lineWidth,
                height * centerDistance + dsize + style.stroke.lineWidth)

        // Draw the background
        if (style.backgroundColor != null) {
            canvas.color = style.backgroundColor
            canvas.drawRect(0f, 0f, canvas.width, canvas.height, true)
        }

        // Draw the maze
        // For each square cell, only the north and west walls are drawn if they are set,
        // except for the first and last rows and columns where other sides may be drawn too.
        // For octogon cells, only draw north, northwest, west and southwest sides.
        val offset = style.stroke.lineWidth / 2
        canvas.translate = Point(offset, offset)
        canvas.color = style.color
        canvas.stroke = style.stroke
        for (x in 0..width) {
            val px = x * centerDistance + dsize
            for (y in -1..height) {
                val py = y * centerDistance + dsize
                val cell = cellAt(x, y)
                if ((x + y) % 2 != 0) {
                    // Square cell
                    if (cell != null && cell.hasSide(Side.NORTH) || cell == null
                            && cellAt(x, y - 1)?.hasSide(Side.SOUTH) == true) {
                        canvas.drawLine(px, py, px + csive, py)
                    }
                    if (cell != null && cell.hasSide(Side.WEST) || cell == null
                            && cellAt(x - 1, y)?.hasSide(Side.EAST) == true) {
                        canvas.drawLine(px, py, px, py + csive)
                    }
                } else {
                    // Octogon cell
                    if (cell != null && cell.hasSide(Side.NORTH) || cell == null
                            && cellAt(x, y - 1)?.hasSide(Side.SOUTH) == true) {
                        canvas.drawLine(px, py - dsize, px + csive, py - dsize)
                    }
                    if (cell != null && cell.hasSide(Side.NORTHWEST) || cell == null
                            && cellAt(x - 1, y - 1)?.hasSide(Side.SOUTHEAST) == true) {
                        canvas.drawLine(px - dsize, py, px, py - dsize)
                    }
                    if (cell != null && cell.hasSide(Side.WEST) || cell == null
                            && cellAt(x - 1, y)?.hasSide(Side.EAST) == true) {
                        canvas.drawLine(px - dsize, py, px - dsize, py + csive)
                    }
                    if (cell != null && cell.hasSide(Side.SOUTHWEST) || cell == null
                            && cellAt(x - 1, y + 1)?.hasSide(Side.NORTHEAST) == true) {
                        canvas.drawLine(px - dsize, py + csive, px, py + csive + dsize)
                    }
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
                val px = pos.x * centerDistance + dsize + csive / 2
                val py = pos.y * centerDistance + dsize + csive / 2
                points.add(Point(px, py))
            }
            canvas.drawPolyline(points)
        }
    }

}