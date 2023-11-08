package me.duro.shopkeepersearch

import me.duro.shopkeepersearch.commands.SearchCommand
import org.bukkit.plugin.java.JavaPlugin

class ShopkeeperSearch : JavaPlugin() {
    override fun onEnable() {
        instance = this

        Config.load()
        registerCommands()

        logger.info("Shopkeeper Search has loaded!")
    }

    private fun registerCommands() {
        getCommand("sks")?.setExecutor(SearchCommand())
        logger.info("Registered commands")
    }

    override fun onDisable() {
        logger.info("Shopkeeper Search has unloaded!")
    }

    companion object {
        lateinit var instance: ShopkeeperSearch
            private set
    }
}
