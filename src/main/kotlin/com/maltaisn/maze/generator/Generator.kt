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

import com.maltaisn.maze.Configuration
import com.maltaisn.maze.MazeType
import com.maltaisn.maze.OutputFormat
import com.maltaisn.maze.ParameterException
import com.maltaisn.maze.maze.Maze
import com.maltaisn.maze.render.RasterCanvas
import java.awt.BasicStroke
import java.awt.Color
import java.io.File


/**
 * Base class for a maze generator. Comparison of generator algorithms can be found
 * [here](http://people.cs.ksu.edu/~ashley78/wiki.ashleycoleman.me/index.php/Perfect_Maze_Generators.html).
 *
 * @property name generator name.
 * @property supportedTypes vararg of supported maze types, empty for all types.
 */
abstract class Generator(private val name: String,
                         private vararg val supportedTypes: MazeType) {

    /**
     * Generate a maze into [maze].
     */
    open fun generate(maze: Maze) {
        // Check if maze was not already generated.
        if (maze.generated) {
            throw IllegalStateException("This maze was already generated.")
        }
        maze.generated = true

        // Make sure this generator supports the maze type
        if (supportedTypes.isNotEmpty() && maze.type !in supportedTypes) {
            throw ParameterException("$name generator cannot generate " +
                    "${maze.type.mazeName.toLowerCase()} mazes.")
        }

        debugExportStep = 0
    }

    // Debug exporting for displaying the mazes on each generator step.
    private var debugExportStep = 0

    protected fun exportMaze(maze: Maze) {
        val canvas = RasterCanvas(OutputFormat.PNG)
        maze.drawTo(canvas, Configuration.Style(30f, null, Color.BLACK,
                BasicStroke(3f), Color.BLUE, BasicStroke(3f), true))
        canvas.exportTo(File(File(System.getProperty("user.dir")), "mazes\\maze-$debugExportStep.png"))
        debugExportStep++
    }

}