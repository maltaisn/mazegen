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

import com.maltaisn.mazegen.Configuration.MazeSet
import com.maltaisn.mazegen.generator.*
import com.maltaisn.mazegen.maze.*
import com.maltaisn.mazegen.render.OutputFormat
import org.json.JSONArray
import org.json.JSONObject
import java.awt.BasicStroke
import java.awt.Color
import java.io.File


/**
 * Parser for the JSON configuration file into a [Configuration] object.
 */
object ConfigurationParser {

    fun parse(from: JSONObject): Configuration {
        // Parse maze sets
        val mazeSets = mutableListOf<MazeSet>()
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
            paramError("No mazes to generate.")
        }

        // Parse output settings
        val output = parseOutput(if (from.has(KEY_OUTPUT)) from.getJSONObject(KEY_OUTPUT) else null)

        // Parse style settings 
        val style = parseStyle(if (from.has(KEY_STYLE)) from.getJSONObject(KEY_STYLE) else null, output.format)

        return Configuration(mazeSets, output, style)
    }

    private fun parseMazeSet(from: JSONObject): MazeSet {
        // Name
        val name = if (from.has(KEY_MAZE_NAME))
            from.getString(KEY_MAZE_NAME) else DEFAULT_MAZE_NAME

        // Count
        val count = if (from.has(KEY_MAZE_COUNT))
            from.getInt(KEY_MAZE_COUNT) else DEFAULT_MAZE_COUNT
        if (count < 1) {
            paramError("At least one maze must be generated for maze with name '$name'.")
        }

        // Generator
        val algorithmJson = if (from.has(KEY_MAZE_ALGORITHM))
            from.get(KEY_MAZE_ALGORITHM) else null
        var algorithm = DEFAULT_MAZE_ALGORITHM
        if (algorithmJson != null) {
            if (algorithmJson is String) {
                algorithm = algorithmJson
            } else {
                if ((algorithmJson as JSONObject).has(KEY_MAZE_ALGORITHM_NAME)) {
                    algorithm = algorithmJson.getString(KEY_MAZE_ALGORITHM_NAME)
                }
            }
        }
        val generator = when (algorithm.toLowerCase()) {
            "ab", "aldous-broder" -> AldousBroderGenerator()
            "bt", "binary-tree" -> BinaryTreeGenerator()
            "el", "eller" -> EllerGenerator()
            "gt", "growing-tree" -> GrowingTreeGenerator()
            "hk", "hunt-kill" -> HuntKillGenerator()
            "kr", "kruskal" -> KruskalGenerator()
            "pr", "prim" -> PrimGenerator()
            "rb", "recursive-backtracker" -> RecursiveBacktrackerGenerator()
            "rd", "recursive-division" -> RecursiveDivisionGenerator()
            "sw", "sidewinder" -> SidewinderGenerator()
            "wi", "wilson" -> WilsonGenerator()
            else -> paramError("Invalid algorithm '$algorithm'.")
        }

        // Generactor-specific settings
        if (algorithmJson is JSONObject) {
            when (generator) {
                is BinaryTreeGenerator -> if (algorithmJson.has(KEY_MAZE_ALGORITHM_BIAS)) {
                    val biasStr = algorithmJson.getString(KEY_MAZE_ALGORITHM_BIAS)
                    generator.bias = when (biasStr.toLowerCase()) {
                        "ne" -> BinaryTreeGenerator.Bias.NORTH_EAST
                        "nw" -> BinaryTreeGenerator.Bias.NORTH_WEST
                        "se" -> BinaryTreeGenerator.Bias.SOUTH_EAST
                        "sw" -> BinaryTreeGenerator.Bias.SOUTH_WEST
                        else -> paramError("Invalid binary tree algorithm bias '$biasStr'")
                    }
                }
                is EllerGenerator -> if (algorithmJson.has(KEY_MAZE_ALGORITHM_BIAS)) {
                    val biasArr = algorithmJson.getJSONArray(KEY_MAZE_ALGORITHM_BIAS)
                    generator.horizontalBias = parsePercentValue(biasArr[0] as String)
                    generator.verticalBias = parsePercentValue(biasArr[1] as String)
                }
                is GrowingTreeGenerator -> if (algorithmJson.has(KEY_MAZE_ALGORITHM_WEIGHTS)) {
                    val weightsJson = algorithmJson.getJSONArray(KEY_MAZE_ALGORITHM_WEIGHTS)
                    generator.randomWeight = weightsJson[0] as Int
                    generator.newestWeight = weightsJson[1] as Int
                    generator.oldestWeight = weightsJson[2] as Int
                }
            }
        }

        // Braiding
        var braiding: Maze.Braiding? = DEFAULT_MAZE_ALGORITHM_BRAID
        if (from.has(KEY_MAZE_BRAID)) {
            val braid = from.get(KEY_MAZE_BRAID)
            braiding = if (braid is Int) {
                Maze.Braiding(braid)
            } else {
                if ((braid as String).endsWith('%')) {
                    Maze.Braiding(parsePercentValue(braid))
                } else {
                    Maze.Braiding(braid.toInt())
                }
            }
        }

        // Shape
        val shape = if (from.has(KEY_MAZE_SHAPE)) {
            val arrStr = from.getString(KEY_MAZE_SHAPE)
            when (arrStr.toLowerCase()) {
                "rectangle" -> BaseShapedMaze.Shape.RECTANGLE
                "triangle" -> BaseShapedMaze.Shape.TRIANGLE
                "hexagon" -> BaseShapedMaze.Shape.HEXAGON
                "rhombus" -> BaseShapedMaze.Shape.RHOMBUS
                else -> paramError("Invalid maze shape '$arrStr'.")
            }
        } else {
            DEFAULT_MAZE_SHAPE
        }

        // Size
        val sizeJson = if (from.has(KEY_MAZE_SIZE)) {
            from.get(KEY_MAZE_SIZE)
        } else {
            paramError("A size must be specified for the maze.")
        }

        // Creator (type + parameters)
        val creator: () -> Maze = when (val type = if (from.has(KEY_MAZE_TYPE)) {
            from.getString(KEY_MAZE_TYPE).toLowerCase()
        } else {
            DEFAULT_MAZE_TYPE
        }) {
            MAZE_ORTHOGONAL, MAZE_UNICURSAL_ORTHOGONAL, MAZE_UPSILON, MAZE_ZETA -> {
                {
                    var width: Int
                    var height: Int
                    if (sizeJson is Int) {
                        width = sizeJson
                        height = sizeJson
                    } else {
                        sizeJson as JSONObject
                        if (sizeJson.has(KEY_MAZE_SIZE)) {
                            val size = sizeJson.getInt(KEY_MAZE_SIZE)
                            width = size
                            height = size
                        } else {
                            width = sizeJson.getInt(KEY_MAZE_SIZE_WIDTH)
                            height = sizeJson.getInt(KEY_MAZE_SIZE_HEIGHT)
                        }
                    }

                    if (type == MAZE_UNICURSAL_ORTHOGONAL) {
                        if (width % 2 != 0 || height % 2 != 0) {
                            paramError("Dimensions of unicursal orthogonal maze must be even.")
                        }
                        width /= 2
                        height /= 2
                    }

                    when (type) {
                        MAZE_ORTHOGONAL -> OrthogonalMaze(width, height)
                        MAZE_UNICURSAL_ORTHOGONAL -> UnicursalOrthogonalMaze(width, height)
                        MAZE_UPSILON -> UpsilonMaze(width, height)
                        MAZE_ZETA -> ZetaMaze(width, height)
                        else -> throw IllegalStateException()
                    }
                }
            }
            MAZE_DELTA, MAZE_SIGMA -> {
                {
                    val width: Int
                    val height: Int
                    if (sizeJson is Int) {
                        width = sizeJson
                        height = sizeJson
                    } else {
                        if (shape == BaseShapedMaze.Shape.HEXAGON
                                || shape == BaseShapedMaze.Shape.TRIANGLE) {
                            paramError("For hexagon and triangle shaped " +
                                    "delta and sigma mazes, size must be an integer.")
                        }
                        sizeJson as JSONObject
                        if (sizeJson.has(KEY_MAZE_SIZE)) {
                            val size = sizeJson.getInt(KEY_MAZE_SIZE)
                            width = size
                            height = size
                        } else {
                            width = sizeJson.getInt(KEY_MAZE_SIZE_WIDTH)
                            height = sizeJson.getInt(KEY_MAZE_SIZE_HEIGHT)
                        }
                    }

                    when (type) {
                        MAZE_DELTA -> DeltaMaze(width, height, shape)
                        MAZE_SIGMA -> SigmaMaze(width, height, shape)
                        else -> throw IllegalStateException()
                    }
                }
            }
            MAZE_THETA -> {
                {
                    val radius: Int
                    var centerRadius = DEFAULT_MAZE_SIZE_CENTER_RADIUS
                    var subdivision = DEFAULT_MAZE_SIZE_SUBDIVISION
                    if (sizeJson is Int) {
                        radius = sizeJson
                    } else {
                        sizeJson as JSONObject
                        radius = sizeJson.getInt(KEY_MAZE_SIZE_RADIUS)
                        if (sizeJson.has(KEY_MAZE_SIZE_CENTER_RADIUS)) {
                            centerRadius = sizeJson.getDouble(KEY_MAZE_SIZE_CENTER_RADIUS)
                        }
                        if (sizeJson.has(KEY_MAZE_SIZE_SUBDIVISION)) {
                            subdivision = sizeJson.getDouble(KEY_MAZE_SIZE_SUBDIVISION)
                        }
                    }
                    ThetaMaze(radius, centerRadius, subdivision)
                }
            }
            MAZE_WEAVE_ORTHOGONAL -> {
                {
                    val width: Int
                    val height: Int
                    var maxWeave = DEFAULT_MAZE_SIZE_MAX_WEAVE
                    if (sizeJson is Int) {
                        width = sizeJson
                        height = sizeJson
                    } else {
                        sizeJson as JSONObject
                        if (sizeJson.has(KEY_MAZE_SIZE)) {
                            val size = sizeJson.getInt(KEY_MAZE_SIZE)
                            width = size
                            height = size
                        } else {
                            width = sizeJson.getInt(KEY_MAZE_SIZE_WIDTH)
                            height = sizeJson.getInt(KEY_MAZE_SIZE_HEIGHT)
                        }
                        if (sizeJson.has(KEY_MAZE_SIZE_MAX_WEAVE)) {
                            maxWeave = sizeJson.getInt(KEY_MAZE_SIZE_MAX_WEAVE)
                        }
                    }
                    WeaveOrthogonalMaze(width, height, maxWeave)
                }
            }
            else -> paramError("Invalid maze type '$type'.")
        }

        // Openings
        val openings = mutableListOf<Position>()
        if (from.has(KEY_MAZE_OPENINGS)) {
            val openingsJson = from.getJSONArray(KEY_MAZE_OPENINGS)
            for (openingJson in openingsJson) {
                val position = parsePosition(openingJson as JSONArray)
                openings.add(Position2D(position[0], position[1]))
            }
        }

        // Solve
        val solve = if (from.has(KEY_MAZE_SOLVE))
            from.getBoolean(KEY_MAZE_SOLVE) else DEFAULT_MAZE_SOLVE
        if (solve && openings.size < 2) {
            paramError("There must be at least two openings defined to solve the maze.")
        }

        // Distance map
        val distanceMap = if (from.has(KEY_MAZE_DIST_MAP))
            from.getBoolean(KEY_MAZE_DIST_MAP) else DEFAULT_MAZE_DIST_MAP

        val distMapStart = if (from.has(KEY_MAZE_DM_START)) {
            val value = from.get(KEY_MAZE_DM_START)
            if (value is String && value == "random") {
                null
            } else {
                val pos = parsePosition(from.getJSONArray(KEY_MAZE_DM_START))
                Position2D(pos[0], pos[1])
            }
        } else {
            DEFAULT_MAZE_DM_START
        }

        return MazeSet(name, count, creator, generator, braiding,
                openings, solve, distanceMap, distMapStart)
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
                paramError("Cannot write to output path: ${path.absolutePath}")
            }

            if (from.has(KEY_OUTPUT_FORMAT)) {
                val formatStr = from.getString(KEY_OUTPUT_FORMAT)
                format = when (formatStr.toLowerCase()) {
                    "png" -> OutputFormat.PNG
                    "jpg" -> OutputFormat.JPG
                    "bmp" -> OutputFormat.BMP
                    "gif" -> OutputFormat.GIF
                    "svg" -> OutputFormat.SVG
                    else -> paramError("Invalid output format '$formatStr'.")
                }
            }

            // SVG format settings
            if (from.has(KEY_OUTPUT_SVG_OPTIMIZE)) {
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

    private fun parseStyle(from: JSONObject?, format: OutputFormat): Configuration.Style {
        var cellSize = DEFAULT_STYLE_CELL_SIZE
        var backgroundColor: Color? = DEFAULT_STYLE_BACKGROUND_COLOR
        var color = DEFAULT_STYLE_COLOR
        var strokeWidth = DEFAULT_STYLE_STROKE_WIDTH
        var solutionColor = DEFAULT_STYLE_SOLUTION_COLOR
        var solutionStrokeWidth = DEFAULT_STYLE_SOLUTION_STROKE_WIDTH
        var strokeCap = DEFAULT_STYLE_STROKE_CAP
        var distMapRange = DEFAULT_STYLE_DM_RANGE
        var distMapColors = DEFAULT_STYLE_DM_COLORS
        var antialiasing = DEFAULT_STYLE_ANTIALIASING

        if (from != null) {
            if (from.has(KEY_STYLE_CELL_SIZE)) {
                cellSize = from.getDouble(KEY_STYLE_CELL_SIZE)
            }
            if (from.has(KEY_STYLE_BACKGROUND_COLOR)) {
                backgroundColor = parseColor(from.getString(KEY_STYLE_BACKGROUND_COLOR))
            }
            if (from.has(KEY_STYLE_STROKE_WIDTH)) {
                strokeWidth = from.getFloat(KEY_STYLE_STROKE_WIDTH)
            }
            if (from.has(KEY_STYLE_COLOR)) {
                color = parseColor(from.getString(KEY_STYLE_COLOR))
            }
            if (from.has(KEY_STYLE_SOLUTION_STROKE_WIDTH)) {
                solutionStrokeWidth = from.getFloat(KEY_STYLE_SOLUTION_STROKE_WIDTH)
            }
            if (from.has(KEY_STYLE_SOLUTION_COLOR)) {
                solutionColor = parseColor(from.getString(KEY_STYLE_SOLUTION_COLOR))
            }
            if (from.has(KEY_STYLE_STROKE_CAP)) {
                val capStr = from.getString(KEY_STYLE_STROKE_CAP)
                strokeCap = when (capStr.toLowerCase()) {
                    "butt" -> BasicStroke.CAP_BUTT
                    "round" -> BasicStroke.CAP_ROUND
                    "square" -> BasicStroke.CAP_SQUARE
                    else -> paramError("Invalid stroke cap '$capStr'.")
                }
            }
            if (from.has(KEY_STYLE_SOLUTION_COLOR)) {
                solutionColor = parseColor(from.getString(KEY_STYLE_SOLUTION_COLOR))
            }
            if (from.has(KEY_STYLE_DM_RANGE)) {
                val value = from.get(KEY_STYLE_DM_RANGE)
                distMapRange = if (value is String && value == "auto") {
                    Configuration.Style.DISTANCE_MAP_RANGE_AUTO
                } else {
                    from.getInt(KEY_STYLE_DM_RANGE)
                }
            }
            if (from.has(KEY_STYLE_DM_COLORS)) {
                val colors = from.getJSONArray(KEY_STYLE_DM_COLORS)
                distMapColors = List(colors.length()) { parseColor(colors[it] as String) }
            }
            if (from.has(KEY_STYLE_ANTIALIASING)) {
                antialiasing = from.getBoolean(KEY_STYLE_ANTIALIASING)
            }
        }

        // If background color is completely transparent and format is SVG or PNG, don't draw it.
        if (backgroundColor?.alpha == 0 && (format == OutputFormat.PNG || format == OutputFormat.SVG)) {
            backgroundColor = null
        }

        val stroke = BasicStroke(strokeWidth, strokeCap, BasicStroke.JOIN_ROUND)
        val solutionStroke = BasicStroke(solutionStrokeWidth, strokeCap, BasicStroke.JOIN_ROUND)

        return Configuration.Style(cellSize, backgroundColor, color, stroke, solutionColor,
                solutionStroke, distMapRange, distMapColors, antialiasing)
    }

    /**
     * Parse a cell position array.
     */
    private fun parsePosition(posArray: JSONArray): IntArray {
        val position = IntArray(posArray.length())
        for (i in 0 until posArray.length()) {
            val pos = posArray[i]
            position[i] = when (pos) {
                is String -> when (pos[0].toUpperCase()) {
                    KEY_MAZE_OPENING_START -> Maze.OPENING_POS_START
                    KEY_MAZE_OPENING_CENTER -> Maze.OPENING_POS_CENTER
                    KEY_MAZE_OPENING_END -> Maze.OPENING_POS_END
                    else -> paramError("Invalid opening position character '$pos'.")
                }
                is Int -> pos
                else -> paramError("Invalid opening position argument '$pos'.")
            }
        }
        return position
    }

    /**
     * Parse a percentage value like "90%".
     */
    private fun parsePercentValue(value: String): Double {
        if (value.endsWith('%')) {
            return value.substring(0, value.length - 1).toDouble() / 100
        }
        paramError("Percentage value expected, got '$value'")
    }

    /**
     * Parse a hex color string like `#RRGGBB` or `#AARRGGBB` to a [Color].
     */
    private fun parseColor(color: String): Color {
        if (color.startsWith('#')) {
            val value = color.substring(1).toLong(16).toInt()
            if (color.length == 7) {
                return Color(value)
            } else if (color.length == 9) {
                return Color(value, true)
            }
        }
        paramError("Invalid color string '$color'.")
    }

    private const val MAZE_ORTHOGONAL = "orthogonal"
    private const val MAZE_WEAVE_ORTHOGONAL = "weaveorthogonal"
    private const val MAZE_UNICURSAL_ORTHOGONAL = "unicursalorthogonal"
    private const val MAZE_DELTA = "delta"
    private const val MAZE_SIGMA = "sigma"
    private const val MAZE_THETA = "theta"
    private const val MAZE_UPSILON = "upsilon"
    private const val MAZE_ZETA = "zeta"

    // Maze set keys and defaults
    private const val KEY_MAZE = "mazes"
    private const val KEY_MAZE_NAME = "name"
    private const val KEY_MAZE_COUNT = "count"
    private const val KEY_MAZE_TYPE = "type"
    private const val KEY_MAZE_SHAPE = "shape"
    private const val KEY_MAZE_BRAID = "braid"
    private const val KEY_MAZE_SOLVE = "solve"
    private const val KEY_MAZE_DIST_MAP = "distanceMap"
    private const val KEY_MAZE_DM_START = "distanceMapStart"

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
    private const val KEY_MAZE_SIZE_MAX_WEAVE = "maxWeave"

    private const val KEY_MAZE_ALGORITHM = "algorithm"
    private const val KEY_MAZE_ALGORITHM_NAME = "name"
    private const val KEY_MAZE_ALGORITHM_WEIGHTS = "weights"
    private const val KEY_MAZE_ALGORITHM_BIAS = "bias"

    private const val DEFAULT_MAZE_NAME = "maze"
    private const val DEFAULT_MAZE_COUNT = 1
    private const val DEFAULT_MAZE_ALGORITHM = "rb"
    private val DEFAULT_MAZE_ALGORITHM_BRAID: Maze.Braiding? = null
    private val DEFAULT_MAZE_TYPE = MAZE_ORTHOGONAL
    private val DEFAULT_MAZE_SHAPE = BaseShapedMaze.Shape.RECTANGLE
    private const val DEFAULT_MAZE_SIZE_CENTER_RADIUS = 1.0
    private const val DEFAULT_MAZE_SIZE_SUBDIVISION = 1.5
    private const val DEFAULT_MAZE_SIZE_MAX_WEAVE = 1
    private const val DEFAULT_MAZE_SOLVE = false
    private const val DEFAULT_MAZE_DIST_MAP = false
    private val DEFAULT_MAZE_DM_START: Position? = null

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
    private const val KEY_STYLE = "style"
    private const val KEY_STYLE_CELL_SIZE = "cellSize"
    private const val KEY_STYLE_BACKGROUND_COLOR = "backgroundColor"
    private const val KEY_STYLE_COLOR = "color"
    private const val KEY_STYLE_STROKE_WIDTH = "strokeWidth"
    private const val KEY_STYLE_SOLUTION_COLOR = "solutionColor"
    private const val KEY_STYLE_SOLUTION_STROKE_WIDTH = "solutionStrokeWidth"
    private const val KEY_STYLE_STROKE_CAP = "strokeCap"
    private const val KEY_STYLE_DM_RANGE = "distanceMapRange"
    private const val KEY_STYLE_DM_COLORS = "distanceMapColors"
    private const val KEY_STYLE_ANTIALIASING = "antialiasing"

    private const val DEFAULT_STYLE_CELL_SIZE = 30.0
    private val DEFAULT_STYLE_BACKGROUND_COLOR = parseColor("#00FFFFFF")
    private val DEFAULT_STYLE_COLOR: Color = Color.BLACK
    private const val DEFAULT_STYLE_STROKE_WIDTH = 3f
    private val DEFAULT_STYLE_SOLUTION_COLOR: Color = Color.BLUE
    private const val DEFAULT_STYLE_SOLUTION_STROKE_WIDTH = 3f
    private const val DEFAULT_STYLE_STROKE_CAP = BasicStroke.CAP_ROUND
    private const val DEFAULT_STYLE_DM_RANGE = Configuration.Style.DISTANCE_MAP_RANGE_AUTO
    private val DEFAULT_STYLE_DM_COLORS = listOf(Color.WHITE, Color.BLACK)
    private const val DEFAULT_STYLE_ANTIALIASING = true

}
