package com.github.rushyverse.events.gui

import com.github.rushyverse.api.extension.asComponent
import com.github.rushyverse.api.extension.withBold
import com.github.rushyverse.api.koin.inject
import com.github.rushyverse.api.player.Client
import com.github.rushyverse.api.translation.getComponent
import com.github.rushyverse.events.EventsPlugin
import com.github.rushyverse.events.client.EventPlayer
import com.github.rushyverse.events.config.EventsPluginConfig
import com.github.rushyverse.events.config.MapsConfig
import com.github.rushyverse.events.game.GameManager
import com.github.rushyverse.hub.gui.commons.GUI
import io.ktor.events.*
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*

class EventsGUI(
    private val plugin: EventsPlugin,
    private val config: EventsPluginConfig,
    private val mapsConfig: MapsConfig
) : GUI("gui.events.title", 9) {

    private val gameManager: GameManager by inject(plugin.id)

    override suspend fun applyItems(client: Client, inv: Inventory) {
        val locale = client.lang().locale
        mapsConfig.maps.forEach {
            inv.addItem(it.icon.apply {
                itemMeta = itemMeta.apply {
                    displayName(
                        plugin.translator.getComponent(it.name, locale)
                            .color(NamedTextColor.YELLOW).withBold()
                    )
                    lore(
                        listOf(
                            plugin.translator.getComponent(it.description, locale)
                                .color(NamedTextColor.GRAY)
                        )
                    )
                }
            })
        }
    }

    private val registerClick = mutableMapOf<UUID, Long>()

    private fun checkExpiredClicks(uuid: UUID): Boolean {
        val expiredKeys = registerClick.filterKeys {
            (System.currentTimeMillis() - registerClick[it]!!) > 3000L
        }

        expiredKeys.forEach {
            registerClick.remove(it.key)
        }


        return registerClick.contains(uuid)
    }

    override suspend fun onClick(client: Client, item: ItemStack, clickType: ClickType) {
        val mapClicked = mapsConfig.maps.firstOrNull { it.icon.type == item.type } ?: return
        val player = client.requirePlayer()

        var game = gameManager.currentEvent

        if (game == null) {
            if (checkExpiredClicks(player.uniqueId))
                return

            if (player.hasPermission(config.runEventPermission)) {
                registerClick[player.uniqueId] = System.currentTimeMillis()
                game = gameManager.createAndSave(mapClicked)
                game.clientJoin(client as EventPlayer)
            } else {
                client.send(
                    plugin.translator.getComponent("not.allowed.to.run", client.lang().locale)
                        .color(NamedTextColor.RED)
                )
            }
        } else {
            if (game.players.contains(player)) {
                return
            }
            game.clientJoin(client as EventPlayer)
        }

    }
}