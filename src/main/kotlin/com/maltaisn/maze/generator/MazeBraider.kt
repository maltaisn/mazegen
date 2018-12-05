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
import java.util.*
import kotlin.random.Random


/**
 * Class that "braids" a maze, meaning it removes deadends
 * by connecting them with the cell on the other side of the deadend.
 *
 * Runtime complexity is O(n) and memory space is O(n).
 */
class MazeBraider {

    /**
     * Remove [count] deadends from [maze].
     */
    fun braidByCount(maze: Maze, count: Int) {
        if (count > 0) {
            val deadends = getAllDeadends(maze)
            braid(deadends, count)
        }
    }

    /**
     * Remove a percentage of deadends from [maze].
     * @param[percent] Percentage, from 0 to 1.
     */
    fun braidByPercentage(maze: Maze, percent: Double) {
        if (percent > 0) {
            val deadends = getAllDeadends(maze)
            braid(deadends, (deadends.size * percent).toInt())
        }
    }

    /**
     * Get a set of all deadends in [maze].
     */
    private fun getAllDeadends(maze: Maze): MutableList<Cell> {
        val deadends = ArrayList<Cell>()
        maze.forEachCell {
            if (it.countSides() == it.getAllSides().size - 1) {
                // A cell is a deadend if it has only one side opened.
                deadends.add(it)
            }
        }
        return deadends
    }

    /**
     * Open [count] deadends from a list of deadends.
     */
    private fun braid(deadends: MutableList<Cell>, count: Int) {
        var removed = 0
        while (deadends.isNotEmpty() && removed < count) {
            val index = Random.nextInt(deadends.size)
            val deadend = deadends[index]
            deadends.removeAt(index)

            for (side in deadend.getAllSides()) {
                if (!deadend.hasSide(side)) {
                    val deadside = side.opposite()
                    val other = deadend.getCellOnSide(deadside)
                    if (other != null) {
                        // If the deadside is not a border, open it.
                        deadend.connectWith(other)
                    }
                    break
                }
            }
            removed++
        }
    }

}