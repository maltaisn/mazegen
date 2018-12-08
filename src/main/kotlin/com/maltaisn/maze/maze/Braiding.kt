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

import com.maltaisn.maze.ParameterException
import kotlin.math.min
import kotlin.math.round


/**
 * Braiding setting for a maze.
 */
class Braiding {

    private val value: Number
    private val byCount: Boolean

    /**
     * Braiding setting to remove [count] deadends.
     */
    constructor(count: Int) {
        if (count < 0) {
            throw ParameterException("Braiding parameter must be a positive number.")
        }

        value = count
        byCount = true
    }

    /**
     * Braiding setting to remove a percentage, [percent], of the total number of deadends.
     */
    constructor(percent: Double) {
        if (percent < 0 || percent > 1) {
            throw ParameterException("Braiding percentage must be between 0 and 1 inclusive.")
        }

        value = percent
        byCount = false
    }

    /**
     * Get the number of deadends to remove with this braiding
     * setting out of the [total] number of deadends.
     */
    fun getNumberOfDeadendsToRemove(total: Int) = if (byCount) {
        min(value.toInt(), total)
    } else {
        round(total * value.toDouble()).toInt()
    }

}