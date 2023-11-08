package me.duro.shopkeepersearch

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.lang.Exception

data class ConfigFile(
    val messages: Messages = Messages(),
    val searchRadius: Double = 50.0,
)

data class Messages(
    val playerOnly: String = "This command can only be executed by players",
    val usage: String = "&cUsage: /sks [item]",
    val missingFlags: String = "&cMissing enchant. /sks [item] [enchant]",
    val itemNotFound: String = "&cThat item does not exist",
    val shopkeeperNotFound: String = "&cCould not find a shopkeeper with %item%.",
    val separator: String = "&8&m                                                 ",
    val shopkeeperInfo: String = """%stocked%
    &2&lOwner: &a%owner%
    &2&lLocation: &aX: %x%, Y: %y%, Z: %z%
    &2&lTrade: &a%trade%""".trimIndent(),
    val outOfStock: String = "&c&l[OUT OF STOCK]"
)

class Config {
    lateinit var config: ConfigFile

    fun load(): Config {
        val file = File(ShopkeeperSearch.instance.dataFolder, "config.yml")

        if (!file.exists()) ShopkeeperSearch.instance.saveResource("config.yml", false)

        val configFile = YamlConfiguration()
        configFile.options().parseComments(true)

        try {
            configFile.load(file)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        config = ConfigFile(
            Messages(
                configFile.getString("messages.player-only") ?: Messages().playerOnly,
                configFile.getString("messages.usage") ?: Messages().usage,
                configFile.getString("messages.missing-flags") ?: Messages().missingFlags,
                configFile.getString("messages.item-not-found") ?: Messages().itemNotFound,
                configFile.getString("messages.shopkeeper-not-found") ?: Messages().shopkeeperNotFound,
                configFile.getString("messages.separator") ?: Messages().separator,
                configFile.getString("messages.shopkeeper-info") ?: Messages().shopkeeperInfo,
                configFile.getString("messages.out-of-stock") ?: Messages().outOfStock
            ),
            configFile.getDouble("search-radius")
        )

        return this
    }

    companion object {
        var instance: Config? = null
            private set

        fun load() {
            val configInstance = Config()
            configInstance.load()
            instance = configInstance
        }
    }
}