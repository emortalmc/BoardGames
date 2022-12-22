package dev.emortal.boardgames.game

import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.instance.block.Block


enum class TeamColor(val block: Block, val textColor: NamedTextColor) {
    RED(Block.RED_CARPET, NamedTextColor.RED),
    ORANGE(Block.ORANGE_CARPET, NamedTextColor.GOLD),
//    GREEN(Block.LIME_CARPET, NamedTextColor.GREEN),
    BLUE(Block.BLUE_CARPET, NamedTextColor.BLUE),
    PINK(Block.PINK_CARPET, NamedTextColor.LIGHT_PURPLE),
    PURPLE(Block.PURPLE_CARPET, NamedTextColor.DARK_PURPLE),
}