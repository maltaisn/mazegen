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

import com.maltaisn.maze.ParameterException
import com.maltaisn.maze.generator.*
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.reflect.full.primaryConstructor
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class GeneratorTest {

    /**
     * For all generators, test all supported maze types 50 times each.
     * Each maze is then checked for loops and cell accessibility.
     */
    @Test
    fun testAllMazesWithAllGenerators() {
        for (generator in GENERATORS) {
            typeLoop@
            for (type in MAZE_TYPES) {
                val allParams = when (type) {
                    OrthogonalMaze::class, UnicursalOrthogonalMaze::class,
                    UpsilonMaze::class, ZetaMaze::class -> arrayOf(arrayOf(MAZE_SIZE, MAZE_SIZE))
                    DeltaMaze::class, SigmaMaze::class -> {
                        val shapes = BaseShapedMaze.Shape.values()
                        Array(shapes.size) { arrayOf(MAZE_SIZE, MAZE_SIZE, shapes[it]) }
                    }
                    ThetaMaze::class -> arrayOf(arrayOf(MAZE_SIZE, 1f, 1.5f))
                    WeaveOrthogonalMaze::class -> Array(5) { arrayOf(MAZE_SIZE, MAZE_SIZE, it) }
                    else -> throw IllegalStateException()
                }

                for (params in allParams) {
                    println("${generator::class.simpleName} on ${type.simpleName}, " +
                            "params: ${Arrays.toString(params)}")
                    for (i in 0 until REPEAT) {
                        var maze = type.primaryConstructor?.call(*params)!!
                        try {
                            generator.generate(maze)
                        } catch (e: ParameterException) {
                            // Generator doesn't support this maze type
                            continue@typeLoop
                        }
                        if (maze is UnicursalOrthogonalMaze) {
                            maze = UnicursalOrthogonalMaze(maze)
                        }

                        // Make sure maze is perfect
                        assertPerfectMaze(maze)

                        // Try to solve the maze, must have a solution
                        maze.createOpening(Position2D(Maze.OPENING_POS_START, Maze.OPENING_POS_START))
                        maze.createOpening(Position2D(Maze.OPENING_POS_END, Maze.OPENING_POS_END))
                        assertTrue(maze.solve(), "Maze has no solution")
                    }
                }
            }
        }
    }

    /**
     * Check if [maze] is perfect, meaning there's always exactly one path going from
     * any two points in the maze. For that to be true, all cells must be accessible and
     * there must be no loops in the maze.
     */
    private fun assertPerfectMaze(maze: Maze) {
        maze.forEachCell { it.visited = false }
        val cells = LinkedList<Cell>()
        var unvisited = maze.getCellCount() - 1
        val start = maze.getRandomCell()
        start.visited = true
        cells.add(start)
        while (cells.isNotEmpty()) {
            val cell = cells.removeFirst()
            var visitedCount = 0
            for (neighbor in cell.getAccessibleNeighbors()) {
                if (neighbor.visited) {
                    visitedCount++
                } else {
                    cells.add(neighbor)
                    neighbor.visited = true
                    unvisited--
                }
            }
            if (cell !== start) {
                assertEquals(1, visitedCount, "Maze is not perfect, it contains loops.")
            }
        }
        assertEquals(0, unvisited, "Maze is not perfect, it has inaccessible parts.")
    }

    companion object {
        private const val MAZE_SIZE = 20
        private const val REPEAT = 50

        private val MAZE_TYPES = arrayOf(
                DeltaMaze::class,
                OrthogonalMaze::class,
                UnicursalOrthogonalMaze::class,
                WeaveOrthogonalMaze::class,
                SigmaMaze::class,
                ThetaMaze::class,
                UpsilonMaze::class,
                ZetaMaze::class
        )

        private val GENERATORS = arrayOf(
                AldousBroderGenerator(),
                BinaryTreeGenerator(),
                EllerGenerator(),
                GrowingTreeGenerator(),
                HuntKillGenerator(),
                KruskalGenerator(),
                PrimGenerator(),
                RecursiveBacktrackerGenerator(),
                RecursiveDivisionGenerator(),
                SidewinderGenerator(),
                WilsonGenerator()
        )
    }

}