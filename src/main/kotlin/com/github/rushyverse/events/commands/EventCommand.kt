package com.github.rushyverse.events.commands

import com.github.rushyverse.api.extension.asComponent
import com.github.rushyverse.api.game.GameState
import com.github.rushyverse.api.koin.inject
import com.github.rushyverse.api.player.ClientManager
import com.github.rushyverse.api.player.getTypedClient
import com.github.rushyverse.events.EventsPlugin
import com.github.rushyverse.events.client.EventPlayer
import com.github.rushyverse.events.game.GameManager
import com.github.rushyverse.events.game.SumoGame
import com.github.shynixn.mccoroutine.bukkit.launch
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.kotlindsl.*

class EventCommand(
    private val plugin: EventsPlugin
) {

    private val manager: GameManager by inject(EventsPlugin.ID)
    private val clients: ClientManager by inject(EventsPlugin.ID)

    /**
     * join - join the current event
     *      - STARTED
     *          La personne qui a appuyé sur l'event (du gui) se tp directement dans la file
     *          join c'est les autre qui vont être tp il faudrait que je fasse un rec pour qu'on comprend mieux
     * run - open event run gui
     * start - start the game
     */
    suspend fun register(plugin: EventsPlugin) {
        commandAPICommand("event") {

            playerExecutor { player, _ ->
                plugin.launch {
                    plugin.eventsGui.open(plugin.clientManager.getTypedClient<EventPlayer>(player))
                }
            }

            subcommand("run") {

                argument(StringArgument("event").replaceSuggestions(ArgumentSuggestions.strings("sumo", "1V1", "popo")))
                playerExecutor { player, args ->
                    val type =
                        plugin.configMaps.maps.firstOrNull { it.name.lowercase() == (args.get("event") as String).lowercase() }

                    if (type == null) {
                        player.sendMessage("Event not recognized.")
                        return@playerExecutor
                    }
                    plugin.launch {
                        val game: SumoGame
                        if (manager.currentEvent == null) {
                            game = manager.createAndSave(type)
                        } else {
                            game = manager.getGame()!!
                        }

                        game.clientJoin(clients.getClient(player) as EventPlayer)
                    }
                }
            }


            subcommand("join") {
                argument(StringArgument("event").replaceSuggestions(ArgumentSuggestions.strings("sumo", "1V1", "popo")))
                playerExecutor { player, _ ->
                    plugin.launch {
                        val client = clients.getClient(player) as EventPlayer
                        manager.getGame()?.clientJoin(client)
                    }
                }
            }

            subcommand("start") {
                playerExecutor { player, _ ->

                    val game = manager.getGame()

                    if (game == null) {
                        player.sendMessage("No event to join.")
                        return@playerExecutor
                    }


                    if (game.state() == GameState.WAITING) {
                        player.sendMessage("Requête envoyée")
                        plugin.launch { game.start() }
                    } else {
                        player.sendMessage("Impossible de faire cela pendant la partie")
                    }

                    /*if (game.state() != GameState.STARTED){
                        plugin.launch { game.start(true) }
                    } else {
                        player.sendMessage("The game is already started.")
                    }*/
                }
            }
        }
    }
}