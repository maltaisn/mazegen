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

import org.jfree.graphics2d.svg.SVGGraphics2D
import java.awt.geom.GeneralPath
import java.util.concurrent.ThreadLocalRandom


/**
 * Class for a flat rectangular maze represented by 2D grid of [RectCell].
 * @param[width] the number of columns
 * @param[height] the number of rows
 * @param[defaultCellValue] default value to assign to cells on construction, no sides by default.
 */
class RectMaze(val width: Int, val height: Int,
               defaultCellValue: Int = RectCell.Side.NONE.value) : Maze {

    private val grid: Array<Array<RectCell>>

    init {
        grid = Array(width) { x ->
            Array(height) { y -> RectCell(this, PositionXY(x, y), defaultCellValue) }
        }
    }

    override fun cellAt(pos: Position): RectCell = if (pos is PositionXY) {
        grid[pos.x][pos.y]
    } else {
        throw IllegalArgumentException("Position has wrong type.")
    }

    override fun optionalCellAt(pos: Position): RectCell? {
        if (pos is PositionXY) {
            if (pos.x < 0 || pos.x >= width || pos.y < 0 || pos.y >= height) return null
            return grid[pos.x][pos.y]
        }
        return null
    }

    override fun getRandomCell(): RectCell {
        val random = ThreadLocalRandom.current()
        return cellAt(PositionXY(random.nextInt(width), random.nextInt(height)))
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Cell> forEachCell(action: (T) -> Boolean) {
        for (x in 0 until width) {
            for (y in 0 until height) {
                val stop = action(cellAt(PositionXY(x, y)) as T)
                if (stop) return
            }
        }
    }

    override fun reset(empty: Boolean) {
        val value = if (empty) RectCell.Side.NONE.value else RectCell.Side.ALL.value
        for (x in 0 until width) {
            for (y in 0 until height) {
                val cell = grid[x][y]
                cell.visited = false
                cell.value = value
            }
        }
    }

    /**
     * Render the maze to SVG.
     * The grid is first scanned horizontally and to find the longest possible line sections,
     * until there's a "hole" in the line, after which a new line is drawn. The same is done
     * vertically. Only north and west walls are checked except for the rightmost and bottommost
     * walls where the south and east walls are checked. A single path object is created.
     */
    override fun renderToSvg(): String {
        val canvas = SVGGraphics2D((width * SVG_CELL_SIZE).toInt(),
                (height * SVG_CELL_SIZE).toInt())
        canvas.stroke = Maze.SVG_STROKE_STYLE
        canvas.color = Maze.SVG_STROKE_COLOR

        val path = GeneralPath()

        // Draw horizontal walls
        for (y in 0 until height + 1) {
            val canvasY = y * SVG_CELL_SIZE
            var startX = 0.0
            var endX = 0.0
            for (x in 0 until width) {
                val cell = if (y == height) null else grid[x][y]
                if (cell != null && cell.hasSide(RectCell.Side.NORTH)
                        || cell == null && grid[x][y - 1].hasSide(RectCell.Side.SOUTH)) {
                    if (startX == endX) {
                        startX = x * SVG_CELL_SIZE
                        endX = startX
                    }
                    endX += SVG_CELL_SIZE
                } else if (startX != endX) {
                    path.moveTo(startX, canvasY)
                    path.lineTo(endX, canvasY)
                    startX = endX
                }
            }
            if (startX != endX) {
                path.moveTo(startX, canvasY)
                path.lineTo(endX, canvasY)
            }
        }

        // Draw vertical walls
        for (x in 0 until width + 1) {
            val canvasX = x * SVG_CELL_SIZE
            var startY = 0.0
            var endY = 0.0
            for (y in 0 until height) {
                val cell = if (x == width) null else grid[x][y]
                if (cell != null && cell.hasSide(RectCell.Side.WEST)
                        || cell == null && grid[x - 1][y].hasSide(RectCell.Side.EAST)) {
                    if (startY == endY) {
                        startY = y * SVG_CELL_SIZE
                        endY = startY
                    }
                    endY += SVG_CELL_SIZE
                } else if (startY != endY) {
                    path.moveTo(canvasX, startY)
                    path.lineTo(canvasX, endY)
                    startY = endY
                }
            }
            if (startY != endY) {
                path.moveTo(canvasX, startY)
                path.lineTo(canvasX, endY)
            }
        }

        if (path.currentPoint != null) {
            path.closePath()
            canvas.draw(path)
        }

        return canvas.svgDocument
    }


    override fun getCellCount(): Int = width * height

    override fun toString(): String {
        return "[width: $width, height: $height]"
    }

    companion object {
        private const val SVG_CELL_SIZE = 10.0
    }

}