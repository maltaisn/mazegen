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
import com.maltaisn.maze.render.Canvas
import com.maltaisn.maze.render.Point
import java.util.*


/**
 * Class for a normal square-tiled orthogonal maze with [width] columns and [height] rows.
 */
open class OrthogonalMaze(width: Int, height: Int) :
        BaseGridMaze<OrthogonalCell>(width, height) {

    final override val grid: Array<Array<OrthogonalCell>>

    init {
        grid = Array(width) { x ->
            Array(height) { y ->
                OrthogonalCell(this, Position2D(x, y))
            }
        }
    }

    override fun drawTo(canvas: Canvas, style: Configuration.Style) {
        val w = grid.size
        val h = grid[0].size
        val csive = style.cellSize
        canvas.init(w * csive + style.stroke.lineWidth,
                h * csive + style.stroke.lineWidth)

        // Draw the background
        if (style.backgroundColor != null) {
            canvas.color = style.backgroundColor
            canvas.drawRect(0f, 0f, canvas.width, canvas.height, true)
        }

        // Draw the maze
        // For each cell, only the north and west walls are drawn if they are set,
        // except for the last row and column where to south and east walls are also drawn.
        val offset = style.stroke.lineWidth / 2
        canvas.translate = Point(offset, offset)
        canvas.color = style.color
        canvas.stroke = style.stroke
        for (x in 0..w) {
            val px = x * csive
            for (y in 0..h) {
                val py = y * csive
                val cell = cellAt(x, y)
                if (cell != null && cell.hasSide(OrthogonalCell.Side.NORTH) || cell == null
                        && cellAt(x, y - 1)?.hasSide(OrthogonalCell.Side.SOUTH) == true) {
                    canvas.drawLine(px, py, px + csive, py)
                }
                if (cell != null && cell.hasSide(OrthogonalCell.Side.WEST) || cell == null
                        && cellAt(x - 1, y)?.hasSide(OrthogonalCell.Side.EAST) == true) {
                    canvas.drawLine(px, py, px, py + csive)
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
                val px = (pos.x + 0.5f) * csive
                val py = (pos.y + 0.5f) * csive
                points.add(Point(px, py))
            }
            canvas.drawPolyline(points)
        }
    }

}