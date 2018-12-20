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

package com.maltaisn.mazegen.maze

import com.maltaisn.mazegen.maze.OrthogonalCell.Side
import java.util.*


/**
 * Class for a square-tiled orthogonal labyrinth made from a base maze.
 * A labyrinth has only one path and no deadends or junctions.
 * Each passage of the base maze is bissected with a wall to make it unicursal.
 */
class UnicursalOrthogonalMaze(width: Int, height: Int) :
        OrthogonalMaze(width, height) {

    /**
     * Convert an orthogonal [maze] into an [UnicursalOrthogonalMaze] twice the size.
     * The unicursal maze path always starts at (0, 0) at always ends at (0, 1).
     */
    constructor(maze: OrthogonalMaze): this(maze.width * 2, maze.height * 2) {
        // Double the size of every cell so that they each take a 2x2 square
        for (x in 0 until maze.width) {
            for (y in 0 until maze.height) {
                val cell = maze.cellAt(x, y)!!
                for (side in cell.getAllSides()) {
                    val nwCell = grid[2 * x][2 * y]
                    val neCell = grid[2 * x + 1][2 * y]
                    val swCell = grid[2 * x][2 * y + 1]
                    val seCell = grid[2 * x + 1][2 * y + 1]
                    if (cell.hasSide(Side.WEST)) {
                        nwCell.closeSide(Side.WEST)
                        swCell.closeSide(Side.WEST)
                    }
                    if (cell.hasSide(Side.NORTH)) {
                        nwCell.closeSide(Side.NORTH)
                        neCell.closeSide(Side.NORTH)
                    }
                    if (x == maze.width - 1 && cell.hasSide(Side.EAST)) {
                        neCell.closeSide(Side.EAST)
                        seCell.closeSide(Side.EAST)
                    }
                    if (y == maze.height - 1 && cell.hasSide(Side.SOUTH)) {
                        swCell.closeSide(Side.SOUTH)
                        seCell.closeSide(Side.SOUTH)
                    }
                }
            }
        }

        // Find paths going through all cells in the maze and make them walls
        maze.forEachCell { it.visited = false }
        val cells = LinkedList<OrthogonalCell>()
        val start = maze.cellAt(0, 0)!!
        start.visited = true
        cells.add(start)
        while (cells.isNotEmpty()) {
            val cell = cells.removeFirst()
            for (neighbor in cell.getAccessibleNeighbors()) {
                if (!neighbor.visited) {
                    cells.add(neighbor as OrthogonalCell)
                    neighbor.visited = true

                    val x1 = cell.position.x
                    val y1 = cell.position.y
                    val x2 = neighbor.position.x
                    val y2 = neighbor.position.y
                    if (x1 != x2) {
                        val cx = x1 + x2
                        grid[cx][2 * y1].closeSide(Side.SOUTH)
                        grid[cx + 1][2 * y1].closeSide(Side.SOUTH)
                    } else {
                        val cy = y1 + y2
                        grid[2 * x1][cy].closeSide(Side.EAST)
                        grid[2 * x1][cy + 1].closeSide(Side.EAST)
                    }
                }
            }
        }
        grid[0][0].closeSide(Side.SOUTH) // Or the result would just be one big loop.
    }

}