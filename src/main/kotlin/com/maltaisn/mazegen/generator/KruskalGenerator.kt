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

package com.maltaisn.mazegen.generator

import com.maltaisn.mazegen.maze.Cell
import com.maltaisn.mazegen.maze.Maze
import com.maltaisn.mazegen.maze.WeaveOrthogonalMaze
import com.maltaisn.mazegen.maze.ZetaMaze
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashSet


/**
 * Implementation of Kruskal's algorithm as described
 * [here](http://weblog.jamisbuck.org/2011/1/3/maze-generation-kruskal-s-algorithm).
 *
 * 1. Initialize a list with all edges and create an empty tree node for each cell.
 * 2. Pop a random edge from the list and if the trees of its cells are not connected,
 *    connect them and connect their trees' root
 * 3. Repeat step 2 until there are no more edges in the list.
 *
 * Kruskal's can't generate zeta and weave orthogonal mazes correctly. This is because
 * the neighbors of the cells change depending on how neighbors are connected together
 * but the implementation of the algorithm checks neighbors only at the beginning.
 *
 * Runtime complexity is O(n) and memory space is O(n).
 */
class KruskalGenerator : Generator() {

    override fun generate(maze: Maze) {
        super.generate(maze)

        maze.fillAll()

        // Get all edges and create a tree node for every cell
        val edgesSet = LinkedHashSet<Edge>()
        val nodesMap = HashMap<Cell, Node>()
        for (cell in maze.getAllCells()) {
            for (neighbor in cell.neighbors) {
                edgesSet.add(Edge(cell, neighbor))
            }
            nodesMap[cell] = Node()
        }
        val edges = edgesSet.toMutableList()
        edges.shuffle()

        while (edges.isNotEmpty()) {
            val edge = edges.removeAt(edges.size - 1)
            val node1 = nodesMap[edge.cell1]!!
            val node2 = nodesMap[edge.cell2]!!
            if (!node1.connectedTo(node2)) {
                node1.connect(node2)
                edge.cell1.connectWith(edge.cell2)
            }
        }
    }

    override fun isMazeSupported(maze: Maze) = maze !is ZetaMaze && maze !is WeaveOrthogonalMaze

    /**
     * A edge for a maze, between two cells, [cell1] and [cell2].
     * Edges are equal to each other if they have the two same cells.
     */
    private class Edge(val cell1: Cell, val cell2: Cell) {

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is Edge) return false
            return cell1 === other.cell1 && cell2 === other.cell2
                    || cell1 === other.cell2 && cell2 === other.cell1
        }

        override fun hashCode() = if (cell1.position < cell2.position) {
            Objects.hash(cell1, cell2)
        } else {
            Objects.hash(cell2, cell1)
        }

    }

    private class Node {

        var parent: Node? = null

        fun root(): Node = if (parent != null) {
            parent!!.root()
        } else {
            this
        }

        fun connectedTo(node: Node) = (root() === node.root())

        fun connect(node: Node) {
            node.root().parent = this
        }
    }

}
