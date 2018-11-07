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

import java.util.concurrent.ThreadLocalRandom


/**
 * Class for a flat hexagonal maze represented by 2D grid of [FlatRectCell].
 * @param[arrangement] defines the arrangement of cells, see [HexMaze.Arrangement].
 * @param[width] the number of columns
 * @param[height] the number of rows. If arrangement is hexagon or triangle, this parameter is ignored.
 * @param[defaultCellValue] default value to assign to cells on construction, no sides by default.
 */
class FlatHexMaze(val arrangement: HexMaze.Arrangement, val width: Int, height: Int = width,
                  defaultCellValue: Int = FlatHexCell.Side.NONE.value) : HexMaze, FlatMaze {

    val height: Int

    /**
     * Hexagonal maze grid. There number of columns is the same as the maze width, except for
     * hexagonal shaped mazes where the number of columns is equal to `width * 2 - 1`.
     * The number of rows varies for each column depending on the arrangement of the maze.
     */
    private val grid: Array<Array<FlatHexCell>>

    /**
     * The offset of the actual Y coordinate of a cell in the grid array, for each column
     * The cell at ```grid[x][y]```'s actual coordinates are `(x ; y + rowOffsets[x])`.
     */
    private val rowOffsets: IntArray

    /**
     * Create a new hexagonal maze with the same width and height, equal to [dimension]
     * Hexagon and triangle mazes should be created with this constructor.
     */
    constructor(arrangment: HexMaze.Arrangement, dimension: Int,
                defaultCellValue: Int = FlatHexCell.Side.NONE.value) :
            this(arrangment, dimension, dimension, defaultCellValue)

    init {
        if (arrangement == HexMaze.Arrangement.TRIANGLE || arrangement == HexMaze.Arrangement.HEXAGON) {
            // Hexagon and triangle mazes have only one dimension parameter.
            this.height = width
        } else {
            this.height = height
        }

        // Create maze grid
        var gridWith = width
        val rowsForColumn: (column: Int) -> Int
        val rowOffset: (column: Int) -> Int
        when (arrangement) {
            HexMaze.Arrangement.RECTANGLE -> {
                rowsForColumn = { height }
                rowOffset = { it / 2 }
            }
            HexMaze.Arrangement.HEXAGON -> {
                gridWith = 2 * width - 1
                rowsForColumn = { gridWith - Math.abs(it - width + 1) }
                rowOffset = { if (it < width) 0 else it - width + 1 }
            }
            HexMaze.Arrangement.TRIANGLE -> {
                rowsForColumn = { it + 1 }
                rowOffset = { 0 }
            }
            HexMaze.Arrangement.RHOMBUS -> {
                rowsForColumn = { height }
                rowOffset = { 0 }
            }
        }

        rowOffsets = IntArray(gridWith)
        grid = Array(gridWith) { x ->
            rowOffsets[x] = rowOffset(x)
            Array(rowsForColumn(x)) { y ->
                FlatHexCell(
                        this, PositionXY(x, y + rowOffsets[x]), defaultCellValue)
            }
        }
    }

    override fun cellAt(pos: Position): FlatHexCell = if (pos is PositionXY) {
        grid[pos.x][pos.y - rowOffsets[pos.x]]
    } else {
        throw IllegalArgumentException("Position has wrong type.")
    }

    override fun optionalCellAt(pos: Position): FlatHexCell? {
        if (pos is PositionXY) {
            if (pos.x < 0 || pos.x >= grid.size) return null
            val actualY = pos.y - rowOffsets[pos.x]
            if (actualY < 0 || actualY >= grid[pos.x].size) return null
            return grid[pos.x][actualY]
        }
        throw IllegalArgumentException("Position has wrong type")
    }

    override fun getRandomCell(): FlatHexCell {
        val random = ThreadLocalRandom.current()
        val x = random.nextInt(grid.size)
        return cellAt(PositionXY(x, random.nextInt(grid[x].size)))
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Cell> forEachCell(action: (T) -> Boolean) {
        for (x in 0 until grid.size) {
            for (y in 0 until grid[x].size) {
                val stop = action(cellAt(PositionXY(x, y)) as T)
                if (stop) return
            }
        }
    }

    override fun reset(empty: Boolean) {
        val value = if (empty) FlatRectCell.Side.NONE.value else FlatRectCell.Side.ALL.value
        for (x in 0 until grid.size) {
            for (y in 0 until grid[x].size) {
                grid[x][y].value = value
            }
        }
    }

    override fun format(): String {
        // (DONT) FIXME: NOT WORKING AT ALL

        val colFirstY = IntArray(grid.size)
        var minColFirstY = Int.MAX_VALUE
        for (x in 0 until grid.size) {
            colFirstY[x] = grid.size - x + 2 * rowOffsets[x]
            if (colFirstY[x] < minColFirstY) minColFirstY = colFirstY[x]
        }

        // Remove empty padding on top and find max height
        var maxTextRows = 0
        for (x in 0 until grid.size) {
            colFirstY[x] -= minColFirstY

            val textRows = colFirstY[x] + 2 * grid[x].size + 1
            if (textRows > maxTextRows) maxTextRows = textRows
        }

        val sb = StringBuilder((3 * grid.size + 2) * maxTextRows)
        for (y in 0 until maxTextRows) {
            for (x in 0 until grid.size) {
                // Find the the cell in that Y text row
                val cellY = Math.floor((y - colFirstY[x] - 1) / 2.0).toInt()
                val cell = optionalCellAt(PositionXY(x, cellY))

                if ((x + y) % 2 == 0) {
                    // A bottom "\__/" part is needed
                    if (cell != null && cell.hasSide(FlatHexCell.Side.SOUTHWEST)
                            || cell == null && optionalCellAt(PositionXY(x - 1, cellY))
                                    ?.hasSide(FlatHexCell.Side.NORTHEAST) == true) {
                        sb.append(SEQ_DIAGONAL_BACKWARD)
                    } else {
                        sb.append(SEQ_DIAGONAL_NONE)
                    }
                    if (cell != null && cell.hasSide(FlatHexCell.Side.SOUTH)
                            || cell == null && optionalCellAt(PositionXY(x, cellY + 1))
                                    ?.hasSide(FlatHexCell.Side.NORTH) == true) {
                        sb.append(SEQ_HORIZONTAL)
                    } else {
                        sb.append(SEQ_HORIZONTAL_NONE)
                    }
                    if (x == grid.size - 1 && cell != null
                            && cell.hasSide(FlatHexCell.Side.SOUTHEAST)) {
                        sb.append(SEQ_DIAGONAL_FORWARD)
                    }
                } else {
                    // A top "/  \" part is needed
                    if (cell != null && cell.hasSide(FlatHexCell.Side.NORTHWEST)
                            || cell == null && optionalCellAt(PositionXY(x - 1, cellY - 1))
                                    ?.hasSide(FlatHexCell.Side.SOUTHEAST) == true) {
                        sb.append(SEQ_DIAGONAL_FORWARD)
                    } else {
                        sb.append(SEQ_DIAGONAL_NONE)
                    }
                    sb.append(SEQ_HORIZONTAL_NONE)
                    if (x == grid.size - 1 && cell != null
                            && cell.hasSide(FlatHexCell.Side.NORTHEAST)) {
                        sb.append(SEQ_DIAGONAL_BACKWARD)
                    }
                }
            }
            sb.append('\n')
        }

        return sb.toString()
    }

    override fun toString(): String {
        return "[arrangement: $arrangement, ${if (arrangement == HexMaze.Arrangement.TRIANGLE
                || arrangement == HexMaze.Arrangement.HEXAGON)
            "dimension : $width" else "width: $width, height: $height"}"
    }

    companion object {
        private const val SEQ_HORIZONTAL = "__"
        private const val SEQ_HORIZONTAL_NONE = "  "
        private const val SEQ_DIAGONAL_NONE = " "
        private const val SEQ_DIAGONAL_FORWARD = "/"
        private const val SEQ_DIAGONAL_BACKWARD = "\\"
    }

}