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

package com.maltaisn.mazegen.generator

import com.maltaisn.mazegen.ParameterException
import com.maltaisn.mazegen.maze.Cell
import com.maltaisn.mazegen.maze.Maze
import com.maltaisn.mazegen.maze.OrthogonalCell.Side
import com.maltaisn.mazegen.maze.OrthogonalMaze
import com.maltaisn.mazegen.maze.UnicursalOrthogonalMaze
import kotlin.random.Random


/**
 * Implementation of Eller's algorithm as described
 * [here](http://weblog.jamisbuck.org/2010/12/29/maze-generation-eller-s-algorithm) and
 * [here](http://people.cs.ksu.edu/~ashley78/wiki.ashleycoleman.me/index.php/Eller's_Algorithm.html).
 * Only works for orthogonal mazes.
 *
 * 1. Iterate over the maze one row at the time
 * 2. Create a new set for each unvisited cell in the row.
 * 3. Randomly connect adjacent cells in the row if they are not part of the same set.
 *    When two cells are connected, merge their sets into one.
 * 4. For each set, randomly carve at least one passage south. Mark the cell carved to as visited.
 * 5. Repeat steps 2 to 4 until the last row.
 * 6. On the last row, connect all adjacent cells not part of the same set together instead of
 *    doing it randomly. Don't carve passage south obviously.
 *
 * Mazes generated share similarities with Kruskal's and Prim's generated mazes.
 * Different vertical or horizontal biases can be applied for different maze textures.
 *
 * Runtime complexity is O(n) and memory space is O(1).
 */
class EllerGenerator : Generator(
        OrthogonalMaze::class, UnicursalOrthogonalMaze::class) {

    /**
     * Percentage of the time two adjacent cells are connected, between 0 and 1.
     * Provides a horizontal bias setting. A high value will result in a more horizontal texture.
     */
    var horizontalBias = 0.5
        set(value) {
            checkBiasValue(value)
            field = value
        }

    /**
     * Percentage of the time cells are carved south for a set, between 0 and 1.
     * Provides a horizontal bias setting. A high value will result in a more vertical texture.
     */
    var verticalBias = 0.5
        set(value) {
            checkBiasValue(value)
            field = value
        }


    private fun checkBiasValue(value: Double) {
        if (value <= 0 || value > 1) {
            throw ParameterException("Bias for Eller's generator must be between 0% and 100%")
        }
    }


    override fun generate(maze: Maze) {
        super.generate(maze)
        maze as OrthogonalMaze

        maze.fillAll()

        val cells = HashMap<Cell, CellSet>()
        val sets = LinkedHashSet<CellSet>()
        var setId = 0
        for (y in 0 until maze.height) {
            val isLastRow = (y == maze.height - 1)

            // Add new sets for the cells in the current row if they were not added to another set
            // when passages were carved south in the last step.
            for (x in 0 until maze.width) {
                val cell = maze.cellAt(x, y)!!
                if (!cells.contains(cell)) {
                    val set = CellSet(setId)
                    setId++
                    set.add(cell)
                    cells[cell] = set
                    sets.add(set)
                }
            }

            // Randomly connect adjacent cells in the current row that are not part of the same set
            for (x in 0 until maze.width - 1) {
                val cell = maze.cellAt(x, y)!!
                val set = cells[cell]!!
                val nextCell = maze.cellAt(x + 1, y)!!
                val nextSet = cells[nextCell]!!
                if (set !== nextSet && (isLastRow || Random.nextDouble(1.0) <= horizontalBias)) {
                    // Connect both cells
                    cell.openSide(Side.EAST)

                    // Merge their sets into one
                    set.addAll(nextSet)
                    for (c in nextSet) {
                        cells[c] = set
                    }
                    sets.remove(nextSet)
                }
            }

            // Randomly carve at least one passage down for each set
            if (!isLastRow) {
                for (set in sets) {
                    // Get a random list of cell indexes to carve from in the set.
                    // Also clear the current row cells from the map and set, they're not needed anymore.
                    val cellsToCarve = ArrayList<Cell>()
                    for (cell in set) {
                        if (Random.nextDouble(1.0) <= verticalBias) {
                            cellsToCarve.add(cell)
                        }
                        cells.remove(cell)
                    }
                    if (cellsToCarve.isEmpty()) {
                        // There must be at least one passage carved for each set.
                        cellsToCarve.add(set.random())
                    }
                    set.clear()

                    for (cell in cellsToCarve) {
                        // Carve passage south
                        cell.openSide(Side.SOUTH)

                        // Add south cell to current set
                        val southCell = cell.getCellOnSide(Side.SOUTH)!!
                        set.add(southCell)
                        cells[southCell] = set
                    }
                }
            }
        }
    }

    /**
     * ArrayList subclass to prevent expensive equals and hashCode because they're not needed.
     * Each set is assigned an ID for a hashcode.
     */
    private class CellSet(val id: Int) : ArrayList<Cell>() {
        override fun equals(other: Any?): Boolean = (this === other)
        override fun hashCode(): Int = id
    }

}