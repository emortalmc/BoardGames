package dev.emortal.boardgames.commands

import dev.emortal.boardgames.game.MinesweeperGame
import dev.emortal.immortal.game.GameManager.game
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.number.ArgumentInteger
import net.minestom.server.entity.Player

object BombsCommand : Command("bombs") {

    init {
        val amount = ArgumentInteger("amount")
        addSyntax({ sender, ctx ->
            val player = sender as? Player ?: return@addSyntax
            val game = player.game as? MinesweeperGame ?: return@addSyntax

            val bombs = ctx.get(amount)
            if (bombs > (game.board.size * game.board.size) - 9) {
                player.sendMessage("That is too many bombs. The max amount of bombs is ${(game.board.size * game.board.size) - 9}")
                return@addSyntax
            }

            if (game.bombsGenerated.get()) {
                player.sendMessage("First click already happened! Do it next game!!!")
                return@addSyntax
            }

            player.sendMessage("Set amount of bombs to $bombs")

            game.bombsToGenerate = bombs
        }, amount)
    }

}