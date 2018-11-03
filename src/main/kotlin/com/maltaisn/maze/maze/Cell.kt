package com.maltaisn.maze.maze


abstract class Cell(protected val maze: Maze<out Cell>, val x: Int, val y: Int) {

    override fun toString(): String = "[x: $x, y: $y]"

}