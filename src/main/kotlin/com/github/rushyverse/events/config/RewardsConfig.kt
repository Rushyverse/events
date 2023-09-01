package com.github.rushyverse.events.config

import kotlinx.serialization.Serializable

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class RewardsConfig(
    val vipMultiplier: Reward,
    val kill: Reward,
    val win: Reward,
    val lose: Reward,
) {

}

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class Reward(
    val xp: Int,
    val coins: Int,
)