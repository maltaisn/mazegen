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

package com.maltaisn.maze.maze

import com.maltaisn.maze.render.SVGCanvas
import org.junit.jupiter.api.Test
import java.awt.BasicStroke
import java.io.File


class SVGTest {

    @Test
    fun myTest() {
        val canvas = SVGCanvas()
        canvas.init(100f, 100f)
        canvas.stroke = BasicStroke(3f)

        canvas.drawArc(50f, 50f, 25f, 25f, 0.0, Math.PI * 0.5)
        canvas.drawArc(25f, 25f, 25f, 25f, Math.PI * 1.5, Math.PI * 0.5)
        canvas.drawArc(50f, 50f, 25f, 25f, Math.PI * 1.0, Math.PI * 0.5)
        canvas.drawArc(75f, 75f, 25f, 25f, Math.PI * 0.5, Math.PI * 0.5)

        canvas.optimize()

        canvas.exportTo(File("D:\\Documents\\nicolas\\code\\kotlin" +
                "\\maze\\mazes\\test.${canvas.format.extension}"))
    }

}