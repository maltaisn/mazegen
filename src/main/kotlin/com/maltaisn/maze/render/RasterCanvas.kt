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
import javax.imageio.ImageIO


/**
 * Canvas for exporting raster image files (PNG, JPG, BMP, GIF).
 */
class RasterCanvas(val format: OutputFormat) : Canvas() {

    private lateinit var graphics: Graphics2D
    private lateinit var buffImage: BufferedImage


    override fun drawLine(x1: Double, y1: Double, x2: Double, y2: Double) {
        initIfNeeded()
        graphics.drawLine(x1.toInt(), y1.toInt(), x2.toInt(), y2.toInt())
    }

    override fun exportTo(file: File) {
        ImageIO.write(buffImage, format.extension, file)
    }

    private fun initIfNeeded() {
        if (!this::graphics.isInitialized) {
            // Remove alpha channel from colors if format doesn't support it
            var type = BufferedImage.TYPE_INT_ARGB
            if (format != OutputFormat.PNG) {
                type = BufferedImage.TYPE_INT_RGB
                strokeColor = Color(strokeColor.rgb and 0xFFFFFF)
                backgroundColor = Color(backgroundColor.rgb and 0xFFFFFF)
            }

            buffImage = BufferedImage((width + strokeWidth + 1).toInt(),
                    (height + strokeWidth + 1).toInt(), type)
            graphics = buffImage.createGraphics()

            // Draw background color if not transparent
            if (backgroundColor.alpha != 0) {
                graphics.color = backgroundColor
                graphics.fillRect(0, 0, buffImage.width, buffImage.height)
            }

            // Translate to allow space for stroke on borders
            val offset = strokeWidth / 2
            graphics.translate(offset, offset)

            // Set stroke style
            graphics.color = strokeColor
            graphics.stroke = BasicStroke(strokeWidth.toFloat(),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        }
    }

}