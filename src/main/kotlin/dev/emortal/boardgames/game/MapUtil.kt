package dev.emortal.boardgames.game

import net.minestom.server.entity.Player
import net.minestom.server.map.framebuffers.Graphics2DFramebuffer
import net.minestom.server.network.packet.server.play.MapDataPacket
import org.tinylog.kotlin.Logger
import java.awt.Graphics2D
import java.util.concurrent.CopyOnWriteArrayList
import javax.imageio.ImageIO

object MapUtil {

    private val packets = CopyOnWriteArrayList<MapDataPacket>()

    fun createMaps() {
        repeat(8) {
            packets.add(packetFromPath("icons/block/${it + 1}.png", it + 1))
        }
        repeat(6) {
            packets.add(packetFromPath("icons/flag/flag${it + 1}.png", it + 1 + 8))
        }
    }

    fun playerInit(player: Player) {
        packets.forEach {
            player.sendPacket(it)
        }
    }

    private fun packetFromPath(path: String, mapId: Int): MapDataPacket {
        val stream = javaClass.classLoader.getResource(path)

        if (stream == null) Logger.error("stream was null")

        val framebuffer = Graphics2DFramebuffer()
        val renderer: Graphics2D = framebuffer.renderer
        val image = ImageIO.read(stream)
        renderer.drawImage(image, null, 0, 0)

        return framebuffer.preparePacket(mapId)
    }

}