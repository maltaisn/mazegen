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

import com.maltaisn.maze.maze.Maze
import com.maltaisn.maze.maze.OrthogonalCell.Side
import com.maltaisn.maze.maze.OrthogonalMaze
import com.maltaisn.maze.maze.UnicursalOrthogonalMaze
import kotlin.random.Random


/**
 * Implementation of the binary tree algorithm as described
 * [here](http://weblog.jamisbuck.org/2011/2/1/maze-generation-binary-tree-algorithm).
 * Only works for orthogonal mazes.
 *
 * 1. Iterate over all cells, randomly opening one side out
 *    of two possibles sides, for example north and east.
 *
 * Runtime complexity is O(n) and memory space is O(1).
 */
class BinaryTreeGenerator : Generator(
        OrthogonalMaze::class, UnicursalOrthogonalMaze::class) {

    /**
     * Bias setting that decides which two sides to connect for a cell.
     * There will always be straight passages the length of the maze on these two sides.
     */
    var bias: Bias = Bias.NORTH_EAST


    override fun generate(maze: Maze) {
        super.generate(maze)
        maze as OrthogonalMaze

        maze.fillAll()

        maze.forEachCell {
            // Randomly select a cell on a side to connect to
            val cell1 = it.getCellOnSide(bias.side1)
            val cell2 = it.getCellOnSide(bias.side2)
            val cell = if (Random.nextBoolean()) {
                cell1 ?: cell2
            } else {
                cell2 ?: cell1
            }

            // Connect the two cells if one was found
            if (cell != null) {
                it.connectWith(cell)
            }
        }
    }

    enum class Bias(val side1: Side, val side2: Side) {
        NORTH_EAST(Side.NORTH, Side.EAST),
        NORTH_WEST(Side.NORTH, Side.WEST),
        SOUTH_EAST(Side.SOUTH, Side.EAST),
        SOUTH_WEST(Side.SOUTH, Side.WEST);
    }

}