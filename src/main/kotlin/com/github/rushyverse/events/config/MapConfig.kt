package com.github.rushyverse.events.config

import com.github.rushyverse.api.serializer.ItemStackSerializer
import kotlinx.serialization.*
import org.bukkit.inventory.ItemStack
import java.io.Serial

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class MapsConfig(
    val maps: Set<MapConfig>
)

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class MapConfig(
    val name: String,
    val description: String,
    val minPlayers: Int,
    val maxPlayers: Int,
    @Serializable(with = ItemStackSerializer::class)
    val icon: ItemStack,
    val permission: String,
    val worldName: String
)