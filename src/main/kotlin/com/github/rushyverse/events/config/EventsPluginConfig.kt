package com.github.rushyverse.events.config

import com.github.rushyverse.api.extension.ItemStack
import com.github.rushyverse.api.translation.Translator
import com.github.rushyverse.api.translation.getComponent
import kotlinx.serialization.Serializable
import org.bukkit.Material
import java.util.*

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class EventsPluginConfig(
    val dataProvider: DataProviderConfig,
    val rewards: RewardsConfig,
    val backToHubCommand: String,
    val runEventPermission: String,
    val items: Set<HotbarItem>,
) {

}

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class DataProviderConfig(
    val apiSharedMemory: Boolean
)

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class HotbarItem(
    val name: String,
    val material: Material,
    val commandOnClick: String,
    val slot: Int,
    val needConfirm: Boolean,
) {
    fun itemStack(translator: Translator, locale: Locale) = ItemStack(material) {
        itemMeta = itemMeta.apply {
            displayName(translator.getComponent(name, locale))
        }
    }
}

