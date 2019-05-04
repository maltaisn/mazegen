/*
 * Copyright (c) 2019 Nicolas Maltais
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

package com.maltaisn.mazegen.render

import java.awt.BasicStroke
import java.awt.Color
import java.io.File


/**
 * Interface for a class that draws graphics and output it to a file.
 * Canvas has an Y-down coordinate system and `(0,0)` is the top left corner.
 */
abstract class Canvas(val format: OutputFormat) {

    var width = SIZE_UNSET
        private set
    var height = SIZE_UNSET
        private set

    /** The stroke style used by the canvas. */
    open var stroke = BasicStroke(1f)

    /** The color used for the stroke and fill by the canvas. */
    open var color: Color = Color.BLACK

    /** The translation applied to this canvas, `null` if no translation is applied. */
    open var translate = Point(0.0, 0.0)

    /** Whether to draw with antialiasing or not. */
    open var antialiasing = true

    /** Only used by [SvgCanvas] to determine the z-index of drawn shapes. */
    open var zIndex = 0

    /**
     * Initialize the canvas with a width and height.
     * Should only be called once.
     */
    open fun init(width: Double, height: Double) {
        if (width == SIZE_UNSET) {
            error("This canvas was already initialized.")
        }

        this.width = width
        this.height = height
    }

    /**
     * Draw a line from ([x1], [y1]) to ([x2], [y2]).
     */
    abstract fun drawLine(x1: Double, y1: Double, x2: Double, y2: Double)

    /**
     * Draw an arc centered at ([cx]; [cy]) of an ellipse with x-radius [rx] and y-radius [ry].
     * The arc starts at angle [startAngle] and ends [extent] radians after, at `start + extent`.
     * Angle are in radians, angle of 0 is at a 3 o'clock position.
     * The arc is drawn counter-clockwise.
     */
    abstract fun drawArc(cx: Double, cy: Double, rx: Double, ry: Double,
                         startAngle: Double, extent: Double)

    /**
     * Draw a rectangle with its top left corner at ([x]; [y]) and with size [width] x [height].
     * @param filled whether to draw a filled rectangle or just the outline.
     */
    abstract fun drawRect(x: Double, y: Double, width: Double, height: Double,
                          filled: Boolean = false)

    /**
     * Draw an ellipse centered at ([cx]; [cy]), with radii of [rx] and [ry].
     * @param filled whether to draw a filled ellipse or just the outline.
     */
    abstract fun drawEllipse(cx: Double, cy: Double, rx: Double, ry: Double,
                             filled: Boolean = false)

    /**
     * Draw a path with [points], a list of points. Can contain [ArcPoint].
     * If [filled], path will be closed first.
     */
    abstract fun drawPath(points: List<Point>, filled: Boolean = false)

    /**
     * Draw [text] centered at ([x]; [y]), for debug purposes only.
     */
    abstract fun drawText(text: String, x: Double, y: Double)

    /**
     * Export the canvas image to [file].
     */
    abstract fun exportTo(file: File)


    companion object {
        const val SIZE_UNSET = -1.0
    }

}
