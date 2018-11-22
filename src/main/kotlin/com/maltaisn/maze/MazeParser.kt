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


/**
 * Class that parses the "mazes" array objects and outputs a list of mazes.
 */
class MazeParser {

    fun parse(config: JSONObject): List<Maze> {
        val name = if (config.has(KEY_NAME)) config.getString(KEY_NAME) else "maze"

        val count = if (config.has(KEY_COUNT)) config.getInt(KEY_COUNT) else 1
        if (count < 1) {
            throw IllegalArgumentException("Wrong value '$count' for count, must be at least 1.")
        }

        val generator = if (config.has(KEY_ALGORITHM)) {
            when (val algStr = config.getString(KEY_ALGORITHM)) {
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

        val type = if (config.has(KEY_TYPE)) {
            when (val typeStr = config.getString(KEY_TYPE)) {
                "rect", "orth" -> MazeType.RECT
                "hex", "sigma" -> MazeType.HEX
                "triangle", "delta" -> MazeType.DELTA
                else -> throw IllegalArgumentException("Invalid maze type '$typeStr'.")
            }
        } else {
            MazeType.RECT
        }

        var width = if (config.has(KEY_WIDTH)) config.getInt(KEY_WIDTH) else null
        var height = if (config.has(KEY_HEIGHT)) config.getInt(KEY_HEIGHT) else null
        val dimension = if (config.has(KEY_DIMENSION)) config.getInt(KEY_DIMENSION) else null

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

        val template = when (type) {
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

        var openings: Array<Opening>? = null
        if (config.has(KEY_OPENINGS)) {
            val openingsJson = config.getJSONArray(KEY_OPENINGS)
            openings = Array(openingsJson.length()) { Opening(openingsJson[it] as JSONArray) }
        }

        val generated = ArrayList<Maze>(count)
        for (i in 0 until count) {
            val startTime = System.currentTimeMillis()
            val maze = template.copy()

            // Generate the maze
            generator.generate(maze)

            // Add the openings
            if (openings != null) {
                maze.createOpenings(*openings)
            }

            generated.add(maze)

            val duration = System.currentTimeMillis() - startTime
            println("Generated '$name' ${i + 1} / $count in $duration ms.")
        }

        return generated
    }

    private enum class MazeType {
        RECT, HEX, DELTA
    }

    companion object {
        private const val KEY_MAZES = "mazes"
        private const val KEY_NAME = "name"
        private const val KEY_COUNT = "count"
        private const val KEY_TYPE = "type"
        private const val KEY_WIDTH = "width"
        private const val KEY_HEIGHT = "height"
        private const val KEY_DIMENSION = "dimension"
        private const val KEY_ARRANGEMENT = "arrangement"
        private const val KEY_ALGORITHM = "algorithm"
        private const val KEY_OPENINGS = "openings"
    }


}