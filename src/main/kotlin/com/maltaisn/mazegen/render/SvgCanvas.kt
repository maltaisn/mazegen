package com.maltaisn.mazegen.render

import java.awt.BasicStroke
import java.awt.Color
import java.io.File
import java.io.PrintWriter
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


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
                PathElement.Start(cx - rx + + translate.x, cy + translate.y),
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
        optimize()

        val nbFmt = SvgNumberFormat(precision)

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

    private fun optimize() {
        if (optimization >= OPTIMIZATION_STYLES) {
            optimizeStyles()
            if (optimization >= OPTIMIZATION_PATHS) {
                // NOT WORKING
                //optimizePaths()

                if (optimization >= OPTIMIZATION_RELATIVE) {
                    optimizeRelative()
                }
            }
        }
    }

    /**
     * Each element (line, rectangle, polygon, etc) is represented by a different
     * path element in SVG by default. Group elements that share the same style.
     * This optimization can result in 50-80% decreased size.
     */
    private fun optimizeStyles() {
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
     * - Removing duplicate subsequent points
     *   (ex: `M0,0 L0,0 L10,0` becomes `M0,0 L20,0`)
     * - Connecting segments that share a point into a polypoint.
     *   (ex: `M0,0 L20,0 M20,0 L20,20` becomes `M0,0 L20,0 L20,20`)
     * - Converting colinear and touching lines into a single one.
     *   (ex: `M0,0 L10,0 L20,0 L30,0` becomes `M0,0 L30,0`)
     * - Converting touching arcs with the same center and radius into a single one.
     */
    private fun optimizePaths() {
        for (path in paths) {
            // Create a list with every polypoint (an open path) in the path.
            // Also remove the polypoints from the elements, leaving only closed paths.
            // Remove duplicate subsequent points from all polypoints.
            val polypoints = mutableListOf<Polypoint>()
            var endIndex = -1
            for (i in path.elements.lastIndex downTo 0) {
                val element = path.elements[i]
                if (element is PathElement.Start) {
                    if (endIndex == -1) {
                        val polypoint = Polypoint()
                        var point: PathElement
                        do {
                            point = path.elements[i]
                            if (point != polypoint.points.lastOrNull()) {
                                // Only add the point if the last point isn't at the same coordinates.
                                // A point at the same coordinates as the last is always useless.
                                polypoint.points += point as PathElement.Point
                                path.elements.removeAt(i)
                            }
                        } while (point !is PathElement.Start)
                        if (polypoint.points.size > 1) {
                            polypoints += polypoint
                        }
                    }
                    endIndex = -1
                } else if (element is PathElement.End) {
                    endIndex = i
                }
            }

            /*
            val heads = HashMap<Point, PathElement.Point>()
            val tails = HashMap<Point, PathElement.Point>()

            // Optimize lines
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
                    polypoint = SvgCanvasOld.Path.Element.Polyline()
                    polypoint.points.add(start)
                    polypoint.points.add(end)
                    heads[start] = polypoint
                    tails[end] = polypoint
                }
            }
            */
        }
    }

    /**
     * Optimize path data by using relative commands instead of absolute ones whenever possible.
     * This will most of the time result in a smaller size but not always.
     */
    private fun optimizeRelative() {

    }

    private class Polypoint(val points: MutableList<PathElement.Point> = mutableListOf()) {
        /*
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
        */
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
                                     val relative: Boolean,
                                     val params: List<Double>?) {

        fun appendTo(svg: StringBuilder, nbFmt: NumberFormat) {
            svg.append(if (relative) symbol.toLowerCase() else symbol.toUpperCase())
            if (params?.isNotEmpty() == true) {
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
        abstract class Point(val x: Double, val y: Double,
                             symbol: Char, relative: Boolean, params: List<Double>?):
                PathElement(symbol, relative, params) {

            private var hash = 0

            override fun equals(other: Any?): Boolean {
                if (other === this) return true
                if (other !is Point) return false
                return x == other.x && y == other.y
            }

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
        class Start(x: Double, y: Double, relative: Boolean = false) :
                Point(x, y, 'M', relative, listOf(x, y))

        /** Line to element, from last point to ([x]; [y]). */
        class Line(x: Double, y: Double, relative: Boolean = false) :
                Point(x, y, 'L', relative, listOf(x, y))

        /**
         * Arc element from current point to ([x]; [y]). The ellipse has radii of [rx] and [ry],
         * and a [rotation]. For arc flags, see [https://www.w3.org/TR/SVG/images/paths/arcs02.svg].
         * Angles are in radians.
         */
        class Arc(x: Double, y: Double, val rx: Double, val ry: Double, val rotation: Double,
                  val largeArc: Boolean, val sweepArc: Boolean, relative: Boolean = false) :
                Point(x, y, 'A', relative, listOf(rx, ry, rotation,
                        if (largeArc) 1.0 else 0.0, if (sweepArc) 1.0 else 0.0, x, y)) {

            /**
             * Arc of an ellipse centered at ([x]; [y]) with radii of `rx` and `ry`.
             * The arc starts on `startAngle` and extends for `extent` (angles are in radians).
             */
            constructor(cx: Double, cy: Double, rx: Double, ry: Double,
                        startAngle: Double, extent: Double, relative: Boolean = false) : this(
                    cx + rx * cos(startAngle + extent), cy - ry * sin(startAngle + extent),
                    rx, ry, 0.0, extent >= PI, extent < 0, relative)
        }

        /** Close path element. */
        object End : PathElement('Z', false, null)

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
                svg.append("stroke:none;")
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

        /** Merge touching lines and arcs into longer segments. */
        const val OPTIMIZATION_PATHS = 2

        /** Convert all path commands to their relative equivalent. */
        const val OPTIMIZATION_RELATIVE = 3
    }

}
