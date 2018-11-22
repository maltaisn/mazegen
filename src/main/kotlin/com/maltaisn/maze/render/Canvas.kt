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

import java.awt.Color
import java.io.File


/**
 * Interface for a class to draw a maze and output it to a file.
 * The canvas uses a positive Y down, positive X right coordinate system.
 */
abstract class Canvas {

    var width = SIZE_NONE
    var height = SIZE_NONE

    var strokeWidth = 1.0
    var strokeColor = Color.BLACK!!
    var backgroundColor = parseColor("#00FFFFFF")

    /**
     * Draw a line from ([x1], [y1]) to ([x2], [y2]).
     */
    abstract fun drawLine(x1: Double, y1: Double, x2: Double, y2: Double)

    /**
     * Export the canvas image to [file].
     */
    abstract fun exportTo(file: File)


    companion object {
        const val SIZE_NONE = -1.0

        fun parseColor(color: String): Color {
            if (color.startsWith('#')) {
                val value = color.substring(1).toInt(16)
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