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
import java.io.PrintWriter
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


/**
 * Canvas for exporting SVG files.
 * SVG path data can be optimized with [optimize] to reduce the file size.
 */
class SVGCanvas : Canvas() {

    /**
     * The maximum number of decimal digits used for all values.
     */
    var precision = 0
        set(value) {
            field = value

            val dfs = DecimalFormatSymbols()
            dfs.decimalSeparator = '.'
            numberFormat.decimalFormatSymbols = dfs
            numberFormat.isGroupingUsed = false
            numberFormat.maximumFractionDigits = value
        }

    private val numberFormat = DecimalFormat.getNumberInstance() as DecimalFormat


    /**
     * List of shapes drawn to this canvas.
     */
    private val shapes = ArrayList<Shape>()

    /**
     * The current translate transform applied to the canvas, null if none.
     */
    private var translate: Point? = null


    init {
        precision = 2
    }

    override fun drawLine(x1: Double, y1: Double, x2: Double, y2: Double) {
        val last = shapes.lastOrNull()
        val lines: Lines
        if (last is Lines && last.color === color
                && last.stroke === stroke
                && last.translate === translate) {
            lines = last
        } else {
            lines = Lines(stroke, color, translate)
            shapes.add(lines)
        }

        lines.points.add(Point(x1, y1))
        lines.points.add(Point(x2, y2))
    }

    override fun drawPolyline(points: LinkedList<Point>) {
        shapes.add(Polyline(stroke, color, translate, points))
    }

    override fun drawRect(x: Double, y: Double, width: Double, height: Double, filled: Boolean) {
        shapes.add(Rectangle(stroke, color, translate, x, y, width, height, filled))
    }

    override fun translate(x: Double, y: Double) {
        translate = if (x == 0.0 && y == 0.0) {
            null
        } else {
            Point(x, y)
        }
    }

    override fun exportTo(file: File) {
        val svg = StringBuilder(4096)
        svg.append("<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\" ")
        svg.append("width=\"${numberFormat.format(width)}\" ")
        val heightStr = numberFormat.format(height)
        svg.append("height=\"$heightStr\">")

        for (shape in shapes) {
            shape.toSvg(svg, numberFormat)
        }

        svg.append("</svg>")

        // Export it to the file
        PrintWriter(file).use { it.print(svg) }
    }

    /**
     * Optimize lines shapes to polylines.
     */
    fun optimize() {
        for (i in 0 until shapes.size) {
            val shape = shapes[i]
            if (shape is Lines) {
                val polylines = shape.optimizeToPolines()
                shapes[i] = polylines
            } else if (shape is Polyline) {
                shape.optimize()
            }
        }
    }

    /**
     * Base class for a shape with a [stroke] style and a [color].
     * @param[filled] whether this shape is filled or not.
     */
    private abstract class Shape(val stroke: BasicStroke, val color: Color, val filled: Boolean,
                                 val translate: Point?) {

        /**
         * Append the SVG representation of this shape to [svg].
         */
        abstract fun toSvg(svg: StringBuilder, numberFormat: DecimalFormat)

        /**
         * Append the SVG style attribute of this shape to [svg].
         */
        protected fun appendAttributes(svg: StringBuilder, numberFormat: DecimalFormat): String {
            svg.append("style=\"")

            // Style attribute
            val colorStr = '#' + Integer.toHexString(color.rgb and 0xFFFFFF)
                    .toUpperCase().padStart(6, '0')
            if (filled) {
                svg.append("stroke:none;")
                svg.append("fill:$colorStr")
            } else {
                svg.append("stroke:$colorStr;")
                svg.append("stroke-width:${numberFormat.format(stroke.lineWidth)};")
                svg.append("stroke-linecap:")
                svg.append(when (stroke.endCap) {
                    BasicStroke.CAP_SQUARE -> "square"
                    BasicStroke.CAP_ROUND -> "round"
                    else -> "butt"
                })
                svg.append(";stroke-linejoin:")
                svg.append(when (stroke.endCap) {
                    BasicStroke.JOIN_MITER -> "miter;stroke-miterlimit:" +
                            numberFormat.format(stroke.miterLimit)
                    BasicStroke.JOIN_ROUND -> "round"
                    else -> "butt"
                })
                svg.append(";fill:none")
            }
            svg.append('"')

            // Translate attribute
            if (translate != null) {
                svg.append(" transform=\"translate(")
                svg.append(numberFormat.format(translate.x))
                svg.append(',')
                svg.append(numberFormat.format(translate.y))
                svg.append(")\"")
            }

            return svg.toString()
        }

    }

    /**
     * Base class for a shape represented with a `<path>` tag in SVG.
     */
    private abstract class PathShape(stroke: BasicStroke, color: Color, translate: Point?) :
            Shape(stroke, color, false, translate) {

        final override fun toSvg(svg: StringBuilder, numberFormat: DecimalFormat) {
            svg.append("<path ")
            appendAttributes(svg, numberFormat)
            svg.append(" d=\"")
            appendPathData(svg, numberFormat)
            svg.append("\"/>")
        }

        /**
         * Append the path data of this shape to [svg].
         */
        abstract fun appendPathData(svg: StringBuilder, numberFormat: DecimalFormat)

        /**
         * Add [point] to the path data in [path].
         * Added point is connected with the last point if [isFirst] is false.
         */
        protected fun addPointToPathData(path: StringBuilder, numberFormat: DecimalFormat,
                                         point: Point, lastPoint: Point?, isFirst: Boolean) {
            if (lastPoint != null && !isFirst) {
                when {
                    point.y == lastPoint.y -> {
                        // Same Y as last point: horizontal line
                        path.append('H')
                        path.append(numberFormat.format(point.x))
                    }
                    point.x == lastPoint.x -> {
                        // Same X as last point: vertical line
                        path.append('V')
                        path.append(numberFormat.format(point.y))
                    }
                    else -> {
                        path.append('L')
                        path.append(numberFormat.format(point.x))
                        path.append(',')
                        path.append(numberFormat.format(point.y))
                    }
                }
            } else {
                path.append('M')
                path.append(numberFormat.format(point.x))
                path.append(',')
                path.append(numberFormat.format(point.y))
            }
        }

    }

    /**
     * Path shape for a list of lines with only 2 points each.
     * This shape can be optimized to a [Polylines] shape.
     */
    private class Lines(stroke: BasicStroke, color: Color, translate: Point?) :
            PathShape(stroke, color, translate) {

        val points = LinkedList<Point>()

        /**
         * Optimize the lines data:
         * - Lines that share a point are connected together to make polylines.
         * - Lines that form a longer line become a single line.
         *
         * For each line drawn, if a point of the line matches the head or the tail of
         * an existing polyline, add the line to it. If not, create a new polyline.
         * If the line connects two polylines, merge them into one.
         *
         * Should not be used with over 40k points, performance is not good.
         */
        fun optimizeToPolines(): Polylines {
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
                    polyline.extendWith(p2, true)
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
                        polylineHeads.remove(prev.points.first)
                        polylineTails.remove(prev.points.last)
                        prev.removeAtEnd(headFound)
                        polyline.extendWith(prev.removeAtEnd(headFound), true)
                        for (point in (if (headFound) prev.points.iterator() else prev.points.descendingIterator())) {
                            polyline.points.addFirst(point)
                        }
                    }
                    polylineHeads[polyline.points.first] = polyline

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
                        polyline.extendWith(p2, false)
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
                            polylineHeads.remove(prev.points.first)
                            polylineTails.remove(prev.points.last)
                            prev.removeAtEnd(headFound)
                            polyline.extendWith(prev.removeAtEnd(headFound), false)
                            for (point in (if (headFound) prev.points.iterator() else prev.points.descendingIterator())) {
                                polyline.points.addLast(point)
                            }
                        }
                        polylineTails[polyline.points.last] = polyline

                    } else {
                        // No polyline head or tail matches one of the points: create a new polyline.
                        polyline = Polyline(stroke, color, translate)
                        polyline.points.add(p1)
                        polyline.points.add(p2)
                        polylineHeads[p1] = polyline
                        polylineTails[p2] = polyline
                    }
                }
            }

            return Polylines(stroke, color, translate, polylineHeads.values.toList())
        }

        override fun appendPathData(svg: StringBuilder, numberFormat: DecimalFormat) {
            var lastPoint: Point? = null
            for (i in 0 until points.size step 2) {
                val p1 = points[i]
                val p2 = points[i + 1]
                addPointToPathData(svg, numberFormat, p1, lastPoint, true)
                addPointToPathData(svg, numberFormat, p2, p1, false)
                lastPoint = p2
            }
        }

    }

    /**
     * Path shape for a list of points that are all connected together.
     */
    private class Polyline(stroke: BasicStroke, color: Color, translate: Point?,
                           val points: LinkedList<Point> = LinkedList()) :
            PathShape(stroke, color, translate) {

        fun removeAtEnd(head: Boolean): Point = if (head) {
            points.removeFirst()
        } else {
            points.removeLast()
        }

        /**
         * Optimize polylines to connect colinear segments together.
         */
        fun optimize() {
            val oldPoints = LinkedList(points)
            points.clear()
            for (point in oldPoints) {
                extendWith(point, false)
            }
        }

        /**
         * If [first] is true, add [with] point to the start of the polyline, otherwise to the end.
         * If new point creates a segment that extends last segment, merge them into one segment.
         */
        fun extendWith(with: Point, first: Boolean) {
            val lastIndex = if (first) 0 else points.size - 1
            val last = if (points.isEmpty()) null else points[lastIndex]
            val beforeLast = if (points.size < 2) null else points[if (first) 1 else points.size - 2]
            if (beforeLast != null && last != null) {
                val lastSlope = (last.y - beforeLast.y) / (last.x - beforeLast.x)
                val newSlope = (with.y - last.y) / (with.x - last.x)
                if (newSlope == lastSlope) {
                    // New point extends the last segment in the polyline, merge them.
                    points[lastIndex] = with
                    return
                }
            }

            if (first) {
                points.addFirst(with)
            } else {
                points.addLast(with)
            }
        }

        override fun appendPathData(svg: StringBuilder, numberFormat: DecimalFormat) {
            var lastPoint: Point? = null
            for (i in 0 until points.size) {
                val point = points[i]
                addPointToPathData(svg, numberFormat, point, lastPoint, i == 0)
                lastPoint = point
            }
        }

    }

    /**
     * Path shape for a list of [Polyline].
     */
    private class Polylines(stroke: BasicStroke, color: Color, translate: Point?,
                            val polylines: List<Polyline> = LinkedList()) :
            PathShape(stroke, color, translate) {

        override fun appendPathData(svg: StringBuilder, numberFormat: DecimalFormat) {
            var lastPoint: Point? = null
            for (polyline in polylines) {
                for (i in 0 until polyline.points.size) {
                    val point = polyline.points[i]
                    addPointToPathData(svg, numberFormat, point, lastPoint, i == 0)
                    lastPoint = point
                }
            }
        }

    }

    /**
     * Shape for a rectangle with a top left corner at ([x] ; [y]) and dimensions [width] x [height].
     * @param[filled] whether the rectangle is filled or not.
     */
    private class Rectangle(stroke: BasicStroke, color: Color, translate: Point?,
                            val x: Double, val y: Double, val width: Double,
                            val height: Double, filled: Boolean) :
            Shape(stroke, color, filled, translate) {

        override fun toSvg(svg: StringBuilder, numberFormat: DecimalFormat) {
            svg.append("<rect ")
            svg.append("x=\"${numberFormat.format(x)}\" ")
            svg.append("y=\"${numberFormat.format(y)}\" ")
            svg.append("width=\"${numberFormat.format(width)}\" ")
            svg.append("height=\"${numberFormat.format(height)}\" ")
            appendAttributes(svg, numberFormat)
            svg.append("/>")
        }

    }

}