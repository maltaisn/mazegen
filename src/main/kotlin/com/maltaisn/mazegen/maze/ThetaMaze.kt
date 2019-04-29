/*
 * Copyright (c) 2019 Nicolas Maltais
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
import com.maltaisn.mazegen.maze.ThetaCell.Side
import com.maltaisn.mazegen.paramError
import com.maltaisn.mazegen.render.ArcPoint
import com.maltaisn.mazegen.render.Canvas
import com.maltaisn.mazegen.render.Point
import kotlin.math.*
import kotlin.random.Random


/**
 * Class for a circle shaped maze represented by 2D grid of [ThetaCell].
 * @property radius number of rows in the radius, minimum is 1.
 * @property centerRadius the radius of the center cell.
 * @param subdivisionFactor when a cell has become this many times the base size,
 * it is subdivided into multiple cells.
 */
class ThetaMaze(private val radius: Int, private val centerRadius: Double = 1.0,
                subdivisionFactor: Double = 1.5) : Maze() {

    private val grid: Array<Array<ThetaCell>>

    init {
        when {
            radius < 1 -> paramError("Radius must be at least 1.")
            centerRadius <= 0 -> paramError("Center radius must be greater than 0.")
            subdivisionFactor <= 0 -> paramError("Subdivision factor must be greater than 0.")
        }

        // Create the grid
        val rows = mutableListOf<Array<ThetaCell>>()
        var lastWidth = 1.0
        for (r in 0 until radius) {
            var width = if (r == 0) 0.0 else (r + centerRadius - 1) * TAU
            if (width < lastWidth * subdivisionFactor) {
                // Only subdivide when circumference is a certain times greater than last.
                width = lastWidth
            } else {
                // Subdivide: row should have at least twice as many cells as last row
                // and be a multiple of the number of cells in the last row.
                width = max(width - width % lastWidth, lastWidth * 2)
                lastWidth = width
            }
            val rowWidth = width.toInt()
            rows.add(Array(rowWidth) { x -> ThetaCell(this, PositionPolar(x, r, rowWidth)) })
        }
        grid = rows.toTypedArray()
    }

    override val cellCount: Int by lazy {
        var count = 0
        for (x in 0 until grid.size) {
            count += grid[x].size
        }
        count
    }

    override fun cellAt(pos: Position) =
            cellAt((pos as PositionPolar).x, pos.y)

    fun cellAt(x: Int, r: Int): ThetaCell? {
        if (r < 0 || r >= grid.size) return null
        return grid[r][Math.floorMod(x, grid[r].size)]
    }

    override fun getRandomCell(): Cell {
        val r = Random.nextInt(grid.size)
        return grid[r][Random.nextInt(grid[r].size)]
    }

    override fun getAllCells(): MutableList<ThetaCell> {
        val list = mutableListOf<ThetaCell>()
        for (r in 0 until grid.size) {
            for (x in 0 until grid[r].size) {
                list.add(grid[r][x])
            }
        }
        return list
    }

    override fun forEachCell(action: (Cell) -> Unit) {
        for (r in 0 until grid.size) {
            for (x in 0 until grid[r].size) {
                action(grid[r][x])
            }
        }
    }

    override fun getOpeningCell(opening: Position): Cell? {
        val r = when (val pos = (opening as Position2D).y) {
            OPENING_POS_START -> 0
            OPENING_POS_CENTER -> grid.size / 2
            OPENING_POS_END -> grid.size - 1
            else -> pos
        }
        val x = when (val pos = opening.x) {
            OPENING_POS_START -> 0
            OPENING_POS_CENTER -> grid[r].size / 2
            OPENING_POS_END -> grid[r].size - 1
            else -> pos
        }
        return cellAt(x, r)
    }

    /**
     * Get the cell in the inward direction of [cell].
     */
    fun getInwardCellOf(cell: Cell): ThetaCell? {
        val pos = cell.position as PositionPolar
        if (pos.y == 0) return null
        val lastRow = grid[pos.y - 1]
        return lastRow[pos.x * lastRow.size / grid[pos.y].size]
    }

    /**
     * Get the first cell found in the outward direction of [cell].
     */
    fun getOutwardCellOf(cell: Cell): ThetaCell? {
        val pos = cell.position as PositionPolar
        if (pos.y == grid.size - 1) return null
        val nextRow = grid[pos.y + 1]
        return nextRow[pos.x * nextRow.size / grid[pos.y].size]
    }

    /**
     * Get the cells in the outward direction of [cell].
     */
    fun getOutwardCellsOf(cell: Cell): List<ThetaCell> {
        val pos = cell.position as PositionPolar
        val cells: List<ThetaCell>
        if (pos.y == grid.size - 1) {
            cells = emptyList()
        } else {
            val nextRow = grid[pos.y + 1]
            val factor = nextRow.size / grid[pos.y].size
            cells = mutableListOf()
            val start = pos.x * factor
            for (x in start until start + factor) {
                cells.add(nextRow[x])
            }
        }
        return cells
    }

    override fun drawTo(canvas: Canvas, style: Configuration.Style) {
        val csize = style.cellSize
        val center = (radius + centerRadius - 1) * csize
        val size = 2 * center + style.stroke.lineWidth
        canvas.init(size, size)

        // Draw the background
        if (style.backgroundColor != null) {
            canvas.color = style.backgroundColor
            canvas.drawRect(0.0, 0.0, canvas.width, canvas.height, true)
        }

        val offset = style.stroke.lineWidth / 2.0
        canvas.translate = Point(offset, offset)

        // Draw the distance map
        if (hasDistanceMap) {
            val distMapColors = style.generateDistanceMapColors(this)

            // Draw center circle cell
            canvas.color = distMapColors[cellAt(0, 0)!!.distanceMapValue]
            canvas.drawEllipse(center, center, centerRadius * csize, centerRadius * csize, true)

            // Draw other cells
            for (r in 1 until radius) {
                val startRadius = (r + centerRadius - 1) * csize
                val endRadius = startRadius + csize
                val width = grid[min(grid.size - 1, r)].size
                for (x in 0 until width) {
                    val extent = 1.0 / width * TAU
                    val startAngle = x * extent
                    canvas.color = distMapColors[cellAt(x, r)!!.distanceMapValue]
                    canvas.drawPath(listOf(
                            ArcPoint(center, center, startRadius, startRadius, startAngle, extent),
                            ArcPoint(center, center, endRadius, endRadius, startAngle + extent, -extent)
                    ), true)
                }
            }
        }

        // Draw the maze
        // For each cell, only the inward and clockwise sides are drawn if they are set,
        // except for the last row where the outward side is also drawn.
        canvas.color = style.color
        canvas.stroke = style.stroke
        for (r in 1..radius) {
            val startRadius = (r + centerRadius - 1) * csize
            val endRadius = startRadius + csize
            val width = grid[min(grid.size - 1, r)].size
            for (x in 0 until width) {
                val extent = 1.0 / width * TAU
                val startAngle = x * extent
                val cell = cellAt(x, r)
                if (cell != null && cell.hasSide(Side.CW)) {
                    val px = cos(startAngle)
                    val py = -sin(startAngle)
                    canvas.drawLine(center + px * startRadius, center + py * startRadius,
                            center + px * endRadius, center + py * endRadius)
                }
                if (cell != null && cell.hasSide(Side.IN) || cell == null
                        && grid[r - 1][x].hasSide(Side.OUT)) {
                    canvas.drawArc(center, center, startRadius, startRadius, startAngle, extent)
                }
            }
        }

        // Draw the solution
        if (solution != null) {
            canvas.color = style.solutionColor
            canvas.stroke = style.solutionStroke

            val solution = solution!!

            var prevPos: PositionPolar? = null
            var currPos: PositionPolar? = null
            var prevAngle = 0.0
            var currAngle = 0.0

            // The starting angle of the arc to draw next
            var arcStartAngle = Double.NaN

            for (i in 0..solution.size) {
                val nextPos: PositionPolar?
                val nextAngle: Double
                if (i == solution.size) {
                    nextPos = null
                    nextAngle = 0.0
                } else {
                    nextPos = solution[i].position as PositionPolar
                    nextAngle = (nextPos.x + 0.5) / nextPos.rowWidth * TAU
                }

                if (currPos != null) {
                    val centerRadius = (currPos.y + 0.5 + centerRadius - 1) * csize
                    val startAngle = if (prevPos != null && prevPos.y > currPos.y) prevAngle else currAngle
                    val endAngle = if (nextPos != null && nextPos.y > currPos.y) nextAngle else currAngle

                    if (arcStartAngle.isNaN()) {
                        // Initialize starting arc angle if needed.
                        arcStartAngle = startAngle
                    }

                    if (prevPos != null && prevPos.y != currPos.y) {
                        // Draw the START line: a line extending last line to the current cell's center
                        val startRadius = centerRadius - (currPos.y - prevPos.y) * csize / 2
                        val px = cos(startAngle)
                        val py = -sin(startAngle)
                        canvas.drawLine(center + px * startRadius, center + py * startRadius,
                                center + px * centerRadius, center + py * centerRadius)

                        arcStartAngle = startAngle
                    }

                    if (nextPos?.y != currPos.y) {
                        if (nextPos != null) {
                            // Draw the END line: a line from the current cell's center to its edge
                            val endRadius = centerRadius - (currPos.y - nextPos.y) * csize / 2
                            val px = cos(endAngle)
                            val py = -sin(endAngle)
                            canvas.drawLine(center + px * centerRadius, center + py * centerRadius,
                                    center + px * endRadius, center + py * endRadius)
                        }

                        // If START and END lines are not adjacent, draw an arc between them.
                        if (!arcStartAngle.isNaN() && endAngle != arcStartAngle) {
                            // Find the arc extent from arcStartAngle.
                            var extent = endAngle - arcStartAngle
                            if (prevPos == null || prevPos.y != currPos.y) {
                                // The previous cell is not on the same row as current cell.
                                // Find the smallest arc between the START and END lines.
                                if (extent < -PI) {
                                    extent += TAU
                                } else if (extent > PI) {
                                    extent -= TAU
                                }
                            } else {
                                // Previous and current cells are on the same row.
                                // Find the direction of the arc to draw (1 = counter-clockwise)
                                // There are many cases giving different directions.
                                val direction = if (prevPos.x < currPos.x
                                        && (prevPos.x != 0 || currPos.x != currPos.rowWidth - 1)
                                        || prevPos.x == currPos.rowWidth - 1 && currPos.x == 0
                                        || prevPos.x == 0 && currPos.x == 1) 1 else -1
                                if (sign(extent).toInt() != direction) {
                                    // Extent is in the wrong direction, change it.
                                    extent += direction * TAU
                                }
                            }
                            canvas.drawArc(center, center, centerRadius, centerRadius, arcStartAngle, extent)
                        }
                    }
                }

                prevPos = currPos
                currPos = nextPos
                prevAngle = currAngle
                currAngle = nextAngle
            }
        }
    }


    override fun toString() = "[radius: $radius, centerRadius: $centerRadius]"


    companion object {
        private const val TAU = PI * 2
    }

}
