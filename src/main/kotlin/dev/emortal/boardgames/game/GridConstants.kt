package dev.emortal.boardgames.game

import dev.emortal.immortal.util.asPos
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.metadata.other.GlowItemFrameMeta
import net.minestom.server.entity.metadata.other.ItemFrameMeta
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.metadata.MapMeta

object GridConstants {

    val UNREVEALED_BLOCK = Block.LIME_CONCRETE
    val UNREVEALED_BLOCK_ALT = Block.LIME_CONCRETE_POWDER
    val REVEALED_BLOCK = Block.STONE

    val NOTHING = 0
    val UNREVEALED = -2
    val BOMB = -9

    fun spawnMapFromNumber(num: Int, instance: Instance, point: Point): Entity? {
        instance.setBlock(point, REVEALED_BLOCK)

        if (num == 0) return null

        val entity = Entity(EntityType.GLOW_ITEM_FRAME)
        val meta = entity.entityMeta as GlowItemFrameMeta
        entity.setNoGravity(true)

        meta.orientation = ItemFrameMeta.Orientation.UP

        meta.item = ItemStack.builder(Material.FILLED_MAP)
            .meta(MapMeta::class.java) {
                it.mapId(num)
            }
            .build()

        entity.setInstance(instance, point.asPos().add(0.0, 1.0, 0.0).withView(0f, -90f))

        return entity
    }

//    fun getBlockFromNumber(num: Int): Block {
//        if (num > 8) throw IndexOutOfBoundsException("Cannot have more bombs than 8. Got $num")
//
//        return when (num) {
//            0 -> REVEALED_BLOCK
//            1 -> Block.WHITE_WOOL
//            2 -> Block.ORANGE_WOOL
//            3 -> Block.MAGENTA_WOOL
//            4 -> Block.LIGHT_BLUE_WOOL
//            5 -> Block.YELLOW_WOOL
//            6 -> Block.LIME_WOOL
//            7 -> Block.PINK_WOOL
//            8 -> Block.GRAY_WOOL
//            else -> throw IndexOutOfBoundsException("Cannot have more bombs than 8. Got $num")
//        }
//    }

}