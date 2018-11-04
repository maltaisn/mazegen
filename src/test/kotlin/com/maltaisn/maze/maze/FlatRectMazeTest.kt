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

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull


class FlatRectMazeTest {

    @Test
    fun defaultValue_coordinates() {
        val maze1 = FlatRectMaze(3, 3)
        for (x in 0 until maze1.width) {
            for (y in 0 until maze1.height) {
                val cell = maze1.cellAt(x, y)
                assertEquals(x, cell.x)
                assertEquals(y, cell.y)
                assertEquals(FlatRectCell.Side.NONE.value, cell.value)
            }
        }

        val maze2 = FlatRectMaze(3, 3, FlatRectCell.Side.ALL.value)
        for (x in 0 until maze2.width) {
            for (y in 0 until maze2.height) {
                val cell = maze2.cellAt(x, y)
                assertEquals(x, cell.x)
                assertEquals(y, cell.y)
                assertEquals(FlatRectCell.Side.ALL.value, cell.value)
            }
        }
    }

    @Test
    fun cellAt() {
        val maze = FlatRectMaze(10, 10)

        val cell1 = maze.cellAt(5, 5)
        assertEquals(cell1.x, 5)
        assertEquals(cell1.y, 5)

        val cell2 = maze.cellAt(9, 0)
        assertEquals(cell2.x, 9)
        assertEquals(cell2.y, 0)
    }

    @Test
    fun optionalCellAt() {
        val maze = FlatRectMaze(10, 10)

        val cell1 = maze.optionalCellAt(5, 5)!!
        assertEquals(cell1.x, 5)
        assertEquals(cell1.y, 5)

        assertNull(maze.optionalCellAt(-1, 5))
        assertNull(maze.optionalCellAt(10, 10))
    }

    @Test
    fun format() {
        val maze1 = FlatRectMaze(1, 1, FlatRectCell.Side.ALL.value)
        assertEquals("┌┐\n└┘", maze1.format())

        val maze2 = FlatRectMaze(3, 3, FlatRectCell.Side.ALL.value)
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