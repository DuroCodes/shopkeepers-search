package me.duro.shopkeepersearch.commands

import com.nisovin.shopkeepers.api.ShopkeepersAPI.getShopkeeperRegistry
import com.nisovin.shopkeepers.api.shopkeeper.player.trade.TradingPlayerShopkeeper
import me.duro.shopkeepersearch.Config
import me.duro.shopkeepersearch.ShopkeeperSearch
import me.duro.shopkeepersearch.Util
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player

class SearchCommand : CommandExecutor, TabExecutor {
    private val validItemNames = Material.entries.map { it.name.lowercase() }

    private val config = Config.instance!!.config

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        try {
            if (sender !is Player) {
                sender.sendMessage(config.messages.playerOnly)
                return true
            }

            if (args.isEmpty()) {
                sender.sendMessage(Util.replaceColorCodes(config.messages.usage))
                return true
            }

            val material = Material.getMaterial(args[0].uppercase())

            if (material == null) {
                sender.sendMessage(Util.replaceColorCodes(config.messages.itemNotFound))
                return true
            }

            if (material === Material.ENCHANTED_BOOK &&
                (args.size == 1 || args[1].isEmpty() || Enchantment.getByKey(NamespacedKey.minecraft(args[1].lowercase())) == null)
            ) {
                sender.sendMessage(Util.replaceColorCodes(config.messages.missingFlags))
                return true
            }

            val matchedShopkeepers = getShopkeeperRegistry().allPlayerShopkeepers
                .filterIsInstance<TradingPlayerShopkeeper>()
                .filter { shopkeeper ->
                    sender.location.distance(shopkeeper.location!!) <= config.searchRadius
                            && shopkeeper.offers.any { Util.matchItem(args, it.resultItem) }
                }

            if (matchedShopkeepers.isEmpty()) {
                sender.sendMessage(
                    Util.replaceColorCodes(
                        config.messages.shopkeeperNotFound
                            .replace("%item%", Util.formatMaterial(material))
                    )
                )

                return true
            }

            val mainScoreboard = Bukkit.getScoreboardManager().mainScoreboard

            val stockedTeam = mainScoreboard.getTeam("shopkeepers_stocked")
                ?: mainScoreboard.registerNewTeam("shopkeepers_stocked")

            val notStockedTeam = mainScoreboard.getTeam("shopkeepers_not_stocked")
                ?: mainScoreboard.registerNewTeam("shopkeepers_not_stocked")

            stockedTeam.color(NamedTextColor.GREEN)
            notStockedTeam.color(NamedTextColor.RED)

            val shopkeeperStr = mutableListOf(config.messages.separator)

            matchedShopkeepers.forEach { shopkeeper ->
                val isStocked = shopkeeper.getTradingRecipes(sender).any {
                    Util.matchItem(args, it.resultItem) && !it.isOutOfStock
                }

                val offer = shopkeeper.offers.filter { Util.matchItem(args, it.resultItem) }[0]

                val tradeInfo = "${offer.item1.amount}x ${Util.formatMaterial(offer.item1.type)}${
                    if (offer.hasItem2()) " + ${offer.item2!!.amount}x ${
                        Util.formatMaterial(offer.item2!!.type)
                    }" else ""
                } → ${offer.resultItem.amount}x ${
                    if (material === Material.ENCHANTED_BOOK) Util.formatEnchantedBook(
                        Enchantment.getByKey(NamespacedKey.minecraft(args[1]))!!
                    ) else Util.formatMaterial(offer.resultItem.type)
                }"

                shopkeeperStr.add(
                    config.messages.shopkeeperInfo
                        .replace("%stocked%", if (isStocked) "" else config.messages.outOfStock)
                        .replace("%owner%", shopkeeper.ownerName)
                        .replace("%x%", shopkeeper.x.toString())
                        .replace("%y%", shopkeeper.y.toString())
                        .replace("%z%", shopkeeper.z.toString())
                        .replace("%trade%", tradeInfo)
                        .replace("%item%", Util.formatMaterial(material)).trimIndent()
                            + "\n${config.messages.separator}"
                )

                val entities = shopkeeper.location!!.getNearbyEntities(0.5, 0.5, 0.5)

                entities.forEach { e ->
                    if (isStocked) {
                        stockedTeam.addEntry(e.uniqueId.toString())
                    } else {
                        notStockedTeam.addEntry(e.uniqueId.toString())
                    }

                    e.isGlowing = true
                }

                Bukkit.getScheduler().runTaskLater(ShopkeeperSearch.instance, Runnable {
                    entities.forEach { e -> e.isGlowing = false }
                }, 200)
            }

            sender.sendMessage(Util.replaceColorCodes(shopkeeperStr.joinToString("\n")))

            return true
        } catch (e: Exception) {
            Bukkit.getServer().logger.info(e.stackTraceToString())
            sender.sendMessage(Util.replaceColorCodes("&cAn error has occurred."))
            return true
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<String>
    ): List<String> {
        if (args.isEmpty()) return emptyList()

        val currentArg = args[0].lowercase()

        if (args.size == 1) return validItemNames.filter { it.contains(currentArg) }

        if (args.size == 2) {
            val material = Material.matchMaterial(args[0]) ?: return emptyList()

            if (material !== Material.ENCHANTED_BOOK) return emptyList()

            val enchants = Enchantment.values().toList().filter {
                !it.key.key.contains("curse")
            }.map { it.key.key }

            return enchants.filter { it.contains(args[1]) }
        }

        return emptyList()
    }
}