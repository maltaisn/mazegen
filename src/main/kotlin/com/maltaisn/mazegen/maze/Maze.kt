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

import com.maltaisn.mazegen.Configuration
import com.maltaisn.mazegen.ParameterException
import com.maltaisn.mazegen.render.Canvas
import java.util.*
import kotlin.math.min
import kotlin.math.round
import kotlin.random.Random


/**
 * Base class for a maze.
 */
abstract class Maze {

    /**
     * The list of cell with openings in the maze.
     */
    private val openings = mutableListOf<Cell>()

    /**
     * The maze solution, a list of the path's cells starting from the first
     * opening and ending on the second. Null if no solution was found yet.
     */
    protected var solution: MutableList<Cell>? = null
        private set

    /**
     * Returns a random cell in the maze.
     */
    abstract val randomCell: Cell

    /**
     * Get the total number of cells in this maze.
     */
    abstract val cellCount: Int

    /**
     * Returns a list containing all the cells in this maze.
     */
    abstract val cellList: MutableList<out Cell>

    /**
     * Returns the cell at [pos] if it exists, otherwise returns null.
     */
    abstract fun cellAt(pos: Position): Cell?

    /**
     * Returns a random cell in the maze.
     */
    abstract fun getRandomCell(): Cell

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
            it.value = it.allSidesValue
            it.visited = false
        }
    }

    /**
     * Create an [opening] in the maze. An exception is thrown if the opening position
     * doesn't match any cell or if the opening already exists.
     */
    fun createOpening(opening: Position) {
        val cell = getOpeningCell(opening)
        if (cell != null) {
            if (openings.contains(cell)) {
                throw ParameterException("Duplicate opening for position ${cell.position}.")
            }

            for (side in cell.allSides) {
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
    abstract fun getOpeningCell(opening: Position): Cell?

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
    fun solve(): Boolean {
        if (openings.size < 2) {
            throw ParameterException("Not enough openings to solve this maze.")
        }

        forEachCell { it.visited = false }

        val start = openings[0]
        val end = openings[1]

        val nodes = PriorityQueue<Node>()

        // Add the start cell as the initial node
        nodes.add(Node(null, start, 0,
                start.position.distanceTo(end.position)))
        start.visited = true

        while (nodes.isNotEmpty()) {
            // Remove the node with the lowest cost from the queue.
            val node = nodes.remove()
            val cell = node.cell

            if (cell === end) {
                // Found path to the end cell
                val path = mutableListOf<Cell>()
                var currentNode: Node? = node
                while (currentNode != null) {
                    path.add(0, currentNode.cell)
                    currentNode = currentNode.parent
                }
                solution = path
                return true
            }

            for (neighbor in cell.findAccessibleNeighbors()) {
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
        return false
    }

    private data class Node(val parent: Node?, val cell: Cell,
                            val costFromStart: Int, val costToEnd: Int) : Comparable<Node> {

        override operator fun compareTo(other: Node): Int = (costFromStart + costToEnd)
                .compareTo(other.costFromStart + other.costToEnd)

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
        val deadends = mutableListOf<Cell>()
        forEachCell {
            if (it.sidesCount == it.allSides.size - 1) {
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

            for (side in deadend.allSides) {
                if (!deadend.hasSide(side)) {
                    val deadside = side.opposite
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
     * Braiding setting for a maze.
     */
    class Braiding {

        private val value: Number
        private val byCount: Boolean

        /**
         * Braiding setting to remove [count] deadends.
         */
        constructor(count: Int) {
            if (count < 0) {
                throw ParameterException("Braiding parameter must be a positive number.")
            }

            value = count
            byCount = true
        }

        /**
         * Braiding setting to remove a percentage, [percent], of the total number of deadends.
         */
        constructor(percent: Double) {
            if (percent < 0 || percent > 1) {
                throw ParameterException("Braiding percentage must be between 0 and 1 inclusive.")
            }

            value = percent
            byCount = false
        }

        /**
         * Get the number of deadends to remove with this braiding
         * setting out of the [total] number of deadends.
         */
        fun getNumberOfDeadendsToRemove(total: Int) = if (byCount) {
            min(value.toInt(), total)
        } else {
            round(total * value.toDouble()).toInt()
        }

        override fun toString() = "Remove " + if (byCount) {
            "$value deadends"
        } else {
            "${value.toDouble() * 100}% of deadends"
        }
    }

    companion object {
        const val OPENING_POS_START = Int.MIN_VALUE
        const val OPENING_POS_CENTER = Int.MIN_VALUE + 1
        const val OPENING_POS_END = Int.MIN_VALUE + 2
    }

}
