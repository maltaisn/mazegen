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
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO


/**
 * Canvas for exporting raster image files (PNG, JPG, BMP, GIF).
 */
class RasterCanvas(private val format: OutputFormat) : Canvas() {

    private lateinit var graphics: Graphics2D
    private lateinit var buffImage: BufferedImage

    override var color: Color = Color.BLACK
        set(value) {
            if (format != OutputFormat.PNG && value.alpha != 255) {
                // If format is not PNG, can't allow colors with an alpha channel
                field = Color(value.rgb and 0xFFFFFF)
            } else {
                field = value
            }
            if (this::graphics.isInitialized) {
                graphics.color = color
            }
        }

    override var stroke: BasicStroke = BasicStroke(1f)
        set(value) {
            field = value
            if (this::graphics.isInitialized) {
                graphics.stroke = stroke
            }
        }

    override fun init(width: Double, height: Double) {
        super.init(width, height)

        buffImage = BufferedImage(Math.ceil(width).toInt(), Math.ceil(height).toInt(),
                if (format == OutputFormat.PNG) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB)
        graphics = buffImage.createGraphics()
    }

    override fun drawLine(x1: Double, y1: Double, x2: Double, y2: Double) {
        graphics.drawLine(x1.toInt(), y1.toInt(), x2.toInt(), y2.toInt())
    }

    override fun drawPolyline(points: LinkedList<Point>) {
        val xPoints = IntArray(points.size)
        val yPoints = IntArray(points.size)
        for (i in 0 until points.size) {
            val point = points[i]
            xPoints[i] = point.x.toInt()
            yPoints[i] = point.y.toInt()
        }
        graphics.drawPolyline(xPoints, yPoints, points.size)
    }

    override fun drawRect(x: Double, y: Double, width: Double, height: Double, filled: Boolean) {
        if (filled) {
            graphics.fillRect(x.toInt(), y.toInt(), width.toInt(), height.toInt())
        } else {
            graphics.drawRect(x.toInt(), y.toInt(), width.toInt(), height.toInt())
        }
    }

    override fun translate(x: Double, y: Double) {
        graphics.translate(x.toInt(), y.toInt())
    }

    override fun exportTo(file: File) {
        ImageIO.write(buffImage, format.extension, file)
    }

}