package com.github.rushyverse.events.listener

import com.github.rushyverse.api.extension.withBold
import com.github.rushyverse.api.player.getTypedClient
import com.github.rushyverse.api.translation.getComponent
import com.github.rushyverse.events.EventsPlugin
import com.github.rushyverse.events.client.EventPlayer
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

class HotbarListener(
    private val plugin: EventsPlugin
) : Listener {

    private val confirmations = mutableMapOf<String, Long>()
    private val TIME_TO_CONFIRM = 3000L

    @EventHandler
    suspend fun onInteract(event: PlayerInteractEvent) {
        val item = event.item
        val action = event.action

        if (item == null) return
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val player = event.player
        val hotbarItem = plugin.config.items.firstOrNull { it.material == item.type } ?: return

        if (hotbarItem.needConfirm) {
            val currentTime = System.currentTimeMillis()
            val timeClicked = confirmations[player.name]

            if (timeClicked == null) {
               askForConfirmation(plugin, player)
            } else {
                val elapsedTime = currentTime - timeClicked
                if (elapsedTime >= TIME_TO_CONFIRM) {
                    askForConfirmation(plugin, player)
                } else {
                    player.performCommand(hotbarItem.commandOnClick)
                }
            }
        }
    }

    private suspend fun askForConfirmation(
        plugin: EventsPlugin,
        player: Player,
    ) {
        val client = plugin.clientManager.getTypedClient<EventPlayer>(player)
        player.sendMessage(
            plugin.translator.getComponent("hotbar.confirmation.message", client.lang().locale)
                .color(NamedTextColor.RED).withBold()
        )

        confirmations[player.name] = System.currentTimeMillis()
    }
}