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

import com.maltaisn.mazegen.maze.Maze


/**
 * Implementation of Aldous-Broder's algorithm as described
 * [here](http://weblog.jamisbuck.org/2011/1/17/maze-generation-aldous-broder-algorithm).
 *
 * 1. Make the initial cell the current cell and mark it as visited.
 * 2. Choose a random neighbor. If the neighbor wasn't visited before:
 *     - Connect it with current cell.
 *     - Mark it as visited.
 *     - Make it the current cell.
 * 3. Repeat step 2 until set is empty.
 *
 * Generated mazes are uniform, meaning all possible mazes are generated
 * with equal probability. However this comes at the cost of efficiency,
 * which is very low, because each cell can be visited many times.
 * The algorithm has to find all the unvisited cells by walking randomly.
 *
 * Runtime complexity is O(n) at best and O(∞) at worst. Memory space is O(1).
 */
class AldousBroderGenerator : Generator() {

    override fun generate(maze: Maze) {
        super.generate(maze)

        maze.fillAll()

        var currentCell = maze.getRandomCell()
        currentCell.visited = true
        var remaining = maze.cellCount - 1
        do {
            // Choose a random neighbor
            val neighbor = currentCell.neighbors.random()
            if (!neighbor.visited) {
                // Neighbor isn't visited, connect it
                currentCell.connectWith(neighbor)
                neighbor.visited = true
                remaining--
            }
            currentCell = neighbor
        } while (remaining > 0)
    }

    override fun isMazeSupported(maze: Maze) = true

}
