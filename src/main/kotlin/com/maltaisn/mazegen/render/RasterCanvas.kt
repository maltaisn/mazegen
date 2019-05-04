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

import java.awt.*
import java.awt.geom.*
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.ceil


/**
 * Canvas for exporting raster image files (PNG, JPG, BMP, GIF).
 */
class RasterCanvas(format: OutputFormat) : Canvas(format) {

    private lateinit var graphics: Graphics2D
    private lateinit var buffImage: BufferedImage

    override var color
        get() = super.color
        set(value) {
            super.color = if (format != OutputFormat.PNG && value.alpha != 255) {
                // If format is not PNG, can't allow colors with an alpha channel
                Color(value.rgb and 0xFFFFFF)
            } else {
                value
            }
            if (this::graphics.isInitialized) {
                graphics.color = color
            }
        }

    override var stroke
        get() = super.stroke
        set(value) {
            super.stroke = value
            if (this::graphics.isInitialized) {
                graphics.stroke = stroke
            }
        }

    override var translate
        get() = super.translate
        set(value) {
            super.translate = value
            if (this::graphics.isInitialized) {
                graphics.transform = transform
                graphics.translate(value.x, value.y)
            }
        }

    /** Default transform saved to reset it when needed */
    private lateinit var transform: AffineTransform

    override var antialiasing
        get() = super.antialiasing
        set(value) {
            super.antialiasing = value
            if (this::graphics.isInitialized) {
                graphics.setRenderingHints(RenderingHints(RenderingHints.KEY_ANTIALIASING,
                        if (value) RenderingHints.VALUE_ANTIALIAS_ON else RenderingHints.VALUE_ANTIALIAS_OFF))
            }
        }


    override fun init(width: Double, height: Double) {
        super.init(width, height)

        val type = if (format == OutputFormat.PNG) {
            BufferedImage.TYPE_INT_ARGB
        } else {
            BufferedImage.TYPE_INT_RGB
        }
        buffImage = BufferedImage(ceil(width).toInt(), ceil(height).toInt(), type)
        graphics = buffImage.createGraphics()
        graphics.font = Font("Arial", Font.PLAIN, 12)
        transform = graphics.transform

        // Set all settings again now that graphics is created
        color = color
        stroke = stroke
        antialiasing = antialiasing
        translate = translate
    }

    override fun drawLine(x1: Double, y1: Double, x2: Double, y2: Double) {
        graphics.draw(Line2D.Double(x1, y1, x2, y2))
    }

    override fun drawArc(cx: Double, cy: Double, rx: Double, ry: Double,
                         startAngle: Double, extent: Double) {
        graphics.draw(Arc2D.Double(cx - rx, cy - ry, 2 * rx, 2 * ry,
                Math.toDegrees(startAngle), Math.toDegrees(extent), Arc2D.OPEN))
    }

    override fun drawRect(x: Double, y: Double, width: Double, height: Double, filled: Boolean) {
        drawShape(Rectangle2D.Double(x, y, width, height), filled)
    }

    override fun drawEllipse(cx: Double, cy: Double, rx: Double, ry: Double, filled: Boolean) {
        drawShape(Ellipse2D.Double(cx - rx, cy - ry, 2 * rx, 2 * ry), filled)
    }

    override fun drawPath(points: List<Point>, filled: Boolean) {
        val path = Path2D.Double()
        var started = false
        for (point in points) {
            if (point is ArcPoint) {
                val arc = Arc2D.Double(point.x - point.rx, point.y - point.ry, 2 * point.rx, 2 * point.ry,
                        Math.toDegrees(point.start), Math.toDegrees(point.extent), Arc2D.OPEN)
                path.append(arc, true)
                started = true
            } else {
                if (started) {
                    path.lineTo(point.x, point.y)
                } else {
                    path.moveTo(point.x, point.y)
                    started = true
                }
            }
        }
        if (filled) {
            path.closePath()
        }
        drawShape(path, filled)
    }

    private fun drawShape(shape: Shape, filled: Boolean) {
        if (filled) {
            if (antialiasing) {
                // To avoid gaps in distance maps, outline and fill must be drawn
                graphics.stroke = BasicStroke(1.5f)
                graphics.draw(shape)
                graphics.stroke = stroke
            }
            graphics.fill(shape)
        } else {
            graphics.draw(shape)
        }
    }

    override fun drawText(text: String, x: Double, y: Double) {
        val bounds = graphics.fontMetrics.getStringBounds(text, graphics)
        graphics.drawString(text, (x - bounds.centerX).toFloat(), (y - bounds.centerY).toFloat())
    }

    override fun exportTo(file: File) {
        ImageIO.write(buffImage, format.extension, file)
    }

}
