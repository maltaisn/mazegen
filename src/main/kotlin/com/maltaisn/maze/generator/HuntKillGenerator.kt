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

import com.maltaisn.maze.maze.FlatCell
import com.maltaisn.maze.maze.FlatMaze


/**
 * Implementation of the hunt-and-kill algorithm for maze generation as described
 * [here](http://weblog.jamisbuck.org/2011/1/24/maze-generation-hunt-and-kill-algorithm).
 *
 * 1. Make the initial cell the current cell and mark it as visited.
 * 2. Find unvisited neighbor to current cell and connect them. (kill mode)
 *    - If there is an unvisited neighbor, mark it as visited,
 *      make it the current cell and repeat step 2
 *    - If not, go to step 3.
 * 3. Scan the grid for an unvisited cell next to a visited cell. (hunt mode)
 *    - If a cell is found, connect it to the visited cell,
 *      mark it as visited and make it the current cell.
 *    - If not, the maze is done.
 */
class HuntKillGenerator(maze: FlatMaze) : Generator<FlatMaze>(maze) {

    override fun generate() {
        maze.reset(false)

        // Get cell on a random starting location
        var currentCell = maze.getRandomCell()
        currentCell.visited = true

        while (true) {
            kill(currentCell)
            val cell = hunt(maze)
            if (cell != null) {
                currentCell = cell
            } else {
                break
            }
        }
    }

    /**
     * Randomly connect cells starting from cell [cell]
     * until there are no neighboring unvisited cells.
     */
    private fun kill(cell: FlatCell) {
        var currentCell = cell
        while (true) {
            // Find an unvisited neighbor cell
            var unvisitedNeighbor: FlatCell? = null
            val neighbors = currentCell.getNeighbors().toMutableList()
            while (neighbors.size > 0) {
                val index = random.nextInt(neighbors.size)
                val neighbor = neighbors[index]
                if (!neighbor.visited) {
                    // Found one
                    unvisitedNeighbor = neighbor
                    break
                } else {
                    neighbors.removeAt(index)
                }
            }
            if (unvisitedNeighbor != null) {
                // Connect with current cell
                currentCell.connectWith(unvisitedNeighbor)

                currentCell = unvisitedNeighbor
                currentCell.visited = true
            } else {
                // No unvisited neighbor cell
                break
            }
        }
    }

    /**
     * Returns the first unvisited cell with a visited neighbor in maze [maze].
     * Returns null if there are no unvisited cells.
     * The cell and its neighbor are connected together.
     */
    private fun hunt(maze: FlatMaze): FlatCell? {
        // Find an unvisited cell next to a visited cell
        var unvisitedCell: FlatCell? = null
        var visitedNeighbor: FlatCell? = null

        maze.forEachCell<FlatCell> { cell ->
            if (!cell.visited) {
                val neighbors = cell.getNeighbors().toMutableList()
                for (neighbor in neighbors) {
                    if (neighbor.visited) {
                        // Neighbor was visited
                        unvisitedCell = cell
                        visitedNeighbor = neighbor
                        return@forEachCell true
                    }
                }
            }
            return@forEachCell false
        }

        unvisitedCell?.connectWith(visitedNeighbor!!)
        unvisitedCell?.visited = true

        return unvisitedCell
    }

}