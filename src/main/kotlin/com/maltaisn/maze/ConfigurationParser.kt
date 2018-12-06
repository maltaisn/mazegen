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

import com.maltaisn.maze.Configuration.MazeSet
import com.maltaisn.maze.Configuration.MazeType
import com.maltaisn.maze.generator.*
import com.maltaisn.maze.maze.*
import com.maltaisn.maze.render.Canvas
import org.json.JSONArray
import org.json.JSONObject
import java.awt.BasicStroke
import java.awt.Color
import java.io.File
import java.util.*


/**
 * Parser for the JSON configuration file into a [Configuration] object.
 */
class ConfigurationParser {

    fun parse(from: JSONObject): Configuration {
        // Parse maze sets
        val mazeSets = ArrayList<MazeSet>()
        val names = HashSet<String>()
        if (from.has(KEY_MAZE)) {
            for (mazeJson in from.get(KEY_MAZE) as JSONArray) {
                val mazeSet = parseMazeSet(mazeJson as JSONObject)
                mazeSets.add(mazeSet)

                // Make sure name is unique or one set will overwrite the other
                var name = mazeSet.name
                var number = 2
                while (names.contains(name)) {
                    name = mazeSet.name + '-' + number
                    number++
                }
                mazeSet.name = name
                names.add(name)
            }
        } else {
            throw ParameterException("No mazes to generate.")
        }

        // Parse output settings
        val output = parseOutput(if (from.has(KEY_OUTPUT))
            from.getJSONObject(KEY_OUTPUT) else null)

        // Parse style settings 
        val style = parseStyle(if (from.has(KEY_STYLE))
            from.getJSONObject(KEY_STYLE) else null)

        return Configuration(mazeSets, output, style)
    }

    private fun parseMazeSet(from: JSONObject): Configuration.MazeSet {
        // Name
        val name = if (from.has(KEY_MAZE_NAME))
            from.getString(KEY_MAZE_NAME) else DEFAULT_MAZE_NAME

        // Count
        val count = if (from.has(KEY_MAZE_COUNT))
            from.getInt(KEY_MAZE_COUNT) else DEFAULT_MAZE_COUNT
        if (count < 1) {
            throw ParameterException("At least one maze must be generated for maze with name '$name'.")
        }

        // Generator and braiding
        val algorithmJson = if (from.has(KEY_MAZE_ALGORITHM))
            from.get(KEY_MAZE_ALGORITHM) else null
        var algorithm = DEFAULT_MAZE_ALGORITHM
        var braiding: Braiding? = DEFAULT_MAZE_ALGORITHM_BRAID
        if (algorithmJson is String) {
            algorithm = algorithmJson
        } else if (algorithmJson is JSONObject) {
            if (algorithmJson.has(KEY_MAZE_ALGORITHM_NAME)) {
                algorithm = algorithmJson.getString(KEY_MAZE_ALGORITHM_NAME)
            }

            if (algorithmJson.has(KEY_MAZE_ALGORITHM_BRAID)) {
                val braid = algorithmJson.get(KEY_MAZE_ALGORITHM_BRAID)
                if (braid is Int) {
                    braiding = Braiding(braid)
                } else if (braid is String) {
                    if (braid.endsWith('%')) {
                        val percent = braid.substring(0, braid.length - 1).toDouble() / 100
                        braiding = Braiding(percent)
                    } else {
                        braiding = Braiding(braid.toInt())
                    }
                }
            }
        }
        val generator = when (algorithm) {
            "ab", "aldous-broder" -> AldousBroderGenerator()
            "gt", "growing-tree" -> GrowingTreeGenerator()
            "hk", "hunt-kill" -> HuntKillGenerator()
            "kr", "kruskal" -> KruskalGenerator()
            "pr", "prim" -> PrimGenerator()
            "rb", "recursive-backtracker" -> RecursiveBacktrackerGenerator()
            "rd", "recursive-division" -> RecursiveDivisionGenerator()
            "wi", "wilson" -> WilsonGenerator()
            else -> throw ParameterException("Invalid algorithm '$algorithm'.")
        }

        // Generactor-specific settings
        if (generator is GrowingTreeGenerator && algorithmJson is JSONObject) {
            if (algorithmJson.has(KEY_MAZE_ALGORITHM_GT_WEIGHTS)) {
                val weights = algorithmJson.getJSONArray(KEY_MAZE_ALGORITHM_GT_WEIGHTS)
                generator.randomWeight = weights[0] as Int
                generator.newestWeight = weights[1] as Int
                generator.oldestWeight = weights[1] as Int
            }
        }

        // Type
        val type = if (from.has(KEY_MAZE_TYPE)) {
            when (val typeStr = from.getString(KEY_MAZE_TYPE)) {
                "rect", "orth" -> MazeType.RECT
                "hex", "sigma" -> MazeType.HEX
                "triangle", "delta" -> MazeType.DELTA
                "polar", "circle", "theta" -> MazeType.POLAR
                else -> throw ParameterException("Invalid maze type '$typeStr'.")
            }
        } else {
            DEFAULT_MAZE_TYPE
        }

        // Size
        val size = if (from.has(KEY_MAZE_SIZE)) {
            from.get(KEY_MAZE_SIZE)
        } else {
            throw ParameterException("A size must be specified for the maze.")
        }

        // Arrangement
        val arrangement = if (from.has(KEY_MAZE_ARRANGEMENT)) {
            when (val arrStr = from.getString(KEY_MAZE_ARRANGEMENT)) {
                "rect", "rectangle" -> Arrangement.RECTANGLE
                "triangle" -> Arrangement.TRIANGLE
                "hex", "hexagon" -> Arrangement.HEXAGON
                "rhombus" -> Arrangement.RHOMBUS
                else -> throw ParameterException("Invalid maze arrangement '$arrStr'.")
            }
        } else {
            DEFAULT_MAZE_ARRANGEMENT
        }

        // Maze creator
        val mazeCreator: () -> Maze = when (type) {
            MazeType.RECT -> {
                val width: Int
                val height: Int
                if (size is Int) {
                    width = size
                    height = size
                } else {
                    val sizeJson = size as JSONObject
                    width = sizeJson.getInt(KEY_MAZE_SIZE_WIDTH)
                    height = sizeJson.getInt(KEY_MAZE_SIZE_HEIGHT)
                }
                { RectMaze(width, height) }
            }
            MazeType.HEX, MazeType.DELTA -> {
                val width: Int
                val height: Int
                if (size is Int) {
                    width = size
                    height = size
                } else {
                    if (arrangement == Arrangement.HEXAGON || arrangement == Arrangement.TRIANGLE) {
                        throw ParameterException("For hexagon and triangle shaped " +
                                "delta and sigma mazes, size must be an integer.")
                    }
                    val sizeJson = size as JSONObject
                    width = sizeJson.getInt(KEY_MAZE_SIZE_WIDTH)
                    height = sizeJson.getInt(KEY_MAZE_SIZE_HEIGHT)
                }
                {
                    if (type == MazeType.HEX) {
                        HexMaze(width, height, arrangement)
                    } else {
                        DeltaMaze(width, height, arrangement)
                    }
                }
            }
            MazeType.POLAR -> {
                val radius: Int
                var centerRadius = DEFAULT_MAZE_SIZE_CENTER_RADIUS
                var subdivision = DEFAULT_MAZE_SIZE_SUBDIVISION
                if (size is Int) {
                    radius = size
                } else {
                    val sizeJson = size as JSONObject
                    radius = sizeJson.getInt(KEY_MAZE_SIZE_RADIUS)
                    if (sizeJson.has(KEY_MAZE_SIZE_CENTER_RADIUS)) {
                        centerRadius = sizeJson.getFloat(KEY_MAZE_SIZE_CENTER_RADIUS)
                    }
                    if (sizeJson.has(KEY_MAZE_SIZE_SUBDIVISION)) {
                        subdivision = sizeJson.getFloat(KEY_MAZE_SIZE_SUBDIVISION)
                    }
                }
                { PolarMaze(radius, centerRadius, subdivision) }
            }
        }

        // Openings
        val openings = ArrayList<Opening>()
        if (from.has(KEY_MAZE_OPENINGS)) {
            val openingsJson = from.getJSONArray(KEY_MAZE_OPENINGS)
            for (openingJson in openingsJson) {
                val openingArray = openingJson as JSONArray
                val position = IntArray(openingArray.length())
                for (i in 0 until openingArray.length()) {
                    val pos = openingArray[i]
                    position[i] = when (pos) {
                        is String -> when (pos[0].toUpperCase()) {
                            KEY_MAZE_OPENING_START -> Opening.POS_START
                            KEY_MAZE_OPENING_CENTER -> Opening.POS_CENTER
                            KEY_MAZE_OPENING_END -> Opening.POS_END
                            else -> throw ParameterException("Invalid opening position character '$pos'.")
                        }
                        is Int -> pos
                        else -> throw ParameterException("Invalid opening position argument '$pos'.")
                    }
                }
                openings.add(Opening(position))
            }
        }

        // Solve
        val solve = if (from.has(KEY_MAZE_SOLVE))
            from.getBoolean(KEY_MAZE_SOLVE) else DEFAULT_MAZE_SOLVE

        return MazeSet(name, count, mazeCreator, generator, braiding, openings, solve)
    }

    private fun parseOutput(from: JSONObject?): Configuration.Output {
        var path = DEFAULT_OUTPUT_PATH
        var format = DEFAULT_OUTPUT_FORMAT
        var svgOptimize = DEFAULT_OUTPUT_SVG_OPTIMIZE
        var svgPrecision = DEFAULT_OUTPUT_SVG_PRECISION
        if (from != null) {
            if (from.has(KEY_OUTPUT_PATH)) {
                path = File(from.getString(KEY_OUTPUT_PATH))
            }
            path.mkdirs()
            if (!path.canWrite()) {
                throw ParameterException("Cannot write to output path: ${path.absolutePath}")
            }

            if (from.has(KEY_OUTPUT_FORMAT)) {
                format = when (val formatStr = from.getString(KEY_OUTPUT_FORMAT).toLowerCase()) {
                    "png" -> OutputFormat.PNG
                    "jpg", "jpeg" -> OutputFormat.JPG
                    "bmp" -> OutputFormat.BMP
                    "gif" -> OutputFormat.GIF
                    "svg" -> OutputFormat.SVG
                    else -> throw ParameterException("Invalid output format '$formatStr'.")
                }
            }

            // SVG format settings
            if (from.has(KEY_OUTPUT_SVG_PRECISION)) {
                svgOptimize = from.getBoolean(KEY_OUTPUT_SVG_OPTIMIZE)
            }
            if (from.has(KEY_OUTPUT_SVG_PRECISION)) {
                svgPrecision = from.getInt(KEY_OUTPUT_SVG_PRECISION)
            }
        }
        return if (format == OutputFormat.SVG) {
            Configuration.SvgOutput(path, svgOptimize, svgPrecision)
        } else {
            Configuration.Output(format, path)
        }
    }

    private fun parseStyle(from: JSONObject?): Configuration.Style {
        var cellSize = DEFAULT_STYLE_CELL_SIZE
        var backgroundColor = DEFAULT_STYLE_BACKGROUND_COLOR
        var color = DEFAULT_STYLE_COLOR
        var strokeWidth = DEFAULT_STYLE_STROKE_WIDTH
        var solutionColor = DEFAULT_STYLE_SOLUTION_COLOR
        var solutionStrokeWidth = DEFAULT_STYLE_SOLUTION_STROKE_WIDTH
        var antialiasing = DEFAULT_STYLE_ANTIALIASING
        if (from != null) {
            if (from.has(KEY_STYLE_CELL_SIZE)) {
                cellSize = from.getFloat(KEY_STYLE_CELL_SIZE)
            }
            if (from.has(KEY_STYLE_BACKGROUND_COLOR)) {
                backgroundColor = Canvas.parseColor(
                        from.getString(KEY_STYLE_BACKGROUND_COLOR))
            }
            if (from.has(KEY_STYLE_STROKE_WIDTH)) {
                strokeWidth = from.getFloat(KEY_STYLE_STROKE_WIDTH)
            }
            if (from.has(KEY_STYLE_COLOR)) {
                color = Canvas.parseColor(from.getString(KEY_STYLE_COLOR))
            }
            if (from.has(KEY_STYLE_SOLUTION_STROKE_WIDTH)) {
                solutionStrokeWidth = from.getFloat(KEY_STYLE_SOLUTION_STROKE_WIDTH)
            }
            if (from.has(KEY_STYLE_SOLUTION_COLOR)) {
                solutionColor = Canvas.parseColor(from.getString(KEY_STYLE_SOLUTION_COLOR))
            }
            if (from.has(KEY_STYLE_ANTIALIASING)) {
                antialiasing = from.getBoolean(KEY_STYLE_ANTIALIASING)
            }
        }
        val stroke = BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        val solutionStroke = BasicStroke(solutionStrokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        return Configuration.Style(cellSize, backgroundColor, color,
                stroke, solutionColor, solutionStroke, antialiasing)
    }

    companion object {
        // Maze set keys and defaults
        private const val KEY_MAZE = "mazes"
        private const val KEY_MAZE_NAME = "name"
        private const val KEY_MAZE_COUNT = "count"
        private const val KEY_MAZE_TYPE = "type"
        private const val KEY_MAZE_ARRANGEMENT = "arrangement"
        private const val KEY_MAZE_SOLVE = "solve"

        private const val KEY_MAZE_OPENINGS = "openings"
        private const val KEY_MAZE_OPENING_START = 'S'
        private const val KEY_MAZE_OPENING_CENTER = 'C'
        private const val KEY_MAZE_OPENING_END = 'E'

        private const val KEY_MAZE_SIZE = "size"
        private const val KEY_MAZE_SIZE_WIDTH = "width"
        private const val KEY_MAZE_SIZE_HEIGHT = "height"
        private const val KEY_MAZE_SIZE_RADIUS = "radius"
        private const val KEY_MAZE_SIZE_CENTER_RADIUS = "centerRadius"
        private const val KEY_MAZE_SIZE_SUBDIVISION = "subdivision"

        private const val KEY_MAZE_ALGORITHM = "algorithm"
        private const val KEY_MAZE_ALGORITHM_NAME = "name"
        private const val KEY_MAZE_ALGORITHM_BRAID = "braid"
        private const val KEY_MAZE_ALGORITHM_GT_WEIGHTS = "gt_weights"

        private const val DEFAULT_MAZE_NAME = "maze"
        private const val DEFAULT_MAZE_COUNT = 1
        private const val DEFAULT_MAZE_ALGORITHM = "rb"
        private val DEFAULT_MAZE_ALGORITHM_BRAID: Braiding? = null
        private val DEFAULT_MAZE_TYPE = MazeType.RECT
        private val DEFAULT_MAZE_ARRANGEMENT = Arrangement.RECTANGLE
        private const val DEFAULT_MAZE_SIZE_CENTER_RADIUS = 1f
        private const val DEFAULT_MAZE_SIZE_SUBDIVISION = 1.5f
        private const val DEFAULT_MAZE_SOLVE = false

        // Output keys and defaults
        private const val KEY_OUTPUT = "output"
        private const val KEY_OUTPUT_PATH = "path"
        private const val KEY_OUTPUT_FORMAT = "format"
        private const val KEY_OUTPUT_SVG_OPTIMIZE = "svgOptimize"
        private const val KEY_OUTPUT_SVG_PRECISION = "svgPrecision"

        private val DEFAULT_OUTPUT_FORMAT = OutputFormat.PNG
        private val DEFAULT_OUTPUT_PATH = File(System.getProperty("user.dir"))
        private const val DEFAULT_OUTPUT_SVG_OPTIMIZE = false
        private const val DEFAULT_OUTPUT_SVG_PRECISION = 2

        // Style keys and defaults
        private const val KEY_STYLE = "output"
        private const val KEY_STYLE_CELL_SIZE = "cellSize"
        private const val KEY_STYLE_BACKGROUND_COLOR = "backgroundColor"
        private const val KEY_STYLE_COLOR = "color"
        private const val KEY_STYLE_STROKE_WIDTH = "strokeWidth"
        private const val KEY_STYLE_SOLUTION_COLOR = "solution_color"
        private const val KEY_STYLE_SOLUTION_STROKE_WIDTH = "solutionStrokeWidth"
        private const val KEY_STYLE_ANTIALIASING = "antialiasing"

        private const val DEFAULT_STYLE_CELL_SIZE = 30f
        private val DEFAULT_STYLE_BACKGROUND_COLOR = Canvas.parseColor("#00FFFFFF")
        private val DEFAULT_STYLE_COLOR = Color.BLACK!!
        private const val DEFAULT_STYLE_STROKE_WIDTH = 3f
        private val DEFAULT_STYLE_SOLUTION_COLOR = Color.BLUE!!
        private const val DEFAULT_STYLE_SOLUTION_STROKE_WIDTH = 3f
        private const val DEFAULT_STYLE_ANTIALIASING = true
    }

}