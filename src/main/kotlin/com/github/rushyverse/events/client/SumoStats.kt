package com.github.rushyverse.events.client

import com.github.rushyverse.api.game.stats.KillableStats
import com.github.rushyverse.api.game.stats.Stats
import com.github.rushyverse.api.game.stats.WinnableStats


data class SumoStats(
    val killableStats: KillableStats = KillableStats(),
    val winnableStats: WinnableStats = WinnableStats(),
) : Stats {

    override fun calculateScore(): Int {
        val score = killableStats.calculateScore() + winnableStats.calculateScore()
        if (score < 0)
            return 0
        return score
    }

    fun kills() = killableStats.kills
    fun incKills() { killableStats.kills = killableStats.kills.inc() }

    fun deaths() = killableStats.deaths
    fun incDeaths() { killableStats.deaths = killableStats.deaths.inc() }

    fun wins() = winnableStats.wins
    fun incWins() { winnableStats.wins = winnableStats.wins.inc() }

    fun loses() = winnableStats.loses
    fun incLoses() { winnableStats.loses = winnableStats.loses.inc() }
}
