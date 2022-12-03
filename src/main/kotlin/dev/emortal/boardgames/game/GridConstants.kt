package dev.emortal.boardgames.game

import net.minestom.server.instance.block.Block

object GridConstants {

    val UNREVEALED_BLOCK = Block.LIME_CONCRETE
    val UNREVEALED_BLOCK_ALT = Block.GREEN_CONCRETE
    val REVEALED_BLOCK = Block.BLACK_WOOL

    val NOTHING = 0
    val UNREVEALED = -2
    val BOMB = -9

    fun getBlockFromNumber(num: Int): Block {
        if (num > 8) throw IndexOutOfBoundsException("Silly billy")

        return when (num) {
            0 -> REVEALED_BLOCK
            1 -> Block.WHITE_WOOL
            2 -> Block.ORANGE_WOOL
            3 -> Block.MAGENTA_WOOL
            4 -> Block.LIGHT_BLUE_WOOL
            5 -> Block.YELLOW_WOOL
            6 -> Block.LIME_WOOL
            7 -> Block.PINK_WOOL
            8 -> Block.GRAY_WOOL
            else -> null
        }!!
    }

}