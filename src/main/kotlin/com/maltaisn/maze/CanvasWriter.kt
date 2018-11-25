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

import com.maltaisn.maze.maze.Maze
import com.maltaisn.maze.render.Canvas
import com.maltaisn.maze.render.OutputFormat
import com.maltaisn.maze.render.RasterCanvas
import com.maltaisn.maze.render.SVGCanvas
import org.json.JSONObject
import java.awt.BasicStroke
import java.awt.Color
import java.io.File


/**
 * Class that takes the "output" configuration parameter and
 * outputs the canvas object of generated mazes to files.
 */
class CanvasWriter(config: JSONObject?) {

    private var path = File(System.getProperty("user.dir"))
    private var format = OutputFormat.PNG

    private var svgOptimize = true
    private var svgPrecision = 2

    // Styling settings
    private var cellSize = 30.0
    private var backgroundColor = Canvas.parseColor("#00FFFFFF")
    private var stroke: BasicStroke
    private var color: Color = Color.BLACK
    private var solutionStroke: BasicStroke
    private var solutionColor: Color = Color.BLUE

    init {
        var strokeWidth = 3f
        var solutionStrokeWidth = 3f

        if (config != null) {
            if (config.has(KEY_PATH)) {
                path = File(config.getString(KEY_PATH))
            }
            path.mkdirs()
            if (!path.canWrite()) {
                throw IllegalArgumentException("Cannot write to output path: ${path.absolutePath}")
            }

            if (config.has(KEY_FORMAT)) {
                format = when (val format = config.getString(KEY_FORMAT).toLowerCase()) {
                    "png" -> OutputFormat.PNG
                    "jpg", "jpeg" -> OutputFormat.JPG
                    "bmp" -> OutputFormat.BMP
                    "gif" -> OutputFormat.GIF
                    "svg" -> OutputFormat.SVG
                    else -> throw IllegalArgumentException("Wrong output format '$format'.")
                }
            }

            // SVG format settings
            if (config.has(KEY_SVG_PRECISION)) {
                svgOptimize = config.getBoolean(KEY_SVG_OPTIMIZE)
            }
            if (config.has(KEY_SVG_PRECISION)) {
                svgPrecision = config.getInt(KEY_SVG_PRECISION)
            }

            // Style settings
            if (config.has(KEY_STYLE)) {
                val styleConfig = config.getJSONObject(KEY_STYLE)
                if (styleConfig.has(KEY_STYLE_CELL_SIZE)) {
                    cellSize = styleConfig.getDouble(KEY_STYLE_CELL_SIZE)
                }
                if (styleConfig.has(KEY_STYLE_BACKGROUND_COLOR)) {
                    backgroundColor = Canvas.parseColor(
                            styleConfig.getString(KEY_STYLE_BACKGROUND_COLOR))
                }
                if (styleConfig.has(KEY_STYLE_STROKE_WIDTH)) {
                    strokeWidth = styleConfig.getFloat(KEY_STYLE_STROKE_WIDTH)
                }
                if (styleConfig.has(KEY_STYLE_COLOR)) {
                    color = Canvas.parseColor(styleConfig.getString(KEY_STYLE_COLOR))
                }
                if (styleConfig.has(KEY_STYLE_SOLUTION_STROKE_WIDTH)) {
                    solutionStrokeWidth = styleConfig.getFloat(KEY_STYLE_SOLUTION_STROKE_WIDTH)
                }
                if (styleConfig.has(KEY_STYLE_SOLUTION_COLOR)) {
                    solutionColor = Canvas.parseColor(styleConfig.getString(KEY_STYLE_SOLUTION_COLOR))
                }
            }
        }

        stroke = BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        solutionStroke = BasicStroke(solutionStrokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    }

    /**
     * Write a [maze] to a file named [filename] (without the extension)
     */
    fun write(maze: Maze, filename: String) {
        val startTime = System.currentTimeMillis()

        val canvas = if (format == OutputFormat.SVG) SVGCanvas() else RasterCanvas(format)

        // If background color is completely transparent and format is SVG or PNG, don't draw it.
        val bgColor = if (backgroundColor.alpha == 0
                && (format == OutputFormat.PNG || format == OutputFormat.SVG)) {
            null
        } else {
            backgroundColor
        }

        maze.drawTo(canvas, cellSize, bgColor, color, stroke, solutionColor, solutionStroke)

        if (canvas is SVGCanvas) {
            // Apply additional SVG settings
            canvas.precision = svgPrecision
            if (svgOptimize) {
                canvas.optimize()
            }
        }

        val fullFilename = filename + '.' + format.extension
        val file = File(path, fullFilename)
        canvas.exportTo(file)

        val duration = System.currentTimeMillis() - startTime
        println("Exported '$fullFilename' in $duration ms.")
    }

    companion object {
        private const val KEY_PATH = "path"
        private const val KEY_FORMAT = "format"
        private const val KEY_SVG_OPTIMIZE = "svg_optimize"
        private const val KEY_SVG_PRECISION = "svg_precision"

        private const val KEY_STYLE = "style"
        private const val KEY_STYLE_CELL_SIZE = "cell_size"
        private const val KEY_STYLE_BACKGROUND_COLOR = "background_color"
        private const val KEY_STYLE_COLOR = "color"
        private const val KEY_STYLE_STROKE_WIDTH = "stroke_width"
        private const val KEY_STYLE_SOLUTION_COLOR = "solution_color"
        private const val KEY_STYLE_SOLUTION_STROKE_WIDTH = "solution_stroke_width"
    }


}