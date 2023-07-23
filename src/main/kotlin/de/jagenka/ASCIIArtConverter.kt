package de.jagenka

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.cache.data.EmojiData
import dev.kord.core.entity.GuildEmoji
import dev.kord.core.event.message.MessageCreateEvent
import java.awt.Image
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.URL
import javax.imageio.ImageIO
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.streams.toList

object ASCIIArtConverter
{
    private var emojis = mutableListOf<Emoji>()
    private var lastEmojiUpdate: Long = 0
    private lateinit var kord: Kord
    private var isInitialized = false

    private val guildEmojiRegex = Regex("<a?:[a-zA-Z0-9_]+:[0-9]+>")

    private const val densityStringComplex = """$@B%8&WM#*oahkbdpqwmZO0QLCJUYXzcvunxrjft/\|()1{}[]?-_+~<>i!lI;:,"^`'. """
    private const val densityStringSimple = """@%#*+=-:. """

    fun Int.convertBrightnessToChar(complex: Boolean = false): Char = if (complex) this.convertBrightnessToCharComplex() else this.convertBrightnessToCharSimple()

    private fun Int.convertBrightnessToCharComplex(): Char = densityStringComplex.reversed()[((this / 255.0) * (densityStringComplex.length - 1)).roundToInt()]

    private fun Int.convertBrightnessToCharSimple(): Char = densityStringSimple.reversed()[((this/255.0) * (densityStringSimple.length - 1)).roundToInt()]

    private fun initialize()
    {
        loadCurrentEmojiList()
        kord = DiscordHandler.kord ?: error("kord not initialized")
        isInitialized = true
    }

    fun convertEmojiToASCII(event: MessageCreateEvent, scale: Double = 1.0): String?
    {
        if (!isInitialized) initialize()

        // searches the message for emojis
        val foundEmojis = findEmojis(event.message.content, event.guildId ?: Snowflake(0))
        if (foundEmojis.isEmpty()) return null

        // loads image of emoji
        val emoji = loadEmoji(foundEmojis[0] ?: emptyList()) ?: run {
            DiscordHandler.sendMessage("Couldn't load emoji!")
            return null
        }

        return emoji.convertBufferedImageToASCII(scale)
    }

    // TODO: Scale to target size
    private fun BufferedImage.convertBufferedImageToASCII(scale: Double = 1.0): String
    {
        val scaledBufferedImage = BufferedImage((this.width * scale).toInt(), (this.height * scale).toInt(), BufferedImage.TYPE_INT_RGB)
        val g = scaledBufferedImage.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.drawImage(this, 0, 0, scaledBufferedImage.width, scaledBufferedImage.height, 0, 0, this.width, this.height, null)
        g.dispose()
        val sb = StringBuilder()
        for (y in 0 until scaledBufferedImage.height)
        {
            sb.append("\n")
            for (x in 0 until scaledBufferedImage.width)
            {
                val clr: Int = scaledBufferedImage.getRGB(x, y)
                val alpha = clr shr 24 and 0xff
                val red = clr shr 16 and 0xff
                val green = clr shr 8 and 0xff
                val blue = clr and 0xff
                val brightness = sqrt(0.299 * red * red + 0.587 * green * green + 0.114 * blue * blue).toInt() * alpha / 255
                sb.append(brightness.convertBrightnessToChar())
            }
        }
        return sb.toString()
    }

    private fun loadEmoji(possibleURLs: List<String>): BufferedImage?
    {
        possibleURLs.forEach { url ->
            try
            {
                return ImageIO.read(URL(url))
            } catch (e: Exception)
            {
                e.printStackTrace()
            }
        }

        return null
    }

    /**
     * @return list of order index to URL
     */
    private fun findEmojis(text: String, guildId: Snowflake): Map<Int, List<String>>
    {
        val result = mutableListOf<Pair<Int, String>>()

        emojis.forEach {
            var startIndex = 0
            var indexToEmoji: Pair<Int, String> = 0 to "🍆"
            while (text.findAnyOf(listOf(it.fullEmoji), startIndex = startIndex)?.also { indexToEmoji = it } != null)
            {
                result.add(indexToEmoji)
                val (i, emoji) = indexToEmoji
                startIndex = i + emoji.length
            }
        }

        result.sortByDescending { it.second }
        result.sortBy { it.first }

        val realResult = mutableListOf<Pair<Int, String>>()
        result.map { it.first }.toSet().forEach { i -> realResult.add(result.first { it.first == i }) }

        val indexToShouldRemove = mutableMapOf<Int, Boolean>().withDefault { false }
        realResult.forEach { (index, emoji) ->
            if (!indexToShouldRemove.getValue(index))
            {
                (index + 1 until index + emoji.length).forEach { indexToShouldRemove[it] = true } // lösche überlappende emoji
            }
        }

        val stringIndexToEmojiString = realResult.filter { !indexToShouldRemove.getValue(it.first) }.toMutableList() // actually delete emojis that were determined to be deleted
        stringIndexToEmojiString.toList().forEach { (index, emoji) ->
            getPossibleAlternatives(emoji).forEach { stringIndexToEmojiString.add(index to it.fullEmoji) }
        }

        val stringIndexToURLs = stringIndexToEmojiString.map { it.first to getEmojiURL(it.second) }.toMutableList()

        guildEmojiRegex.findAll(text).forEach { matchResult ->
            matchResult.value.split(":")[2].removeSuffix(">").toLongOrNull()?.let { id ->
                stringIndexToURLs.add(matchResult.range.first to getGuildEmojiURL(Snowflake(id), guildId))
            }
        }

        val toBeReturned = mutableMapOf<Int, List<String>>()
        stringIndexToURLs.groupBy({ it.first }, { it.second }).toList().sortedBy { it.first }.forEachIndexed { index, (key, value) -> toBeReturned[index] = value }
        return toBeReturned
    }

    private fun loadCurrentEmojiList()
    {
        try
        {
            val url = URL("https://www.unicode.org/Public/emoji/latest/emoji-test.txt")
            val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000 // timing out in a minute
            val reader = BufferedReader(InputStreamReader(conn.inputStream))

            emojis = reader.readLines()
                .filter { !it.startsWith("#") && it.isNotBlank() }
                .map {
                    Emoji(
                        fullEmoji = it.substringBefore(";").trim().split(" ")
                            .map {
                                try
                                {
                                    it.toInt(16)
                                } catch (_: NumberFormatException)
                                {
                                    println("Error with $it")
                                    0
                                }
                            }.joinToString(separator = "") { Character.toChars(it).joinToString("") },
                        name = it.substringAfter(it.substringAfter("#").trim().split(" ")[1]).trim()
                    )
                }.toMutableList()

            lastEmojiUpdate = System.currentTimeMillis()
            reader.close()
        } catch (e: Exception)
        {
            println("ewwow")
        }
    }

    fun getPossibleAlternatives(emoji: String): List<Emoji>
    {
        val alts = mutableListOf<Emoji>()
        emojis.forEach {
            if (it.fullEmoji == emoji)
            {
                alts.addAll(emojis.filter { inner -> it.name == inner.name })
            }
        }
        return alts.distinctBy { it.fullEmoji }
    }

    fun getEmojiURL(emoji: String): String
    {
        val id = emoji.codePoints().toList()
            .joinToString(separator = "-") { it.toString(16) }
        return "https://cdn.jsdelivr.net/gh/twitter/twemoji@latest/assets/72x72/$id.png"
    }

    fun getGuildEmojiURL(guildEmojiId: Snowflake, guildId: Snowflake): String
    {
        return GuildEmoji(EmojiData(guildEmojiId, guildId), kord).image.cdnUrl.toUrl()
    }

    fun unicodeEmojiToHex(emoji: String): List<String>
    {
        return emoji.codePoints().toList().map { it.toString(16) }
    }
}

class Emoji(val fullEmoji: String, val name: String)
{
    override fun toString(): String
    {
        return "$fullEmoji $name"
    }
}