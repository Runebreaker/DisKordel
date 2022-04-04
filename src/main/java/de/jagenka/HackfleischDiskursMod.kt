package de.jagenka

import com.google.gson.internal.Streams
import com.google.gson.stream.JsonReader
import de.jagenka.Util.unwrap
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.MessageType
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.stat.Stats
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.WorldSavePath
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Position
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.StringReader
import java.nio.file.Files
import java.util.*
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.math.min

//TODO command interface

@Suppress("UNUSED")
object HackfleischDiskursMod : ModInitializer
{
    private const val MOD_ID = "hackfleisch-diskurs-mod"

    var uuid: UUID = UUID.randomUUID()

    private lateinit var minecraftServer: MinecraftServer

    override fun onInitialize() //TODO: schedule for later?
    {
        //register commands
        CommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            WhoisCommand.register(dispatcher)
            WhereIsCommand.register(dispatcher)
        }

        val path = FabricLoader.getInstance().configDir.resolve("hackfleisch-diskurs.yaml")
        val confLoader = YamlConfigurationLoader.builder().path(path).build()
        val root = confLoader.load()

        val token = root.node("bot-token").get(String::class.java)
        val guildId = root.node("bot-guild").get(Long::class.java)
        val channelId = root.node("bot-channel").get(Long::class.java)

        if (token == null)
        {
            root.node("bot-token").set("INSERT_TOKEN_HERE")
            confLoader.save(root)
            println("bot-token missing!")
        }
        if (guildId == null)
        {
            root.node("bot-guild").set("INSERT_GUILD_ID_HERE")
            confLoader.save(root)
            println("bot-guild missing!")
        }
        if (channelId == null)
        {
            root.node("bot-channel").set("INSERT_CHANNEL_ID_HERE")
            confLoader.save(root)
            println("bot-channel missing!")
        }
        if (token != null && guildId != null && channelId != null) DiscordBot.initialize(token, guildId, channelId)

        println("hackfleisch-diskurs-mod has been initialized.")
    }

    /**
     * @return List of Pair of real playerName and deathCount
     */
    fun getDeathScore(playerName: String): List<Pair<String, Int>>
    {
        if (!checkMinecraftServer()) return emptyList()

        val result = ArrayList<Pair<String, Int>>()

        val possiblePlayers = Users.find(playerName)

        minecraftServer.scoreboard.getAllPlayerScores(minecraftServer.scoreboard.getObjective("deaths"))
            .forEach {
                possiblePlayers.forEach { player ->
                    if (it.playerName.equals(player.minecraftName, ignoreCase = true)) result.add(Pair(it.playerName, it.score))
                }
                if (it.playerName.equals(playerName, ignoreCase = true)) result.add(Pair(it.playerName, it.score))
            }

        return result
    }

    /**
     * @return List of Pair of real playerName and playtime in ticks
     */
    fun getPlaytime(playerName: String): List<Pair<String, Int>>
    {
        if (!checkMinecraftServer()) return emptyList()

        val result = ArrayList<Pair<String, Int>>()

        val possibleUsers = Users.find(playerName).toMutableList()
        if (possibleUsers.find { user -> user.minecraftName.equals(playerName, ignoreCase = true) } == null)
            possibleUsers.add(User("", "", playerName)) // if someone is not registered

        val onlinePlayers = minecraftServer.playerManager.playerList
        val statsPath = minecraftServer.getSavePath(WorldSavePath.STATS)
        var foundSomeoneOnline = false

        possibleUsers.forEach { possibleUser ->
            onlinePlayers.forEach { onlinePlayer ->
                if (possibleUser.minecraftName.equals(onlinePlayer.name.asString(), ignoreCase = true))
                {
                    val playtime = onlinePlayer.statHandler.getStat(Stats.CUSTOM, Stats.PLAY_TIME)
                    result.add(Pair(onlinePlayer.name.asString(), playtime))
                    foundSomeoneOnline = true
                }
            }
        }
        if (!foundSomeoneOnline)
        {
            val statsOnDiskMap = HashMap<String, Int>() // key: minecraftName, value: playtime

            Files.list(statsPath).forEach { statFile ->
                val jsonReader = JsonReader(StringReader(statFile.readText()))
                val jsonElement = Streams.parse(jsonReader)
                val jsonObject = jsonElement.asJsonObject

                jsonObject.entrySet().find { it.key == "stats" }
                    ?.let {
                        it.value.asJsonObject.entrySet().find { it.key == "minecraft:custom" }
                            ?.let {
                                it.value.asJsonObject.entrySet().find { it.key == "minecraft:play_time" }
                                    ?.let { playtimeEntry ->
                                        val playerUUID = statFile.fileName.name.dropLast(5)
                                        statsOnDiskMap[minecraftServer.userCache.getByUuid(UUID.fromString(playerUUID)).unwrap()?.name.toString()] = playtimeEntry.value.asInt
                                    }
                            }

                    }
            }

            possibleUsers.forEach { possibleUser -> // TODO: move to own method -> make playtime leaderboard
                val offlinePlayer = minecraftServer.userCache.findByName(possibleUser.minecraftName).unwrap()
                statsOnDiskMap.forEach { (name, playtime) ->
                    if (name.equals(offlinePlayer?.name.toString(), ignoreCase = true))
                    {
                        result.add(Pair(name, playtime))
                    }
                }
            }
        }

        return result
    }

    fun getScoreFromScoreboard() //TODO
    {
        minecraftServer.scoreboard.getAllPlayerScores(minecraftServer.scoreboard.getObjective("deaths")).forEach { println("${it.playerName}: ${it.score}") }
    }

    fun getOnlinePlayers(): List<String>
    {
        if (!checkMinecraftServer()) return emptyList()
        val list = ArrayList<String>()
        minecraftServer.playerManager.playerList.forEach { list.add(it.name.asString()) }
        return list
    }

    @JvmStatic
    fun broadcastMessage(message: String, formatting: Formatting = Formatting.WHITE, sender: UUID = uuid)
    {
        if (!checkMinecraftServer()) return
        val text = LiteralText(message).formatted(formatting)
        minecraftServer.playerManager.broadcast(text, MessageType.CHAT, sender)
    }

    fun doThing()
    {
        if (!checkMinecraftServer()) return

        val hideo = minecraftServer.userCache.findByName("HideoTurismo").unwrap()?.id.toString()
        val statsPath = minecraftServer.getSavePath(WorldSavePath.STATS)
        Files.list(statsPath).forEach { statFile ->
            if (statFile.toString().contains(hideo))
            {
                val jsonReader = JsonReader(StringReader(statFile.readText()))
                val jsonElement = Streams.parse(jsonReader)
                val jsonObject = jsonElement.asJsonObject

                jsonObject.entrySet().find { it.key == "stats" }
                    ?.let {
                        it.value.asJsonObject.entrySet().find { it.key == "minecraft:custom" }
                            ?.let {
                                it.value.asJsonObject.entrySet().find { it.key == "minecraft:play_time" }
                                    ?.let { println(it.value) }
                            }
                    }
            }
        }


        //minecraftServer.commandManager.execute(minecraftServer.commandSource, "say helö") //cmd coming from MinecraftServer
        //minecraftServer.playerManager.playerList.get(0).dataTracker ??  Stats.PLAY_TIME
//        minecraftServer.playerManager.playerList.forEach {
//            val playtime = it.statHandler.getStat(Stats.CUSTOM, Stats.PLAY_TIME)
//            println("${it.name.asString()} has played for $playtime ticks")
//        }
    }

    fun runCommand(cmd: String)
    {
        if (!checkMinecraftServer()) return
        minecraftServer.commandManager.execute(minecraftServer.commandSource, cmd)
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
    }

    fun getPerformanceMetrics(): PerformanceMetrics
    {
        if (!checkMinecraftServer()) return PerformanceMetrics(0.0, 0.0)
        val mspt = MathHelper.average(minecraftServer.lastTickLengths) * 1.0E-6
        val tps = min(1000.0 / mspt, 20.0)

        return PerformanceMetrics(mspt, tps)
    }

    fun getPlayerPosition(playerString: String): Position?
    {
        if (!checkMinecraftServer()) return null
        val player = minecraftServer.playerManager.getPlayer(playerString) ?: return null
        return player.pos
    }

    private fun checkMinecraftServer(): Boolean
    {
        return HackfleischDiskursMod::minecraftServer.isInitialized
    }

    fun sendMessageToPlayer(player: ServerPlayerEntity, text: String)
    {
        player.sendMessage(Text.of(text), MessageType.CHAT, uuid)
    }

    //to set MinecraftServer instance coming from Mixin
    @JvmStatic
    fun onServerLoaded(minecraftServer: MinecraftServer)
    {
        this.minecraftServer = minecraftServer
    }
}

data class PerformanceMetrics(val mspt: Double, val tps: Double)