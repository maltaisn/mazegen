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

package com.maltaisn.maze.maze

import com.maltaisn.maze.render.Canvas
import java.awt.BasicStroke
import java.awt.Color
import java.util.*


/**
 * Base class for a maze.
 */
abstract class Maze {

    /**
     * The name of the maze, can be null for none.
     */
    var name: String? = null

    /**
     * Whether this maze has been generated yet or not.
     * A maze can only be generated once.
     */
    var generated = false

    /**
     * The list of cell with openings in the maze.
     */
    private val openings = ArrayList<Cell>()

    /**
     * The maze solution, a list of the path's cells starting from the first
     * opening and ending on the second. Null if no solution was found yet.
     */
    var solution: LinkedList<Cell>? = null
        private set


    /**
     * Returns the cell at [pos].
     */
    abstract fun cellAt(pos: Position): Cell

    /**
     * Returns the cell at [pos] if it exists, otherwise returns null.
     */
    abstract fun optionalCellAt(pos: Position): Cell?

    /**
     * Returns a random cell in the maze.
     */
    abstract fun getRandomCell(): Cell

    /**
     * Get the total number of cells in this maze.
     */
    abstract fun getCellCount(): Int

    /**
     * Returns a set containing all the cells in this maze.
     */
    abstract fun getAllCells(): LinkedHashSet<out Cell>

    /**
     * Call [action] on every cell
     */
    abstract fun forEachCell(action: (Cell) -> (Unit))

    /**
     * Clear all sides of all cells in the maze.
     */
    fun resetAll() {
        forEachCell { it.value = 0 }
    }

    /**
     * Set all sides of all cells in the maze.
     */
    fun fillAll() {
        forEachCell { it.value = it.getAllSideValue().value }
    }

    /**
     * Create an [opening] in the maze. An exception is thrown if the opening position
     * doesn't match any cell or if the opening already exists.
     */
    fun createOpening(opening: Opening) {
        val cell = getOpeningCell(opening)
        if (cell != null) {
            if (openings.contains(cell)) {
                throw IllegalArgumentException("Duplicate opening.")
            }

            for (side in cell.getAllSides()) {
                if (cell.getCellOnSide(side) == null) {
                    cell.openSide(side)
                    break
                }
            }

            openings.add(cell)
        } else {
            throw IllegalArgumentException("Opening describes no cell in the maze.")
        }
    }

    /**
     * Get the cell described by the [opening] position.
     */
    abstract fun getOpeningCell(opening: Opening): Cell?

    /**
     * Find the solution of the maze, starting from the first opening and ending
     * on the second opening. The solution is a list of cells in the path, starting
     * from the start cell, or null if there's no solution.
     */
    fun solve() {
        if (!generated) {
            throw IllegalStateException("Maze must be generated before being solved.")
        } else if (openings.size < 2) {
            throw IllegalStateException("Not enough openings to solve this maze.")
        }

        forEachCell { it.visited = false }

        val start = openings[0]
        val end = openings[1]

        val set = LinkedHashSet<Node>()

        // Add the start cell as the initial node
        set.add(Node(null, start, 0,
                start.position.distanceTo(end.position)))

        while (set.isNotEmpty()) {
            // Find the frontier cell with the lowest cost and remove it from set.
            val node = set.minBy { it.costFromStart + it.costToEnd }!!
            set.remove(node)

            val cell = node.cell
            cell.visited = true
            for (neighbor in cell.getNeighbors()) {
                if (!neighbor.visited && !neighbor.hasSide(neighbor.findSideOfCell(cell)!!)) {
                    if (neighbor === end) {
                        // Found path to the end cell
                        val path = LinkedList<Cell>()
                        path.addFirst(end)
                        var currentNode: Node? = node
                        while (currentNode != null) {
                            path.addFirst(currentNode.cell)
                            currentNode = currentNode.parent
                        }
                        solution = path
                        return
                    }

                    // Add all unvisited neighbors to the frontier set
                    set.add(Node(node, neighbor, node.costFromStart + 1,
                            neighbor.position.distanceTo(end.position)))
                    neighbor.visited = true
                }
            }
        }

        // All cells were visited, no path was found.
        solution = null
    }

    private data class Node(val parent: Node?, val cell: Cell,
                            val costFromStart: Int, val costToEnd: Int) {

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is Node) return false
            return cell === other.cell
        }

        override fun hashCode(): Int = cell.hashCode()

    }


    /**
     * Draw the maze to a [canvas], with an arbritrary [cellSize] and other styling settings.
     */
    abstract fun drawTo(canvas: Canvas,
                        cellSize: Double, backgroundColor: Color?,
                        color: Color, stroke: BasicStroke,
                        solutionColor: Color, solutionStroke: BasicStroke)

}