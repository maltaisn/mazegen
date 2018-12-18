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

package com.maltaisn.maze.generator

import com.maltaisn.maze.maze.Cell
import com.maltaisn.maze.maze.Maze
import com.maltaisn.maze.maze.OrthogonalCell.Side
import com.maltaisn.maze.maze.OrthogonalMaze
import com.maltaisn.maze.maze.UnicursalOrthogonalMaze
import kotlin.random.Random


/**
 * Implementation of the sidewinder algorithm as described
 * [here](http://weblog.jamisbuck.org/2011/2/3/maze-generation-sidewinder-algorithm).
 * Only works for orthogonal mazes.
 *
 * 1. Iterate over all rows
 * 2. For each cell in the row, either carve a passage east or not
 *     - If it's carved, add the cell to a set.
 *     - If not, choose a random cell from the set and carve a
 *       passage north to it, then clear the set.
 *
 * Runtime complexity is O(n) and memory space is O(1).
 */
class SidewinderGenerator : Generator(
        OrthogonalMaze::class, UnicursalOrthogonalMaze::class) {

    override fun generate(maze: Maze) {
        super.generate(maze)
        maze as OrthogonalMaze

        maze.fillAll()

        for (y in 0 until maze.height) {
            val run = ArrayList<Cell>()
            for (x in 0 until maze.width) {
                val cell = maze.cellAt(x, y)!!
                run.add(cell)

                val northCell = cell.getCellOnSide(Side.NORTH)
                val eastCell = cell.getCellOnSide(Side.EAST)
                if (eastCell != null && (northCell == null || Random.nextBoolean())) {
                    // Carve passage east
                    cell.openSide(Side.EAST)
                } else if (northCell != null) {
                    // Carve a passage north from a random cell in the set
                    run.random().openSide(Side.NORTH)
                    run.clear()
                }
            }
        }
    }

}