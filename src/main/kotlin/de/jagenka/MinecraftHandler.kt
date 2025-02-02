package de.jagenka

import de.jagenka.config.Config
import dev.kord.core.entity.effectiveName
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.minecraft.network.message.MessageType
import net.minecraft.network.message.SignedMessage
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.*
import net.minecraft.util.Formatting
import net.minecraft.util.math.Position
import org.slf4j.LoggerFactory
import kotlin.math.min

object MinecraftHandler
{
    val logger = LoggerFactory.getLogger("diskordel")

    var minecraftServer: MinecraftServer? = null

    // to set MinecraftServer instance coming from Mixin (actually from FabricAPI)
    fun onServerLoaded(minecraftServer: MinecraftServer)
    {
        this.minecraftServer = minecraftServer

        minecraftServer.playerManager.isWhitelistEnabled = true

        Main.scope.launch {
            UserRegistry.loadUserCache()
            UserRegistry.loadRegisteredUsersFromFile()
            UserRegistry.loadGameProfilesFromPlayerData()
            UserRegistry.saveCacheToFile()
        }
    }

    fun registerMixins()
    {
        //register chat message
        ServerMessageEvents.CHAT_MESSAGE.register { message, sender, params ->
            Main.scope.launch {
                handleMinecraftChatMessage(message.content, sender)
            }
        }

        //register system message
        ServerMessageEvents.GAME_MESSAGE.register { server, message, overlay ->
            Main.scope.launch {
                handleMinecraftSystemMessage(message)
            }
        }
    }

    private suspend fun handleMinecraftChatMessage(message: Text, sender: ServerPlayerEntity)
    {
        val user = UserRegistry.getMinecraftUser(sender.uuid) ?: return
        DiscordHandler.sendWebhookMessage(username = user.name, avatarURL = user.getSkinURL(), text = message.string)
    }

    private suspend fun handleMinecraftSystemMessage(message: Text)
    {
        val key = (message.content as? TranslatableTextContent)?.key ?: return

        if (key.startsWith("multiplayer.player.joined"))
        {
            handleLoginMessage(message)
        } else if (key.startsWith("multiplayer.player.left"))
        {
            handleLogoutMessage(message)
        } else if (key.startsWith("death."))
        {
            handleDeathMessage(message)
        } else if (key.startsWith("chat.type.advancement."))
        {
            handleAdvancementMessage(message)
        }
    }

    fun handleSayCommand(message: SignedMessage, params: MessageType.Parameters)
    {
        Main.scope.launch {
            if (params.type == minecraftServer?.registryManager?.get(RegistryKeys.MESSAGE_TYPE)?.get(MessageType.SAY_COMMAND))
            {
                val user = UserRegistry.getMinecraftUser(message.sender)
                val text = message.content

                DiscordHandler.sendWebhookMessage(
                    username = Config.configEntry.discordSettings.serverName,
                    avatarURL = "",
                    text = if (user != null) "[${user.name}] ${text.string}" else text.string,
                    escapeMarkdown = false
                )
            }
        }
    }

    /**
     * Sends a message looking like it came from a player, but stylized with > and cursive text.
     * If the first word is not a known player name, the message is sent as whatever is set as `serverName` in Config
     */
    private suspend fun sendSystemMessageAsPlayer(text: Text)
    {
        val string = text.string
        val firstWord = string.split(" ").firstOrNull()
        val user = firstWord?.let { playerName ->
            UserRegistry.getMinecraftUser(playerName)
        }

        DiscordHandler.sendWebhookMessage(
            username = user?.name ?: Config.configEntry.discordSettings.serverName,
            avatarURL = user?.getSkinURL() ?: "",
            text = if (user != null) "> *${string.removePrefix(firstWord).trim()}*" else "> *$string*",
            escapeMarkdown = false
        )
    }

    private suspend fun handleLoginMessage(text: Text)
    {
        sendSystemMessageAsPlayer(text)
    }

    private suspend fun handleLogoutMessage(text: Text)
    {
        sendSystemMessageAsPlayer(text)
    }

    private suspend fun handleDeathMessage(text: Text)
    {
        sendSystemMessageAsPlayer(text)
    }

    private suspend fun handleAdvancementMessage(text: Text)
    {
        sendSystemMessageAsPlayer(text)
    }

    suspend fun sendMessageFromDiscord(event: MessageCreateEvent)
    {
        val author = event.message.author
        val authorName = event.message.getAuthorAsMemberOrNull()?.effectiveName ?: author?.effectiveName ?: "unknown user"
        val associatedUser = UserRegistry.findUser(author?.id)

        val authorText = Text.of(
            "[$authorName]"
        ).getWithStyle(
            Style.EMPTY
                .withFormatting(Formatting.BLUE)
                .withHoverEvent(associatedUser?.minecraft?.name?.let { HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(it)) })
        ).firstOrNull()

        val referencedAuthorText = Text.of(
            event.message.referencedMessage?.getAuthorAsMemberOrNull()?.let {
                "@${it.effectiveName}"
            } ?: event.message.referencedMessage?.data?.author?.username?.let {
                "@$it"
            } ?: ""
        ).getWithStyle(
            Style.EMPTY
                .withFormatting(Formatting.BLUE)
                .withHoverEvent(
                    HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        Text.of(event.message.referencedMessage?.author?.let { UserRegistry.findUser(it.id)?.minecraft?.name })
                    )
                )
        ).firstOrNull()

        val content = Text.of(event.message.content)

        sendChatMessage(Texts.join(listOfNotNull(authorText, referencedAuthorText, content), Text.of(" ")))
    }

    fun getOnlinePlayers(): List<String>
    {
        minecraftServer?.let { server ->
            val list = ArrayList<String>()
            server.playerManager.playerList.forEach { list.add(it.name.string) }
            return list
        }

        return emptyList()
    }

    fun runCommand(cmd: String)
    {
        minecraftServer?.commandManager?.executeWithPrefix(minecraftServer?.commandSource, cmd)
    }

    fun runWhitelistAdd(player: String)
    {
        if (player.isEmpty()) return
        runCommand("whitelist add $player")
    }

    fun runWhitelistRemove(player: String)
    {
        if (player.isEmpty()) return
        runCommand("whitelist remove $player")
        runCommand("kick $player")
    }

    fun getPerformanceMetrics(): PerformanceMetrics
    {
        minecraftServer?.let { server ->
            val mspt = server.lastTickLengths.average() * 1.0E-6 // average is in nanoseconds -> convert to milliseconds
            val tps = min(1000.0 / mspt, 20.0)

            return PerformanceMetrics(mspt, tps)
        }

        return PerformanceMetrics(0.0, 0.0)
    }

    fun getPlayerPosition(playerString: String): Position?
    {
        minecraftServer?.let { server ->
            val player = server.playerManager.getPlayer(playerString) ?: return null
            return player.pos
        }
        return null
    }

    fun sendMessageToPlayer(player: ServerPlayerEntity, text: String)
    {
        player.sendMessage(Text.of(text))
    }

    fun sendChatMessage(message: String)
    {
        sendChatMessage(Text.of(message))
    }

    fun sendChatMessage(text: Text)
    {
        minecraftServer?.playerManager?.broadcast(text, false)
    }

    fun ServerPlayerEntity.sendPrivateMessage(text: String)
    {
        this.sendMessage(Text.of(text))
    }
}