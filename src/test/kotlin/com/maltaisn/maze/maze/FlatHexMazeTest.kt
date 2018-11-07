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


class FlatHexMazeTest {

    @Test
    fun grid_arrangment_rectangle() {
        val maze1 = FlatHexMaze(HexMaze.Arrangement.RECTANGLE, 4, 1)
        val grid1 = arrayOf(
                "**  ",
                "  **"
        )
        assertMazeHasGrid(maze1, grid1)

        val maze2 = FlatHexMaze(HexMaze.Arrangement.RECTANGLE, 5, 3)
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
    fun grid_arrangment_triangle() {
        val maze1 = FlatHexMaze(HexMaze.Arrangement.TRIANGLE, 2)
        val grid1 = arrayOf(
                "**",
                " *"
        )
        assertMazeHasGrid(maze1, grid1)

        val maze2 = FlatHexMaze(HexMaze.Arrangement.TRIANGLE, 5)
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
    fun grid_arrangment_hexagon() {
        val maze1 = FlatHexMaze(HexMaze.Arrangement.HEXAGON, 1)
        val grid1 = arrayOf("*")
        assertMazeHasGrid(maze1, grid1)

        val maze2 = FlatHexMaze(HexMaze.Arrangement.HEXAGON, 3)
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
    fun grid_arrangment_rhombus() {
        val maze1 = FlatHexMaze(HexMaze.Arrangement.RHOMBUS, 1)
        val grid1 = arrayOf("*")
        assertMazeHasGrid(maze1, grid1)

        val maze2 = FlatHexMaze(HexMaze.Arrangement.RHOMBUS, 5)
        val grid2 = arrayOf(
                "*****",
                "*****",
                "*****",
                "*****",
                "*****"
        )
        assertMazeHasGrid(maze2, grid2)
    }

    private fun assertMazeHasGrid(maze: FlatHexMaze, grid: Array<String>) {
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

    @Test
    internal fun format() {
        TODO("not implemented")
    }

}