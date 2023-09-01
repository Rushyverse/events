package com.github.rushyverse.events.listener

import com.github.rushyverse.hub.gui.commons.GUI
import com.github.rushyverse.api.koin.inject
import com.github.rushyverse.api.player.ClientManager
import com.github.rushyverse.events.EventsPlugin
import com.github.rushyverse.events.client.EventPlayer
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.PlayerInventory

class GUIListener(
    val plugin: EventsPlugin,
    val listOfGui: Set<GUI>
) : Listener {

    val clients: ClientManager by inject(plugin.id)

    @EventHandler
    suspend fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as Player
        val world = player.world
        val item = event.currentItem ?: return
        val client = clients.getClient(player)

        listOfGui.firstOrNull { it.viewers.contains(client) }?.onClick(client, item, event.click)

        val clickedInv = event.clickedInventory
        if (clickedInv != null && clickedInv is PlayerInventory) {
            player.inventory.heldItemSlot = event.slot
            plugin.server.pluginManager.callEvent(
                PlayerInteractEvent(
                    player,
                    Action.RIGHT_CLICK_AIR,
                    item,
                    null,
                    BlockFace.NORTH
                )
            )
        }
    }

    @EventHandler
    suspend fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as Player
        val world = player.world

        clients.getClientOrNull(player)?.apply {
            listOfGui.forEach {
                if (it.viewers.contains(this)) {
                    it.close(this)
                }
            }
        }

    }
}