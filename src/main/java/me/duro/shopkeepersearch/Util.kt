package me.duro.shopkeepersearch

import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.meta.EnchantmentStorageMeta

class Util {
    companion object {
        fun replaceColorCodes(input: String): Component {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(input)
        }

        fun formatMaterial(material: Material): String {
            val materialName = material.name
            return formatString(materialName)
        }

        fun formatEnchantedBook(enchant: Enchantment): String {
            return formatString("Enchanted_Book (${formatString(enchant.key.key)})")
        }

        private fun formatString(str: String): String {
            return str.split("_").joinToString(" ") { s ->
                s.lowercase().replaceFirstChar {
                    it.uppercaseChar()
                }
            }
        }

        fun matchItem(search: Array<out String>, itemStack: UnmodifiableItemStack): Boolean {
            if (search.isEmpty()) return false

            val itemType = search[0]
            if (!itemType.equals(itemStack.type.name, true)) return false

            if (itemStack.type == Material.ENCHANTED_BOOK) {
                val enchantStorage = itemStack.itemMeta as? EnchantmentStorageMeta
                val enchant = Enchantment.getByKey(NamespacedKey.minecraft(search[1]))

                if (enchantStorage != null && enchant != null) {
                    return enchantStorage.storedEnchants.keys.contains(enchant)
                }
            }

            return true
        }
    }
}