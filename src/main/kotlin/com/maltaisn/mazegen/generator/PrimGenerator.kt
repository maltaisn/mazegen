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
 * Implementation of Prim's algorithm as described
 * [here](http://weblog.jamisbuck.org/2011/1/10/maze-generation-prim-s-algorithm).
 *
 * 1. Create an empty set containing only a random initial cell, marked as visited.
 * 2. Choose a random cell from the set
 *     - Connect it with a visited neighbor.
 *     - Add all of its unvisited neighbors to the set.
 *     - Remove it from the set.
 *     - Mark it as visited.
 * 3. Repeat step 2 until set is empty.
 *
 * Generated mazes have a lot more deadends than those generated with the recursive
 * backtracker for example, there are also very few long passages.
 *
 * Runtime complexity is O(n) and memory space is O(n).
 */
class PrimGenerator : Generator() {

    override fun generate(maze: Maze) {
        super.generate(maze)

        maze.fillAll()

        val initialCell = maze.getRandomCell()
        initialCell.visited = true
        val set = mutableSetOf(initialCell)
        do {
            val currentCell = set.random()
            var connected = false
            for (neighbor in currentCell.neighbors.shuffled()) {
                if (!connected && neighbor.visited) {
                    currentCell.connectWith(neighbor)
                    connected = true
                } else if (!neighbor.visited) {
                    set.add(neighbor)
                }
            }
            currentCell.visited = true
            set.remove(currentCell)
        } while (set.isNotEmpty())
    }

    override fun isMazeSupported(maze: Maze) = true

}
