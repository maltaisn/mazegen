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
                grid[x][y].value = value
            }
        }
    }

    override fun renderToSvg(): String {
        val height = (height * SVG_CELL_SIZE).toInt()
        val canvas = SVGGraphics2D((width * SVG_CELL_SIZE).toInt(), height)
        canvas.stroke = Maze.SVG_STROKE_STYLE
        canvas.color = Maze.SVG_STROKE_COLOR

        val path = GeneralPath()
        for (x in 0 until width) {
            for (y in 0 until height) {
                val cell = grid[x][y]

                // Draw north and east sides if cell has them
                if (cell.hasSide(RectCell.Side.NORTH)) {
                    path.moveTo(x * SVG_CELL_SIZE, y * SVG_CELL_SIZE)
                    path.lineTo((x + 1) * SVG_CELL_SIZE, y * SVG_CELL_SIZE)
                }
                if (cell.hasSide(RectCell.Side.WEST)) {
                    path.moveTo(x * SVG_CELL_SIZE, y * SVG_CELL_SIZE)
                    path.lineTo(x * SVG_CELL_SIZE, (y + 1) * SVG_CELL_SIZE)
                }

                // Only draw east and south sides for rightmost and bottommost cells
                if (y == height - 1 && cell.hasSide(RectCell.Side.SOUTH)) {
                    path.moveTo(x * SVG_CELL_SIZE, (y + 1) * SVG_CELL_SIZE)
                    path.lineTo((x + 1) * SVG_CELL_SIZE, (y + 1) * SVG_CELL_SIZE)
                }
                if (x == width - 1 && cell.hasSide(RectCell.Side.EAST)) {
                    path.moveTo((x + 1) * SVG_CELL_SIZE, y * SVG_CELL_SIZE)
                    path.lineTo((x + 1) * SVG_CELL_SIZE, (y + 1) * SVG_CELL_SIZE)
                }
            }
        }
        path.closePath()
        canvas.draw(path)

        return canvas.svgDocument
    }

    override fun toString(): String {
        return "[width: $width, height: $height]"
    }

    companion object {
        private const val SVG_CELL_SIZE = 10.0
    }

}