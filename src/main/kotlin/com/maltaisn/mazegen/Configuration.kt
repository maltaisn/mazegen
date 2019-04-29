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

import com.maltaisn.mazegen.generator.Generator
import com.maltaisn.mazegen.maze.Maze
import com.maltaisn.mazegen.maze.Position
import com.maltaisn.mazegen.render.Canvas
import com.maltaisn.mazegen.render.OutputFormat
import com.maltaisn.mazegen.render.RasterCanvas
import com.maltaisn.mazegen.render.SvgCanvas
import java.awt.BasicStroke
import java.awt.Color
import java.io.File
import java.lang.Math.floor
import kotlin.math.max


/**
 * Class representing the JSON configuration file content.
 * Use [ConfigurationParser] to create it from JSON.
 */
class Configuration(val mazeSets: List<MazeSet>,
                    val output: Output,
                    val style: Style) {

    /**
     * A set of mazes with the same settings.
     */
    class MazeSet(var name: String,
                  val count: Int,
                  val creator: () -> Maze,
                  val generator: Generator,
                  val braiding: Maze.Braiding?,
                  val openings: List<Position>,
                  val solve: Boolean,
                  val distanceMap: Boolean,
                  val distanceMapStart: Position?,
                  val separateExport: Boolean)

    /**
     * Output settings.
     */
    open class Output(val format: OutputFormat, val path: File) {

        open fun createCanvas(): Canvas = RasterCanvas(format)
    }

    /**
     * Output settings for the SVG format.
     */
    class SvgOutput(path: File, private val optimization: Int,
                    private val precision: Int) : Output(OutputFormat.SVG, path) {

        override fun createCanvas(): Canvas {
            val canvas = SvgCanvas()
            canvas.optimization = optimization
            canvas.precision = precision
            return canvas
        }
    }

    /**
     * Style settings for drawing the mazes.
     */
    class Style(val cellSize: Double,
                val backgroundColor: Color?,
                val color: Color,
                val stroke: BasicStroke,
                val solutionColor: Color,
                val solutionStroke: BasicStroke,
                val distanceMapRange: Int,
                val distanceMapColors: List<Color>,
                val antialiasing: Boolean) {

        /**
         * Generate the list of colors for each distance in the [maze].
         * If [range] is automatic, there will be as many colors as the greatest distance.
         * If [range] is smaller than the greatest distance, the colors will be looped.
         */
        fun generateDistanceMapColors(maze: Maze): List<Color> {
            assert(maze.hasDistanceMap)

            // Find greatest distance in the maze
            var maxDistance = 0
            maze.forEachCell {
                if (it.distanceMapValue > maxDistance) {
                    maxDistance = it.distanceMapValue
                }
            }

            val colorCount = distanceMapColors.size
            val range = if (distanceMapRange == DISTANCE_MAP_RANGE_AUTO) {
                maxDistance
            } else {
                max(distanceMapRange, colorCount - 1)
            }

            // Interpolate all colors in the list over the range
            val colors = mutableListOf<Color>()
            for (i in 0..maxDistance) {
                val progress = i.toDouble() / range * (colorCount - 1)
                val index = floor(progress).toInt() % colorCount
                colors += interpolateColors(distanceMapColors[index],
                        distanceMapColors[(index + 1) % colorCount], (progress % 1).toFloat())
            }
            return colors
        }

        private fun interpolateColors(start: Color, end: Color, percent: Float): Color {
            val inversePercent = 1 - percent
            val r = start.red * inversePercent + end.red * percent
            val g = start.green * inversePercent + end.green * percent
            val b = start.blue * inversePercent + end.blue * percent
            val a = start.alpha * inversePercent + end.alpha * percent
            return Color(r.toInt(), g.toInt(), b.toInt(), a.toInt())
        }

        companion object {
            /**
             * Use for [distanceMapRange] to indicate that the colors should be
             * divided over the longest path length, each being used exactly once.
             */
            const val DISTANCE_MAP_RANGE_AUTO = 0
        }
    }

}
