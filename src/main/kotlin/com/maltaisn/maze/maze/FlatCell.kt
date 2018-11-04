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


/**
 * Interface for the cell of a flat maze.
 */
interface FlatCell : Cell {

    /**
     * Returns the neighbor cell on the [side] of the cell.
     * If the neighbor doesn't exist, returns null.
     */
    fun getCellOnSide(side: Side): FlatCell?

    /**
     * Get the list of all non-null neighbor cells of this cell.
     */
    fun getNeighbors(): List<FlatCell>

    /**
     * Returns true if [side] is set.
     */
    fun hasSide(side: Side): Boolean

    /**
     * Opens (removes) [side] of the cell.
     */
    fun openSide(side: Side)

    /**
     * Closes (adds) [side] of the cell.
     */
    fun closeSide(side: Side)

    /**
     * Toggles [side] of the cell.
     */
    fun toggleSide(side: Side)

    /**
     * Connect this cell with another cell [cell] if they are neighbors of the same maze.
     * Does nothing otherwise. The common side of both cells is opened (removed).
     */
    fun connectWith(cell: FlatCell)

    /**
     * Marker interface for a flat cell side.
     */
    interface Side

}