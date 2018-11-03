package com.maltaisn.maze.generator

import com.maltaisn.maze.maze.Cell
import com.maltaisn.maze.maze.Maze
import java.util.concurrent.ThreadLocalRandom


abstract class Generator<T : Maze<out Cell>> {

    protected val random = ThreadLocalRandom.current()!!

    abstract fun generate(width: Int, height: Int): T

}