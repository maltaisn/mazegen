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


class CellTest {

    @Test
    fun hasSide() {
        val maze = RectMaze(1, 1,
                RectCell.Side.NORTH.value or RectCell.Side.SOUTH.value)
        val cell = maze.cellAt(PositionXY(0, 0))
        assertTrue(cell.hasSide(RectCell.Side.NORTH))
        assertFalse(cell.hasSide(RectCell.Side.EAST))
        assertTrue(cell.hasSide(RectCell.Side.SOUTH))
        assertFalse(cell.hasSide(RectCell.Side.WEST))
        assertFalse(cell.hasSide(RectCell.Side.ALL))
        assertFalse(cell.hasSide(RectCell.Side.NONE))

        cell.value = RectCell.Side.NONE.value
        assertFalse(cell.hasSide(RectCell.Side.NORTH))
        assertFalse(cell.hasSide(RectCell.Side.ALL))
        assertTrue(cell.hasSide(RectCell.Side.NONE))

        cell.value = RectCell.Side.ALL.value
        assertTrue(cell.hasSide(RectCell.Side.NORTH))
        assertTrue(cell.hasSide(RectCell.Side.ALL))
        assertFalse(cell.hasSide(RectCell.Side.NONE))
    }

    @Test
    fun getCellOnSide() {
        val maze = RectMaze(3, 3)

        val middleCell = maze.cellAt(PositionXY(1, 1))

        val northCell = middleCell.getCellOnSide(RectCell.Side.NORTH)!!
        assertEquals(PositionXY(1, 0), northCell.position)

        val southCell = middleCell.getCellOnSide(RectCell.Side.SOUTH)!!
        assertEquals(PositionXY(1, 2), southCell.position)

        val eastCell = middleCell.getCellOnSide(RectCell.Side.EAST)!!
        assertEquals(PositionXY(2, 1), eastCell.position)

        val westCell = middleCell.getCellOnSide(RectCell.Side.WEST)!!
        assertEquals(PositionXY(0, 1), westCell.position)

        assertNull(middleCell.getCellOnSide(RectCell.Side.NONE))
        assertNull(northCell.getCellOnSide(RectCell.Side.NORTH))
        assertNull(eastCell.getCellOnSide(RectCell.Side.EAST))

        assertSame(middleCell, northCell.getCellOnSide(RectCell.Side.SOUTH))
        assertSame(middleCell, westCell.getCellOnSide(RectCell.Side.EAST))
    }

    @Test
    fun getNeighbors() {
        val maze1 = RectMaze(3, 3)
        val nwCell = maze1.cellAt(PositionXY(0, 0))
        val nCell = maze1.cellAt(PositionXY(1, 0))
        val neCell = maze1.cellAt(PositionXY(2, 0))
        val eCell = maze1.cellAt(PositionXY(2, 1))
        val mCell = maze1.cellAt(PositionXY(1, 1))
        val wCell = maze1.cellAt(PositionXY(0, 1))
        val sCell = maze1.cellAt(PositionXY(1, 2))

        assertEquals(listOf(nCell, eCell, sCell, wCell), mCell.getNeighbors())
        assertEquals(listOf(neCell, mCell, nwCell), nCell.getNeighbors())
        assertEquals(listOf(nCell, wCell), nwCell.getNeighbors())

        val maze2 = RectMaze(1, 1)
        assertTrue(maze2.cellAt(PositionXY(0, 0)).getNeighbors().isEmpty())
    }

    @Test
    fun connectWith() {
        val maze = RectMaze(3, 3, RectCell.Side.ALL.value)
        val middleCell = maze.cellAt(PositionXY(1, 1))

        val northCell = maze.cellAt(PositionXY(1, 0))
        middleCell.connectWith(northCell)
        assertFalse(middleCell.hasSide(RectCell.Side.NORTH))
        assertFalse(northCell.hasSide(RectCell.Side.SOUTH))

        val eastCell = maze.cellAt(PositionXY(2, 1))
        middleCell.connectWith(eastCell)
        assertFalse(middleCell.hasSide(RectCell.Side.EAST))
        assertFalse(eastCell.hasSide(RectCell.Side.WEST))

        val southCell = maze.cellAt(PositionXY(1, 2))
        southCell.connectWith(middleCell)
        assertFalse(middleCell.hasSide(RectCell.Side.SOUTH))
        assertFalse(southCell.hasSide(RectCell.Side.NORTH))

        val westCell = maze.cellAt(PositionXY(0, 1))
        westCell.connectWith(middleCell)
        assertFalse(middleCell.hasSide(RectCell.Side.WEST))
        assertFalse(westCell.hasSide(RectCell.Side.EAST))
    }

    @Test
    fun findSideOfCell() {
        val maze = RectMaze(3, 3, RectCell.Side.ALL.value)
        val middleCell = maze.cellAt(PositionXY(1, 1))

        val northCell = maze.cellAt(PositionXY(1, 0))
        assertEquals(RectCell.Side.NORTH, middleCell.findSideOfCell(northCell))
        assertEquals(RectCell.Side.SOUTH, northCell.findSideOfCell(middleCell))

        val eastCell = maze.cellAt(PositionXY(2, 1))
        assertEquals(RectCell.Side.EAST, middleCell.findSideOfCell(eastCell))
        assertEquals(RectCell.Side.WEST, eastCell.findSideOfCell(middleCell))

        val northEastCell = maze.cellAt(PositionXY(2, 0))
        assertNull(middleCell.findSideOfCell(northEastCell))
        assertNull(northEastCell.findSideOfCell(middleCell))
        assertEquals(RectCell.Side.WEST, northEastCell.findSideOfCell(northCell))
    }

}