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

import java.util.concurrent.ThreadLocalRandom


/**
 * Class for a flat rectangular maze represented by 2D grid of [FlatRectCell].
 * @param[width] the number of columns
 * @param[height] the number of rows
 * @param[defaultCellValue] default value to assign to cells on construction, no sides by default.
 */
class FlatRectMaze(val width: Int, val height: Int,
                   defaultCellValue: Int = FlatRectCell.Side.NONE.value) : RectMaze, FlatMaze {

    private val grid: Array<Array<FlatRectCell>>

    init {
        grid = Array(width) { x ->
            Array(height) { y -> FlatRectCell(this, PositionXY(x, y), defaultCellValue) }
        }
    }

    override fun cellAt(pos: Position): FlatRectCell = if (pos is PositionXY) {
        grid[pos.x][pos.y]
    } else {
        throw IllegalArgumentException("Position has wrong type.")
    }

    override fun optionalCellAt(pos: Position): FlatRectCell? {
        if (pos is PositionXY) {
            if (pos.x < 0 || pos.x >= width || pos.y < 0 || pos.y >= height) return null
            return grid[pos.x][pos.y]
        }
        return null
    }

    override fun getRandomCell(): FlatRectCell {
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
        val value = if (empty) FlatRectCell.Side.NONE.value else FlatRectCell.Side.ALL.value
        for (x in 0 until width) {
            for (y in 0 until height) {
                grid[x][y].value = value
            }
        }
    }

    override fun format(): String {
        val w = width + 1
        val h = height + 1
        val sb = StringBuilder((w + 1) * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val nw = optionalCellAt(PositionXY(x - 1, y - 1))
                val ne = optionalCellAt(PositionXY(x, y - 1))
                val sw = optionalCellAt(PositionXY(x - 1, y))
                val se = optionalCellAt(PositionXY(x, y))

                var value = FlatRectCell.Side.NONE.value
                if (nw != null && nw.hasSide(FlatRectCell.Side.EAST)
                        || ne != null && ne.hasSide(FlatRectCell.Side.WEST)) {
                    value = value or FlatRectCell.Side.NORTH.value
                }
                if (ne != null && ne.hasSide(FlatRectCell.Side.SOUTH)
                        || se != null && se.hasSide(FlatRectCell.Side.NORTH)) {
                    value = value or FlatRectCell.Side.EAST.value
                }
                if (sw != null && sw.hasSide(FlatRectCell.Side.EAST)
                        || se != null && se.hasSide(FlatRectCell.Side.WEST)) {
                    value = value or FlatRectCell.Side.SOUTH.value
                }
                if (nw != null && nw.hasSide(FlatRectCell.Side.SOUTH)
                        || sw != null && sw.hasSide(FlatRectCell.Side.NORTH)) {
                    value = value or FlatRectCell.Side.WEST.value
                }
                sb.append(CHARS[value])
            }
            sb.append('\n')
        }
        sb.deleteCharAt(sb.length - 1)
        return sb.toString()
    }

    override fun toString(): String {
        return "[width: $width, height: $height]"
    }

    companion object {
        private const val CHARS = " ╵╶└╷│┌├╴┘─┴┐┤┬┼"
    }

}