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
import com.maltaisn.maze.maze.RectCell.Side
import com.maltaisn.maze.maze.RectMaze
import java.util.*
import kotlin.random.Random


/**
 * Implementation of the recursive division algorithm as described
 * [here](http://weblog.jamisbuck.org/2011/1/12/maze-generation-recursive-division-algorithm).
 * Only works for orthogonal mazes. Unlike many others, this algorithm is a wall-adder.
 * The implementation doesn't actually make use of recursion to prevent stack overflow.
 *
 * 1. Start with a maze with no walls except borders
 * 2. Draw a wall splitting the area into two smaller areas and carve a passage in that wall.
 * 3. Repeat step 2 for each subareas until they are too small to be split.
 *
 * Runtime complexity is O(n) and memory space is O(n).
 * This is generally the fastest algorithm for orthogonal mazes.
 */
object RecursiveDivisionGenerator : Generator() {

    override fun generate(maze: Maze) {
        if (maze !is RectMaze) {
            throw IllegalArgumentException("Recursive division generator only work on orthogonal mazes.")
        }

        super.generate(maze)

        maze.resetAll()

        // Set the border walls
        for (i in 0 until maze.width) {
            maze.cellAt(i, 0)!!.closeSide(Side.NORTH)
            maze.cellAt(i, maze.height - 1)!!.closeSide(Side.SOUTH)
        }
        for (i in 0 until maze.height) {
            maze.cellAt(0, i)!!.closeSide(Side.WEST)
            maze.cellAt(maze.width - 1, i)!!.closeSide(Side.EAST)
        }

        // Start recursive division
        val areas = LinkedList<Area>()
        areas.add(Area(0, 0, maze.width, maze.height))
        while (areas.isNotEmpty()) {
            val area = areas.removeFirst()

            if (area.w == 1 || area.h == 1) {
                // Can't divide any further.
                continue
            }

            // Choose the orientation to divide on
            // Always divide the area on its longest side.
            val horizontal = (area.w < area.h || area.w == area.h && Random.nextBoolean())

            val sx: Int  // Wall start x
            val sy: Int  // Wall start y
            val dx: Int
            val dy: Int
            val length: Int  // Wall length
            val side: Side  // Side of cells to close to draw the wall
            if (horizontal) {
                sx = area.x
                sy = area.y + Random.nextInt(1, area.h)
                dx = 1
                dy = 0
                length = area.w
                side = Side.NORTH
            } else {
                sx = area.x + Random.nextInt(1, area.w)
                sy = area.y
                dx = 0
                dy = 1
                length = area.h
                side = Side.WEST
            }

            // Set the new wall and carve a passage into it
            val passagePos = Random.nextInt(length)
            var px = sx
            var py = sy
            for (i in 0 until length) {
                if (i != passagePos) {
                    maze.cellAt(px, py)!!.closeSide(side)
                }
                px += dx
                py += dy
            }

            // Divide the two created areas again
            if (horizontal) {
                areas.add(Area(area.x, area.y, area.w, py - area.y))
                areas.add(Area(area.x, py, area.w, area.h - (py - area.y)))
            } else {
                areas.add(Area(area.x, area.y, px - area.x, area.h))
                areas.add(Area(px, area.y, area.w - (px - area.x), area.h))
            }
        }
    }

    private data class Area(val x: Int, val y: Int, val w: Int, val h: Int)

}