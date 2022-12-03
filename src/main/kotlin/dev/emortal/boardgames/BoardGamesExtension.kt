package dev.emortal.boardgames

import dev.emortal.boardgames.game.MinesweeperGame
import dev.emortal.immortal.game.GameManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.GameMode
import net.minestom.server.event.EventDispatcher
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.event.player.PlayerBlockPlaceEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.event.player.PlayerUseItemOnBlockEvent
import net.minestom.server.extensions.Extension
import net.minestom.server.listener.BlockPlacementListener
import net.minestom.server.network.packet.client.play.ClientCreativeInventoryActionPacket
import net.minestom.server.network.packet.client.play.ClientPickItemPacket
import net.minestom.server.network.packet.client.play.ClientPlayerBlockPlacementPacket
import net.minestom.server.utils.chunk.ChunkUtils

class BoardGamesExtension : Extension() {
    override fun initialize() {
        // Disable creative actions
        MinecraftServer.getPacketListenerManager().setListener(ClientCreativeInventoryActionPacket::class.java) { packet, player ->
            if (player.gameMode != GameMode.CREATIVE) return@setListener
            player.inventory.clear()
        }
        MinecraftServer.getPacketListenerManager().setListener(ClientPickItemPacket::class.java) { packet, player ->
            if (player.gameMode != GameMode.CREATIVE) return@setListener
            player.inventory.clear()
        }
//
        MinecraftServer.getPacketListenerManager().setListener(ClientPlayerBlockPlacementPacket::class.java) { packet, player ->
            val hand = packet.hand()
            val blockFace = packet.blockFace()
            val blockPosition = packet.blockPosition()

            val instance = player.instance ?: return@setListener

            // Prevent outdated/modified client data
            val interactedChunk = instance.getChunkAt(blockPosition)
            if (!ChunkUtils.isLoaded(interactedChunk)) {
                // Client tried to place a block in an unloaded chunk, ignore the request
                return@setListener
            }

            player.inventory.update()
            interactedChunk?.sendChunk(player)

            val usedItem = player.getItemInHand(hand)
            val interactedBlock = instance.getBlock(blockPosition)

            // Interact at block
            // FIXME: onUseOnBlock
            val playerBlockInteractEvent =
                PlayerBlockInteractEvent(player, hand, interactedBlock, blockPosition, blockFace)
            EventDispatcher.call(playerBlockInteractEvent)
            val useMaterial = usedItem.material()
            if (!useMaterial.isBlock) {
                // Player didn't try to place a block but interacted with one
                val event = PlayerUseItemOnBlockEvent(player, hand, usedItem, blockPosition, blockFace)
                EventDispatcher.call(event)
                return@setListener
            }

        }


        eventNode.addListener(PlayerSpawnEvent::class.java) {
            if (it.isFirstSpawn) {
                it.player.setResourcePack(MinesweeperGame.resourcePack)
            }

        }


        GameManager.registerGame<MinesweeperGame>(
            "minesweeper",
            Component.text("Minesweeper", NamedTextColor.RED),
            showsInSlashPlay = true
        )

        logger.info("[${origin.name}] Initialized!")
    }

    override fun terminate() {
        logger.info("[${origin.name}] Terminated!")
    }
}