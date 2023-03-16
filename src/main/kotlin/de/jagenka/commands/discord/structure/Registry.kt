package de.jagenka.commands.discord.structure

import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on

object Registry
{
    private val messageCommandPrefix: String = "!"
    private val interactsWithBots: Boolean = true
    internal val commands = mutableMapOf<String, MessageCommand>()

    /**
     * suspend function to be called if a command needs admin, but the sender does not have the admin role.
     */
    var needsAdminResponse: suspend (event: MessageCreateEvent) -> Unit = {
        it.message.channel.createMessage("You need to be admin to do that!")
    }

    /**
     * suspend function to be called if a command can only execute in a channel marked "NSFW", but isn't marked as such.
     */
    var needsNSFWResponse: suspend (event: MessageCreateEvent) -> Unit = {
        it.message.channel.createMessage("You can only do that in a channel marked \"NSFW\"!")
    }

    var isSenderAdmin: suspend (event: MessageCreateEvent) -> Boolean = {
        it.member?.isOwner() == true // TODO: adminRole
    }

    fun setup(kord: Kord)
    {
        kord.on<MessageCreateEvent> {
            if (interactsWithBots)
            {
                // return if author is self or undefined
                if (message.author?.id == kord.selfId) return@on
            } else
            {
                // return if author is a bot or undefined
                if (message.author?.isBot != false) return@on
            }

            if (!message.content.startsWith(messageCommandPrefix)) return@on

            val args = this.message.content.split(" ")
            val firstWord = args.getOrNull(0) ?: return@on

            val command = commands[firstWord.removePrefix(messageCommandPrefix)] ?: return@on

            command.run(this, args)
        }
    }

    /**
     * Register your own implementation of a MessageCommand here. This method will also call `command.prepare(kord)`.
     * @param command is said custom implementation of MessageCommand.
     */
    fun register(command: MessageCommand)
    {
        command.ids.forEach {
            if (commands.containsKey(it)) error("command id `$it` is already assigned to command ${commands[it]}")
            commands[it] = command
        }

        command.prepare(this)
    }

    internal suspend fun getShortHelpTexts(event: MessageCreateEvent): List<String>
    {
        return commands.values.toSortedSet().filter { isSenderAdmin.invoke(event) || it.allowedArgumentCombinations.any { !it.needsAdmin } }
            .map { it.ids.joinToString(separator = "|", postfix = ": ${it.helpText}") { "`$messageCommandPrefix$it`" } }
    }

    internal suspend fun getHelpTextsForCommand(id: String, event: MessageCreateEvent): List<String>
    {
        return commands[id]?.allowedArgumentCombinations?.filter { !it.needsAdmin || isSenderAdmin.invoke(event) }?.map {
            it.arguments.joinToString(prefix = "`$messageCommandPrefix$id ", separator = " ") {
                it.displayInHelp
            }.trim() + "`: ${it.helpText}"
        } ?: listOf("`$messageCommandPrefix$id` is not a valid command.")
    }
}