package com.github.rushyverse.events.game

import com.github.rushyverse.api.extension.asComponent
import com.github.rushyverse.api.extension.withBold
import com.github.rushyverse.api.extension.withItalic
import com.github.rushyverse.api.game.GameState
import com.github.rushyverse.api.koin.inject
import com.github.rushyverse.api.translation.Translator
import com.github.rushyverse.api.translation.getComponent
import com.github.rushyverse.events.EventsPlugin
import com.github.rushyverse.events.client.EventPlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import java.util.*

object GameScoreboard {

    private val emptyLine = Component.empty()
    private val scoreboardTitle = "<gradient:yellow:gold:red>Sumo Event"
        .asComponent().withBold()
    private val serverIpAddress = "<gradient:light_purple:dark_purple:red>play.rushy.space"
        .asComponent().withBold()
    private val waitingForGameStart = "<Yellow>Waiting for game to start..."
        .asComponent().withItalic()

    private val translator: Translator by inject(EventsPlugin.ID)

    suspend fun update(
        client: EventPlayer,
        game: SumoGame,
        timeFormatted: String = "0"
    ) {
        val locale = client.lang().locale
        val lines = mutableListOf<Component>()
        val stats = client.stats

        lines.add(emptyLine)
        lines.add(waitingForGameStart)
        lines.add(emptyLine)

        if (game.state() == GameState.STARTED) {
            lines.add(timeLine(locale, timeFormatted))
            lines.add(playersLine(locale, game.players.size, game.mapConfig.maxPlayers))
            lines.add(emptyLine)
        }

        lines.add(serverIpAddress)

        client.scoreboard().apply {
            updateTitle(scoreboardTitle)
            updateLines(lines)
        }
    }

    private fun timeLine(locale: Locale, timeFormatted: String) =
        translator.getComponent("scoreboard.time", locale, arrayOf("<gold>$timeFormatted"))
            .color(NamedTextColor.YELLOW)

    private fun playersLine(locale: Locale, players: Int, maxPlayers: Int) =
        translator.getComponent("scoreboard.players", locale, arrayOf("<yellow>$players</yellow>", "<gold>$maxPlayers"))
            .color(NamedTextColor.GREEN)

    private fun playerVsPlayer(): Nothing = TODO()
}
