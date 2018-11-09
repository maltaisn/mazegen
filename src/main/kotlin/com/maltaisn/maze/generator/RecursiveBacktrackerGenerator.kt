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


/**
 * Implementation of a recursive backtracking maze generator as described
 * [here](http://weblog.jamisbuck.org/2010/12/27/maze-generation-recursive-backtracking).
 *
 * 1. Make the initial cell the current cell and mark it as visited.
 * 2. Find unvisited neighbor to current cell and connect them.
 *    - If there is an unvisited neighbor, mark it as visited,
 *      add it to stack and make it the current cell.
 *    - If not, pop the last cell on the stack and make it the current cell.
 * 3. Repeat step 2 until stack is empty.
 */
class RecursiveBacktrackerGenerator(maze: Maze) : Generator(maze) {

    override fun generate() {
        maze.reset(false)

        // Get cell on a random starting location
        var currentCell = maze.getRandomCell()
        currentCell.visited = true

        val stack = mutableListOf<Cell>()
        while (true) {
            // Find an unvisited neighbor cell
            var unvisitedNeighbor: Cell? = null
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

                // Add current cell to the stack
                stack.add(currentCell)

                // Make the connected cell the current cell
                currentCell = unvisitedNeighbor
                currentCell.visited = true

            } else {
                // No unvisited neighbor cell
                // Go back to last cell in stack to check for unvisited neighbors
                if (stack.isNotEmpty()) {
                    currentCell = stack.removeAt(stack.size - 1)
                } else {
                    break
                }
            }
        }
    }

}