package com.maltaisn.maze.maze


class FlatMaze(width: Int, height: Int,
               defaultCellValue: Int = FlatCell.Side.NONE.value) : Maze<FlatCell>(width, height) {

    init {
        grid = Array(width) { x ->
            Array(height) { y -> FlatCell(this, x, y, defaultCellValue) }
        }
    }

    override fun format(): String {
        val w = width + 1
        val h = height + 1
        val sb = StringBuilder((w + 1) * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val nw = optionalCellAt(x - 1, y - 1)
                val ne = optionalCellAt(x, y - 1)
                val sw = optionalCellAt(x - 1, y)
                val se = optionalCellAt(x, y)

                var value = FlatCell.Side.NONE.value
                if (nw != null && nw.hasSide(FlatCell.Side.EAST)
                        || ne != null && ne.hasSide(FlatCell.Side.WEST)) {
                    value = value or FlatCell.Side.NORTH.value
                }
                if (ne != null && ne.hasSide(FlatCell.Side.SOUTH)
                        || se != null && se.hasSide(FlatCell.Side.NORTH)) {
                    value = value or FlatCell.Side.EAST.value
                }
                if (sw != null && sw.hasSide(FlatCell.Side.EAST)
                        || se != null && se.hasSide(FlatCell.Side.WEST)) {
                    value = value or FlatCell.Side.SOUTH.value
                }
                if (nw != null && nw.hasSide(FlatCell.Side.SOUTH)
                        || sw != null && sw.hasSide(FlatCell.Side.NORTH)) {
                    value = value or FlatCell.Side.WEST.value
                }
                sb.append(CHARS[value])
            }
            sb.append('\n')
        }
        sb.deleteCharAt(sb.length - 1)
        return sb.toString()
    }

    companion object {
        private const val CHARS = " ╵╶└╷│┌├╴┘─┴┐┤┬┼"
    }

}