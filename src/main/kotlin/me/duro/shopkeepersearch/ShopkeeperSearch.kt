package me.duro.shopkeepersearch

import me.duro.shopkeepersearch.commands.SearchCommand
import org.bukkit.plugin.java.JavaPlugin

class ShopkeeperSearch : JavaPlugin() {
    lateinit var config: Config

    override fun onEnable() {
        instance = this

        config = ConfigLoader().data

        getCommand("sks")?.setExecutor(SearchCommand())
    }

    override fun onDisable() {}

    companion object {
        lateinit var instance: ShopkeeperSearch
    }
}
