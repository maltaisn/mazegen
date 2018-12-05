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
import org.json.JSONArray
import org.json.JSONObject
import kotlin.system.measureTimeMillis


/**
 * Class that parses the "mazes" array objects and outputs a list of mazes.
 */
class MazeParser {

    fun parse(config: JSONObject): List<Maze> {
        val name = if (config.has(KEY_NAME)) config.getString(KEY_NAME) else null

        val count = if (config.has(KEY_COUNT)) config.getInt(KEY_COUNT) else 1
        if (count < 1) {
            throw IllegalArgumentException("Wrong value '$count' for count, must be at least 1.")
        }

        // Generator
        val algorithmJson = if (config.has(KEY_ALGORITHM)) config.get(KEY_ALGORITHM) else null

        var algorithm = "rb"
        if (algorithmJson is String) {
            algorithm = algorithmJson
        } else if (algorithmJson is JSONObject) {
            if (algorithmJson.has(KEY_ALGORITHM_NAME)) {
                algorithm = algorithmJson.getString(KEY_ALGORITHM_NAME)
            }
        }
        val generator = when (algorithm) {
            "ab", "aldous-broder" -> AldousBroderGenerator()
            "gt", "growing-tree" -> GrowingTreeGenerator()
            "hk", "hunt-kill" -> HuntKillGenerator()
            "kr", "kruskal" -> KruskalGenerator()
            "pr", "prim" -> PrimGenerator()
            "rb", "recursive-backtracker" -> RecursiveBacktrackerGenerator()
            "wi", "wilson" -> WilsonGenerator()
            else -> throw IllegalArgumentException("Invalid algorithm '$algorithm'.")
        }

        // Generactor-specific settings
        if (generator is GrowingTreeGenerator && algorithmJson is JSONObject) {
            if (algorithmJson.has(KEY_ALGORITHM_GT_WEIGHTS)) {
                val weights = algorithmJson.getJSONArray(KEY_ALGORITHM_GT_WEIGHTS)
                generator.setChooseByWeight(weights[0] as Int,
                        weights[1] as Int, weights[2] as Int)
            }
        }

        val type = if (config.has(KEY_TYPE)) {
            when (val typeStr = config.getString(KEY_TYPE)) {
                "rect", "orth" -> MazeType.RECT
                "hex", "sigma" -> MazeType.HEX
                "triangle", "delta" -> MazeType.DELTA
                "polar", "circle", "theta" -> MazeType.POLAR
                else -> throw IllegalArgumentException("Invalid maze type '$typeStr'.")
            }
        } else {
            MazeType.RECT
        }

        val size = if (config.has(KEY_SIZE)) {
            config.get(KEY_SIZE)
        } else {
            throw IllegalArgumentException("A size must be specified for the maze.")
        }

        val arrangement = if (config.has(KEY_ARRANGEMENT)) {
            when (val arrStr = config.getString(KEY_ARRANGEMENT)) {
                "rect", "rectangle" -> Arrangement.RECTANGLE
                "triangle" -> Arrangement.TRIANGLE
                "hex", "hexagon" -> Arrangement.HEXAGON
                "rhombus" -> Arrangement.RHOMBUS
                else -> throw IllegalArgumentException("Invalid maze arrangement '$arrStr'.")
            }
        } else {
            Arrangement.RECTANGLE
        }

        val createMaze: () -> Maze = when (type) {
            MazeType.RECT -> {
                val width: Int
                val height: Int
                if (size is Int) {
                    width = size
                    height = size
                } else {
                    val sizeJson = size as JSONObject
                    width = sizeJson.getInt(KEY_SIZE_WIDTH)
                    height = sizeJson.getInt(KEY_SIZE_HEIGHT)
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
                        throw IllegalArgumentException("For hexagon and triangle shaped " +
                                "delta and sigma mazes, size must be an integer.")
                    }
                    val sizeJson = size as JSONObject
                    width = sizeJson.getInt(KEY_SIZE_WIDTH)
                    height = sizeJson.getInt(KEY_SIZE_HEIGHT)
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
                var centerRadius = 1f
                var subdivision = 1.5f
                if (size is Int) {
                    radius = size
                } else {
                    val sizeJson = size as JSONObject
                    radius = sizeJson.getInt(KEY_SIZE_RADIUS)
                    if (sizeJson.has(KEY_SIZE_CENTER_RADIUS)) {
                        centerRadius = sizeJson.getFloat(KEY_SIZE_CENTER_RADIUS)
                    }
                    if (sizeJson.has(KEY_SIZE_SUBDIVISION)) {
                        subdivision = sizeJson.getFloat(KEY_SIZE_SUBDIVISION)
                    }
                }
                { PolarMaze(radius, centerRadius, subdivision) }
            }
        }

        val openings = mutableListOf<Opening>()
        if (config.has(KEY_OPENINGS)) {
            val openingsJson = config.getJSONArray(KEY_OPENINGS)
            for (openingJson in openingsJson) {
                openings.add(Opening(openingJson as JSONArray))
            }
        }

        val solve = config.has(KEY_SOLVE) && config.getBoolean(KEY_SOLVE)

        val generated = ArrayList<Maze>(count)
        for (i in 0 until count) {
            val maze = createMaze()
            maze.name = name

            // Generate the maze
            val genTime = measureTimeMillis {
                generator.generate(maze)

                // Add the openings
                for (opening in openings) {
                    maze.createOpening(opening)
                }

                // Braiding
                if (algorithmJson is JSONObject) {
                    if (algorithmJson.has(KEY_ALGORITHM_BRAID)) {
                        val braider = MazeBraider()
                        val braid = algorithmJson.get(KEY_ALGORITHM_BRAID)
                        if (braid is Int) {
                            braider.braidByCount(maze, braid)
                        } else if (braid is String) {
                            if (braid.endsWith('%')) {
                                val percent = braid.substring(0, braid.length - 1).toDouble() / 100
                                braider.braidByPercentage(maze, percent)
                            } else {
                                braider.braidByCount(maze, braid.toInt())
                            }
                        }
                    }
                }
            }
            println("Generated '${maze.name}' ${i + 1} / $count in $genTime ms.")

            // Solve the maze
            if (solve) {
                val solveTime = measureTimeMillis { maze.solve() }
                println("Solved '${maze.name}' ${i + 1} / $count in $solveTime ms.")
            }

            generated.add(maze)
        }

        return generated
    }

    private enum class MazeType {
        RECT, HEX, DELTA, POLAR
    }

    companion object {
        private const val KEY_NAME = "name"
        private const val KEY_COUNT = "count"
        private const val KEY_TYPE = "type"
        private const val KEY_SIZE = "size"
        private const val KEY_ARRANGEMENT = "arrangement"
        private const val KEY_OPENINGS = "openings"
        private const val KEY_SOLVE = "solve"

        private const val KEY_SIZE_WIDTH = "width"
        private const val KEY_SIZE_HEIGHT = "height"
        private const val KEY_SIZE_RADIUS = "radius"
        private const val KEY_SIZE_CENTER_RADIUS = "centerRadius"
        private const val KEY_SIZE_SUBDIVISION = "subdivision"

        private const val KEY_ALGORITHM = "algorithm"
        private const val KEY_ALGORITHM_NAME = "name"
        private const val KEY_ALGORITHM_GT_WEIGHTS = "gt_weights"
        private const val KEY_ALGORITHM_BRAID = "braid"
    }


}