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

import com.maltaisn.maze.Configuration
import com.maltaisn.maze.ParameterException
import com.maltaisn.maze.render.Canvas
import kotlin.random.Random


/**
 * Base class for a maze.
 */
abstract class Maze {

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
    protected var solution: ArrayList<Cell>? = null
        private set

    /**
     * Returns the cell at [pos] if it exists, otherwise returns null.
     */
    abstract fun cellAt(pos: Position): Cell?

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
    abstract fun getAllCells(): MutableList<out Cell>

    /**
     * Call [action] on every cell.
     */
    abstract fun forEachCell(action: (Cell) -> Unit)

    /**
     * Clears all sides of all cells in the maze, resets all visited flags.
     */
    fun resetAll() {
        forEachCell {
            it.value = 0
            it.visited = false
        }
    }

    /**
     * Sets all sides of all cells in the maze, resets all visited flags.
     */
    fun fillAll() {
        forEachCell {
            it.value = it.getAllSidesValue()
            it.visited = false
        }
    }

    /**
     * Create an [opening] in the maze. An exception is thrown if the opening position
     * doesn't match any cell or if the opening already exists.
     */
    fun createOpening(opening: Opening) {
        val cell = getOpeningCell(opening)
        if (cell != null) {
            if (openings.contains(cell)) {
                throw ParameterException("Duplicate opening.")
            }

            for (side in cell.getAllSides()) {
                if (cell.getCellOnSide(side) == null) {
                    cell.openSide(side)
                    break
                }
            }

            openings.add(cell)
        } else {
            throw ParameterException("Opening describes no cell in the maze.")
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
     *
     * This uses the A* star algorithm as described
     * [here](https://en.wikipedia.org/wiki/A*_search_algorithm#Description).
     *
     * 1. Make the starting cell the root node and add it to a list.
     * 2. Find the node in the list with the lowest "cost". Cost is the sum of the
     *    cost from the start and the lowest possible cost to the end. The cost from
     *    the start is the number of cells travelled to get to the node's cell. The
     *    lowest cost to the end is the minimum number of cells that have to be
     *    travelled to get to the end cell. Remove this node from the list.
     * 3. Add nodes of all unvisited neighbor of this node's cell
     *    to the list and mark them as visited.
     * 4. Repeat step 2 and 3 until the list is empty, hence there is no solution, or
     *    when the cell of the node selected at step 2 is the end cell.
     *
     * Runtime complexity is O(n) and memory space is O(n).
     */
    fun solve() {
        if (!generated) {
            throw IllegalStateException("Maze must be generated before being solved.")
        } else if (openings.size < 2) {
            throw ParameterException("Not enough openings to solve this maze.")
        }

        forEachCell { it.visited = false }

        val start = openings[0]
        val end = openings[1]

        val nodes = ArrayList<Node>()

        // Add the start cell as the initial node
        nodes.add(Node(null, start, 0,
                start.position.distanceTo(end.position)))

        while (nodes.isNotEmpty()) {
            // Find the node with the lowest cost and remove it from list.
            var lowestCost = Int.MAX_VALUE
            var lowestIndex = -1
            for (i in 0 until nodes.size) {
                val node = nodes[i]
                val cost = node.costFromStart + node.costToEnd
                if (cost < lowestCost) {
                    lowestCost = cost
                    lowestIndex = i
                }
            }
            val node = nodes.removeAt(lowestIndex)
            val cell = node.cell
            cell.visited = true

            if (cell === end) {
                // Found path to the end cell
                val path = ArrayList<Cell>()
                var currentNode: Node? = node
                while (currentNode != null) {
                    path.add(0, currentNode.cell)
                    currentNode = currentNode.parent
                }
                solution = path
                return
            }

            for (neighbor in cell.getAccessibleNeighbors()) {
                if (!neighbor.visited) {
                    // Add all unvisited neighbors to the nodes list
                    nodes.add(Node(node, neighbor, node.costFromStart + 1,
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
     * Open a number of deadends set by the braiding [setting].
     */
    fun braid(setting: Braiding) {
        val deadends = java.util.ArrayList<Cell>()
        forEachCell {
            if (it.countSides() == it.getAllSides().size - 1) {
                // A cell is a deadend if it has only one side opened.
                deadends.add(it)
            }
        }

        val count = setting.getNumberOfDeadendsToRemove(deadends.size)

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

    /**
     * Draw the maze to a [canvas] with [style] settings.
     */
    abstract fun drawTo(canvas: Canvas, style: Configuration.Style)

}