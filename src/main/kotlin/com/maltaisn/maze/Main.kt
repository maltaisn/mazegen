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
import com.maltaisn.maze.maze.Arrangement
import com.maltaisn.maze.maze.DeltaMaze
import com.maltaisn.maze.maze.HexMaze
import com.maltaisn.maze.maze.RectMaze
import picocli.CommandLine
import picocli.CommandLine.*
import java.io.File
import java.io.PrintWriter

fun main(args: Array<String>) {
    CommandLine.run(Options(), *args)
}

@Command()
private class Options : Runnable {

    @Spec
    private lateinit var commandSpec: Model.CommandSpec

    @Option(names = ["-g", "--generator"],
            description = ["Maze generation algorithm to use, one of:" +
                    "%n'ab', 'aldous-broder' for Aldous-Broder's." +
                    "%n'gt', 'growing-tree' for growing tree." +
                    "%n'hk', 'hunt-kill' for hunt-and-kill." +
                    "%n'kr', 'kruskal' for Kruskal's." +
                    "%n'pr', 'prim' for Prim's." +
                    "%n'rb', 'recursive-backtracker' for recursive backtracker (default)" +
                    "%n'wi', 'wilson' for Wilson's."],
            converter = [GeneratorConverter::class])
    private var generator: Generator = RecursiveBacktrackerGenerator()

    @Option(names = ["-c", "--count"],
            description = ["Number of mazes to generate. Default is 1."])
    private var count: Int = 1

    @Option(names = ["-o", "--output"],
            description = ["Output path. Default output is current directory."])
    private var output: File = File(System.getProperty("user.dir"))

    @Option(names = ["-f", "--filename"],
            description = ["Output filename, without extension. Default is 'maze'."])
    private var filename: String = "maze"

    @Option(names = ["-t", "--type"],
            description = ["Maze type, one of:" +
                    "%n'rect', 'orth' for rectangular cells." +
                    "%n'hex', 'sigma' for hexagonal cells." +
                    "%n'triangle', 'delta' for triangular cells."],
            converter = [MazeTypeConverter::class])
    private var mazeType: MazeType = MazeType.RECT

    @Option(names = ["-d", "--dimen"],
            description = ["Maze dimensions, depends on the maze type."],
            arity = "1..*",
            required = true)
    private lateinit var dimensions: IntArray

    @Option(names = ["-a", "--arrangement"],
            description = ["Arrangment of maze cells for hex and delta mazes, one of:" +
                    "%n'rect', 'rectangle': rectangle shaped." +
                    "%n'triangle': triangle shaped." +
                    "%n'hex', 'hexagon': hexagon shaped. " +
                    "%n'rhombus': rhombus (parallelogram) shaped."],
            converter = [ArrangementConverter::class])
    private var arrangement: Arrangement = Arrangement.RECTANGLE


    override fun run() {
        // Validate output path
        output.mkdirs()
        if (!output.canWrite()) {
            throw ParameterException(commandSpec.commandLine(),
                    "No write access to output path: ${output.absolutePath}")
        }

        // Validate count
        if (count < 1) {
            throw ParameterException(commandSpec.commandLine(),
                    "At least 1 maze must be generated.")
        }

        // Validate dimensions
        for (dimen in dimensions) {
            if (dimen < MIN_DIMENSION || dimen > MAX_DIMENSION) {
                throw ParameterException(commandSpec.commandLine(),
                        "Dimensions must be between $MIN_DIMENSION and $MAX_DIMENSION.")
            }
        }

        // Create the maze template from parameters
        val maze = when (mazeType) {
            MazeType.RECT -> {
                val width = dimensions[0]
                val height = when {
                    dimensions.size == 1 -> width
                    dimensions.size == 2 -> dimensions[1]
                    else -> {
                        throw ParameterException(commandSpec.commandLine(),
                                "Too many dimensions given for rectangular maze.")
                    }
                }
                println()
                RectMaze(width, height)
            }
            MazeType.HEX, MazeType.DELTA -> {
                val width = dimensions[0]
                val height = when {
                    dimensions.size == 1 -> width
                    dimensions.size == 2 && (arrangement == Arrangement.RECTANGLE
                            || arrangement == Arrangement.RHOMBUS) -> dimensions[1]
                    else -> {
                        throw ParameterException(commandSpec.commandLine(),
                                "Too many dimensions given for maze type and arrangement.")
                    }
                }
                if (mazeType == MazeType.HEX) {
                    HexMaze(width, height, arrangement)
                } else {
                    DeltaMaze(width, height, arrangement)
                }
            }
        }

        // Generate and render mazes
        for (i in 1..count) {
            generator.generate(maze)

            var name: String
            if (count == 1) {
                name = filename
            } else {
                name = "$filename-$i"
            }
            name += ".svg"

            PrintWriter(File(output, name)).use {
                it.print(maze.renderToSvg())
            }
            println("Generated maze $i / $count")
        }
    }

    private enum class MazeType {
        RECT, HEX, DELTA
    }

    private class GeneratorConverter : ITypeConverter<Generator> {
        override fun convert(value: String) = when (value) {
            "ab", "aldous-broder" -> AldousBroderGenerator()
            "gt", "growing-tree" -> GrowingTreeGenerator()
            "hk", "hunt-kill" -> HuntKillGenerator()
            "kr", "kruskal" -> KruskalGenerator()
            "pr", "prim" -> PrimGenerator()
            "rb", "recursive-backtracker" -> RecursiveBacktrackerGenerator()
            "wi", "wilson" -> WilsonGenerator()
            else -> throw IllegalArgumentException("Invalid algorithm.")
        }
    }

    private class MazeTypeConverter : ITypeConverter<MazeType> {
        override fun convert(value: String) = when (value) {
            "rect", "orth" -> MazeType.RECT
            "hex", "sigma" -> MazeType.HEX
            "triangle", "delta" -> MazeType.DELTA
            else -> throw IllegalArgumentException("Invalid maze type.")
        }
    }

    private class ArrangementConverter : ITypeConverter<Arrangement> {
        override fun convert(value: String) = when (value) {
            "rect", "rectangle" -> Arrangement.RECTANGLE
            "triangle" -> Arrangement.TRIANGLE
            "hex", "hexagon" -> Arrangement.HEXAGON
            "rhombus" -> Arrangement.RHOMBUS
            else -> throw IllegalArgumentException("Invalid maze arrangement.")
        }
    }

    companion object {
        private const val MIN_DIMENSION = 1
        private const val MAX_DIMENSION = 65536
    }

}