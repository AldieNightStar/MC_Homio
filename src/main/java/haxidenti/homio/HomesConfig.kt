package haxidenti.homio

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.World
import haxidenti.homio.HomesConfig
import org.bukkit.Bukkit
import org.bukkit.Location
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

    companion object {

        private const val maxCounter = 10

        private fun noDots(s: String): String {
            return s.replace(".", "_")
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