package com.maltaisn.maze.maze

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull


class MazeTest {

    @Test
    fun cellAt() {
        val maze = FlatMaze(10, 10)

        val cell1 = maze.cellAt(5, 5)
        assertEquals(cell1.x, 5)
        assertEquals(cell1.y, 5)

        val cell2 = maze.cellAt(9, 0)
        assertEquals(cell2.x, 9)
        assertEquals(cell2.y, 0)
    }

    @Test
    fun optionalCellAt() {
        val maze = FlatMaze(10, 10)

        val cell1 = maze.optionalCellAt(5, 5)!!
        assertEquals(cell1.x, 5)
        assertEquals(cell1.y, 5)

        assertNull(maze.optionalCellAt(-1, 5))
        assertNull(maze.optionalCellAt(10, 10))
    }

}