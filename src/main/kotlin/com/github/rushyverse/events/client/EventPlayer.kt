package com.github.rushyverse.events.client

import com.github.rushyverse.api.player.Client
import com.github.rushyverse.events.config.Reward
import kotlinx.coroutines.CoroutineScope
import java.util.*

class EventPlayer(
    val stats: SumoStats = SumoStats(),
    uuid: UUID,
    scope: CoroutineScope
) : Client(uuid, scope) {

    /**
     * Gives instantly a reward to the client.
     * For each positive value in the [Reward], the client will receive
     * a notification message and sound.
     * @param reward The reward to apply on the client.
     */
    fun reward(reward: Reward) {
        if (reward.coins > 0) {
            // TODO: inc coins value
            send("§E+${reward.coins} coins")
        }
        if (reward.xp > 0) {
            requirePlayer().giveExp(reward.xp)
            send("§D+${reward.xp} xp")
        }
    }
}