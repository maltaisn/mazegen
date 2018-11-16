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

package com.maltaisn.maze

import com.maltaisn.maze.generator.*
import com.maltaisn.maze.maze.*
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.io.FileInputStream
import java.io.PrintWriter


fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        val file = File(args[0])
        if (file.exists()) {
            val inputStream = FileInputStream(file)
            val config = JSONObject(JSONTokener(inputStream))
            inputStream.close()
            Main(config)
        } else {
            throw IllegalArgumentException("Config file doesn't exists.")
        }
    } else {
        throw IllegalArgumentException("No config file provided.")
    }
}

private class Main(from: JSONObject) {

    init {
        if (!from.has(KEY_MAZES)) {
            throw IllegalArgumentException("No mazes to generate.")
        }

        var output = File(System.getProperty("user.dir"))
        if (from.has(KEY_OUTPUT)) {
            output = File(from.getString(KEY_OUTPUT))
            output.mkdirs()
            if (!output.canWrite()) {
                throw IllegalArgumentException("Cannot write to output path: ${output.absolutePath}")
            }
        }

        for (mazeJson in from.getJSONArray(KEY_MAZES)) {
            val mazeGen = MazeGen(mazeJson as JSONObject)
            for (i in 0 until mazeGen.count) {
                val maze = mazeGen.generate()

                var filename = mazeGen.name
                if (mazeGen.count > 1) {
                    filename += "-${i + 1}"
                }
                filename += ".svg"

                PrintWriter(File(output, filename)).use {
                    it.print(maze.renderToSvg())
                }

                println("Generated and exported maze '${mazeGen.name}' ${i + 1} / ${mazeGen.count}")
            }
        }
    }

    private class MazeGen(from: JSONObject) {

        val maze: Maze
        val name: String
        var count: Int
        var generator: Generator

        init {
            name = if (from.has(KEY_NAME)) from.getString(KEY_NAME) else "maze"

            count = if (from.has(KEY_COUNT)) from.getInt(KEY_COUNT) else 1
            if (count < 1) {
                throw IllegalArgumentException("Wrong value '$count' for count, must be at least 1.")
            }

            generator = if (from.has(KEY_ALGORITHM)) {
                when (val algStr = from.getString(KEY_ALGORITHM)) {
                    "ab", "aldous-broder" -> AldousBroderGenerator()
                    "gt", "growing-tree" -> GrowingTreeGenerator()
                    "hk", "hunt-kill" -> HuntKillGenerator()
                    "kr", "kruskal" -> KruskalGenerator()
                    "pr", "prim" -> PrimGenerator()
                    "rb", "recursive-backtracker" -> RecursiveBacktrackerGenerator()
                    "wi", "wilson" -> WilsonGenerator()
                    else -> throw IllegalArgumentException("Invalid algorithm '$algStr'.")
                }
            } else {
                RecursiveBacktrackerGenerator()
            }

            val type = if (from.has(KEY_TYPE)) {
                when (val typeStr = from.getString(KEY_TYPE)) {
                    "rect", "orth" -> MazeType.RECT
                    "hex", "sigma" -> MazeType.HEX
                    "triangle", "delta" -> MazeType.DELTA
                    else -> throw IllegalArgumentException("Invalid maze type '$typeStr'.")
                }
            } else {
                MazeType.RECT
            }

            var width = if (from.has(KEY_WIDTH)) from.getInt(KEY_WIDTH) else null
            var height = if (from.has(KEY_HEIGHT)) from.getInt(KEY_HEIGHT) else null
            val dimension = if (from.has(KEY_DIMENSION)) from.getInt(KEY_DIMENSION) else null

            val arrangement = if (from.has(KEY_ARRANGEMENT)) {
                when (val arrStr = from.getString(KEY_ARRANGEMENT)) {
                    "rect", "rectangle" -> Arrangement.RECTANGLE
                    "triangle" -> Arrangement.TRIANGLE
                    "hex", "hexagon" -> Arrangement.HEXAGON
                    "rhombus" -> Arrangement.RHOMBUS
                    else -> throw IllegalArgumentException("Invalid maze arrangement '$arrStr'.")
                }
            } else {
                Arrangement.RECTANGLE
            }

            maze = when (type) {
                MazeType.RECT -> {
                    if (dimension == null && (width == null || height == null)
                            || dimension != null && (width != null || height != null)) {
                        throw IllegalArgumentException("For orthogonal mazes, only 'dimension' " +
                                "or both 'width' and 'height' must be defined.")
                    }
                    if (dimension != null) {
                        width = dimension
                        height = dimension
                    }
                    RectMaze(width!!, height!!)
                }
                MazeType.HEX, MazeType.DELTA -> {
                    when (arrangement) {
                        Arrangement.HEXAGON, Arrangement.TRIANGLE -> {
                            if (dimension == null || width != null || height != null) {
                                throw IllegalArgumentException("For hexagon and triangle shaped delta and sigma " +
                                        "mazes, only 'dimension' must be defined, but not 'width' nor 'height'.")
                            }
                            width = dimension
                            height = dimension
                        }
                        else -> {
                            if (dimension == null && (width == null || height == null)
                                    || dimension != null && (width != null || height != null)) {
                                throw IllegalArgumentException("For rectangle and rhombus shaped delta and sigma " +
                                        "mazes, only 'dimension' or both 'width' and 'height' must be defined.")
                            }
                            if (dimension != null) {
                                width = dimension
                                height = dimension
                            }
                        }
                    }
                    if (type == MazeType.HEX) {
                        HexMaze(width!!, height!!, arrangement)
                    } else {
                        DeltaMaze(width!!, height!!, arrangement)
                    }
                }
            }
        }

        fun generate(): Maze {
            generator.generate(maze)
            return maze
        }

        private enum class MazeType {
            RECT, HEX, DELTA
        }

        companion object {
            private const val KEY_NAME = "name"
            private const val KEY_COUNT = "count"
            private const val KEY_TYPE = "type"
            private const val KEY_WIDTH = "width"
            private const val KEY_HEIGHT = "height"
            private const val KEY_DIMENSION = "dimension"
            private const val KEY_ARRANGEMENT = "arrangement"
            private const val KEY_ALGORITHM = "algorithm"
        }

    }

    companion object {
        private const val KEY_MAZES = "mazes"
        private const val KEY_OUTPUT = "output"
    }

}