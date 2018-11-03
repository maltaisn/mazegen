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

import com.maltaisn.maze.maze.FlatCell
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals


class FlatMazeTest {

    @Test
    fun defaultValue_coordinates() {
        val maze1 = FlatMaze(3, 3)
        for (x in 0 until maze1.width) {
            for (y in 0 until maze1.height) {
                val cell = maze1.cellAt(x, y)
                assertEquals(x, cell.x)
                assertEquals(y, cell.y)
                assertEquals(FlatCell.Side.NONE.value, cell.value)
            }
        }

        val maze2 = FlatMaze(3, 3, FlatCell.Side.ALL.value)
        for (x in 0 until maze2.width) {
            for (y in 0 until maze2.height) {
                val cell = maze2.cellAt(x, y)
                assertEquals(x, cell.x)
                assertEquals(y, cell.y)
                assertEquals(FlatCell.Side.ALL.value, cell.value)
            }
        }
    }

    @Test
    fun format() {
        val maze1 = FlatMaze(1, 1, FlatCell.Side.ALL.value)
        assertEquals("┌┐\n└┘", maze1.format())

        val maze2 = FlatMaze(3, 3, FlatCell.Side.ALL.value)
        maze2.cellAt(0, 0).connectWith(maze2.cellAt(1, 0))
        maze2.cellAt(1, 0).connectWith(maze2.cellAt(1, 1))
        maze2.cellAt(1, 1).connectWith(maze2.cellAt(2, 1))
        maze2.cellAt(2, 1).connectWith(maze2.cellAt(2, 0))
        maze2.cellAt(0, 1).connectWith(maze2.cellAt(1, 1))
        maze2.cellAt(0, 1).connectWith(maze2.cellAt(0, 2))
        maze2.cellAt(0, 2).connectWith(maze2.cellAt(1, 2))
        maze2.cellAt(1, 2).connectWith(maze2.cellAt(2, 2))
        assertEquals("┌─┬┐\n├╴╷│\n│╶─┤\n└──┘", maze2.format())
    }

}