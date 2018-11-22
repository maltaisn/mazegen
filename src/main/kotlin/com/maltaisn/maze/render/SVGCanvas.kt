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
import java.io.PrintWriter
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.collections.HashMap


/**
 * Canvas for exporting SVG files.
 * SVG path data can be optimized with [optimize] to reduce the file size.
 */
class SVGCanvas : Canvas() {

    /**
     * The maximum number of decimal digits used in path coordinates.
     */
    var precision = 0
        set(value) {
            val dfs = DecimalFormatSymbols()
            dfs.decimalSeparator = '.'
            numberFormat = DecimalFormat.getNumberInstance() as DecimalFormat
            numberFormat.decimalFormatSymbols = dfs
            numberFormat.isGroupingUsed = false
            numberFormat.maximumFractionDigits = value
        }

    private lateinit var numberFormat: DecimalFormat

    /**
     * List of line points. Points are indexes `i` and `i + 1`
     * are connected to form a line (where `i` an even number).
     */
    private val points = LinkedList<Point>()

    /**
     * Collection of polylines.
     * Each polyline is a list of points that are all connected together.
     * Null if [optimize] wasn't called yet.
     */
    private var polylines: Collection<Polyline>? = null

    init {
        precision = 2
    }

    override fun drawLine(x1: Double, y1: Double, x2: Double, y2: Double) {
        points.add(Point(x1, y1))
        points.add(Point(x2, y2))
    }

    /**
     * Optimize the SVG path data:
     * - Lines that share a point are connected together to make polylines.
     * - Lines that form a longer line become a single line.
     *
     * For each line drawn with [drawLine], if a point of the line matches the head or
     * the tail of an existing polyline, add the line to it. If not, create a new polyline.
     * If the line connects two polylines, merge them into one.
     *
     * Should not be used with over 40k points, performance is not good.
     */
    fun optimize() {
        polylines = null

        val polylineHeads = HashMap<Point, Polyline>()
        val polylineTails = HashMap<Point, Polyline>()
        for (i in 0 until points.size step 2) {
            var p1 = points[i]
            var p2 = points[i + 1]

            // Check if one of the polylines head is the same as one of the points.
            var polyline = polylineHeads[p1]
            if (polyline == null) {
                polyline = polylineHeads[p2]
                if (polyline != null) {
                    val temp = p1
                    p1 = p2
                    p2 = temp
                }
            }
            if (polyline != null) {
                // Found a polyline with a head at p2.
                extendPolyline(polyline, true, p2)
                polylineHeads.remove(p1)

                // Check if there's already a polyline with a head or a tail at p2.
                var headFound = true
                var prev = polylineHeads[p2]
                if (prev == null) {
                    prev = polylineTails[p2]
                    headFound = false
                }
                if (prev != null) {
                    // Found another polyline adjacent to this one, connect them.
                    polylineHeads.remove(prev.first)
                    polylineTails.remove(prev.last)
                    prev.removeAtEnd(headFound)
                    extendPolyline(polyline, true, prev.removeAtEnd(headFound))
                    for (point in (if (headFound) prev.iterator() else prev.descendingIterator())) {
                        polyline.addFirst(point)
                    }
                }
                polylineHeads[polyline.first] = polyline

            } else {
                // Check if one of the point is the same as a polyline tail.
                polyline = polylineTails[p1]
                if (polyline == null) {
                    polyline = polylineTails[p2]
                    if (polyline != null) {
                        val temp = p1
                        p1 = p2
                        p2 = temp
                    }
                }
                if (polyline != null) {
                    // Found a polyline with a tail at p2.
                    extendPolyline(polyline, false, p2)
                    polylineTails.remove(p1)

                    // Check if there's already a polyline with a head or a tail at p2.
                    var headFound = false
                    var prev = polylineTails[p2]
                    if (prev == null) {
                        prev = polylineHeads[p2]
                        headFound = true
                    }
                    if (prev != null) {
                        // Found another polyline adjacent to this one, connect them.
                        polylineHeads.remove(prev.first)
                        polylineTails.remove(prev.last)
                        prev.removeAtEnd(headFound)
                        extendPolyline(polyline, false, prev.removeAtEnd(headFound))
                        for (point in (if (headFound) prev.iterator() else prev.descendingIterator())) {
                            polyline.addLast(point)
                        }
                    }
                    polylineTails[polyline.last] = polyline

                } else {
                    // No polyline head or tail matches one of the points: create a new polyline.
                    polyline = Polyline()
                    polyline.add(p1)
                    polyline.add(p2)
                    polylineHeads[p1] = polyline
                    polylineTails[p2] = polyline
                }
            }
        }

        polylines = polylineHeads.values.toList()
    }

    /**
     * If [first] is true, add [with] point to the start of the [polyline], otherwise to the end.
     * If new point creates a segment that extends last segment, merge them into one segment.
     */
    private fun extendPolyline(polyline: Polyline, first: Boolean, with: Point) {
        val index = if (first) 0 else polyline.size - 1
        val last = polyline[index]
        val beforeLast = polyline[if (first) 1 else polyline.size - 2]
        if (beforeLast.x == last.x && last.x == with.x
                || beforeLast.y == last.y && last.y == with.y) {
            // New point extends the last segment in the polyline, merge them.
            polyline[index] = with
        } else if (last != with) {
            if (first) {
                polyline.addFirst(with)
            } else {
                polyline.addLast(with)
            }
        }
    }

    override fun exportTo(file: File) {
        val svg = StringBuilder(4096)
        svg.append("<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\" ")
        svg.append("width=\"${numberFormat.format(width + strokeWidth)}\" ")
        svg.append("height=\"${numberFormat.format(height + strokeWidth)}\">")

        // Add a filled rect for background color if not transparent
        if (backgroundColor.alpha != 0) {
            svg.append("<rect width=\"100%\" height=\"100%\" " +
                    "fill=\"${colorToHex(backgroundColor)}\"/>")
        }

        // Stroke style
        svg.append("<path style=\"")
        svg.append("stroke-width:${numberFormat.format(strokeWidth)};")
        svg.append("stroke:${colorToHex(strokeColor)};")
        svg.append("stroke-opacity:${strokeColor.alpha / 255.0};")
        svg.append("stroke-linecap:round;stroke-linejoin:round;fill:none\" ")

        // The transform matrix is used to convert between
        // the renderer Y-down coordinate system to SVG's Y-up.
        val offSetStr = numberFormat.format((strokeWidth / 2))
        svg.append("transform=\"matrix(1,0,0,1,0,0) translate($offSetStr $offSetStr)\"")

        svg.append(" d=\"")
        svg.append(getPathData())
        svg.append("\"/></svg>")

        // Export it to the file
        PrintWriter(file).use { it.print(svg) }
    }

    private fun getPathData(): String {
        // Create the SVG path data
        val path = StringBuilder()
        var lastPoint: Point? = null
        if (polylines != null) {
            // SVG was optimized to polylines: draw them.
            for (polyline in polylines!!) {
                for (i in 0 until polyline.size) {
                    val p = polyline[i]
                    addPointToPathData(path, p, lastPoint, i == 0)
                    lastPoint = p
                }
            }
        } else {
            // SVG wasn't optimized: draw segments between even and odd indexed points
            for (i in 0 until points.size step 2) {
                val p1 = points[i]
                val p2 = points[i + 1]
                addPointToPathData(path, p1, lastPoint, true)
                addPointToPathData(path, p2, p1, false)
                lastPoint = p2
            }
        }
        return path.toString()
    }

    private fun addPointToPathData(path: StringBuilder, point: Point,
                                   lastPoint: Point?, isFirst: Boolean) {
        if (lastPoint != null) {
            val relative = point - lastPoint
            when {
                isFirst -> {
                    path.append('m')
                    path.append(numberFormat.format(relative.x))
                    path.append(',')
                    path.append(numberFormat.format(relative.y))
                }
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
            path.append('M')
            path.append(numberFormat.format(point.x))
            path.append(',')
            path.append(numberFormat.format(point.y))
        }
    }

    private fun colorToHex(color: Color): String =
            '#' + Integer.toHexString(color.rgb and 0xFFFFFF).padStart(6, '0')

    private data class Point(val x: Double, val y: Double) {

        private var hash = 0

        operator fun plus(point: Point): Point = Point(x + point.x, y + point.y)

        operator fun minus(point: Point): Point = Point(x - point.x, y - point.y)

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is Point) return false
            return x == other.x && y == other.y
        }

        /**
         * Hash function taken from [javafx.geometry.Point2D].
         */
        override fun hashCode(): Int {
            if (hash == 0) {
                var bits = 7L
                bits = 31L * bits + x.toBits()
                bits = 31L * bits + y.toBits()
                hash = (bits xor (bits shr 32)).toInt()
            }
            return hash
        }

    }

    private class Polyline : LinkedList<Point>() {

        fun removeAtEnd(head: Boolean): Point = if (head) {
            removeFirst()
        } else {
            removeLast()
        }

    }

}