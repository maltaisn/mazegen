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
import com.maltaisn.mazegen.maze.ZetaCell.Side
import com.maltaisn.mazegen.render.Canvas
import com.maltaisn.mazegen.render.Point
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.sqrt


/**
 * Class for a square-tiled orthogonal maze with [width] columns and [height] rows.
 * Like a [OrthogonalMaze] but cells allow passages at 45 degrees.
 */
class ZetaMaze(width: Int, height: Int) :
        BaseGridMaze<ZetaCell>(width, height) {

    override val grid: Array<Array<ZetaCell>>

    init {
        grid = Array(width) { x ->
            Array(height) { y ->
                ZetaCell(this, Position2D(x, y))
            }
        }
    }

    override fun drawTo(canvas: Canvas, style: Configuration.Style) {
        val ssize = style.cellSize  // Size of an octogon wall (side)
        val dsize = sqrt(2.0) / 2 * ssize  // Diagonal wall size
        val csize = 2 * dsize + ssize  // Full cell size

        canvas.init(width * csize + style.stroke.lineWidth,
                height * csize + style.stroke.lineWidth)

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
            for (x in 0 until width) {
                val px = x * csize
                val cx = px + csize / 2
                for (y in 0 until height) {
                    val py = y * csize
                    val cy = py + csize / 2
                    val hdsize = dsize / 2
                    val cell = cellAt(x, y)!!

                    // Create a polygon for the cell
                    // For each diagonal side, there are 3 cases resulting in different vertices
                    // for that corner. Vertices must be added in clockwise order to the polygon
                    // that's why northeast and southwest are differenciated from the other two.
                    val vertices = LinkedList<Point>()
                    for (side in cell.allSides) {
                        if (side.isDiagonal) {
                            val pos = side.relativePos
                            val positive = side == Side.NORTHEAST || side == Side.SOUTHWEST
                            when {
                                cell.hasDiagonalPassageOnSide(side) -> {
                                    // Case 1: there's a diagonal passage on this side.
                                    // Add 2 vertices for an octogon-like diagonal side.
                                    val p1 = Point(cx + pos.x * ssize / 2, cy + pos.y * csize / 2)
                                    val p2 = Point(cx + pos.x * csize / 2, cy + pos.y * ssize / 2)
                                    if (positive) {
                                        vertices += p1
                                        vertices += p2
                                    } else {
                                        vertices += p2
                                        vertices += p1
                                    }
                                }
                                !cell.hasSide(side) -> {
                                    // Case 2: the diagonal side opens on a diagonal passage.
                                    // Add 4 vertices forming an outgrown spanning half the passage.
                                    val p1 = Point(cx + pos.x * ssize / 2, cy + pos.y * csize / 2)
                                    val p2 = Point(cx + pos.x * (ssize / 2 + hdsize), cy + pos.y * (csize / 2 + hdsize))
                                    val p3 = Point(cx + pos.x * (csize / 2 + hdsize), cy + pos.y * (ssize / 2 + hdsize))
                                    val p4 = Point(cx + pos.x * csize / 2, cy + pos.y * ssize / 2)
                                    if (positive) {
                                        vertices += p1
                                        vertices += p2
                                        vertices += p3
                                        vertices += p4
                                    } else {
                                        vertices += p4
                                        vertices += p3
                                        vertices += p2
                                        vertices += p1
                                    }

                                }
                                else -> {
                                    // Case 3: there's a square corner, add a single corner vertex.
                                    vertices += Point(cx + pos.x * csize / 2, cy + pos.y * csize / 2)
                                }
                            }
                        }
                    }

                    canvas.color = colorMapColors[cell.colorMapDistance]
                    canvas.drawPolygon(vertices, true)
                }
            }
        }

        // Draw the maze
        // For each cell, draw west and north walls, as well as all 45 degrees lines.
        // On the last row and column, the east and south walls are also drawn.
        canvas.color = style.color
        canvas.stroke = style.stroke
        for (x in 0 until width) {
            val px = x * csize
            val cx = px + csize / 2
            for (y in 0 until height) {
                val py = y * csize
                val cy = py + csize / 2
                val cell = cellAt(x, y)!!

                val sides = HashMap<Side, Boolean>()
                val passages = HashMap<Side, Boolean>()
                for (side in cell.allSides) {
                    if (side.isDiagonal) {
                        val hasSide = cell.hasSide(side)
                        sides[side] = hasSide
                        val hasPassage = cell.hasDiagonalPassageOnSide(side)
                        passages[side] = hasPassage

                        if (hasSide && hasPassage) {
                            // Draw a diagonal line if there's a diagonal passage on this side
                            // of the cell and this cell has also a wall.
                            val pos = side.relativePos
                            canvas.drawLine(cx + pos.x * ssize / 2, cy + pos.y * csize / 2,
                                    cx + pos.x * csize / 2, cy + pos.y * ssize / 2)
                        }
                    }
                }

                // Draw the west and north walls
                // They must take account of the presence of diagonal passages.
                if (cell.hasSide(Side.WEST)) {
                    val start = if (!sides[Side.NORTHWEST]!!
                            || passages[Side.NORTHWEST]!!) dsize else 0.0
                    val end = csize - if (!sides[Side.SOUTHWEST]!!
                            || passages[Side.SOUTHWEST]!!) dsize else 0.0
                    canvas.drawLine(px, py + start, px, py + end)
                }
                if (cell.hasSide(Side.NORTH)) {
                    val start = if (!sides[Side.NORTHWEST]!!
                            || passages[Side.NORTHWEST]!!) dsize else 0.0
                    val end = csize - if (!sides[Side.NORTHEAST]!!
                            || passages[Side.NORTHEAST]!!) dsize else 0.0
                    canvas.drawLine(px + start, py, px + end, py)
                }

                // On the last row and column, also draw east and north walls
                if (x == width - 1 && cell.hasSide(Side.EAST)) {
                    canvas.drawLine(px + csize, py, px + csize, py + csize)
                }
                if (y == height - 1 && cell.hasSide(Side.SOUTH)) {
                    canvas.drawLine(px, py + csize, px + csize, py + csize)
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
                val px = (pos.x + 0.5) * csize
                val py = (pos.y + 0.5) * csize
                points.add(Point(px, py))
            }
            canvas.drawPolyline(points)
        }
    }

}
