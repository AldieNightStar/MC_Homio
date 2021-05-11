package haxidenti.homio

import org.bukkit.configuration.file.YamlConfiguration
import haxidenti.homio.HomesConfig
import org.bukkit.*
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.io.File
import java.lang.Exception
import java.lang.NullPointerException
import java.lang.RuntimeException
import java.util.logging.Level

class HomesConfig(dataFolder: File) {

    private val config: YamlConfiguration
    private val dataFolder: File
    private var counter = 10

    fun setHomeFor(name: String, location: Location) {
        val world = location.world ?: return
        val playerDataPrefix = "homes." + noDots(name) + "."
        config[playerDataPrefix + "world"] = world.name
        config[playerDataPrefix + "x"] = location.blockX
        config[playerDataPrefix + "y"] = location.blockY
        config[playerDataPrefix + "z"] = location.blockZ
        countAndSave()
    }

    fun getHomeFor(name: String): Location? {
        return try {
            val playerDataPrefix = "homes." + noDots(name) + "."
            val world = config.getString(playerDataPrefix + "world") ?: return null
            val x = config.getInt(playerDataPrefix + "x")
            val y = config.getInt(playerDataPrefix + "y")
            val z = config.getInt(playerDataPrefix + "z")
            Location(Bukkit.getWorld(world), x.toDouble(), y.toDouble(), z.toDouble())
        } catch (e: Exception) {
            null
        }
    }

    fun setTPointFor(playerName: String, pointName: String, location: Location) {
        try {
            val dataPrefix = "points." + noDots(playerName) + "." + noDots(pointName) + "."
            val world = location.world ?: return
            config[dataPrefix + "world"] = world.name
            config[dataPrefix + "x"] = location.blockX
            config[dataPrefix + "y"] = location.blockY
            config[dataPrefix + "z"] = location.blockZ
            countAndSave()
        } catch (ignored: Exception) {
        }
    }

    fun getTPointFor(playerName: String, pointName: String): Location? {
        return try {
            val dataPrefix = "points." + noDots(playerName) + "." + noDots(pointName) + "."
            val world = config.getString(dataPrefix + "world") ?: return null
            val x = config.getInt(dataPrefix + "x")
            val y = config.getInt(dataPrefix + "y")
            val z = config.getInt(dataPrefix + "z")
            Location(Bukkit.getWorld(world), x.toDouble(), y.toDouble(), z.toDouble())
        } catch (e: Exception) {
            null
        }
    }

    fun removeTPointFor(playerName: String, pointName: String) {
        try {
            config["points." + noDots(playerName) + "." + noDots(pointName)] = null
        } catch (ignored: Exception) {
        }
    }

    private fun countAndSave() {
        counter -= 1
        if (counter < 0) {
            counter = maxCounter
            save()
        }
    }

    fun save() {
        try {
            config.save(File(dataFolder, "homio.yml"))
        } catch (e: Exception) {
            Bukkit.getLogger().log(Level.SEVERE, "Can't save file \"homio.yml\": " + e.message)
        }
    }

    fun getTPointListOf(playerName: String): Set<String> {
        return try {
            val dataPrefix = "points." + noDots(playerName)
            config.getConfigurationSection(dataPrefix)!!.getKeys(false)
        } catch (e: NullPointerException) {
            emptySet()
        }
    }

    fun giveMagicStickFor(player: Player, pointNameStr: String, free: Boolean): Boolean {
        var pointName = pointNameStr
        val inventory = player.inventory
        val item = inventory.itemInMainHand
        if (!free && item.type != Material.STICK) {
            player.sendMessage(ChatColor.RED.toString() + "You need to keep STICK in your main hand!")
            return true
        }
        val isSingleItem = item.amount == 1
        val loc: Location?
        if (pointName.startsWith("*")) {
            loc = player.location
        } else {
            loc = getTPointFor(player.name, pointName)
        }
        if (loc == null) {
            player.sendMessage(ChatColor.RED.toString() + "No such point!")
            return true
        }
        val magicStick = ItemStack(Material.STICK)
        run {
            val meta = magicStick.itemMeta!!
            if (pointName.startsWith("*")) {
                pointName = pointName.substring(1)
                if (pointName.isEmpty()) {
                    pointName = "Unnamed location"
                }
            }
            meta.setDisplayName(Main.TPOINT_STICK_PREFIX + pointName)
            meta.lore = mutableListOf(generateLocationString(loc))
            meta.addEnchant(Enchantment.BINDING_CURSE, 1, false)
            magicStick.setItemMeta(meta)
        }
        if (!free) {
            inventory.setItemInMainHand(magicStick)
            if (!isSingleItem) {
                player.dropItem(true)
                item.amount = item.amount - 1
                inventory.setItemInMainHand(item)
            }
            player.updateInventory()
        } else {
            val location = player.location
            location.world!!.dropItem(location, magicStick)
        }
        player.sendMessage(ChatColor.YELLOW.toString() + "Magic stick is got!")
        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
        return true
    }

    companion object {

        private const val maxCounter = 10

        private fun noDots(s: String): String {
            return s.replace(".", "_")
        }

        fun generateLocationString(location: Location): String {
            return "*" + location.world!!.name + "|" + location.blockX + "|" + location.blockY + "|" + location.blockZ
        }
    }

    init {
        if (!dataFolder.exists()) {
            if (!dataFolder.mkdir()) {
                throw RuntimeException("Homio config folder did not created successfully!")
            }
        }
        config = YamlConfiguration.loadConfiguration(File(dataFolder, "homio.yml"))
        this.dataFolder = dataFolder
    }
}