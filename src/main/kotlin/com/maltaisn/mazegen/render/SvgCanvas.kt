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

package com.maltaisn.mazegen.render

import java.awt.BasicStroke
import java.awt.Color
import java.io.File
import java.io.PrintWriter
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.*


/**
 * Canvas for exporting SVG files.
 * SVG path data can be optimized with [optimize] to reduce the file size.
 */
class SvgCanvas : Canvas(OutputFormat.SVG) {

    private val shapes = LinkedList<Shape>()

    private lateinit var currentStyle: Style

    override var color: Color = Color.BLACK
        set(value) {
            field = value
            updateStyle()
        }

    override var stroke: BasicStroke = BasicStroke(1f)
        set(value) {
            field = value
            updateStyle()
        }

    override var translate: Point? = null
        set(value) {
            super.translate = value
            field = super.translate
            updateStyle()
        }

    override var antialiasing = true
        set(value) {
            field = value
            updateStyle()
        }

    /** The maximum number of decimal digits used for all values. */
    var precision: Int = 0
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
     * Whether or not to optimize the SVG document.
     * Optimization is done at the same time as export.
     */
    var optimize = false


    init {
        precision = 2
        updateStyle()
    }

    private fun updateStyle() {
        val translatePoint = if (translate != null) SvgPoint(translate!!) else null
        currentStyle = Style(stroke, color, translatePoint, antialiasing)
    }


    override fun drawLine(x1: Double, y1: Double, x2: Double, y2: Double) {
        getLastPath().elements.add(Path.Element.Line(SvgPoint(x1, y1), SvgPoint(x2, y2)))
    }

    override fun drawArc(x: Double, y: Double, rx: Double, ry: Double,
                         start: Double, extent: Double) {
        getLastPath().elements.add(Path.Element.Arc(x, y, rx, ry, start, extent))
    }

    override fun drawPolyline(points: LinkedList<Point>) {
        val polypoint = Path.Element.Polyline()
        for (point in points) {
            polypoint.points.add(SvgPoint(point))
        }
        getLastPath().elements.add(polypoint)
    }

    override fun drawRect(x: Double, y: Double, width: Double, height: Double, filled: Boolean) {
        shapes.add(Rectangle(currentStyle, x, y, width, height, filled))
    }

    private fun getLastPath(): Path {
        val last = shapes.lastOrNull()
        return if (last is Path && last.style === currentStyle) {
            last
        } else {
            val path = Path(currentStyle)
            shapes.add(path)
            path
        }
    }

    override fun exportTo(file: File) {
        // Optimize if needed
        if (optimize) optimize()

        // Create SVG text
        val svg = StringBuilder(8192)
        svg.append("<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\" ")
        svg.append("width=\"${numberFormat.format(width)}\" ")
        val heightStr = numberFormat.format(height)
        svg.append("height=\"$heightStr\">")
        for (shape in shapes) {
            shape.appendTo(svg, numberFormat)
        }
        svg.append("</svg>")

        // Export it to the file
        PrintWriter(file).use { it.print(svg) }
    }

    /**
     * Optimize lines in paths to polylines.
     */
    private fun optimize() {
        for (shape in shapes) {
            if (shape is Path) {
                shape.optimize()
            }
        }
    }

    /**
     * SVG element that with a string representation.
     */
    private interface SvgElement {
        /**
         * Append this SVG element to [svg] StringBuilder.
         */
        fun appendTo(svg: StringBuilder, numberFormat: DecimalFormat)
    }

    /**
     * SVG shape with a [style].
     */
    private abstract class Shape(val style: Style) : SvgElement

    /**
     * SVG point in a path element.
     */
    private open class SvgPoint(x: Double, y: Double) : Point(x, y), SvgElement {

        constructor(point: Point) : this(point.x, point.y)

        override fun appendTo(svg: StringBuilder, numberFormat: DecimalFormat) {
            svg.append(numberFormat.format(this.x))
            svg.append(',')
            svg.append(numberFormat.format(this.y))
        }

    }

    /**
     * Path shape that can contain lines and arcs.
     */
    private class Path(style: Style) : Shape(style) {

        val elements = LinkedList<Element>()

        /**
         * Optimize the elements data:
         * - Lines that share a point are connected together to make polylinse.
         * - Lines that together form a longer line are merged.
         *
         * For each line drawn, if one of its point matches the head or the tail of
         * an existing poyline, the segment is added to it. If not, a new poyline is created.
         * If the line connects two poylines together, they are merged into one.
         *
         * Should not be used with over 40k points, performance is not good.
         * Arcs are not optimized because it's too complicated for the purpose of this.
         */
        fun optimize() {
            val newElements = LinkedList<Element>()

            val heads = HashMap<Point, Element.Polyline>()
            val tails = HashMap<Point, Element.Polyline>()
            for (element in elements) {
                when (element) {
                    is Element.Polyline -> {
                        element.optimize()
                        newElements.add(element)
                    }
                    is Element.Arc -> {
                        // Arcs could be part of the same optimization as lines, but it adds
                        // a lot of complexity to the code and it's only use for theta mazes anyway.
                        newElements.add(element)
                    }
                    is Element.Line -> {
                        var start = element.start
                        var end = element.end

                        // Check if one of the polylines head is the same as one of the points.
                        var polypoint = heads[start]
                        if (polypoint == null) {
                            polypoint = heads[end]
                            if (polypoint != null) {
                                val temp = start
                                start = end
                                end = temp
                            }
                        }
                        if (polypoint != null) {
                            // Found a polyline with a head at p2.
                            polypoint.extendWith(end, true)
                            heads.remove(start)

                            // Check if there's already a polyline with a head or a tail at p2.
                            var headFound = true
                            var prev = heads[end]
                            if (prev == null) {
                                prev = tails[end]
                                headFound = false
                            }
                            if (prev != null) {
                                // Found another polyline adjacent to this one, connect them.
                                heads.remove(prev.points.first)
                                tails.remove(prev.points.last)
                                prev.removeAtEnd(headFound)
                                polypoint.extendWith(prev.removeAtEnd(headFound), true)
                                for (point in (if (headFound) prev.points.iterator() else prev.points.descendingIterator())) {
                                    polypoint.points.addFirst(point)
                                }
                            }
                            heads[polypoint.points.first] = polypoint

                        } else {
                            // Check if one of the point is the same as a polyline tail.
                            polypoint = tails[start]
                            if (polypoint == null) {
                                polypoint = tails[end]
                                if (polypoint != null) {
                                    val temp = start
                                    start = end
                                    end = temp
                                }
                            }
                            if (polypoint != null) {
                                // Found a polyline with a tail at p2.
                                polypoint.extendWith(end, false)
                                tails.remove(start)

                                // Check if there's already a polyline with a head or a tail at p2.
                                var headFound = false
                                var prev = tails[end]
                                if (prev == null) {
                                    prev = heads[end]
                                    headFound = true
                                }
                                if (prev != null) {
                                    // Found another polyline adjacent to this one, connect them.
                                    heads.remove(prev.points.first)
                                    tails.remove(prev.points.last)
                                    prev.removeAtEnd(headFound)
                                    polypoint.extendWith(prev.removeAtEnd(headFound), false)
                                    for (point in (if (headFound) prev.points.iterator() else prev.points.descendingIterator())) {
                                        polypoint.points.addLast(point)
                                    }
                                }
                                tails[polypoint.points.last] = polypoint

                            } else {
                                // No polyline head or tail matches one of the points: create a new polyline.
                                polypoint = Element.Polyline()
                                polypoint.points.add(start)
                                polypoint.points.add(end)
                                heads[start] = polypoint
                                tails[end] = polypoint
                            }
                        }
                    }
                }
            }

            newElements.addAll(heads.values)
            elements.clear()
            elements.addAll(newElements)
        }

        override fun appendTo(svg: StringBuilder, numberFormat: DecimalFormat) {
            svg.append("<path ")
            style.appendTo(svg, numberFormat, false)
            svg.append(" d=\"")
            for (element in elements) {
                element.appendTo(svg, numberFormat)
            }
            svg.append("\"/>")
        }

        /**
         * An element in a path.
         */
        sealed class Element : SvgElement {

            /**
             * A line in a path from [start] to [end].
             */
            class Line(val start: SvgPoint, val end: SvgPoint): Element() {

                override fun appendTo(svg: StringBuilder, numberFormat: DecimalFormat) {
                    svg.append('M')
                    start.appendTo(svg, numberFormat)
                    svg.append('L')
                    end.appendTo(svg, numberFormat)
                }

            }

            /**
             * An arc in a path. Angles are in radians.
             */
            class Arc(val cx: Double, val cy: Double, rx: Double, ry: Double,
                      val startAngle: Double, extent: Double):
                    Element() {

                val start = SvgPoint(cx + rx * cos(startAngle),
                        cy - ry * sin(startAngle))
                val end = SvgPoint(cx + rx * cos(startAngle + extent),
                        cy - ry * sin(startAngle + extent))
                val radius = SvgPoint(rx, ry)
                val extent = min(extent, PI * 2)

                override fun appendTo(svg: StringBuilder, numberFormat: DecimalFormat) {
                    svg.append('M')
                    start.appendTo(svg, numberFormat)

                    val radiusSb = StringBuilder()
                    radius.appendTo(radiusSb, numberFormat)
                    val radiusStr = radiusSb.toString()

                    if (extent == PI * 2) {
                        // Arc is a whole circle, but SVG can't draw that as a single arc.
                        // So draw two connected half circles.
                        val halfAngle = startAngle + PI
                        svg.append('A')
                        svg.append(radiusStr)
                        svg.append(",0,0,0,")
                        SvgPoint(cx + cos(halfAngle) * radius.x,
                                cy - sin(halfAngle) * radius.y).appendTo(svg, numberFormat)
                        svg.append('A')
                        svg.append(radiusStr)
                        svg.append(",0,0,0,")
                        end.appendTo(svg, numberFormat)
                    } else {
                        svg.append('A')
                        svg.append(radiusStr)
                        svg.append(",0,")
                        svg.append(if (extent > PI) '1' else '0')
                        svg.append(',')
                        svg.append(if (extent < 0) '1' else '0')
                        svg.append(',')
                        end.appendTo(svg, numberFormat)
                    }
                }
            }

            /**
             * A path element made of a points all connected together by lines.
             */
            class Polyline : Element() {

                val points = LinkedList<SvgPoint>()

                fun removeAtEnd(head: Boolean): SvgPoint = if (head) {
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
                fun extendWith(with: SvgPoint, first: Boolean) {
                    val lastIndex = if (first) 0 else points.size - 1
                    val last = points.getOrNull(lastIndex)

                    // Extending with point: slope between the last and before last point
                    // must be the same as the slope between the last and the new point.
                    if (last != null) {
                        val beforeLast = points.getOrNull(if (first) 1 else points.size - 2)
                        if (beforeLast != null) {
                            val lastSlope = (last.y - beforeLast.y) / (last.x - beforeLast.x)
                            val newSlope = (with.y - last.y) / (with.x - last.x)
                            if ((lastSlope - newSlope).absoluteValue < 0.001) {
                                // New point extends the last segment in the polyline, merge them.
                                points[lastIndex] = with
                                return
                            }
                        }
                    }

                    // Can't extend, just add the new point.
                    if (first) {
                        points.addFirst(with)
                    } else {
                        points.addLast(with)
                    }
                }

                override fun appendTo(svg: StringBuilder, numberFormat: DecimalFormat) {
                    var lastPoint: SvgPoint? = null
                    for (point in points) {
                        when {
                            lastPoint == null -> {
                                svg.append('M')
                                point.appendTo(svg, numberFormat)
                            }
                            point.y == lastPoint.y -> {
                                // Same Y as last point: horizontal line
                                svg.append('H')
                                svg.append(numberFormat.format(point.x))
                            }
                            point.x == lastPoint.x -> {
                                // Same X as last point: vertical line
                                svg.append('V')
                                svg.append(numberFormat.format(point.y))
                            }
                            else -> {
                                svg.append('L')
                                point.appendTo(svg, numberFormat)
                            }
                        }
                        lastPoint = point
                    }
                }

            }
        }
    }

    /**
     * Shape for a rectangle with a top left corner
     * at ([x] ; [y]) and size [width] by [height].
     */
    private class Rectangle(style: Style,
                            val x: Double, val y: Double,
                            val width: Double, val height: Double,
                            val filled: Boolean) :
            Shape(style) {

        override fun appendTo(svg: StringBuilder, numberFormat: DecimalFormat) {
            svg.append("<rect ")
            svg.append("x=\"${numberFormat.format(x)}\" ")
            svg.append("y=\"${numberFormat.format(y)}\" ")
            svg.append("width=\"${numberFormat.format(width)}\" ")
            svg.append("height=\"${numberFormat.format(height)}\" ")
            style.appendTo(svg, numberFormat, filled)
            svg.append("/>")
        }
    }

    /**
     * Class defining a shape style.
     */
    private data class Style(val stroke: BasicStroke, val color: Color,
                             val translate: SvgPoint?, val antialiasing: Boolean) {

        /**
         * Append the style to [svg] as attributes.
         * @param filled Whether style is for a filled shape or not.
         */
        fun appendTo(svg: StringBuilder, numberFormat: DecimalFormat, filled: Boolean) {
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
                translate.appendTo(svg, numberFormat)
                svg.append(")\"")
            }

            // Antialiasing
            if (!antialiasing) {
                svg.append(" shape-rendering=\"crispEdges\"")
            }
        }

    }

}