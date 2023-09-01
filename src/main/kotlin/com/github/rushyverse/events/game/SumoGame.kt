package com.github.rushyverse.events.game

import com.github.rushyverse.api.extension.BukkitRunnable
import com.github.rushyverse.api.extension.asComponent
import com.github.rushyverse.api.extension.format
import com.github.rushyverse.api.game.GameData
import com.github.rushyverse.api.game.GameState
import com.github.rushyverse.api.koin.inject
import com.github.rushyverse.api.player.ClientManager
import com.github.rushyverse.api.schedule.SchedulerTask
import com.github.rushyverse.api.time.FormatTime
import com.github.rushyverse.events.EventsPlugin
import com.github.rushyverse.events.client.EventPlayer
import com.github.rushyverse.events.config.EventsPluginConfig
import com.github.rushyverse.events.config.MapConfig
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.scope
import org.bukkit.*
import org.bukkit.entity.Firework
import org.bukkit.entity.Player
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random.Default.nextBoolean
import kotlin.random.Random.Default.nextInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class SumoGame(
    val plugin: EventsPlugin,
    val world: World,
    val config: EventsPluginConfig,
    val mapConfig: MapConfig
) {
    private val clients: ClientManager by inject(plugin.id)
    private val manager: GameManager by inject(plugin.id)

    val data = GameData("event", 0)
    val players: Collection<Player> get() = world.players
    val gameTask = SchedulerTask(plugin.scope, 1.seconds)
    val createdTime = System.currentTimeMillis()
    var startedTime: Long = 0 // value set when the game starts
    var endTime: Long = 0

    suspend fun start(force: Boolean = false) {
        if (force) {
            data.state = GameState.STARTED

            broadcast("game.message.started")

            // TODO: teleport 2 random players and tp others around the ring

            startedTime = System.currentTimeMillis()

            gameTask.add("scoreboardUpdate") { gameTimeTask() }
            gameTask.run()
        } else {
            val time = AtomicInteger(5)

            data.state = GameState.STARTING

            gameTask.add { startingTask(this, time) }
            gameTask.run()
        }

        manager.sharedGameData.saveUpdate(data)
    }

    fun state() = data.state

    private suspend fun startingTask(task: SchedulerTask.Task, atomicTime: AtomicInteger) {
        val time = atomicTime.get()
        if (time == 0) {
            task.remove() // end the repeating task
            start(true)
            return
        }
        broadcast("game.message.starting", arrayOf(time))
        atomicTime.set(time - 1)
    }

    /*
     * Represents the task during the game.
     * Update every seconds the scoreboard with the current
     * formatted time since the game was started.
     */
    private suspend fun gameTimeTask() {
        val time = (System.currentTimeMillis() - startedTime)
            .milliseconds

        val clientsByLang = clients.clients.values
            .filter { it.player?.world == world }
            .groupBy { it.lang() }

        clientsByLang.keys.forEach { lang ->
            val format = time.format(FormatTime.short(plugin.translator, lang.locale))
            clientsByLang[lang]?.forEach {
                GameScoreboard.update(it as EventPlayer, this, format)
            }
        }
    }

    suspend fun clientJoin(client: EventPlayer) {
        val player = client.requirePlayer()

        player.inventory.apply {
            clear()
            config.items.forEach {
                setItem(it.slot, it.itemStack(plugin.translator, client.lang().locale))
            }
        }
        player.teleport(world.spawnLocation)

        broadcast("player.join.event", arrayOf(player.name))

        if (startedTime == 0L)
            GameScoreboard.update(client, this)

        manager.joinQueue.remove(player.uniqueId)
    }

    suspend fun clientLeave(client: EventPlayer) {
        val playersSize = players.size

        data.players = playersSize

        // Leave while game is starting and the current number of players is not reached
        if (playersSize < mapConfig.minPlayers) {

            when (data.state) {
                GameState.STARTING -> {
                    gameTask.tasks[0].remove()
                    broadcast("game.message.client.leave.starting")
                    data.state = GameState.WAITING
                }

                GameState.STARTED -> {
                    end(null)
                }

                else -> {}
            }

        }

        manager.sharedGameData.saveUpdate(data)
    }


    fun giveWinRewards(client: EventPlayer) {

    }

    /**
     * Ends this game.
     * The game state is set to ENDING while players are teleported and the world is destroyed.
     */
    suspend fun end(winner: EventPlayer?) {
        data.state = GameState.ENDING
        gameTask.cancelAndJoin()
        manager.sharedGameData.saveUpdate(data)

        if (winner == null) {
            broadcast("game.end.other")
            ejectPlayersAndDestroy()
        } else {
            broadcast("game.end.winner", arrayOf(winner.requirePlayer().name))
            giveWinRewards(winner)

            val taskFireworks = BukkitRunnable {
                winner.player?.location?.spawnRandomFirework()
            }.runTaskTimer(plugin, 0, 15L)

            // Ending the previous task, eject the players and destroy the game after 10s
            BukkitRunnable {
                taskFireworks.cancel()
                ejectPlayersAndDestroy()
            }.runTaskLater(plugin, 200L)
        }
    }

    private fun ejectPlayersAndDestroy() {
        for (player in players) {
            player.performCommand(config.backToHubCommand)
        }

        BukkitRunnable {
            plugin.launch {
                manager.removeGameAndDeleteWorld(this@SumoGame)
            }
        }.runTaskLater(plugin, 40L)
    }

    suspend fun broadcast(key: String, args: Array<Any> = emptyArray()) = plugin.broadcast(world.players, key,
        argumentBuilder = { args })
}

private fun Location.spawnRandomFirework() {
    val effect = FireworkEffect.builder()
        .with(FireworkEffect.Type.entries.toTypedArray().random())
        .withColor(Color.fromRGB(nextInt(256), nextInt(256), nextInt(256)))
        .withFade(Color.fromRGB(nextInt(256), nextInt(256), nextInt(256)))
        .flicker(nextBoolean())
        .trail(nextBoolean())
        .build()

    val firework = this.world.spawn(this, Firework::class.java)
    val fireworkMeta = firework.fireworkMeta.apply {
        addEffect(effect)
        power = nextInt(1, 3)
    }
    firework.fireworkMeta = fireworkMeta
}