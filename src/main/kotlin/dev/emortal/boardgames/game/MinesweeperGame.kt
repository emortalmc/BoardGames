package dev.emortal.boardgames.game

import dev.emortal.boardgames.game.MinesweeperLoseMessages.loseMessages
import dev.emortal.immortal.game.Game
import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameState
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.EventNode
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.event.player.PlayerBlockBreakEvent
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.event.player.PlayerBlockPlaceEvent
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.instance.Chunk
import net.minestom.server.instance.Instance
import net.minestom.server.instance.batch.AbsoluteBlockBatch
import net.minestom.server.instance.block.Block
import net.minestom.server.resourcepack.ResourcePack
import net.minestom.server.scoreboard.Team
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import net.minestom.server.timer.TaskSchedule
import net.minestom.server.utils.NamespaceID
import org.tinylog.kotlin.Logger
import world.cepi.kstom.Manager
import world.cepi.kstom.event.listenOnly
import java.net.URL
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class MinesweeperGame : Game() {

    override val maxPlayers: Int = 5
    override val minPlayers: Int = 1
    override val countdownSeconds: Int = 20
    override val canJoinDuringGame: Boolean = false
    override val showScoreboard: Boolean = true
    override val showsJoinLeaveMessages: Boolean = true
    override val allowsSpectators: Boolean = true

    companion object {
        const val Y_OFFSET = 64
        const val BOARD_SIZE = 25

        val playerTeamTag = Tag.Integer("team")

//        private const val resourcePackUrl = "https://github.com/EmortalMC/BoardGames/releases/download/1.0.0/minesweeper.zip"
//        private const val blankResourcePackUrl = "https://github.com/EmortalMC/BoardGames/releases/download/1.0.0/nothing.zip"
//
//        private val hexArray = "0123456789ABCDEF".toCharArray()
//        private fun bytesToHex(bytes: ByteArray): String {
//            val hexChars = CharArray(bytes.size * 2)
//            for (j in bytes.indices) {
//                val v = bytes[j].toInt() and 0xFF
//                hexChars[j * 2] = hexArray[v ushr 4]
//                hexChars[j * 2 + 1] = hexArray[v and 0x0F]
//            }
//            return String(hexChars)
//        }
//
//        val resourcePack = ResourcePack.forced(resourcePackUrl, refreshSha1(), Component.text("This resource pack is required for Minesweeper!", NamedTextColor.RED).append(Component.text("\n\npsst... it's actually very useful", NamedTextColor.DARK_GRAY)))
//        val blankResourcePack = ResourcePack.forced(blankResourcePackUrl, "blank", Component.text("Removing the Minesweeper texture pack"))
//
//        private fun refreshSha1(): String {
//            val digest = MessageDigest.getInstance("SHA-1")
//            val fileInputStream = URL(resourcePackUrl).openStream()
//            var n = 0
//            val buffer = ByteArray(8192)
//            while (n != -1) {
//                n = fileInputStream.read(buffer)
//                if (n > 0)
//                    digest.update(buffer, 0, n)
//            }
//            fileInputStream.close()
//            return bytesToHex(digest.digest())
//        }
    }


    lateinit var board: Array<IntArray>
    val flags = AtomicInteger(0)
    val bombs = mutableListOf<GridPos>()
    var bombsGenerated = AtomicBoolean(false)
    var firstClick = AtomicBoolean(true)

    private var playerThatLost: UUID? = null

    override fun gameCreated() {
        val eventNode = instance!!.eventNode()

        eventNode.listenOnly<PlayerBlockPlaceEvent> {
            isCancelled = true
            player.inventory.update()
            player.chunk?.sendChunk(player)
        }

        eventNode.listenOnly<InventoryPreClickEvent> {
            isCancelled = true
        }

        eventNode.listenOnly<PlayerBlockBreakEvent> {
            isCancelled = true

            if (player.hasTag(GameManager.playerSpectatingTag)) return@listenOnly
            if (gameState != GameState.PLAYING) return@listenOnly

            if (block.name().endsWith("carpet")) {
                return@listenOnly
            }
            if (instance.getBlock(blockPosition.add(0.0, 1.0, 0.0)).name().endsWith("carpet")) {
                return@listenOnly
            }

            val boardSizeX = board.size
            val boardSizeZ = board.first().size

            if (
                blockPosition.blockY() != Y_OFFSET ||
                blockPosition.blockX() >= boardSizeX || blockPosition.blockX() < 0 ||
                blockPosition.blockZ() >= boardSizeZ || blockPosition.blockZ() < 0
            ) {
                return@listenOnly
            }

            if (player.itemInMainHand.isAir) {
                val clickedX = blockPosition.blockX()
                val clickedZ = blockPosition.blockZ()

                val clickedSquare = board[clickedX][clickedZ]

                if (clickedSquare == GridConstants.BOMB) {
                    // lose :)
                    val startingBatch = AbsoluteBlockBatch()

                    bombs.forEach {
                        startingBatch.setBlock(it.x, Y_OFFSET, it.z, Block.TNT)
                    }
                    startingBatch.setBlock(blockPosition, Block.TNT)

                    startingBatch.apply(instance) {
                        instance.setBlock(blockPosition, Block.TNT)
                    }

                    playerThatLost = player.uuid
                    victory(emptyList())
                    sendMessage(
                        Component.text()
                            .append(Component.text(player.username, NamedTextColor.RED))
                            .append(Component.space())
                            .append(Component.text(loseMessages.random(), NamedTextColor.GRAY))
                            .build()
                    )

//                    spawnTNT(blockPosition.add(0.5, 0.0, 0.5), instance, 30)

                    return@listenOnly
                }

                val surroundingBombs = GridUtils.bombsSurrounding(board, clickedX, clickedZ)

                if (clickedSquare != surroundingBombs) {
                    board[clickedX][clickedZ] = surroundingBombs
                    GridConstants.spawnMapFromNumber(surroundingBombs, instance, blockPosition.withY(Y_OFFSET.toDouble()))

                    player.playSound(
                        Sound.sound(
                            SoundEvent.ENTITY_ITEM_PICKUP,
                            Sound.Source.MASTER,
                            0.5f,
                            0.7f
                        ),
                        Sound.Emitter.self()
                    )
                }

                var future = CompletableFuture.completedFuture(null)
                val changed = if (firstClick.get()) {
                    firstClick.set(false)

                    var iter = 0
                    var list: List<GridPos>
                    var batch: AbsoluteBlockBatch
                    while (true) {
                        batch = generateBoard()
                        generateBombs(board, boardSizeX, boardSizeZ, clickedX, clickedZ)
                        list = GridUtils.revealOpen(board, clickedX, clickedZ)

                        if (solve(board)) {
                            break
                        }

                        iter++
                    }

                    board[clickedX][clickedZ] = 0
                    GridConstants.spawnMapFromNumber(0, instance, blockPosition.withY(Y_OFFSET.toDouble()))

                    Logger.info("Took ${iter} iterations to get a solvable board")

                    future = CompletableFuture()
                    batch.apply(instance) {
                        future.complete(null)
                    }

                    list
                } else {
                    GridUtils.revealOpen(board, clickedX, clickedZ)
                }

                val unrevealed = board.sumOf { it.count { it == GridConstants.UNREVEALED } }
                if (unrevealed <= 0) {
                    victory(players)
                }

                if (changed.isNotEmpty()) {
                    future.thenRun {
                        var currentIter = 0
                        instance.scheduler().submitTask {
                            instance.playSound(
                                Sound.sound(
                                    SoundEvent.ENTITY_ITEM_PICKUP,
                                    Sound.Source.MASTER,
                                    0.5f,
                                    0.7f + (currentIter.toFloat() / 50f / 5f)
                                ), Sound.Emitter.self()
                            )

                            repeat(5) {
                                val it = changed[currentIter]

                                val x = it.x
                                val z = it.z
                                val gridSquare = board[x][z]

                                GridConstants.spawnMapFromNumber(gridSquare, instance, Pos(x.toDouble(), Y_OFFSET.toDouble(), z.toDouble()))
                                instance.setBlock(
                                    x, Y_OFFSET + 1, z,
                                    Block.AIR
                                )

                                currentIter++
                                if (currentIter >= changed.size) {
                                    return@submitTask TaskSchedule.stop()
                                }
                            }

                            TaskSchedule.nextTick()
                        }
                    }
                }
            }
        }
    }

    private fun generateBombs(board: Array<IntArray>, boardSizeX: Int, boardSizeZ: Int, clickedX: Int, clickedZ: Int) {
        bombs.clear()

        // Generate random bombs
        var bombsPlaced = 0

        val bombsToPlace = ((boardSizeX * boardSizeZ).toDouble() * 0.16).toInt()
        while (bombsPlaced < bombsToPlace) {
            val randomX = board.indices.random()
            val randomZ = board.first().indices.random()

            val gridSquare = board[randomX][randomZ]

            // Do not spawn near the mouse
            if (
                Direction8.values().any { clickedX == randomX + it.offX && clickedZ == randomZ + it.offZ }
                || (clickedX == randomX && clickedZ == randomZ)
            ) continue

            // Grid isn't already bomb, place bomb here
            if (gridSquare != GridConstants.BOMB) {
                board[randomX][randomZ] = GridConstants.BOMB
                bombs.add(GridPos(randomX, randomZ))
                //batch.setBlock(randomX, Y_OFFSET, randomZ, Block.TNT)
                bombsPlaced++
            }
        }

        bombsGenerated.set(true)
    }

    override fun gameStarted() {
        generateBoard().apply(instance!!, null)

        instance!!.scheduler().buildTask {
            refreshActionbar(instance!!, bombs.size, flags.get())
        }.repeat(TaskSchedule.tick(30)).schedule()

        players.forEachIndexed { i, it ->
            it.setTag(playerTeamTag, i)

            val teamColors = TeamColor.values()
            val teamColor = teamColors[i % teamColors.size]
            val team = dev.emortal.immortal.game.Team(teamColor.name, teamColor.textColor)

            it.displayName = Component.text(it.username, teamColor.textColor)
            it.team = team.scoreboardTeam
        }
    }

    override fun gameEnded() {

    }

    override fun playerJoin(player: Player) {

        MapUtil.playerInit(player)

        player.gameMode = GameMode.CREATIVE
    }

    override fun spectatorJoin(player: Player) {
        MapUtil.playerInit(player)
    }

    override fun playerLeave(player: Player) {
    }

    override fun registerEvents(eventNode: EventNode<InstanceEvent>) = with(eventNode) {

        listenOnly<PlayerBlockInteractEvent> {
            if (player.hasTag(GameManager.playerSpectatingTag)) return@listenOnly
            if (hand != Player.Hand.MAIN) return@listenOnly

            if (block.name().endsWith("carpet")) {
                instance.setBlock(blockPosition, Block.AIR)

                player.playSound(Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.MASTER, 0.6f, 1.8f))

                refreshActionbar(instance, bombs.size, flags.decrementAndGet())
                return@listenOnly
            }

            val boardSizeX = board.size
            val boardSizeZ = board.first().size

            if (
                blockPosition.blockY() != Y_OFFSET ||
                blockPosition.blockX() >= boardSizeX || blockPosition.blockX() < 0 ||
                blockPosition.blockZ() >= boardSizeZ || blockPosition.blockZ() < 0
            ) {
                isCancelled = true
                return@listenOnly
            }

            if (instance.getBlock(blockPosition.add(0.0, 1.0, 0.0)).name().endsWith("carpet")) {
                isCancelled = true
                return@listenOnly
            }

            val gridSquare = board[blockPosition.blockX()][blockPosition.blockZ()]

            if (gridSquare != GridConstants.UNREVEALED && gridSquare != GridConstants.BOMB) {
                isCancelled = true
                return@listenOnly
            }

            val teamColor = TeamColor.values()[player.getTag(playerTeamTag)]
            instance.setBlock(blockPosition.add(0.0, 1.0, 0.0), teamColor.block)
            player.playSound(Sound.sound(SoundEvent.ENTITY_ENDER_DRAGON_FLAP, Sound.Source.MASTER, 0.6f, 2f))

            refreshActionbar(instance, bombs.size, flags.incrementAndGet())

        }
    }

    override fun instanceCreate(): CompletableFuture<Instance> {
        val instanceFuture = CompletableFuture<Instance>()

        val dimension = Manager.dimensionType.getDimension(NamespaceID.from("fullbright"))!!
        val newInstance = Manager.instance.createInstanceContainer(dimension)

        newInstance.setGenerator {
            it.modifier().fillHeight(64, 65, Block.GRASS_BLOCK)
            it.modifier().fillHeight(60, 64, Block.DIRT)
        }
        newInstance.enableAutoChunkLoad(false)

        val radius = 3
        val chunkFutures = mutableListOf<CompletableFuture<Chunk>>()
        for (x in -radius..radius) {
            for (z in -radius..radius) {
                chunkFutures.add(newInstance.loadChunk(x, z))
            }
        }

        CompletableFuture.allOf(*chunkFutures.toTypedArray()).thenRun {
            instanceFuture.complete(newInstance)
        }

        return instanceFuture
    }

    override fun gameWon(winningPlayers: Collection<Player>) {
//        if (winningPlayers.isEmpty()) {
            // Game was lost
//            val losingPlayer = players.firstOrNull { it.uuid == playerThatLost } ?: return
//
//            val angleInc = (2 * PI) / (players.size - 1)
//            var i = 0
//            players.forEachIndexed { _, plr ->
//                if (plr.uuid == playerThatLost) return@forEachIndexed
//
//                val spawnAngle = angleInc * i
//                val x = cos(spawnAngle) * 3.5
//                val z = sin(spawnAngle) * 3.5
//                val spawnPosition = losingPlayer.position.add(x, 0.0, z).withLookAt(losingPlayer.eyePosition())
//
//                val entity = Entity(EntityType.AREA_EFFECT_CLOUD)
//                val meta = entity.entityMeta as AreaEffectCloudMeta
//                entity.setNoGravity(true)
//                meta.radius = 0f
//                entity.setInstance(instance!!, spawnPosition).thenRun {
//                    plr.teleport(spawnPosition)
//                    entity.addPassenger(plr)
//                }
//                i++
//            }
//        }
    }

    private fun generateBoard(): AbsoluteBlockBatch {
        flags.set(0)
        bombs.clear()

        // Create board
        board = Array(BOARD_SIZE) { IntArray(BOARD_SIZE) { GridConstants.UNREVEALED } }
        bombsGenerated.set(false)

        val batch = AbsoluteBlockBatch()

        for (x in board.indices) {
            for (z in board.first().indices) {
                if ((x + z) % 2 == 0) {
                    batch.setBlock(x, Y_OFFSET, z, GridConstants.UNREVEALED_BLOCK)
                } else {
                    batch.setBlock(x, Y_OFFSET, z, GridConstants.UNREVEALED_BLOCK_ALT)
                }
                batch.setBlock(x, Y_OFFSET + 1, z, Block.AIR)
                batch.setBlock(x, Y_OFFSET + 2, z, Block.AIR)

            }
        }

        return batch
//        batch.apply(instance!!, null)
    }

    private fun refreshActionbar(instance: Instance, bombs: Int, flags: Int) {
        instance.sendActionBar(
            Component.text()
                .append(Component.text("✷ ", NamedTextColor.RED))
                .append(Component.text(bombs, NamedTextColor.RED))
                .append(Component.text(" TNT", NamedTextColor.RED))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text("⚑ ", NamedTextColor.GREEN))
                .append(Component.text(flags, NamedTextColor.GREEN))
                .append(Component.text(" FLAGS", NamedTextColor.GREEN))
                .build()
        )
    }

    /**
     * Finds out whether game is possible without guessing
     */
    private fun solve(board: Array<IntArray>): Boolean {
        val nextToSearch = mutableSetOf<GridPos>()
        val alreadyDone = mutableSetOf<GridPos>()
        val flagPoses = mutableSetOf<GridPos>()
        var solveIter = 0

        val copiedBoard = Array(BOARD_SIZE) { IntArray(BOARD_SIZE) { GridConstants.UNREVEALED } }
        board.forEachIndexed { x, it ->
            it.forEachIndexed { y, a ->
                copiedBoard[x][y] = a
            }
        }

        fun stuff(): Boolean {
            nextToSearch.forEach {
                val bombsSurrounding = GridUtils.bombsSurrounding(copiedBoard, it.x, it.z)
                copiedBoard[it.x][it.z] = bombsSurrounding
                GridUtils.revealOpen(copiedBoard, it.x, it.z)
            }

            nextToSearch.clear()

            // Start by getting corners
            val numbered = mutableSetOf<Pair<GridPos, Int>>()
            copiedBoard.forEachIndexed { x, it ->
                it.forEachIndexed { y, it ->
                    val gridPos = GridPos(x, y)
                    if (alreadyDone.contains(gridPos)) return@forEachIndexed
                    if (it > 0) numbered.add(gridPos to it)
                }
            }


            numbered.forEach { square ->
                // The amount of undiscovered tiles surrounding this tile equals the amount of bombs
                // this means all surrounding tiles are bombs
                val neighbours = GridUtils.neighbours(copiedBoard, square.first.x, square.first.z)
                val unrevealedNeighbours =
                    neighbours.filter { it.second == GridConstants.UNREVEALED || it.second == GridConstants.BOMB }

                if (unrevealedNeighbours.size == square.second) {
                    unrevealedNeighbours.forEach {
                        flagPoses.add(it.first)
                    }
                }
            }

            numbered.forEach { square ->
                val neighbours = GridUtils.neighbours(copiedBoard, square.first.x, square.first.z)
                val unrevealedNeighbours =
                    neighbours.filter { it.second == GridConstants.UNREVEALED || it.second == GridConstants.BOMB }
                val flaggedNeighbours =
                    neighbours.filter { flagPoses.contains(it.first) }

                if (flaggedNeighbours.size == square.second) {
                    alreadyDone.add(square.first)
                    unrevealedNeighbours.forEach {
                        if (flaggedNeighbours.contains(it)) return@forEach

                        nextToSearch.add(it.first)
                    }
                }
            }

            val unrevealed = copiedBoard.sumOf { it.count { it == GridConstants.UNREVEALED } }
            if (unrevealed <= 0) {
                nextToSearch.clear()
                // SOLVABLE
                return true
            }

            if (nextToSearch.isEmpty()) {
                // UNSOLVABLE
                Logger.info("failed iter ${solveIter}")
                return false
            }

            solveIter++
            return stuff()
        }

        return stuff()
    }

}