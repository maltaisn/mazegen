package com.maltaisn.maze.maze


class FlatCell(maze: Maze<FlatCell>, x: Int, y: Int) : Cell(maze, x, y) {

    var value: Int = Side.NONE.value
        set(value) {
            field = value and Side.ALL.value
        }

    constructor(maze: Maze<FlatCell>, x: Int, y: Int, value: Int) : this(maze, x, y) {
        this.value = value
    }

    fun getCellOnSide(side: Side): FlatCell? = when (side) {
        Side.NORTH -> if (y == 0) null else maze.grid[x][y - 1] as FlatCell
        Side.EAST -> if (x == maze.width - 1) null else maze.grid[x + 1][y] as FlatCell
        Side.SOUTH -> if (y == maze.height - 1) null else maze.grid[x][y + 1] as FlatCell
        Side.WEST -> if (x == 0) null else maze.grid[x - 1][y] as FlatCell
        else -> null
    }

    fun getNeighbors(): List<FlatCell> = listOfNotNull(
            getCellOnSide(Side.NORTH),
            getCellOnSide(Side.EAST),
            getCellOnSide(Side.SOUTH),
            getCellOnSide(Side.WEST)
    )

    fun hasSide(side: Side): Boolean {
        if (side == Side.NONE) return value == Side.NONE.value
        return (value and side.value) == side.value
    }

    fun openSide(side: Side) {
        changeSide(side) { v, s -> v and s.inv() }
    }

    fun closeSide(side: Side) {
        changeSide(side, Int::or)
    }

    fun toggleSide(side: Side) {
        changeSide(side, Int::xor)
    }

    private fun changeSide(side: Side, operation: (v: Int, s: Int) -> Int) {
        if (side == Side.NONE) {
            return
        } else if (side == Side.ALL) {
            for (s in listOf(Side.NORTH, Side.EAST, Side.SOUTH, Side.WEST)) {
                val cell = getCellOnSide(s)
                if (cell != null) {
                    cell.value = operation.invoke(cell.value, s.opposite().value)
                }
            }
        } else {
            val cell = getCellOnSide(side)
            if (cell != null) {
                cell.value = operation.invoke(cell.value, side.opposite().value)
            }
        }
        value = operation.invoke(value, side.value)
    }

    fun connectWith(cell: FlatCell) {
        if (cell.maze !== maze) return

        var side: Side? = null
        if (cell.x == x && cell.y == y - 1) {
            side = Side.NORTH
        } else if (cell.x == x + 1 && cell.y == y) {
            side = Side.EAST
        } else if (cell.x == x && cell.y == y + 1) {
            side = Side.SOUTH
        } else if (cell.x == x - 1 && cell.y == y) {
            side = Side.WEST
        }
        if (side != null) {
            cell.value = cell.value and side.opposite().value.inv()
            value = value and side.value.inv()
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("[x: $x, y: $y, sides: ")
        when (value) {
            Side.NONE.value -> sb.append("NONE")
            Side.ALL.value -> sb.append("ALL")
            else -> {
                if (hasSide(Side.NORTH)) sb.append("N,")
                if (hasSide(Side.EAST)) sb.append("E,")
                if (hasSide(Side.SOUTH)) sb.append("S,")
                if (hasSide(Side.WEST)) sb.append("W,")
                sb.deleteCharAt(sb.length - 1)
            }
        }
        sb.append("]")
        return sb.toString()
    }

    enum class Side(val value: Int) {
        NONE(0),
        NORTH(1),
        EAST(2),
        SOUTH(4),
        WEST(8),
        ALL(NORTH.value or EAST.value or SOUTH.value or WEST.value);

        fun opposite(): Side = when (this) {
            NONE -> NONE
            NORTH -> SOUTH
            SOUTH -> NORTH
            EAST -> WEST
            WEST -> EAST
            ALL -> ALL
        }
    }
}