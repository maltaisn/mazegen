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

import com.maltaisn.maze.SVGRenderer
import com.maltaisn.maze.maze.RectCell.Side
import java.util.concurrent.ThreadLocalRandom


/**
 * Class for a square-tiled maze represented by 2D grid of [RectCell].
 */
class RectMaze : Maze {

    val width: Int
    val height: Int

    private val grid: Array<Array<RectCell>>

    /**
     * Create an empty maze with [width] columns and [height] rows.
     */
    constructor(width: Int, height: Int) {
        if (width < 1 || height < 1) {
            throw IllegalArgumentException("Dimensions must be at least 1.")
        }

        this.width = width
        this.height = height
        grid = Array(width) { x ->
            Array(height) { y ->
                RectCell(this, PositionXY(x, y), Side.NONE.value)
            }
        }
    }

    private constructor(maze: RectMaze) {
        width = maze.width
        height = maze.height
        grid = Array(maze.grid.size) { maze.grid.get(it).clone() }
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

    override fun getCellCount(): Int = width * height

    override fun getAllCells(): LinkedHashSet<RectCell> {
        val set = LinkedHashSet<RectCell>(width * height)
        for (x in 0 until width) {
            for (y in 0 until height) {
                set.add(grid[x][y])
            }
        }
        return set
    }

    override fun createOpenings(vararg openings: Opening) {
        for (opening in openings) {
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

            val cell = optionalCellAt(PositionXY(x, y))
            if (cell != null) {
                for (side in cell.getAllSides()) {
                    if (cell.getCellOnSide(side) == null) {
                        cell.openSide(side)
                        break
                    }
                }
            } else {
                throw IllegalArgumentException("Opening describes no cell in the maze.")
            }
        }
    }

    override fun reset(empty: Boolean) {
        val value = if (empty) Side.NONE.value else Side.ALL.value
        for (x in 0 until width) {
            for (y in 0 until height) {
                val cell = grid[x][y]
                cell.visited = false
                cell.value = value
            }
        }
    }

    override fun copy(): Maze = RectMaze(this)

    /**
     * Render the maze to SVG.
     * For each cell, only the north and east sides are drawn if they are set,
     * except for the last row and column where to south and east side are also drawn.
     */
    override fun renderToSvg(): SVGRenderer {
        val svg = SVGRenderer((width * SVG_CELL_SIZE).toInt(),
                (height * SVG_CELL_SIZE).toInt())
        svg.strokeColor = Maze.SVG_STROKE_COLOR
        svg.strokeWidth = Maze.SVG_STROKE_WIDTH

        for (x in 0..width) {
            val px = x * SVG_CELL_SIZE
            for (y in 0..height) {
                val py = y * SVG_CELL_SIZE
                val cell = optionalCellAt(PositionXY(x, y))
                if (cell != null && cell.hasSide(Side.NORTH) || cell == null
                        && optionalCellAt(PositionXY(x, y - 1))?.hasSide(Side.SOUTH) != null) {
                    svg.drawLine(px, py, px + SVG_CELL_SIZE, py)
                }
                if (cell != null && cell.hasSide(Side.WEST) || cell == null
                        && optionalCellAt(PositionXY(x - 1, y))?.hasSide(Side.EAST) != null) {
                    svg.drawLine(px, py, px, py + SVG_CELL_SIZE)
                }
            }
        }

        return svg
    }

    override fun toString(): String {
        return "[width: $width, height: $height]"
    }

    companion object {
        private const val SVG_CELL_SIZE = 10.0
    }

}