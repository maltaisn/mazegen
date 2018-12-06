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

package com.maltaisn.maze.render

import java.awt.BasicStroke
import java.awt.Color
import java.io.File
import java.util.*


/**
 * Interface for a class that draws graphics and output it to a file.
 */
abstract class Canvas(val format: OutputFormat) {

    var width = SIZE_UNSET
        private set
    var height = SIZE_UNSET
        private set

    /**
     * The stroke style used by the canvas.
     */
    abstract var stroke: BasicStroke

    /**
     * The color used for the stroke and fill by the canvas.
     */
    abstract var color: Color

    /**
     * The translation applied to this canvas.
     * Null if no translation is applied.
     */
    open var translate: Point? = null
        set(value) {
            field = if (value != null && value.x == 0f && value.y == 0f) {
                null
            } else {
                value
            }
        }

    abstract var antialiasing: Boolean

    /**
     * Initialize the canvas with a width and height.
     * Should only be called once.
     */
    open fun init(width: Float, height: Float) {
        if (width == SIZE_UNSET) {
            throw IllegalArgumentException("This canvas was already initialized.")
        }

        this.width = width
        this.height = height
    }

    /**
     * Draw a line from ([x1], [y1]) to ([x2], [y2]).
     */
    abstract fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float)

    /**
     * Draw a polyline with [points], a list of points.
     */
    abstract fun drawPolyline(points: LinkedList<Point>)

    /**
     * Draw an arc centered at ([x]; [y]) of an ellipse with x-radius [rx] and y-radius [ry].
     * The arc starts at angle [start] and ends [extent] radians after, at `start + extent`.
     * Angle are in radians, angle of 0 is at a 3 o'clock position.
     * The arc is drawn counter-clockwise.
     */
    abstract fun drawArc(x: Float, y: Float, rx: Float, ry: Float,
                         start: Double, extent: Double)

    /**
     * Draw a rectangle with its top left corner at ([x]; [y]) and with dimensions [width] x [height].
     * @param[filled] whether to draw a filled rect or just the outline.
     */
    abstract fun drawRect(x: Float, y: Float, width: Float, height: Float, filled: Boolean)

    /**
     * Export the canvas image to [file].
     */
    abstract fun exportTo(file: File)


    companion object {
        const val SIZE_UNSET = -1f

        /**
         * Parse a hex color string like `#RRGGBB` or `#AARRGGBB` to a [Color].
         */
        fun parseColor(color: String): Color {
            if (color.startsWith('#')) {
                val value = color.substring(1).toLong(16).toInt()
                if (color.length == 7) {
                    return Color(value)
                } else if (color.length == 9) {
                    return Color(value, true)
                }
            }
            throw IllegalArgumentException("Bad color string '$color'.")
        }
    }

}