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

import java.awt.Color
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols


/**
 * Simple SVG file builder to create extremely optimized SVG paths.
 * Might be a bit slow for very large mazes.
 *
 * TODO connect polylines the end when another starts
 */
class SVGRenderer(val width: Int, val height: Int) {

    var strokeWidth = 1.0
    var strokeColor: Color = Color.BLACK

    /**
     * The maximum number of decimal digits used in path coordinates.
     */
    var precision = 0
        set(value) {
            val dfs = DecimalFormatSymbols()
            dfs.decimalSeparator = '.'
            numberFormat = DecimalFormat.getNumberInstance() as DecimalFormat
            numberFormat.decimalFormatSymbols = dfs
            numberFormat.maximumFractionDigits = value
        }

    private lateinit var numberFormat: DecimalFormat

    /**
     * List of line points. Points are indexes `i` and `i + 1`
     * are connected to form a line (where `i` an even number).
     */
    private val points = ArrayList<Point>(8192)

    /**
     * List of polylines. Each polyline is a list of points that are all connected together.
     * Null if [optimize] wasn't called yet.
     */
    private var polylines: ArrayList<ArrayList<Point>>? = null

    init {
        precision = 2
    }

    fun drawLine(x1: Double, y1: Double, x2: Double, y2: Double) {
        points.add(Point(x1, y1))
        points.add(Point(x2, y2))
    }

    fun optimize() {
        polylines = ArrayList()
        for (i in 0 until points.size step 2) {
            // If a point is equal to the head or the tail of a polyline, attach the new line to it.
            val p1 = points[i]
            val p2 = points[i + 1]
            var found = false
            for (polyline in polylines!!) {
                val first = polyline.first()
                if (first == p1) {
                    extendPolyline(polyline, true, p2)
                    found = true
                } else if (first == p2) {
                    extendPolyline(polyline, true, p1)
                    found = true
                } else {
                    val last = polyline.last()
                    if (last == p1) {
                        extendPolyline(polyline, false, p2)
                        found = true
                    } else if (last == p2) {
                        extendPolyline(polyline, false, p1)
                        found = true
                    }
                }
                if (found) break
            }
            if (!found) {
                // New line is at the end of no polyline, create a new one.
                val polyline = ArrayList<Point>()
                polyline.add(p1)
                polyline.add(p2)
                polylines!!.add(polyline)
            }
        }


    }

    private fun extendPolyline(polyline: MutableList<Point>, first: Boolean, with: Point) {
        val index = if (first) 0 else polyline.size - 1
        val last = polyline[index]
        val beforeLast = polyline[if (first) 1 else polyline.size - 2]
        if (beforeLast.x == last.x && last.x == with.x
                || beforeLast.y == last.y && last.y == with.y) {
            // New point is colinear with last segment in the polyline
            // Extend last segment without adding new point
            polyline[index] = with
        } else if (last != with) {
            if (first) {
                polyline.add(0, with)
            } else {
                polyline.add(with)
            }
        }
    }

    /**
     * Creates and returns the SVG file content corresponding to the drawn objects.
     */
    fun create(): String {
        val path = StringBuilder()
        if (polylines != null) {
            for (polyline in polylines!!) {
                var lastPoint: Point? = null
                for (i in 0 until polyline.size) {
                    val point = polyline[i]
                    if (lastPoint != null) {
                        val relative = point - lastPoint
                        when {
                            point.y == lastPoint.y -> {
                                // Same Y as last point: horizontal line
                                path.append('h')
                                path.append(numberFormat.format(relative.x))
                            }
                            point.x == lastPoint.x -> {
                                // Same X as last point: vertical line
                                path.append('v')
                                path.append(numberFormat.format(relative.y))
                            }
                            else -> {
                                path.append('l')
                                path.append(numberFormat.format(relative.x))
                                path.append(',')
                                path.append(numberFormat.format(relative.y))
                            }
                        }
                    } else {
                        // First point of polyline
                        path.append('M')
                        path.append(numberFormat.format(point.x))
                        path.append(',')
                        path.append(numberFormat.format(point.y))
                    }
                    lastPoint = point
                }
            }
        } else {
            for (i in 0 until points.size step 2) {
                val p1 = points[i]
                val p2 = points[i + 1]
                path.append('M')
                path.append(numberFormat.format(p1.x))
                path.append(',')
                path.append(numberFormat.format(p1.y))
                path.append('L')
                path.append(numberFormat.format(p2.x))
                path.append(',')
                path.append(numberFormat.format(p2.y))
            }
        }

        val colorHex = Integer.toHexString(strokeColor.rgb and 0xFFFFFF).padStart(6, '0')
        val color = "stroke:#$colorHex;stroke-opacity:${strokeColor.alpha / 255.0}"
        return "<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\" " +
                "width=\"$width\" height=\"$height\"><path style=\"stroke-width:$strokeWidth;" +
                "$color;stroke-linecap:round;stroke-linejoin:round;fill:none\" " +
                "transform=\"matrix(1,0,0,1,0,0)\" d=\"$path\"/></svg>"
    }

    private data class Point(val x: Double, val y: Double) {
        operator fun plus(point: Point): Point = Point(x + point.x, y + point.y)
        operator fun minus(point: Point): Point = Point(x - point.x, y - point.y)
    }

}