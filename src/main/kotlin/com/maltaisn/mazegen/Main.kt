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

package com.maltaisn.mazegen

import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.io.FileInputStream


fun main(args: Array<String>) {
    try {
        if (args.isNotEmpty()) {
            for (arg in args) {
                val file = File(arg)
                if (file.exists()) {
                    if (args.size > 1) {
                        println("Configuration file: ${file.absolutePath}")
                    }
                    val configJson = FileInputStream(file).use { JSONObject(JSONTokener(it)) }
                    MazeGenerator((ConfigurationParser.parse(configJson))).generate()
                } else {
                    throw ParameterException("Configuration file " +
                            "at ${file.absolutePath} doesn't exists.")
                }
            }
        } else {
            throw ParameterException("No configuration file provided.")
        }
    } catch (exception: ParameterException) {
        println("ERROR: ${exception.message}")
    }
}
