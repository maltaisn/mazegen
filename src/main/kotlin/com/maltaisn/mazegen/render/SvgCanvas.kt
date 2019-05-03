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
import java.io.PrintWriter
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.*


/**
 * Canvas for exporting SVG files.
 * SVG path data can be optimized with [optimize] to reduce the file size.
 */
class SvgCanvas : Canvas(OutputFormat.SVG) {

    private val paths = mutableListOf<Path>()

    private lateinit var currentStyle: Style

    override var color: Color = Color.BLACK
        set(value) {
            if (field != value) {
                field = value
                updateStyle()
            }
        }

    override var stroke = BasicStroke(1f)
        set(value) {
            if (field != value) {
                field = value
                updateStyle()
            }
        }

    private var filled = false
        set(value) {
            if (field != value) {
                field = value
                updateStyle()
            }
        }

    override var antialiasing = true
        set(value) {
            if (field != value) {
                field = value
                updateStyle()
            }
        }

    /** The maximum number of decimal digits used for all values. */
    var precision: Int = 2

    /**
     * The optimization level for the SVG document.
     * Optimization is done at the same time as export.
     */
    var optimization = OPTIMIZATION_STYLES


    init {
        updateStyle()
    }


    override fun drawLine(x1: Double, y1: Double, x2: Double, y2: Double) {
        this.filled = false
        paths += Path(currentStyle, mutableListOf(
                PathElement.Start(x1 + translate.x, y1 + translate.y),
                PathElement.Line(x2 - x1, y2 - y1, true)))
    }

    override fun drawArc(cx: Double, cy: Double, rx: Double, ry: Double,
                         startAngle: Double, extent: Double) {
        if (extent < PI * 2) {
            this.filled = false
            val dx = rx * cos(startAngle)
            val dy = -ry * sin(startAngle)
            paths += Path(currentStyle, mutableListOf(
                    PathElement.Start(cx + dx + translate.x, cy + dy + translate.y),
                    PathElement.Arc(-dx, -dy, rx, ry, startAngle, extent, true)))
        } else {
            drawEllipse(cx, cy, rx, ry, false)
        }
    }

    override fun drawRect(x: Double, y: Double,
                          width: Double, height: Double, filled: Boolean) {
        this.filled = filled
        paths += Path(currentStyle, mutableListOf(
                PathElement.Start(x + translate.x, y + translate.y),
                PathElement.Line(width, 0.0, true),
                PathElement.Line(0.0, height, true),
                PathElement.Line(-width, 0.0, true),
                PathElement.End))
    }

    override fun drawEllipse(cx: Double, cy: Double,
                             rx: Double, ry: Double, filled: Boolean) {
        this.filled = filled
        paths += Path(currentStyle, mutableListOf(
                PathElement.Start(cx - rx + +translate.x, cy + translate.y),
                PathElement.Arc(rx * 2, 0.0, rx, ry, 0.0,
                        largeArc = true, sweepArc = false, relative = true),
                PathElement.Arc(-rx * 2, 0.0, rx, ry, 0.0,
                        largeArc = true, sweepArc = false, relative = true),
                PathElement.End))
    }

    override fun drawPath(points: List<Point>, filled: Boolean) {
        this.filled = filled
        val elements = mutableListOf<PathElement>()
        for (point in points) {
            val x = point.x + translate.x
            val y = point.y + translate.y
            if (point is ArcPoint) {
                val startX = x + point.rx * cos(point.start)
                val startY = y - point.ry * sin(point.start)
                elements += if (elements.isEmpty()) {
                    PathElement.Start(startX, startY)
                } else {
                    PathElement.Line(startX, startY)
                }
                elements += PathElement.Arc(x, y,
                        point.rx, point.ry, point.start, point.extent)
            } else {
                elements += if (elements.isEmpty()) {
                    PathElement.Start(x, y)
                } else {
                    PathElement.Line(x, y)
                }
            }
        }
        if (filled) {
            elements += PathElement.End
        }
        paths += Path(currentStyle, elements)
    }

    override fun drawText(text: String, x: Double, y: Double) {
        throw UnsupportedOperationException("SVG canvas doesn't support drawing text.")
    }


    override fun exportTo(file: File) {
        val nbFmt = SvgNumberFormat(precision)

        // Optimize
        optimize(nbFmt)

        // Create SVG text
        val svg = StringBuilder(8192)
        svg.append("<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\" ")
        svg.append("width=\"${nbFmt.format(width)}\" ")
        svg.append("height=\"${nbFmt.format(height)}\">")
        for (shape in paths) {
            shape.appendTo(svg, nbFmt)
        }
        svg.append("</svg>")

        // Export it to the file
        PrintWriter(file).use { it.print(svg) }
    }

    private fun updateStyle() {
        currentStyle = Style(stroke, color, filled, antialiasing)
    }

    private fun optimize(nbFmt: NumberFormat) {
        optimizeStyles()
        optimizePolypoints()
        optimizeColinear()
        optimizeRelative(nbFmt)
    }

    /**
     * Each element (line, rectangle, polygon, etc) is represented by a different
     * path element in SVG by default. Group elements that share the same style.
     * This optimization can result in up to 90% decreased size.
     */
    private fun optimizeStyles() {
        if (optimization < OPTIMIZATION_STYLES) return

        val pathsMap = paths.groupBy { it.style }
        paths.clear()

        for ((style, stylePaths) in pathsMap) {
            val elements = mutableListOf<PathElement>()
            stylePaths.flatMapTo(elements) { it.elements }
            paths += Path(style, elements)
        }
    }

    /**
     * Optimize path data by removing unneeded points. This involves:
     * - Removing duplicate subsequent points,
     *   e.g.: `M0,0 L0,0 L10,0` becomes `M0,0 L10,0`.
     * - Connecting segments that share a point into a polypoint,
     *   e.g.: `M0,0 L20,0 M20,0 L20,20` becomes `M0,0 L20,0 L20,20`.
     * This optimization can result in up to 20% decreased size.
     */
    private fun optimizePolypoints() {
        if (optimization < OPTIMIZATION_POLYPOINTS) return

        for (path in paths) {
            val elements = path.elements

            // Make every path point absolute
            var lastPoint: PathElement.Point? = null
            for (element in elements) {
                if (element is PathElement.Point) {
                    if (lastPoint != null && element.relative) {
                        element.x += lastPoint.x
                        element.y += lastPoint.y
                        element.relative = false
                    }
                    lastPoint = element
                }
            }

            // Create a list with every polypoint (an open path) in the path.
            // Also remove the polypoints from the elements, leaving only closed paths.
            // Remove duplicate subsequent points from all polypoints.
            val polypoints = mutableListOf<Polypoint>()
            var endIndex = -1
            for (i in elements.lastIndex downTo 0) {
                val element = elements[i]
                if (element is PathElement.Start) {
                    if (endIndex == -1) {
                        val points = LinkedList<PathElement.Point>()
                        points += elements.removeAt(i) as PathElement.Point
                        val polypoint = Polypoint(points)
                        while (i < elements.size) {
                            val point = elements[i]
                            if (point is PathElement.Start) {
                                break
                            } else if (point != points.lastOrNull()) {
                                // Only add the point if the last point isn't at the same coordinates.
                                // A point at the same coordinates as the last is always useless.
                                points += point as PathElement.Point
                            }
                            elements.removeAt(i)
                        }
                        if (points.size > 1) {
                            polypoints += polypoint
                        }
                    }
                    endIndex = -1
                } else if (element is PathElement.End) {
                    endIndex = i
                }
            }

            // FIXME not working for many mazes
            //   - Theta: arcs are reversed half of the time
            //   - Weave orthogonal: missing parts of the maze

            // Do as many passes as needed, usually 2.
            val endpoints = HashMap<PathElement.Point, Polypoint>()
            do {
                val initialCount = polypoints.size
                for (polypoint in polypoints) {
                    val head = polypoint.head
                    val tail = polypoint.tail

                    if (connectPolypoint(endpoints, polypoint, head)) continue
                    if (connectPolypoint(endpoints, polypoint, tail)) continue

                    endpoints[tail] = polypoint
                    endpoints[head] = polypoint
                }

                polypoints.clear()
                polypoints += endpoints.mapNotNull {
                    if (it.key is PathElement.Start) it.value else null
                }
                endpoints.clear()
            } while (polypoints.size != initialCount)

            elements += polypoints.flatMap { it.points }
        }
    }

    /**
     * Connects a [polypoint] to an existing polypoint in the map of [endpoints]
     * if one shares an [endpoint]. Returns true if a connection was made.
     */
    private fun connectPolypoint(endpoints: HashMap<PathElement.Point, Polypoint>,
                                 polypoint: Polypoint, endpoint: PathElement.Point): Boolean {
        val other = endpoints[endpoint]
        if (other != null && other !== polypoint) {
            endpoints -= endpoint

            // Check if the polypoint is in the right direction
            val pointIsStart = endpoint is PathElement.Start
            val otherIsStart = other.head == endpoint
            if (pointIsStart == otherIsStart) {
                polypoint.switchHead()
            }

            // Append the new polypoint to the existing one.
            val newEndpoint: PathElement.Point
            if (otherIsStart) {
                // Append before the start
                other.points.removeFirst()
                other.points.addAll(0, polypoint.points)
                newEndpoint = other.head
            } else {
                // Append after the end
                polypoint.points.removeFirst()
                other.points += polypoint.points
                newEndpoint = other.tail
            }

            // Update the "other" polypoint's endpoint since it changed.
            if (newEndpoint in endpoints) {
                // The new endpoint already exist. Connect "other" with the existing endpoint owner,
                // except if head equals tail meaning the "other" polypoint forms a loop.
                val head = other.head
                val tail = other.tail
                if (head != tail) {
                    endpoints -= if (newEndpoint === tail) head else tail
                    connectPolypoint(endpoints, other, newEndpoint)
                }
            } else {
                endpoints[newEndpoint] = other
            }

            return true
        }
        return false
    }

    /**
     * Merge connected segments with the same slope (colinear) into a single segment.
     * This optimization can result in up to 40% decreased size.
     */
    private fun optimizeColinear() {
        if (optimization < OPTIMIZATION_COLINEAR) return

        for (path in paths) {
            val elements = path.elements

            var lastSlope = Double.NaN
            var lastX = 0.0
            var lastY = 0.0

            var i = 0
            while (i < elements.size) {
                val element = elements[i]

                if (element is PathElement.Point) {
                    when (element) {
                        is PathElement.Start -> lastSlope = Double.NaN
                        is PathElement.Line -> {
                            val slope = (element.y - lastY) / (element.x - lastX)
                            if (slope == lastSlope) {
                                val lastElement = elements[i - 1] as PathElement.Line
                                lastElement.x = element.x
                                lastElement.y = element.y
                                elements.removeAt(i)
                                i--
                            }
                            lastSlope = slope
                        }
                        is PathElement.Arc -> {
                            lastSlope = Double.NaN
                            // Connected arcs with the same center and radii could be
                            // optimized into a single arc but it's much more complicated.
                        }
                    }
                    lastX = element.x
                    lastY = element.y
                } else {
                    lastSlope = Double.NaN
                }
                i++
            }
        }
    }

    /**
     * Optimize path data by using relative commands instead of absolute ones whenever possible.
     * This optimization can result in up to 70% decreased size.
     */
    private fun optimizeRelative(nbFmt: NumberFormat) {
        if (optimization < OPTIMIZATION_RELATIVE) return

        val minDiff = 10.0.pow(-precision)
        val reprBuffer = StringBuilder()

        for (path in paths) {
            val elements = path.elements
            val first = elements.first() as PathElement.Start

            // Keep track of last point absolute coordinates but also of the actual
            // last point coordinates (taking path data precision into account).
            // Since relative commands are repeatedly added to the last absolute coordinate
            // the rounding error accumulates. So an absolute point is needed once in a while.
            var last = PrecisionPoint(first.x, first.y,
                    first.x.roundToPrecision(), first.y.roundToPrecision())

            var start = last.copy()

            for (i in 1 until elements.size) {
                val element = elements[i]
                if (element is PathElement.Point) {
                    assert(!element.relative)  // A prior optimization step is supposed to have made every point absolute

                    val newX = element.x - last.x
                    val newY = element.y - last.y
                    last.x = element.x
                    last.y = element.y
                    last.actualX += newX.roundToPrecision()
                    last.actualY += newY.roundToPrecision()

                    if (last.roundingError <= minDiff) {
                        element.appendTo(reprBuffer, nbFmt)
                        val absoluteLength = reprBuffer.length
                        reprBuffer.clear()

                        // Make point relative
                        element.x = newX
                        element.y = newY
                        element.relative = true

                        element.appendTo(reprBuffer, nbFmt)
                        val relativeLength = reprBuffer.length
                        reprBuffer.clear()

                        if (relativeLength >= absoluteLength) {
                            // Relative point representation is not shorter, use absolute.
                            element.x = last.x
                            element.y = last.y
                            element.relative = false
                        }
                    }

                    if (!element.relative) {
                        // Kept absolute coordinates, either because it's shorter or rounding error became too great
                        last.actualX = element.x.roundToPrecision()
                        last.actualY = element.y.roundToPrecision()
                    }

                    if (element is PathElement.Start) {
                        start = last.copy()
                    }
                } else if (element is PathElement.End) {
                    last = start
                }
            }
        }
    }

    private data class PrecisionPoint(var x: Double, var y: Double,
                                      var actualX: Double, var actualY: Double) {

        val roundingError: Double
            get() = max((x - actualX).absoluteValue, (y - actualY).absoluteValue)
    }

    private fun Double.roundToPrecision(): Double {
        val power = 10.0.pow(precision)
        return (this * power).roundToInt() / power
    }

    private class Polypoint(val points: LinkedList<PathElement.Point> = LinkedList()) {

        val head: PathElement.Start
            get() = points.first() as PathElement.Start

        val tail: PathElement.Point
            get() = points.last()

        /** Switch the polypoint start element position. */
        fun switchHead() {
            val first = points.removeFirst()
            points.reverse()
            points.addFirst(first)

            val firstX = first.x
            val firstY = first.y
            for (i in 0 until points.size - 1) {
                val curr = points[i]
                val next = points[i + 1]
                curr.x = next.x
                curr.y = next.y
            }
            tail.x = firstX
            tail.y = firstY
        }

        override fun toString() = points.joinToString(" ")
    }

    /**
     * SVG shape with a [style]. Every shape is represented as a path element.
     */
    private class Path(val style: Style,
                       val elements: MutableList<PathElement> = mutableListOf()) {

        fun appendTo(svg: StringBuilder, nbFmt: NumberFormat) {
            svg.append("<path ")
            style.appendTo(svg, nbFmt)
            svg.append(" d=\"")
            for (element in elements) {
                element.appendTo(svg, nbFmt)
            }
            svg.append("\"/>")
        }

        override fun toString(): String {
            val sb = StringBuilder()
            appendTo(sb, SvgNumberFormat(3))
            return sb.toString()
        }
    }

    private sealed class PathElement(private val symbol: Char,
                                     var relative: Boolean) {

        open fun createParamsList(): List<Double> = emptyList()

        open fun appendTo(svg: StringBuilder, nbFmt: NumberFormat) {
            svg.append(if (relative) symbol.toLowerCase() else symbol.toUpperCase())

            val params = createParamsList()
            if (params.isNotEmpty()) {
                for (param in params) {
                    svg.append(nbFmt.format(param))
                    svg.append(',')
                }
                svg.deleteCharAt(svg.length - 1)
            }
        }

        override fun toString(): String {
            val sb = StringBuilder()
            appendTo(sb, SvgNumberFormat(3))
            return sb.toString()
        }

        /** Base class for an element with a point coordinate. */
        abstract class Point(x: Double, y: Double,
                             symbol: Char, relative: Boolean) :
                PathElement(symbol, relative) {

            var x = x
                set(value) {
                    field = value
                    hash = 0
                }

            var y = y
                set(value) {
                    field = value
                    hash = 0
                }

            private var hash = 0

            override fun createParamsList() = listOf(x, y)

            override fun equals(other: Any?) = other === this ||
                    other is Point && x == other.x && y == other.y

            /** Hash function taken from [javafx.geometry.Point2D]. */
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

        /** Move to element, at ([x]; [y]). */
        class Start(x: Double, y: Double, relative: Boolean = false) : Point(x, y, 'M', relative)

        /** Line to element, from last point to ([x]; [y]). */
        class Line(x: Double, y: Double, relative: Boolean = false) : Point(x, y, 'L', relative) {

            override fun appendTo(svg: StringBuilder, nbFmt: NumberFormat) {
                if (relative) {
                    if (x == 0.0) {
                        svg.append('v')
                        svg.append(nbFmt.format(y))
                        return
                    } else if (y == 0.0) {
                        svg.append('h')
                        svg.append(nbFmt.format(x))
                        return
                    }
                }

                svg.append(if (relative) 'l' else 'L')
                svg.append(nbFmt.format(x))
                svg.append(',')
                svg.append(nbFmt.format(y))
            }
        }

        /**
         * Arc element from current point to ([x]; [y]). The ellipse has radii of [rx] and [ry],
         * and a [rotation]. For arc flags, see [https://www.w3.org/TR/SVG/images/paths/arcs02.svg].
         * Angles are in radians.
         */
        class Arc(x: Double, y: Double, val rx: Double, val ry: Double, val rotation: Double,
                  val largeArc: Boolean, val sweepArc: Boolean, relative: Boolean = false) :
                Point(x, y, 'A', relative) {

            /**
             * Arc of an ellipse centered at ([x]; [y]) with radii of `rx` and `ry`.
             * The arc starts on `startAngle` and extends for `extent` (angles are in radians).
             */
            constructor(cx: Double, cy: Double, rx: Double, ry: Double,
                        startAngle: Double, extent: Double, relative: Boolean = false) : this(
                    cx + rx * cos(startAngle + extent), cy - ry * sin(startAngle + extent),
                    rx, ry, 0.0, extent >= PI, extent < 0, relative)

            override fun createParamsList() = listOf(rx, ry, rotation,
                    if (largeArc) 1.0 else 0.0, if (sweepArc) 1.0 else 0.0, x, y)
        }

        /** Close path element. */
        object End : PathElement('Z', false)
    }

    private data class Style(val stroke: BasicStroke,
                             val color: Color,
                             val filled: Boolean,
                             val antialiasing: Boolean) {

        fun appendTo(svg: StringBuilder, nbFmt: NumberFormat) {
            svg.append("style=\"")

            // Style attribute
            val colorStr = '#' + Integer.toHexString(color.rgb and 0xFFFFFF)
                    .toUpperCase().padStart(6, '0')
            if (filled) {
                svg.append("fill:")
                svg.append(colorStr)
                svg.append(';')
                if (antialiasing) {
                    svg.append("stroke:")
                    svg.append(colorStr)
                    svg.append(";stroke-width:1;")  // This is really awful
                }
            } else {
                svg.append("stroke:")
                svg.append(colorStr)
                svg.append(";stroke-width:")
                svg.append(nbFmt.format(stroke.lineWidth))
                svg.append(";stroke-linecap:")
                svg.append(when (stroke.endCap) {
                    BasicStroke.CAP_SQUARE -> "square"
                    BasicStroke.CAP_ROUND -> "round"
                    else -> "butt"
                })
                svg.append(";stroke-linejoin:")
                when (stroke.endCap) {
                    BasicStroke.JOIN_MITER -> {
                        svg.append("miter;stroke-miterlimit:")
                        svg.append(nbFmt.format(stroke.miterLimit))
                    }
                    BasicStroke.JOIN_ROUND -> svg.append("round")
                    else -> svg.append("butt")
                }
                svg.append(";fill:none")
            }
            svg.append('"')

            // Antialiasing
            if (!antialiasing) {
                svg.append(" shape-rendering=\"crispEdges\"")
            }
        }
    }

    private class SvgNumberFormat(precision: Int) : DecimalFormat() {
        init {
            decimalFormatSymbols = DecimalFormatSymbols().apply {
                decimalSeparator = '.'
            }
            isGroupingUsed = false
            maximumFractionDigits = precision
        }
    }

    companion object {
        /** No optimization. */
        const val OPTIMIZATION_NONE = 0

        /** Merge all paths with the same style into one. */
        const val OPTIMIZATION_STYLES = 1

        /** Merge touching lines and arcs into polypoints. */
        const val OPTIMIZATION_POLYPOINTS = 2

        /** Merge colinear lines into a single line, same radius and center arcs into single arc. */
        const val OPTIMIZATION_COLINEAR = 3

        /** Use only relative commands. */
        const val OPTIMIZATION_RELATIVE = 4
    }

}
