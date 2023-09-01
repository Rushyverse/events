package com.github.rushyverse.events.listener

import com.github.rushyverse.api.koin.inject
import com.github.rushyverse.api.player.ClientManager
import com.github.rushyverse.events.EventsPlugin
import com.github.rushyverse.events.client.EventPlayer
import com.github.rushyverse.events.game.GameManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class AuthenticationListener : Listener {

    private val games : GameManager by inject(EventsPlugin.ID)
    private val clients : ClientManager by inject(EventsPlugin.ID)

    @EventHandler
    suspend fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        val world = player.world
        val game = games.getByWorld(world)

        game?.apply {
            clientLeave(clients.getClient(player) as EventPlayer)
        }

        event.quitMessage(null)
    }
}