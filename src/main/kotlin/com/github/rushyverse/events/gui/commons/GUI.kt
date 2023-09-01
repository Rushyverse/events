package com.github.rushyverse.hub.gui.commons

import com.github.rushyverse.api.extension.asComponent
import com.github.rushyverse.api.koin.inject
import com.github.rushyverse.api.player.Client
import com.github.rushyverse.api.translation.Translator
import com.github.rushyverse.api.translation.getComponent
import com.github.rushyverse.events.EventsPlugin
import org.bukkit.Bukkit
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

/**
 * Abstract representation of a GUI.
 * @property titleKey String the translation title key of the GUI.
 * @property size Int must be mulitple of 9, can't be superior to 54.
 * @property viewers MutableList<Client>
 */
abstract class GUI(
    val titleKey: String,
    val size: Int,
) {

    protected val translator: Translator by inject(EventsPlugin.ID)
    val viewers: MutableList<Client> = mutableListOf()

    suspend fun open(client: Client) {
        val translatedTitle = if (titleKey.contains(".")) {
            translator.getComponent(titleKey, client.lang().locale)
        } else titleKey.asComponent()
        val inv = Bukkit.createInventory(null, size, translatedTitle)
        client.requirePlayer().openInventory(inv)
        applyItems(client, inv)
        viewers.add(client)
    }

    suspend fun close(client: Client) {
        viewers.remove(client)
        client.requirePlayer().closeInventory()
    }

    suspend fun sync() {
        for (viewer in viewers) {
            applyItems(viewer, viewer.requirePlayer().openInventory.topInventory)
        }
    }

    abstract suspend fun applyItems(client: Client, inv: Inventory)

    abstract suspend fun onClick(client: Client, item: ItemStack, clickType: ClickType)
}