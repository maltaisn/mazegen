package com.maltaisn.maze.generator

import com.maltaisn.maze.maze.FlatCell
import com.maltaisn.maze.maze.FlatMaze


class HuntKillGenerator : Generator<FlatMaze>() {

    override fun generate(width: Int, height: Int): FlatMaze {
        val maze = FlatMaze(width, height, FlatCell.Side.ALL.value)

        // Get cell on a random starting location
        var currentCell = maze.cellAt(
                random.nextInt(0, width),
                random.nextInt(0, height))

        while (true) {
            kill(currentCell)
            val cell = hunt(maze)
            if (cell != null) {
                currentCell = cell
            } else {
                break
            }
        }

        return maze
    }

    /**
     * Randomly connect cells starting from cell [cell]
     * until there are no neighboring unvisited cells.
     */
    private fun kill(cell: FlatCell) {
        var currentCell = cell
        while (true) {
            // Find an unvisited neighbor cell
            var unvisitedNeighbor: FlatCell? = null
            val neighbors = currentCell.getNeighbors().toMutableList()
            while (neighbors.size > 0) {
                val index = random.nextInt(neighbors.size)
                val neighbor = neighbors[index]
                if (neighbor.hasSide(FlatCell.Side.ALL)) {
                    // Found one
                    unvisitedNeighbor = neighbor
                    break
                } else {
                    neighbors.removeAt(index)
                }
            }
            if (unvisitedNeighbor != null) {
                // Connect with current cell
                currentCell.connectWith(unvisitedNeighbor)
                currentCell = unvisitedNeighbor
            } else {
                break
            }
        }
    }

    /**
     * Returns the first unvisited cell with a visited neighbor in maze [maze].
     * Returns null if there are no unvisited cells.
     * The cell and its neighbor are connected together.
     */
    private fun hunt(maze: FlatMaze): FlatCell? {
        // Find an unvisited cell next to a visited cell
        var unvisitedCell: FlatCell? = null
        var unvisitedCellNeighbor: FlatCell? = null

        outer@
        for (x in 0 until maze.width) {
            for (y in 0 until maze.height) {
                val cell = maze.cellAt(x, y)
                if (cell.hasSide(FlatCell.Side.ALL)) {
                    val neighbors = cell.getNeighbors().toMutableList()
                    for (neighbor in neighbors) {
                        if (!neighbor.hasSide(FlatCell.Side.ALL)) {
                            // Neighbor was visited
                            unvisitedCell = cell
                            unvisitedCellNeighbor = neighbor
                            break@outer
                        }
                    }
                }
            }
        }
        unvisitedCell?.connectWith(unvisitedCellNeighbor!!)
        return unvisitedCell
    }

}