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

import org.json.JSONArray


/**
 * Class defining an opening position in a maze.
 */
class Opening(from: JSONArray) {

    val position: IntArray = IntArray(from.length())

    init {
        for (i in 0 until from.length()) {
            val pos = from[i]
            when (pos) {
                is String -> position[i] = when (pos[0]) {
                    CHAR_START -> POS_START
                    CHAR_CENTER -> POS_CENTER
                    CHAR_END -> POS_END
                    else -> throw IllegalArgumentException("Wrong opening position character '$pos'.")
                }
                is Int -> position[i] = pos
                else -> throw IllegalArgumentException("Wrong opening position argument '$pos'.")
            }
        }
    }

    companion object {
        const val POS_START = -3
        const val POS_CENTER = -2
        const val POS_END = -1

        const val CHAR_START = 'S'
        const val CHAR_CENTER = 'C'
        const val CHAR_END = 'E'
    }

}