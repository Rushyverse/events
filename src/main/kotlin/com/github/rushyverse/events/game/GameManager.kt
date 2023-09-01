package com.github.rushyverse.events.game

import com.github.rushyverse.api.game.SharedGameData
import com.github.rushyverse.api.koin.inject
import com.github.rushyverse.api.player.getTypedClient
import com.github.rushyverse.events.EventsPlugin
import com.github.rushyverse.events.client.EventPlayer
import com.github.rushyverse.events.config.MapConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.WorldType
import org.bukkit.entity.Player
import java.io.File
import java.util.*
import kotlin.coroutines.CoroutineContext

class GameManager(
    private val plugin: EventsPlugin
) {

    var currentEvent: SumoGame? = null

    val sharedGameData: SharedGameData by inject<SharedGameData>()
    val games: MutableList<SumoGame> = mutableListOf()
    val joinQueue: MutableMap<UUID, SumoGame> = mutableMapOf()

    fun getByWorld(world: World): SumoGame? {
        games.forEach {
            if (it.world == world) return it
        }

        return null
    }

    suspend fun createAndSave(config: MapConfig): SumoGame {
        val worldName = "event1"
        var world = plugin.server.getWorld(worldName)
        if (world == null) {
            world = createWorldFromTemplate(worldName, config)
            currentEvent = SumoGame(plugin, world, plugin.config, plugin.configMaps.maps.first()).also {
                games.add(it)

                sharedGameData.saveUpdate(it.data)
            }
            return currentEvent!!
        } else {
            throw IllegalStateException("A game already exists for this world $worldName")
        }
    }

    /**
     * Should be executed in [Dispatcher.IO] coroutine context.
     */
    private suspend fun createWorldFromTemplate(
        worldName: String,
        config: MapConfig,
        coroutineContext: CoroutineContext = Dispatchers.IO
    ): World {
        val templateWorld = File(plugin.mapsDir, config.worldName)
        val target = File(plugin.tempDir, worldName)
        val correctPath = target.path.replace("\\", "/")
        val creator = WorldCreator(correctPath)
        withContext(coroutineContext) {
            templateWorld.copyRecursively(target, true)
            creator.apply {
                type(WorldType.FLAT)
                environment(World.Environment.NORMAL)
                generateStructures(false)
            }
        }

        return plugin.server.createWorld(creator) ?: throw IllegalStateException("Can't create world for $worldName")
    }

    /**
     * Unloads and deletes the world of the given game.
     * After the file deletion, game is removed from the list
     * and from [SharedGameData].
     * This method also calls [SharedGameData.callOnChange] to update related games services.
     *
     * May not work properly if players are still present in the world.
     */
    suspend fun removeGameAndDeleteWorld(
        game: SumoGame,
        coroutineContext: CoroutineContext = Dispatchers.IO
    ) {
        val world = game.world
        val file = world.worldFolder

        Bukkit.unloadWorld(world, false)

        withContext(coroutineContext) {
            file.deleteRecursively()
        }

        sharedGameData.apply {
            games.clear()
            callOnChange()
        }

        games.remove(game)
    }


    fun getGame(gameIndex: Int = 0) = games.firstOrNull()

    fun isInJoinQueue(player: Player): Boolean {
        return joinQueue.contains(player.uniqueId)
    }
}