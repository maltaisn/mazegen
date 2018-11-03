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


class MazeTest {

    @Test
    fun cellAt() {
        val maze = FlatMaze(10, 10)

        val cell1 = maze.cellAt(5, 5)
        assertEquals(cell1.x, 5)
        assertEquals(cell1.y, 5)

        val cell2 = maze.cellAt(9, 0)
        assertEquals(cell2.x, 9)
        assertEquals(cell2.y, 0)
    }

    @Test
    fun optionalCellAt() {
        val maze = FlatMaze(10, 10)

        val cell1 = maze.optionalCellAt(5, 5)!!
        assertEquals(cell1.x, 5)
        assertEquals(cell1.y, 5)

        assertNull(maze.optionalCellAt(-1, 5))
        assertNull(maze.optionalCellAt(10, 10))
    }

}