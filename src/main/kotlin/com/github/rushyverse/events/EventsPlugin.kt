package com.github.rushyverse.events

import com.charleskorn.kaml.Yaml
import com.github.rushyverse.api.Plugin
import com.github.rushyverse.api.configuration.reader.IFileReader
import com.github.rushyverse.api.configuration.reader.YamlFileReader
import com.github.rushyverse.api.configuration.reader.readConfigurationFile
import com.github.rushyverse.api.extension.registerListener
import com.github.rushyverse.api.koin.loadModule
import com.github.rushyverse.api.player.Client
import com.github.rushyverse.api.serializer.LocationSerializer
import com.github.rushyverse.api.translation.ResourceBundleTranslator
import com.github.rushyverse.api.translation.registerResourceBundleForSupportedLocales
import com.github.rushyverse.events.client.EventPlayer
import com.github.rushyverse.events.commands.EventCommand
import com.github.rushyverse.events.config.MapConfig
import com.github.rushyverse.events.config.EventsPluginConfig
import com.github.rushyverse.events.config.MapsConfig
import com.github.rushyverse.events.game.GameManager
import com.github.rushyverse.events.gui.EventsGUI
import com.github.rushyverse.events.listener.*
import com.github.shynixn.mccoroutine.bukkit.scope
import dev.jorel.commandapi.CommandAPI
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import kotlinx.coroutines.plus
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.bukkit.entity.Player
import java.io.File
import java.util.*

class EventsPlugin : Plugin(ID, BUNDLE_EVENTS) {

    companion object {
        const val BUNDLE_EVENTS = "events_translate"
        const val ID = "Events"
    }

    lateinit var config: EventsPluginConfig private set
    lateinit var configMaps: MapsConfig private set

    lateinit var mapsDir: File private set
    lateinit var tempDir: File private set

    lateinit var eventsGui: EventsGUI private set

    override suspend fun onEnableAsync() {
        super.onEnableAsync()
        modulePlugin<EventsPlugin>()

        // interesting:  ("team" withColor "red").asComponent()
        val configReader = createYamlReader()
        config = configReader.readConfigurationFile<EventsPluginConfig>("config.yml")
        configMaps = configReader.readConfigurationFile<MapsConfig>("maps.yml")

        logger.info("Configuration Summary: $config")
        logger.info("Configuration Maps: $configMaps")

        mapsDir = File(dataFolder, "maps").apply { mkdirs() }
        tempDir = setupTempDir()

        eventsGui = EventsGUI(this, config, configMaps)

        loadModule(id) {
            single { GameManager(this@EventsPlugin) }
        }

        EventCommand(this).register(this)

        registerListener { HotbarListener(this) }
        registerListener { AuthenticationListener() }
        registerListener { UndesirableEventListener() }
        registerListener { GameListener() }
        registerListener { GUIListener(this, setOf(eventsGui)) }

    }

    /**
     * Create a new instance of yaml reader.
     * @return The instance of the yaml reader.
     */
    private fun createYamlReader(): IFileReader {
        val yaml = Yaml(
            serializersModule = SerializersModule {
                contextual(LocationSerializer)
            }
        )
        return YamlFileReader(this, yaml)
    }

    private fun setupTempDir() = File(dataFolder, "temp").apply {
        if (exists()) {
            // Clear the directory content
            this.listFiles()?.forEach {
                it.deleteRecursively()
            }
        } else {
            mkdirs()
        }
    }

    override fun createClient(player: Player): Client {
        return EventPlayer(
            uuid = player.uniqueId,
            scope = scope + SupervisorJob(scope.coroutineContext.job)
        )
    }

    override fun createTranslator(): ResourceBundleTranslator {
        return super.createTranslator().apply {
            registerResourceBundleForSupportedLocales(BUNDLE_EVENTS, ResourceBundle::getBundle)
        }
    }
}