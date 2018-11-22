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

package com.maltaisn.maze

import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.io.FileInputStream


fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        val file = File(args[0])
        if (file.exists()) {
            val inputStream = FileInputStream(file)
            val configJson = JSONObject(JSONTokener(inputStream))
            inputStream.close()
            Main().parseConfig(configJson)
        } else {
            throw IllegalArgumentException("Config file doesn't exists.")
        }
    } else {
        throw IllegalArgumentException("No config file provided.")
    }
}

private class Main {

    fun parseConfig(config: JSONObject) {
        val canvasWriter = CanvasWriter(if (config.has(KEY_OUTPUT))
            config.getJSONObject(KEY_OUTPUT) else null)
        val mazeParser = MazeParser()

        // Parse mazes and export them
        if (!config.has(KEY_MAZES)) {
            println("No mazes to generate")
            return
        }
        for (mazeConfig in config.getJSONArray(KEY_MAZES)) {
            val mazes = mazeParser.parse(mazeConfig as JSONObject)
            for (i in 0 until mazes.size) {
                val maze = mazes[i]
                val name = maze.name ?: DEFAULT_FILENAME
                canvasWriter.write(maze, if (mazes.size == 1) name else "$name-${i + 1}")
            }
        }
    }

    companion object {
        private const val KEY_MAZES = "mazes"
        private const val KEY_OUTPUT = "output"

        private const val DEFAULT_FILENAME = "maze"
    }

}