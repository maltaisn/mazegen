package com.maltaisn.maze

import com.maltaisn.maze.generator.HuntKillGenerator


fun main(args: Array<String>) {
    print(HuntKillGenerator().generate(100, 100).format())
}