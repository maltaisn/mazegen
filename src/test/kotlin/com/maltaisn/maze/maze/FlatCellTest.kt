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

import org.junit.jupiter.api.Test
import kotlin.test.*


class FlatCellTest {

    @Test
    fun hasSide() {
        val maze = FlatMaze(1, 1, FlatCell.Side.NORTH.value or FlatCell.Side.SOUTH.value)
        val cell = maze.cellAt(0, 0)
        assertTrue(cell.hasSide(FlatCell.Side.NORTH))
        assertFalse(cell.hasSide(FlatCell.Side.EAST))
        assertTrue(cell.hasSide(FlatCell.Side.SOUTH))
        assertFalse(cell.hasSide(FlatCell.Side.WEST))
        assertFalse(cell.hasSide(FlatCell.Side.ALL))
        assertFalse(cell.hasSide(FlatCell.Side.NONE))

        cell.value = FlatCell.Side.NONE.value
        assertFalse(cell.hasSide(FlatCell.Side.NORTH))
        assertFalse(cell.hasSide(FlatCell.Side.ALL))
        assertTrue(cell.hasSide(FlatCell.Side.NONE))

        cell.value = FlatCell.Side.ALL.value
        assertTrue(cell.hasSide(FlatCell.Side.NORTH))
        assertTrue(cell.hasSide(FlatCell.Side.ALL))
        assertFalse(cell.hasSide(FlatCell.Side.NONE))
    }

    @Test
    fun getCellOnSide() {
        val maze = FlatMaze(3, 3)

        val middleCell = maze.cellAt(1, 1)

        val northCell = middleCell.getCellOnSide(FlatCell.Side.NORTH)!!
        assertEquals(1, northCell.x)
        assertEquals(0, northCell.y)

        val southCell = middleCell.getCellOnSide(FlatCell.Side.SOUTH)!!
        assertEquals(1, southCell.x)
        assertEquals(2, southCell.y)

        val eastCell = middleCell.getCellOnSide(FlatCell.Side.EAST)!!
        assertEquals(0, eastCell.x)
        assertEquals(1, eastCell.y)

        val westCell = middleCell.getCellOnSide(FlatCell.Side.WEST)!!
        assertEquals(2, westCell.x)
        assertEquals(1, westCell.y)

        assertNull(middleCell.getCellOnSide(FlatCell.Side.NONE))
        assertNull(northCell.getCellOnSide(FlatCell.Side.NORTH))
        assertNull(eastCell.getCellOnSide(FlatCell.Side.EAST))

        assertSame(middleCell, northCell.getCellOnSide(FlatCell.Side.SOUTH))
        assertSame(middleCell, westCell.getCellOnSide(FlatCell.Side.EAST))
    }

    @Test
    fun changeSide() {
        val maze1 = FlatMaze(3, 3, FlatCell.Side.ALL.value)

        val middleCell = maze1.cellAt(1, 1)
        val northCell = maze1.cellAt(1, 0)
        middleCell.openSide(FlatCell.Side.NORTH)
        assertFalse(middleCell.hasSide(FlatCell.Side.NORTH))
        assertFalse(northCell.hasSide(FlatCell.Side.SOUTH))

        middleCell.closeSide(FlatCell.Side.NORTH)
        assertTrue(middleCell.hasSide(FlatCell.Side.NORTH))
        assertTrue(northCell.hasSide(FlatCell.Side.SOUTH))

        middleCell.openSide(FlatCell.Side.ALL)
        assertTrue(middleCell.hasSide(FlatCell.Side.NONE))
        assertFalse(northCell.hasSide(FlatCell.Side.SOUTH))

        middleCell.closeSide(FlatCell.Side.ALL)
        middleCell.toggleSide(FlatCell.Side.NORTH)
        assertFalse(middleCell.hasSide(FlatCell.Side.NORTH))
        assertFalse(northCell.hasSide(FlatCell.Side.SOUTH))

        middleCell.toggleSide(FlatCell.Side.NORTH)
        assertTrue(middleCell.hasSide(FlatCell.Side.NORTH))
        assertTrue(northCell.hasSide(FlatCell.Side.SOUTH))

        val maze2 = FlatMaze(1, 1)
        val cell2 = maze2.cellAt(0, 0)
        cell2.toggleSide(FlatCell.Side.ALL)
        assertTrue(cell2.hasSide(FlatCell.Side.ALL))
    }

    @Test
    fun getNeighbors() {
        val maze1 = FlatMaze(3, 3)
        val nwCell = maze1.cellAt(0, 0)
        val nCell = maze1.cellAt(1, 0)
        val neCell = maze1.cellAt(2, 0)
        val eCell = maze1.cellAt(2, 1)
        val mCell = maze1.cellAt(1, 1)
        val wCell = maze1.cellAt(0, 1)
        val sCell = maze1.cellAt(1, 2)

        assertEquals(listOf(nCell, eCell, sCell, wCell), mCell.getNeighbors())
        assertEquals(listOf(neCell, mCell, nwCell), nCell.getNeighbors())
        assertEquals(listOf(nCell, wCell), nwCell.getNeighbors())

        val maze2 = FlatMaze(1, 1)
        assertTrue(maze2.cellAt(0, 0).getNeighbors().isEmpty())
    }

    @Test
    fun connectWith() {
        val maze = FlatMaze(3, 3, FlatCell.Side.ALL.value)
        val middleCell = maze.cellAt(1, 1)

        val northCell = maze.cellAt(1, 0)
        middleCell.connectWith(northCell)
        assertFalse(middleCell.hasSide(FlatCell.Side.NORTH))
        assertFalse(northCell.hasSide(FlatCell.Side.SOUTH))

        val eastCell = maze.cellAt(2, 1)
        middleCell.connectWith(eastCell)
        assertFalse(middleCell.hasSide(FlatCell.Side.EAST))
        assertFalse(eastCell.hasSide(FlatCell.Side.WEST))

        val southCell = maze.cellAt(1, 2)
        southCell.connectWith(middleCell)
        assertFalse(middleCell.hasSide(FlatCell.Side.SOUTH))
        assertFalse(southCell.hasSide(FlatCell.Side.NORTH))

        val westCell = maze.cellAt(0, 1)
        westCell.connectWith(middleCell)
        assertFalse(middleCell.hasSide(FlatCell.Side.WEST))
        assertFalse(westCell.hasSide(FlatCell.Side.EAST))
    }

}