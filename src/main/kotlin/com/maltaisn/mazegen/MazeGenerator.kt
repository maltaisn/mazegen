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

package com.maltaisn.mazegen

import com.maltaisn.mazegen.maze.Maze
import com.maltaisn.mazegen.maze.UnicursalOrthogonalMaze
import java.io.File
import kotlin.system.measureTimeMillis


/**
 * Takes a configuration object to generate and export mazes to files.
 */
class MazeGenerator(private val config: Configuration) {

    /**
     * Generate, solve and export all mazes described by [config].
     */
    fun generate() {
        for (i in 0 until config.mazeSets.size) {
            val mazeSet = config.mazeSets[i]

            var indent1 = ""
            if (config.mazeSets.size > 1) {
                println("Generating maze set '${mazeSet.name}' (${i + 1} / ${config.mazeSets.size})")
                indent1 += "  "
            }

            for (j in 1..mazeSet.count) {
                var filename = mazeSet.name
                var indent2 = indent1
                if (mazeSet.count > 1) {
                    println(indent1 + "Generating maze $j / ${mazeSet.count}")
                    indent2 += "  "
                    filename += "-$j"
                }

                // Generate
                print(indent2 + "Generating...\r")
                var maze = mazeSet.creator()
                var duration = measureTimeMillis {
                    maze = generateMaze(maze, mazeSet)
                }
                println(indent2 + "Generated in $duration ms")

                // Solve
                if (mazeSet.solve) {
                    print(indent2 + "Solving...\r")
                    duration = measureTimeMillis {
                        maze.solve()
                    }
                    println(indent2 + "Solved in $duration ms")
                }

                // Distance map
                if (mazeSet.distanceMap) {
                    print("Generating distance map...\r")
                    duration = measureTimeMillis {
                        maze.generateDistanceMap(mazeSet.distanceMapStart)
                    }
                    println(indent2 + "Distance map generated in $duration ms")
                }

                // Export
                print(indent2 + "Exporting...\r")
                duration = measureTimeMillis {
                    exportMaze(maze, filename)
                }
                println(indent2 + "Exported in $duration ms")
            }

            println()
        }
    }

    /**
     * Generate, add openings and braid a [maze] with configuration of [mazeSet].
     */
    private fun generateMaze(maze: Maze, mazeSet: Configuration.MazeSet): Maze {
        var vmaze = maze

        // Generate
        mazeSet.generator.generate(vmaze)
        if (vmaze is UnicursalOrthogonalMaze) {
            vmaze = UnicursalOrthogonalMaze(vmaze)
        }

        // Add openings
        for (opening in mazeSet.openings) {
            vmaze.createOpening(opening)
        }

        // Braid
        if (mazeSet.braiding != null) {
            vmaze.braid(mazeSet.braiding)
        }

        return vmaze
    }

    /**
     * Write a [maze] to a file named [filename] (without the extension).
     */
    private fun exportMaze(maze: Maze, filename: String) {
        val output = config.output
        val style = config.style
        val format = output.format

        val canvas = output.createCanvas()
        canvas.antialiasing = style.antialiasing

        // Draw to canvas
        maze.drawTo(canvas, style)

        // Export to file
        val fullFilename = filename + '.' + format.extension
        val file = File(output.path, fullFilename)
        canvas.exportTo(file)
    }

}
