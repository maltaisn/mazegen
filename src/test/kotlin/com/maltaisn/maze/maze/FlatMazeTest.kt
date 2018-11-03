package com.maltaisn.maze.maze

import com.maltaisn.maze.maze.FlatCell
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals


class FlatMazeTest {

    @Test
    fun defaultValue_coordinates() {
        val maze1 = FlatMaze(3, 3)
        for (x in 0 until maze1.width) {
            for (y in 0 until maze1.height) {
                val cell = maze1.cellAt(x, y)
                assertEquals(x, cell.x)
                assertEquals(y, cell.y)
                assertEquals(FlatCell.Side.NONE.value, cell.value)
            }
        }

        val maze2 = FlatMaze(3, 3, FlatCell.Side.ALL.value)
        for (x in 0 until maze2.width) {
            for (y in 0 until maze2.height) {
                val cell = maze2.cellAt(x, y)
                assertEquals(x, cell.x)
                assertEquals(y, cell.y)
                assertEquals(FlatCell.Side.ALL.value, cell.value)
            }
        }
    }

    @Test
    fun format() {
        val maze1 = FlatMaze(1, 1, FlatCell.Side.ALL.value)
        assertEquals("┌┐\n└┘", maze1.format())

        val maze2 = FlatMaze(3, 3, FlatCell.Side.ALL.value)
        maze2.cellAt(0, 0).connectWith(maze2.cellAt(1, 0))
        maze2.cellAt(1, 0).connectWith(maze2.cellAt(1, 1))
        maze2.cellAt(1, 1).connectWith(maze2.cellAt(2, 1))
        maze2.cellAt(2, 1).connectWith(maze2.cellAt(2, 0))
        maze2.cellAt(0, 1).connectWith(maze2.cellAt(1, 1))
        maze2.cellAt(0, 1).connectWith(maze2.cellAt(0, 2))
        maze2.cellAt(0, 2).connectWith(maze2.cellAt(1, 2))
        maze2.cellAt(1, 2).connectWith(maze2.cellAt(2, 2))
        assertEquals("┌─┬┐\n├╴╷│\n│╶─┤\n└──┘", maze2.format())
    }

}