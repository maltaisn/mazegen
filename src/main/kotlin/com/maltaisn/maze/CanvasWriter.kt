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

    private var cellSize = 10.0
    private var strokeWidth = 1.0
    private var strokeColor = Color.BLACK!!
    private var backgroundColor = Canvas.parseColor("#00FFFFFF")

    init {
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
                if (styleConfig.has(KEY_STYLE_STROKE_WIDTH)) {
                    strokeWidth = styleConfig.getDouble(KEY_STYLE_STROKE_WIDTH)
                }
                if (styleConfig.has(KEY_STYLE_STROKE_COLOR)) {
                    strokeColor = Canvas.parseColor(
                            styleConfig.getString(KEY_STYLE_STROKE_COLOR))
                }
                if (styleConfig.has(KEY_STYLE_BACKGROUND_COLOR)) {
                    backgroundColor = Canvas.parseColor(
                            styleConfig.getString(KEY_STYLE_BACKGROUND_COLOR))
                }
            }
        }
    }

    /**
     * Write a [maze] to a file.
     * @param[filename] Name of the file, without the extension.
     */
    fun write(maze: Maze, filename: String) {
        val startTime = System.currentTimeMillis()

        val canvas = if (format == OutputFormat.SVG) SVGCanvas() else RasterCanvas(format)
        canvas.strokeWidth = strokeWidth
        canvas.strokeColor = strokeColor
        canvas.backgroundColor = backgroundColor

        maze.drawTo(canvas, cellSize)

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
        println("Exported to '$fullFilename' in $duration ms.")
    }

    companion object {
        private const val KEY_PATH = "path"
        private const val KEY_FORMAT = "format"
        private const val KEY_SVG_OPTIMIZE = "svg_optimize"
        private const val KEY_SVG_PRECISION = "svg_precision"

        private const val KEY_STYLE = "style"
        private const val KEY_STYLE_CELL_SIZE = "cell_size"
        private const val KEY_STYLE_STROKE_COLOR = "stroke_color"
        private const val KEY_STYLE_STROKE_WIDTH = "stroke_width"
        private const val KEY_STYLE_BACKGROUND_COLOR = "background_color"
    }


}