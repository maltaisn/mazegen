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

package com.maltaisn.mazegen.maze

import com.maltaisn.mazegen.ParameterException
import com.maltaisn.mazegen.generator.*
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue


internal class GeneratorTest {

    private var mazesGenerated = 0

    /**
     * For all genenerators, test all maze types on 4 different sizes, 30 times each.
     * Each maze is then checked for loops and cell accessibility (perfect maze).
     * In total, 19 400 mazes are generated and checked in 2-4 min.
     */
    @Test
    fun testAllMazesWithAllGenerators() {
        for (generator in GENERATORS) {
            println("====== ${generator.javaClass.simpleName} ======")

            // Orthogonal (120 mazes)
            println("\n=== ORTHOGONAL ===")
            testMazeType { size ->
                val maze = OrthogonalMaze(size, size)
                generator.generate(maze)
                maze
            }

            // Upsilon (120 mazes)
            println("\n=== UPSILON ===")
            testMazeType { size ->
                val maze = UpsilonMaze(size, size)
                generator.generate(maze)
                maze
            }

            // Zeta (120 mazes)
            println("\n=== ZETA ===")
            testMazeType { size ->
                val maze = ZetaMaze(size, size)
                generator.generate(maze)
                maze
            }

            // Unicursal orthogonal (120 mazes)
            println("\n=== UNICURSAL ORTHOGONAL ===")
            testMazeType { size ->
                val maze = UnicursalOrthogonalMaze(size, size)
                generator.generate(maze)
                UnicursalOrthogonalMaze(maze)
            }

            // Weave orthogonal (600 mazes)
            println("\n=== WEAVE ORTHOGONAL ===")
            for (maxWeave in 0..4) {
                testMazeType { size ->
                    val maze = WeaveOrthogonalMaze(size, size, maxWeave)
                    generator.generate(maze)
                    maze
                }
            }

            // Delta (480 mazes)
            println("\n=== DELTA ===")
            for (shape in BaseShapedMaze.Shape.values()) {
                println("SHAPE = ${shape.toString().toLowerCase()}")
                testMazeType { size ->
                    val maze = DeltaMaze(size, size, shape)
                    generator.generate(maze)
                    maze
                }
            }

            // Sigma (480 mazes)
            println("\n=== SIGMA ===")
            for (shape in BaseShapedMaze.Shape.values()) {
                println("SHAPE = ${shape.toString().toLowerCase()}")
                testMazeType { size ->
                    val maze = SigmaMaze(size, size, shape)
                    generator.generate(maze)
                    maze
                }
            }

            // Theta (800 mazes)
            println("\n=== THETA ===")
            for (centerRadius in 1..4) {
                println("CENTER RADIUS = $centerRadius")
                for (subdivisionFactor in THETA_SUBDIVISIONS) {
                    println("SUBDIVISION = $subdivisionFactor")
                    testMazeType(10) { size ->
                        val maze = ThetaMaze(size, centerRadius.toDouble(), subdivisionFactor)
                        generator.generate(maze)
                        maze
                    }
                }
            }

            println()
        }

        println("Done. $mazesGenerated mazes generated.")
    }

    /**
     * Test mazes created by a lambda on many sizes, 30 times each.
     */
    private fun testMazeType(count: Int = 30, mazeCreator: (Int) -> Maze) {
        for (size in MAZE_SIZES) {
            repeat(count) {
                val maze = try {
                    mazeCreator(size)
                } catch (e: ParameterException) {
                    // Maze type not supported by the generator.
                    return
                }

                if (it == 0) {
                    println("SIZE = $size")
                }
                println("Maze ${it + 1} / $count")

                // Make sure maze is perfect
                assertPerfectMaze(maze)

                // Try to solve the maze, must have a solution
                maze.createOpening(Position2D(Maze.OPENING_POS_START, Maze.OPENING_POS_START))
                maze.createOpening(Position2D(Maze.OPENING_POS_END, Maze.OPENING_POS_END))
                assertTrue(maze.solve(), "Maze has no solution")

                mazesGenerated++
            }
            println()
        }
        println()
    }

    /**
     * Check if [maze] is perfect, meaning there's always exactly one path going from
     * any two points in the maze. For that to be true, all cells must be accessible and
     * there must be no loops in the maze.
     */
    private fun assertPerfectMaze(maze: Maze) {
        maze.forEachCell { it.visited = false }
        val cells = LinkedList<Cell>()
        var unvisited = maze.cellCount - 1
        val start = maze.getRandomCell()
        start.visited = true
        cells.add(start)
        while (cells.isNotEmpty()) {
            val cell = cells.removeFirst()
            var visitedCount = 0
            for (neighbor in cell.findAccessibleNeighbors()) {
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
        private val MAZE_SIZES = listOf(2, 5, 10, 20)
        private val THETA_SUBDIVISIONS = listOf(0.5, 1.0, 1.5, 2.5, 4.0)

        private val GENERATORS = listOf(
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
