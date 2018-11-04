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


class FlatRectCellTest {

    @Test
    fun hasSide() {
        val maze = FlatRectMaze(1, 1, FlatRectCell.Side.NORTH.value or FlatRectCell.Side.SOUTH.value)
        val cell = maze.cellAt(0, 0)
        assertTrue(cell.hasSide(FlatRectCell.Side.NORTH))
        assertFalse(cell.hasSide(FlatRectCell.Side.EAST))
        assertTrue(cell.hasSide(FlatRectCell.Side.SOUTH))
        assertFalse(cell.hasSide(FlatRectCell.Side.WEST))
        assertFalse(cell.hasSide(FlatRectCell.Side.ALL))
        assertFalse(cell.hasSide(FlatRectCell.Side.NONE))

        cell.value = FlatRectCell.Side.NONE.value
        assertFalse(cell.hasSide(FlatRectCell.Side.NORTH))
        assertFalse(cell.hasSide(FlatRectCell.Side.ALL))
        assertTrue(cell.hasSide(FlatRectCell.Side.NONE))

        cell.value = FlatRectCell.Side.ALL.value
        assertTrue(cell.hasSide(FlatRectCell.Side.NORTH))
        assertTrue(cell.hasSide(FlatRectCell.Side.ALL))
        assertFalse(cell.hasSide(FlatRectCell.Side.NONE))
    }

    @Test
    fun getCellOnSide() {
        val maze = FlatRectMaze(3, 3)

        val middleCell = maze.cellAt(1, 1)

        val northCell = middleCell.getCellOnSide(FlatRectCell.Side.NORTH)!!
        assertEquals(1, northCell.x)
        assertEquals(0, northCell.y)

        val southCell = middleCell.getCellOnSide(FlatRectCell.Side.SOUTH)!!
        assertEquals(1, southCell.x)
        assertEquals(2, southCell.y)

        val eastCell = middleCell.getCellOnSide(FlatRectCell.Side.EAST)!!
        assertEquals(0, eastCell.x)
        assertEquals(1, eastCell.y)

        val westCell = middleCell.getCellOnSide(FlatRectCell.Side.WEST)!!
        assertEquals(2, westCell.x)
        assertEquals(1, westCell.y)

        assertNull(middleCell.getCellOnSide(FlatRectCell.Side.NONE))
        assertNull(northCell.getCellOnSide(FlatRectCell.Side.NORTH))
        assertNull(eastCell.getCellOnSide(FlatRectCell.Side.EAST))

        assertSame(middleCell, northCell.getCellOnSide(FlatRectCell.Side.SOUTH))
        assertSame(middleCell, westCell.getCellOnSide(FlatRectCell.Side.EAST))
    }

    @Test
    fun changeSide() {
        val maze1 = FlatRectMaze(3, 3, FlatRectCell.Side.ALL.value)

        val middleCell = maze1.cellAt(1, 1)
        val northCell = maze1.cellAt(1, 0)
        middleCell.openSide(FlatRectCell.Side.NORTH)
        assertFalse(middleCell.hasSide(FlatRectCell.Side.NORTH))
        assertFalse(northCell.hasSide(FlatRectCell.Side.SOUTH))

        middleCell.closeSide(FlatRectCell.Side.NORTH)
        assertTrue(middleCell.hasSide(FlatRectCell.Side.NORTH))
        assertTrue(northCell.hasSide(FlatRectCell.Side.SOUTH))

        middleCell.openSide(FlatRectCell.Side.ALL)
        assertTrue(middleCell.hasSide(FlatRectCell.Side.NONE))
        assertFalse(northCell.hasSide(FlatRectCell.Side.SOUTH))

        middleCell.closeSide(FlatRectCell.Side.ALL)
        middleCell.toggleSide(FlatRectCell.Side.NORTH)
        assertFalse(middleCell.hasSide(FlatRectCell.Side.NORTH))
        assertFalse(northCell.hasSide(FlatRectCell.Side.SOUTH))

        middleCell.toggleSide(FlatRectCell.Side.NORTH)
        assertTrue(middleCell.hasSide(FlatRectCell.Side.NORTH))
        assertTrue(northCell.hasSide(FlatRectCell.Side.SOUTH))

        val maze2 = FlatRectMaze(1, 1)
        val cell2 = maze2.cellAt(0, 0)
        cell2.toggleSide(FlatRectCell.Side.ALL)
        assertTrue(cell2.hasSide(FlatRectCell.Side.ALL))
    }

    @Test
    fun getNeighbors() {
        val maze1 = FlatRectMaze(3, 3)
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

        val maze2 = FlatRectMaze(1, 1)
        assertTrue(maze2.cellAt(0, 0).getNeighbors().isEmpty())
    }

    @Test
    fun connectWith() {
        val maze = FlatRectMaze(3, 3, FlatRectCell.Side.ALL.value)
        val middleCell = maze.cellAt(1, 1)

        val northCell = maze.cellAt(1, 0)
        middleCell.connectWith(northCell)
        assertFalse(middleCell.hasSide(FlatRectCell.Side.NORTH))
        assertFalse(northCell.hasSide(FlatRectCell.Side.SOUTH))

        val eastCell = maze.cellAt(2, 1)
        middleCell.connectWith(eastCell)
        assertFalse(middleCell.hasSide(FlatRectCell.Side.EAST))
        assertFalse(eastCell.hasSide(FlatRectCell.Side.WEST))

        val southCell = maze.cellAt(1, 2)
        southCell.connectWith(middleCell)
        assertFalse(middleCell.hasSide(FlatRectCell.Side.SOUTH))
        assertFalse(southCell.hasSide(FlatRectCell.Side.NORTH))

        val westCell = maze.cellAt(0, 1)
        westCell.connectWith(middleCell)
        assertFalse(middleCell.hasSide(FlatRectCell.Side.WEST))
        assertFalse(westCell.hasSide(FlatRectCell.Side.EAST))
    }

}