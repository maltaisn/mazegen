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
import com.maltaisn.mazegen.ParameterException
import com.maltaisn.mazegen.render.Canvas
import com.maltaisn.mazegen.render.Point
import java.awt.Color
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.sign


/**
 * Class for a square-tiled orthogonal maze with [width] columns and [height] rows.
 * Like an [OrthogonalMaze] but allows passages over or under other passages.
 *
 * @property maxWeave the maximum number of rows or columns a passage can go over or under.
 */
class WeaveOrthogonalMaze(width: Int, height: Int, val maxWeave: Int) :
        BaseGridMaze<WeaveOrthogonalCell>(width, height) {

    override val grid: Array<Array<WeaveOrthogonalCell>>

    init {
        if (maxWeave < 0) {
            throw ParameterException("Max weave setting must be positive.")
        }
    }

    init {
        grid = Array(width) { x ->
            Array(height) { y ->
                WeaveOrthogonalCell(this, Position2D(x, y))
            }
        }
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
            canvas.drawRect(0.0, 0.0, canvas.width, canvas.height, true)
        }

        val offset = style.stroke.lineWidth / 2.0
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
                val cx = (pos.x + 0.5) * csive
                val cy = (pos.y + 0.5) * csive
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
            val cx = (x + 0.5) * csive
            for (y in 0 until height) {
                val cy = (y + 0.5) * csive
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
        return "[${super.toString()}, maxWeave: $maxWeave]"
    }

    companion object {
        const val INSET_SIZE_RATIO = 0.15
    }

}