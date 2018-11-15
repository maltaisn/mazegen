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


class HexMazeTest {

    @Test
    fun grid_arrangement_rectangle() {
        val maze1 = HexMaze(4, 1, Arrangement.RECTANGLE)
        val grid1 = arrayOf(
                "**  ",
                "  **"
        )
        assertMazeHasGrid(maze1, grid1)

        val maze2 = HexMaze(5, 3, Arrangement.RECTANGLE)
        val grid2 = arrayOf(
                "**   ",
                "**** ",
                "*****",
                "  ***",
                "    *"
        )
        assertMazeHasGrid(maze2, grid2)
    }

    @Test
    fun grid_arrangement_triangle() {
        val maze1 = HexMaze(2, Arrangement.TRIANGLE)
        val grid1 = arrayOf(
                "**",
                " *"
        )
        assertMazeHasGrid(maze1, grid1)

        val maze2 = HexMaze(5, Arrangement.TRIANGLE)
        val grid2 = arrayOf(
                "*****",
                " ****",
                "  ***",
                "   **",
                "    *"
        )
        assertMazeHasGrid(maze2, grid2)
    }

    @Test
    fun grid_arrangement_hexagon() {
        val maze1 = HexMaze(1, Arrangement.HEXAGON)
        val grid1 = arrayOf("*")
        assertMazeHasGrid(maze1, grid1)

        val maze2 = HexMaze(3, Arrangement.HEXAGON)
        val grid2 = arrayOf(
                "***  ",
                "**** ",
                "*****",
                " ****",
                "  ***"
        )
        assertMazeHasGrid(maze2, grid2)
    }

    @Test
    fun grid_arrangement_rhombus() {
        val maze1 = HexMaze(1, Arrangement.RHOMBUS)
        val grid1 = arrayOf("*")
        assertMazeHasGrid(maze1, grid1)

        val maze2 = HexMaze(5, Arrangement.RHOMBUS)
        val grid2 = arrayOf(
                "*****",
                "*****",
                "*****",
                "*****",
                "*****"
        )
        assertMazeHasGrid(maze2, grid2)
    }

    private fun assertMazeHasGrid(maze: HexMaze, grid: Array<String>) {
        for (y in 0 until grid.size) {
            for (x in 0 until grid[y].length) {
                val pos = PositionXY(x, y)
                val cell = maze.optionalCellAt(pos)
                if (cell != null) {
                    assertEquals('*', grid[y][x])
                    assertEquals(pos, cell.position)
                } else {
                    assertEquals(' ', grid[y][x])
                }
            }
        }
    }

}