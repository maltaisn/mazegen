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

package com.maltaisn.mazegen.maze

import kotlin.math.absoluteValue
import kotlin.math.min


/**
 * A position in a 2D polar coordinate system.
 * @param x the index in the row array.
 * @param r the radial distance of this position from the center.
 * @param rowWidth the length of the row array, optional.
 */
class PositionPolar(x: Int, r: Int, val rowWidth: Int = 0) : Position2D(x, r) {

    /**
     * The super method wouldn't work because a polar system wraps around on the theta axis.
     * For example the distance between a cell at 0 degrees and 359 degrees is 1 not 359.
     */
    override fun distanceTo(pos: Position): Int {
        val dx = (x - (pos as PositionPolar).x).absoluteValue
        return (y - pos.y).absoluteValue + min(dx, rowWidth - dx)
    }

    override operator fun plus(pos: Position) =
            PositionPolar(x + (pos as PositionPolar).x, y + pos.y)

    override fun toString() = "[x: $x, r: $y]"

}
