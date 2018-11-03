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


/**
 * Implementation of maze for a standard 2D maze.
 * @param[defaultCellValue] default value to assign to cells on construction
 */
class FlatMaze(width: Int, height: Int,
               defaultCellValue: Int = FlatCell.Side.NONE.value) : Maze<FlatCell>(width, height) {

    init {
        grid = Array(width) { x ->
            Array(height) { y -> FlatCell(this, x, y, defaultCellValue) }
        }
    }

    override fun format(): String {
        val w = width + 1
        val h = height + 1
        val sb = StringBuilder((w + 1) * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val nw = optionalCellAt(x - 1, y - 1)
                val ne = optionalCellAt(x, y - 1)
                val sw = optionalCellAt(x - 1, y)
                val se = optionalCellAt(x, y)

                var value = FlatCell.Side.NONE.value
                if (nw != null && nw.hasSide(FlatCell.Side.EAST)
                        || ne != null && ne.hasSide(FlatCell.Side.WEST)) {
                    value = value or FlatCell.Side.NORTH.value
                }
                if (ne != null && ne.hasSide(FlatCell.Side.SOUTH)
                        || se != null && se.hasSide(FlatCell.Side.NORTH)) {
                    value = value or FlatCell.Side.EAST.value
                }
                if (sw != null && sw.hasSide(FlatCell.Side.EAST)
                        || se != null && se.hasSide(FlatCell.Side.WEST)) {
                    value = value or FlatCell.Side.SOUTH.value
                }
                if (nw != null && nw.hasSide(FlatCell.Side.SOUTH)
                        || sw != null && sw.hasSide(FlatCell.Side.NORTH)) {
                    value = value or FlatCell.Side.WEST.value
                }
                sb.append(CHARS[value])
            }
            sb.append('\n')
        }
        sb.deleteCharAt(sb.length - 1)
        return sb.toString()
    }

    companion object {
        private const val CHARS = " ╵╶└╷│┌├╴┘─┴┐┤┬┼"
    }

}