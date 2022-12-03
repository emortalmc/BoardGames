package dev.emortal.boardgames.game

object GridUtils {

    fun neighbours(board: Array<IntArray>, x: Int, z: Int): List<Pair<GridPos, Int>> {
        val boardSizeX = board.size
        val boardSizeZ = board.first().size

        val list = mutableListOf<Pair<GridPos, Int>>()

        Direction8.values().forEach {
            val newPositionX = x + it.offX
            val newPositionZ = z + it.offZ

            // Ignore directions that go off the grid
            if (
                newPositionX < 0 || newPositionZ < 0
                || newPositionX >= boardSizeX || newPositionZ >= boardSizeZ
            ) return@forEach

            val gridSquare = board[newPositionX][newPositionZ]
            list.add(GridPos(newPositionX, newPositionZ) to gridSquare)
        }

        return list
    }

    fun bombsSurrounding(board: Array<IntArray>, x: Int, z: Int): Int {
        val boardSizeX = board.size
        val boardSizeZ = board.first().size

        var bombsNearby = 0

        Direction8.values().forEach {
            val newPositionX = x + it.offX
            val newPositionZ = z + it.offZ

            // Ignore directions that go off the grid
            if (
                newPositionX < 0 || newPositionZ < 0
                || newPositionX >= boardSizeX || newPositionZ >= boardSizeZ
            ) return@forEach

            val gridSquare = board[newPositionX][newPositionZ]
            if (gridSquare == GridConstants.BOMB) {
                bombsNearby++
            }
        }

        return bombsNearby
    }

    fun revealOpen(board: Array<IntArray>, x: Int, z: Int): List<GridPos> {
        val boardSizeX = board.size
        val boardSizeZ = board.first().size

        val changed = mutableListOf<GridPos>()

        Direction8.values().forEach {
            val newPositionX = x + it.offX
            val newPositionZ = z + it.offZ

            // Ignore directions that go off the grid
            if (
                newPositionX < 0 || newPositionZ < 0
                || newPositionX >= boardSizeX || newPositionZ >= boardSizeZ
            ) return@forEach

            val gridPos = GridPos(newPositionX, newPositionZ)

            val gridSquare = board[newPositionX][newPositionZ]
            if (gridSquare != GridConstants.UNREVEALED) {
                return@forEach
            }

            if (gridSquare <= 0) {
                if (board[x][z] > 0 && board[newPositionX][newPositionZ] == GridConstants.UNREVEALED) return@forEach
                board[newPositionX][newPositionZ] = GridConstants.NOTHING
                changed.add(gridPos)
            }

            val bombsNearby = bombsSurrounding(board, newPositionX, newPositionZ)
            if (bombsNearby > 0) {
                if (board[x][z] > 0 && board[newPositionX][newPositionZ] == GridConstants.UNREVEALED) return@forEach
                board[newPositionX][newPositionZ] = bombsNearby
                changed.add(gridPos)
                return@forEach
            }

            // do not search from corners
            changed.addAll(revealOpen(board, newPositionX, newPositionZ))
        }

        return changed
    }

}