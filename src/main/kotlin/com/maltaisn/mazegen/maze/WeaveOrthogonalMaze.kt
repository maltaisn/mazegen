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
import com.maltaisn.mazegen.maze.WeaveOrthogonalCell.Side
import com.maltaisn.mazegen.paramError
import com.maltaisn.mazegen.render.Canvas
import com.maltaisn.mazegen.render.Point
import java.awt.Color
import java.util.*
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
            paramError("Max weave setting must be positive.")
        }
    }

    init {
        grid = Array(width) { x ->
            Array(height) { y ->
                WeaveOrthogonalCell(this, Position2D(x, y))
            }
        }
    }

    /**
     * Overridden since a single cell can have two distance map distances:
     * one for the tunnel and one for the bridge.
     * @param startPos Starting position (distance of zero), can be `null` for a random cell.
     * If the starting cell has a tunnel, the algorithm will always start on on the bridge.
     */
    override fun generateDistanceMap(startPos: Position?) {
        val inf = Int.MAX_VALUE - 1
        forEachCell {
            it as WeaveOrthogonalCell
            it.visited = false
            it.distanceMapValue = inf
            it.distanceMapTunnelValue = inf
        }

        val startCell = if (startPos == null) {
            getRandomCell()
        } else {
            getOpeningCell(startPos) ?: paramError(
                    "Distance map start position describes no cell in the maze.")
        }
        startCell.distanceMapValue = 0

        val cellList = getAllCells()
        while (cellList.isNotEmpty()) {
            // Get and remove the cell with the lowest distance
            var minIndex = 0
            for (i in 1 until cellList.size) {
                val cell = cellList[i]
                if (cell.distanceMapValue < cellList[minIndex].distanceMapValue) {
                    minIndex = i
                }
            }

            val minCell = cellList.removeAt(minIndex)
            minCell.visited = true

            if (minCell.distanceMapValue == inf) {
                // This means the only cells left are inaccessible from the start cell.
                // The distance map can't be generated completely.
                error("Could not generate distance map, maze has inaccessible cells.")
            }

            val distance = minCell.distanceMapValue
            for (neighbor in minCell.findAccessibleNeighbors()) {
                if (!neighbor.visited) {
                    // If there are cells between the cell and its neighbor, then the passage
                    // goes through a tunnel. Set tunnel distances on tunnel cells.
                    var x = minCell.position.x
                    var y = minCell.position.y
                    val dx = neighbor.position.x - x
                    val dy = neighbor.position.y - y
                    val diff = max(dx.absoluteValue, dy.absoluteValue)
                    if (diff > 1) {
                        for (i in 1 until diff) {
                            x += dx.sign
                            y += dy.sign
                            grid[x][y].distanceMapTunnelValue = distance + i
                        }
                    }

                    // Compare neighbor distance calculated from this cell with
                    // its current distance. If new distance is smaller, update it.
                    val newDistance = distance + diff
                    if (newDistance < neighbor.distanceMapValue) {
                        neighbor.distanceMapValue = newDistance
                    }
                }
            }
        }

        hasDistanceMap = true
    }

    override fun clearDistanceMap() {
        if (hasDistanceMap) {
            forEachCell {
                it as WeaveOrthogonalCell
                it.distanceMapValue = -1
                it.distanceMapTunnelValue = -1
            }
            hasDistanceMap = false
        }
    }


    override fun drawTo(canvas: Canvas, style: Configuration.Style) {
        val csize = style.cellSize
        val outSize = csize * INSET_SIZE_RATIO
        val inSize = csize / 2 - outSize
        canvas.init(width * csize + style.stroke.lineWidth,
                height * csize + style.stroke.lineWidth)

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
            for (x in 0 until width) {
                val cx = (x + 0.5) * csize
                for (y in 0 until height) {
                    val cy = (y + 0.5) * csize
                    val cell = cellAt(x, y)!!

                    // Draw the tunnel color with a rectangle, either vertical or horizontal.
                    if (cell.hasTunnel) {
                        canvas.color = distMapColors[cell.distanceMapTunnelValue]
                        if (cell.hasSide(Side.NORTH)) {
                            canvas.drawRect(cx - inSize, cy - csize / 2, 2 * inSize, csize, true)
                        } else {
                            canvas.drawRect(cx - csize / 2, cy - inSize, csize, 2 * inSize, true)
                        }
                    }

                    // Draw the bridge color with a polygon
                    // Vertices must be added in clockwise order to the polygon
                    // that's why north and east are differenciated from the other two.
                    val vertices = LinkedList<Point>()
                    for (side in cell.allSides) {
                        val pos = side.relativePos
                        val positive = side == Side.NORTH || side == Side.EAST
                        val ex = 1 - pos.x.absoluteValue
                        val ey = 1 - pos.y.absoluteValue

                        vertices += if (positive) {
                            Point(cx + (pos.x - ex) * inSize, cy + (pos.y - ey) * inSize)
                        } else {
                            Point(cx + (pos.x + ex) * inSize, cy + (pos.y + ey) * inSize)
                        }
                        if (!cell.hasSide(side)) {
                            val p1 = Point(cx + (pos.x - ex) * (inSize + ey * outSize),
                                    cy + (pos.y - ey) * (inSize + ex * outSize))
                            val p2 = Point(cx + (pos.x + ex) * (inSize + ey * outSize),
                                    cy + (pos.y + ey) * (inSize + ex * outSize))
                            if (positive) {
                                vertices += p1
                                vertices += p2
                            } else {
                                vertices += p2
                                vertices += p1
                            }
                        }
                    }
                    canvas.color = distMapColors[cell.distanceMapValue]
                    canvas.drawPath(vertices, true)
                }
            }
        }

        // Draw the solution
        // The solution is drawn first because it can go under some passages and drawing it
        // after the maze would show awkward line endings.
        if (solution != null) {
            canvas.color = style.solutionColor
            canvas.stroke = style.solutionStroke

            val solution = solution!!
            var prevPoint: Point? = null
            for (i in 0 until solution.size) {
                canvas.color = Color.BLUE

                val cell = solution[i]
                val pos = cell.position as Position2D
                val cx = (pos.x + 0.5) * csize
                val cy = (pos.y + 0.5) * csize
                if (prevPoint != null) {
                    canvas.drawLine(prevPoint.x, prevPoint.y, cx, cy)
                }

                val nextCell = solution.getOrNull(i + 1)
                if (nextCell != null) {
                    val nextPos = nextCell.position as Position2D
                    val dx = nextPos.x - pos.x
                    val dy = nextPos.y - pos.y
                    var nextPoint = Point(cx + dx.sign * csize / 2, cy + dy.sign * csize / 2)
                    canvas.drawLine(cx, cy, nextPoint.x, nextPoint.y)
                    prevPoint = nextPoint

                    if (dx.absoluteValue > 1 || dy.absoluteValue > 1) {
                        // The solution goes in a tunnel
                        val dxs = dx.sign
                        val dys = dy.sign
                        for (j in 0 until max(dx.absoluteValue, dy.absoluteValue) - 1) {
                            canvas.drawLine(prevPoint!!.x, prevPoint.y,
                                    prevPoint.x + dxs * outSize, prevPoint.y + dys * outSize)
                            nextPoint = Point(prevPoint.x + dxs * csize, prevPoint.y + dys * csize)
                            canvas.drawLine(prevPoint.x + dxs * (csize - outSize),
                                    prevPoint.y + dys * (csize - outSize),
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
            val cx = (x + 0.5) * csize
            for (y in 0 until height) {
                val cy = (y + 0.5) * csize
                val cell = cellAt(x, y)!!
                for (side in cell.allSides) {
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
                    if (cell.hasTunnel || !hasSide) {
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

    override fun toString() = super.toString().dropLast(1) + ", maxWeave: $maxWeave]"


    companion object {
        const val INSET_SIZE_RATIO = 0.15
    }

}
