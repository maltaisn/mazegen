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
import com.maltaisn.maze.render.Canvas
import com.maltaisn.maze.render.Point
import java.awt.Color
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.sign
import kotlin.random.Random


/**
 * Class for a square-tiled maze represented by 2D grid of [WeaveOrthogonalCell].
 * Like a [OrthogonalMaze] but allows passages over other passages.
 * Create an empty maze with [width] columns and [height] rows.
 *
 * @property maxWeave the maximum number of rows or columns a passage can go over or under.
 */
class WeaveOrthogonalMaze(val width: Int, val height: Int,
                          val maxWeave: Int) : Maze(MazeType.WEAVE_ORTHOGONAL) {

    private val grid: Array<Array<WeaveOrthogonalCell>>

    init {
        if (width < 1 || height < 1) {
            throw ParameterException("Dimensions must be at least 1.")
        } else if (maxWeave < 0) {
            throw ParameterException("Max weave setting must be positive.")
        }

        grid = Array(width) { x ->
            Array(height) { y ->
                WeaveOrthogonalCell(this, Position2D(x, y))
            }
        }
    }


    override fun cellAt(pos: Position) =
            cellAt((pos as Position2D).x, pos.y)

    fun cellAt(x: Int, y: Int): WeaveOrthogonalCell? {
        if (x < 0 || x >= width || y < 0 || y >= height) return null
        return grid[x][y]
    }

    override fun getRandomCell(): WeaveOrthogonalCell {
        return grid[Random.nextInt(width)][Random.nextInt(height)]
    }

    override fun getCellCount(): Int = width * height

    override fun getAllCells(): MutableList<WeaveOrthogonalCell> {
        val set = ArrayList<WeaveOrthogonalCell>(width * height)
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
        val csive = style.cellSize
        val outSize = csive * INSET_SIZE_RATIO
        val inSize = csive / 2 - outSize
        canvas.init(width * csive + style.stroke.lineWidth,
                height * csive + style.stroke.lineWidth)

        // Draw the background
        if (style.backgroundColor != null) {
            canvas.color = style.backgroundColor
            canvas.drawRect(0f, 0f, canvas.width, canvas.height, true)
        }

        val offset = style.stroke.lineWidth / 2
        canvas.translate = Point(offset, offset)

        // Draw the solution
        if (solution != null) {
            canvas.color = style.solutionColor
            canvas.stroke = style.solutionStroke

            val solution = solution!!
            var prevPoint: Point? = null
            for (i in 0 until solution.size) {
                canvas.color = Color.BLUE

                val cell = solution[i]
                val pos = cell.position as Position2D
                val cx = (pos.x + 0.5f) * csive
                val cy = (pos.y + 0.5f) * csive
                if (prevPoint != null) {
                    canvas.drawLine(prevPoint.x, prevPoint.y, cx, cy)
                }

                val nextCell = solution.getOrNull(i + 1)
                if (nextCell != null) {
                    val nextPos = nextCell.position as Position2D
                    val dx = nextPos.x - pos.x
                    val dy = nextPos.y - pos.y
                    var nextPoint = Point(cx + dx.sign * csive / 2, cy + dy.sign * csive / 2)
                    canvas.drawLine(cx, cy, nextPoint.x, nextPoint.y)
                    prevPoint = nextPoint

                    if (dx.absoluteValue > 1 || dy.absoluteValue > 1) {
                        // The solution goes in a tunnel
                        val dxs = dx.sign
                        val dys = dy.sign
                        for (j in 0 until max(dx.absoluteValue, dy.absoluteValue) - 1) {
                            canvas.drawLine(prevPoint!!.x, prevPoint.y,
                                    prevPoint.x + dxs * outSize, prevPoint.y + dys * outSize)
                            nextPoint = Point(prevPoint.x + dxs * csive, prevPoint.y + dys * csive)
                            canvas.drawLine(prevPoint.x + dxs * (csive - outSize),
                                    prevPoint.y + dys * (csive - outSize),
                                    nextPoint.x, nextPoint.y)
                            prevPoint = nextPoint
                        }
                    }
                }
            }
        }

        // Draw the maze
        canvas.color = style.color
        canvas.stroke = style.stroke
        for (x in 0 until width) {
            val cx = (x + 0.5f) * csive
            for (y in 0 until height) {
                val cy = (y + 0.5f) * csive
                val cell = cellAt(x, y)!!
                for (side in cell.getAllSides()) {
                    val hasSide = cell.hasSide(side)
                    val pos = side.relativePos
                    val ex = 1 - pos.x.absoluteValue
                    val ey = 1 - pos.y.absoluteValue
                    if (hasSide) {
                        canvas.drawLine(cx + (pos.x - ex) * inSize,
                                cy + (pos.y - ey) * inSize,
                                cx + (pos.x + ex) * inSize,
                                cy + (pos.y + ey) * inSize)
                    }
                    if (cell.hasTunnel() || !hasSide) {
                        canvas.drawLine(cx + (pos.x - ex) * inSize,
                                cy + (pos.y - ey) * inSize,
                                cx + (pos.x - ex) * (inSize + ey * outSize),
                                cy + (pos.y - ey) * (inSize + ex * outSize))
                        canvas.drawLine(cx + (pos.x + ex) * inSize,
                                cy + (pos.y + ey) * inSize,
                                cx + (pos.x + ex) * (inSize + ey * outSize),
                                cy + (pos.y + ey) * (inSize + ex * outSize))
                    }
                }
            }
        }
    }

    override fun toString(): String {
        return "[width: $width, height: $height, maxWeave: $maxWeave]"
    }

    companion object {
        const val INSET_SIZE_RATIO = 0.15f
    }

}