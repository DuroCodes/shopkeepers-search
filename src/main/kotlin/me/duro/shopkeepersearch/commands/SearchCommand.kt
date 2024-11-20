package me.duro.shopkeepersearch.commands

import com.nisovin.shopkeepers.api.ShopkeepersAPI.getShopkeeperRegistry
import com.nisovin.shopkeepers.api.shopkeeper.offers.TradeOffer
import com.nisovin.shopkeepers.api.shopkeeper.player.trade.TradingPlayerShopkeeper
import me.duro.shopkeepersearch.*
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
    private val config = ShopkeeperSearch.instance.config

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        try {
            if (sender !is Player) {
                sender.sendMessage(config.messages.playerOnly)
                return true
            }

            if (args.isEmpty()) {
                sender.sendMessage(replaceColorCodes(config.messages.usage))
                return true
            }

            val material = Material.getMaterial(args[0].uppercase())
            if (material == null) {
                sender.sendMessage(replaceColorCodes(config.messages.itemNotFound))
                return true
            }

            val enchantKey = args.getOrNull(1)?.lowercase()
            val enchant = enchantKey?.let { enchantRegistry.get(NamespacedKey.minecraft(it)) }
            if (material == Material.ENCHANTED_BOOK && enchant == null) {
                sender.sendMessage(replaceColorCodes(config.messages.missingFlags))
                return true
            }

            val matchedShopkeepers = findMatchingShopkeepers(sender, args)
            if (matchedShopkeepers.isEmpty()) {
                sender.sendMessage(
                    replaceColorCodes(
                        config.messages.shopkeeperNotFound.replace("%item%", formatMaterial(material))
                    )
                )
                return true
            }

            displayShopkeepers(sender, matchedShopkeepers, material, enchant, args)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            sender.sendMessage(replaceColorCodes("&cAn error has occurred."))
            return true
        }
    }

    override fun onTabComplete(
        sender: CommandSender, command: Command, alias: String, args: Array<String>
    ): List<String> {
        if (args.isEmpty()) return emptyList()

        val currentArg = args[0].lowercase()

        if (args.size == 1) return validItemNames.filter { it.contains(currentArg) }

        if (args.size == 2) {
            val material = Material.matchMaterial(args[0]) ?: return emptyList()
            if (material !== Material.ENCHANTED_BOOK) return emptyList()

            val enchants = enchantRegistry.map { it.key.key }

            return enchants.filter { it.contains(args[1]) }
        }

        return emptyList()
    }

    private fun findMatchingShopkeepers(
        sender: Player, args: Array<out String>
    ) = getShopkeeperRegistry().allShopkeepers.filterIsInstance<TradingPlayerShopkeeper>().filter { shopkeeper ->
        sender.location.distance(shopkeeper.location!!) <= config.searchRadius && shopkeeper.offers.any {
            matchItem(
                args, it.resultItem
            )
        }
    }

    private fun displayShopkeepers(
        sender: Player,
        matchedShopkeepers: List<TradingPlayerShopkeeper>,
        material: Material,
        enchant: Enchantment?,
        args: Array<out String>
    ) {
        val mainScoreboard = Bukkit.getScoreboardManager().mainScoreboard
        val stockedTeam = mainScoreboard.getOrCreateTeam("shopkeepers_stocked", NamedTextColor.GREEN)
        val notStockedTeam = mainScoreboard.getOrCreateTeam("shopkeepers_not_stocked", NamedTextColor.RED)
        val shopkeeperStr = mutableListOf(config.messages.separator)

        matchedShopkeepers.forEach { shopkeeper ->
            val isStocked =
                shopkeeper.getTradingRecipes(sender).any { matchItem(args, it.resultItem) && !it.isOutOfStock }
            val offer = shopkeeper.offers.first { matchItem(args, it.resultItem) }
            val tradeInfo = buildTradeInfo(offer, material, enchant)

            shopkeeperStr.add(
                config.messages.shopkeeperInfo.formatShopkeeperInfo(shopkeeper, tradeInfo, material, isStocked)
            )
            shopkeeperStr.add(config.messages.separator)

            shopkeeper.location?.getNearbyEntities(0.5, 0.5, 0.5)?.forEach { entity ->
                (if (isStocked) stockedTeam else notStockedTeam).addEntry(entity.uniqueId.toString())
                entity.isGlowing = true
                Bukkit.getScheduler().runTaskLater(
                    ShopkeeperSearch.instance, Runnable { entity.isGlowing = false }, 200
                )
            }
        }

        sender.sendMessage(replaceColorCodes(shopkeeperStr.joinToString("\n")))
    }

    private fun buildTradeInfo(offer: TradeOffer, material: Material, enchant: Enchantment?) =
        "${offer.item1.amount}x ${formatMaterial(offer.item1.type)}" + (if (offer.hasItem2()) " + ${offer.item2!!.amount}x ${
            formatMaterial(offer.item2!!.type)
        }" else "") + " → ${offer.resultItem.amount}x ${
            if (material == Material.ENCHANTED_BOOK) formatEnchantedBook(enchant!!) else formatMaterial(offer.resultItem.type)
        }"

    private fun org.bukkit.scoreboard.Scoreboard.getOrCreateTeam(name: String, color: NamedTextColor) =
        getTeam(name) ?: registerNewTeam(name).apply { this.color(color) }

    private fun String.formatShopkeeperInfo(
        shopkeeper: TradingPlayerShopkeeper, tradeInfo: String, material: Material, isStocked: Boolean
    ) = replace("%stocked%", if (isStocked) "" else config.messages.outOfStock).replace(
        "%owner%", shopkeeper.ownerName
    ).replace("%x%", shopkeeper.x.toString()).replace("%y%", shopkeeper.y.toString())
        .replace("%z%", shopkeeper.z.toString()).replace("%trade%", tradeInfo)
        .replace("%item%", formatMaterial(material)).trimIndent()
}