/*
 * Copyright (c) 2019 Nicolas Maltais
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

import com.maltaisn.mazegen.maze.Cell
import com.maltaisn.mazegen.maze.Maze
import java.util.*


/**
 * Implementation of a recursive backtracking maze generator as described
 * [here](http://weblog.jamisbuck.org/2010/12/27/maze-generation-recursive-backtracking),
 * also known as randomized depth-first search.
 * The implementation doesn't actually make use of recursion to prevent stack overflow.
 *
 * 1. Make the initial cell the current cell and mark it as visited.
 * 2. Find unvisited neighbor to current cell and connect them.
 *    - If there is an unvisited neighbor, mark it as visited,
 *      add it to stack and make it the current cell.
 *    - If not, pop the last cell on the stack and make it the current cell.
 * 3. Repeat step 2 until stack is empty.
 *
 * Generated mazes have few deadends and long passages.
 *
 * Runtime complexity is O(n) and memory space is O(n).
 */
class RecursiveBacktrackerGenerator : Generator() {

    override fun generate(maze: Maze) {
        super.generate(maze)

        maze.fillAll()

        // Get cell on a random starting location
        var currentCell = maze.getRandomCell()
        currentCell.visited = true

        val stack = LinkedList<Cell>()
        while (true) {
            // Find an unvisited neighbor cell
            val unvisitedNeighbor = currentCell.neighbors.shuffled().find { !it.visited }
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
                    currentCell = stack.removeLast()
                } else {
                    break
                }
            }
        }
    }

    override fun isMazeSupported(maze: Maze) = true

}
