package com.github.rushyverse.events.listener

import com.github.rushyverse.api.extension.event.cancel
import com.github.rushyverse.api.koin.inject
import com.github.rushyverse.api.player.ClientManager
import com.github.rushyverse.events.EventsPlugin
import com.github.rushyverse.events.client.EventPlayer
import com.github.rushyverse.events.game.GameManager
import net.kyori.adventure.text.Component
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.inventory.Inventory

class GameListener : Listener {

    private val games: GameManager by inject(EventsPlugin.ID)
    private val clients: ClientManager by inject(EventsPlugin.ID)

    @EventHandler
    suspend fun onChangeWorld(event: PlayerChangedWorldEvent) {
        val from = event.from

        if (from.name.contains("events")) {
            val game = games.getByWorld(from) ?: return
            val player = event.player

            game.clientLeave(clients.getClient(player) as EventPlayer)
        }
    }

    @EventHandler
    suspend fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.player
        val game = games.getByWorld(player.world) ?: return
        val client = clients.getClient(player) as EventPlayer
        val killer = player.killer

        client.stats.incDeaths()

        event.keepInventory = true

        val deathMessage = StringBuilder(player.name)
        deathMessage.append(" ")

        if (killer != null) {
            val clientKiller = clients.getClient(killer) as EventPlayer

            clientKiller.stats.incKills()
            clientKiller.reward(game.config.rewards.kill)

            deathMessage.append("was killed by ${killer.name}")
        }

        event.deathMessage(Component.text(deathMessage.toString()))
    }

    private fun getWoolIfContain(inv: Inventory): Material? {
        for (item in inv.contents) {
            val type = item?.type ?: continue

            if (type.name.endsWith("_WOOL")) {
                return item.type
            }
        }

        return null
    }

    @EventHandler
    suspend fun onEntityDamage(event: EntityDamageEvent) {
        val entity = event.entity
        val world = entity.world

        val game = games.getByWorld(world) ?: return

        if (entity.type != EntityType.PLAYER) {
            event.isCancelled = true
        } else {
            val client = clients.getClient(entity.name) as EventPlayer
        }
    }

    @EventHandler
    suspend fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        val world = player.world
        val game = games.getByWorld(world) ?: return
        val client = clients.getClient(player) as EventPlayer


    }
}