package haxidenti.homio

import org.bukkit.*
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.function.Consumer

class Main : JavaPlugin(), Listener {
    private var config: HomesConfig? = null
    override fun onEnable() {
        server.pluginManager.registerEvents(this, this)
        config = HomesConfig(dataFolder)
    }

    override fun onDisable() {
        config!!.save()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        val name = command.name
        if (sender is Player) {
            if (name.equals("home", ignoreCase = true)) {
                // ====================
                // HOME Command
                // ====================
                val location = config!!.getHomeFor(sender.name)
                if (location == null) {
                    sender.sendMessage(ChatColor.RED.toString() + "No home!")
                    return true
                }
                sender.teleport(location)
                playTeleportSoundFor(sender)
                return true
            } else if (name.equals("sethome", ignoreCase = true)) {
                // ====================
                // SETHOME Command
                // ====================
                val location = sender.location
                config!!.setHomeFor(sender.name, location)
                sender.sendMessage(ChatColor.YELLOW.toString() + "Welcome to your home!")
                return true
            } else if (name.equals("tpoint", ignoreCase = true)) {
                if (args.size < 2) {
                    try {
                        // ========================================
                        // Display Help and list of teleport points
                        // ========================================
                        val tpoints = config!!.getTPointListOf(sender.name)
                        sender.sendMessage(ChatColor.GRAY.toString() + "To create/modify: " + ChatColor.AQUA + "/tpoint set NAME")
                        sender.sendMessage(ChatColor.GRAY.toString() + "To remove: " + ChatColor.AQUA + "/tpoint rem NAME")
                        sender.sendMessage(ChatColor.GRAY.toString() + "To teleport: " + ChatColor.AQUA + "/tpoint tp NAME")
                        sender.sendMessage(ChatColor.GRAY.toString() + "Magic stick: " + ChatColor.AQUA + "/tpoint stick NAME")
                        sender.sendMessage(ChatColor.GRAY.toString() + "Magic stick: " + ChatColor.AQUA + "/tpoint stick *NAME")
                        sender.sendMessage(
                                ChatColor.GRAY.toString() + "Next portal LVL cost: " + ChatColor.GOLD + getRequirementMultiplier(
                                        tpoints.size
                                )
                        )
                        sender.sendMessage("")
                        val sb = StringBuilder()
                        tpoints.forEach(Consumer { t: String? -> sb.append(t).append(" ") })
                        sender.sendMessage(ChatColor.YELLOW.toString() + "Teleport Points: " + ChatColor.GREEN + sb.toString())
                        return true
                    } catch (ignored: Exception) {
                    }
                } else {
                    val cmd = args[0]
                    var pointName = args[1]
                    if (!isValidName(pointName)) {
                        sender.sendMessage(ChatColor.RED.toString() + "Point name is invalid!")
                        return true
                    }
                    if (cmd.equals("tp", ignoreCase = true)) {
                        // =================
                        // TP to point
                        // =================
                        val location = config!!.getTPointFor(sender.name, pointName)
                        if (location == null) {
                            sender.sendMessage(ChatColor.RED.toString() + "No such point!")
                            return true
                        }
                        sender.teleport(location)
                        playTeleportSoundFor(sender)
                        return true
                    } else if (cmd.equals("set", ignoreCase = true)) {
                        // =================
                        // New point
                        // =================
                        val count = config!!.getTPointListOf(sender.name).size
                        val prevPoint = config!!.getTPointFor(sender.name, pointName)
                        if (prevPoint == null) {
                            // ============================
                            // NEW POINT
                            // ============================
                            val levelRequired = getRequirementMultiplier(count)
                            var playerLevel = sender.level
                            if (playerLevel < levelRequired) {
                                sender.sendMessage(ChatColor.RED.toString() + "Level is too low. Required level: " + ChatColor.GREEN + levelRequired)
                                sender.sendMessage(ChatColor.RED.toString() + "Point cannot be set!")
                                return true
                            }
                            playerLevel -= levelRequired
                            sender.level = playerLevel
                            config!!.setTPointFor(sender.name, pointName, sender.location)
                            sender.sendMessage(ChatColor.YELLOW.toString() + "New point set!")
                            return true
                        } else {
                            // ============================
                            // MODIFY POINT
                            // ============================
                            config!!.setTPointFor(sender.name, pointName, sender.location)
                            sender.sendMessage(ChatColor.YELLOW.toString() + "Point updated!")
                        }
                        return true
                    } else if (cmd.equals("rem", ignoreCase = true)) {
                        // =================
                        // Remove point
                        // =================
                        val loc = config!!.getTPointFor(sender.name, pointName)
                        if (loc != null) {
                            config!!.removeTPointFor(sender.name, pointName)
                            sender.sendMessage(ChatColor.YELLOW.toString() + "Deleted point!")
                        } else {
                            sender.sendMessage(ChatColor.RED.toString() + "No such point")
                        }
                        return true
                    } else if (cmd.equals("stick", ignoreCase = true)) {
                        // =================
                        // Give tpoint stick
                        // =================
                        val inventory = sender.inventory
                        val item = inventory.itemInMainHand
                        if (item.type != Material.STICK) {
                            sender.sendMessage(ChatColor.RED.toString() + "You need to keep STICK in your main hand!")
                            return true
                        }
                        val isSingleItem = item.amount == 1
                        val loc: Location?
                        if (pointName.startsWith("*")) {
                            loc = sender.location
                        } else {
                            loc = config!!.getTPointFor(sender.name, pointName)
                        }
                        if (loc == null) {
                            sender.sendMessage(ChatColor.RED.toString() + "No such point!")
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
                            meta.setDisplayName(TPOINT_STICK_PREFIX + pointName)
                            meta.lore = mutableListOf(generateLocationString(loc))
                            meta.addEnchant(Enchantment.BINDING_CURSE, 1, false)
                            magicStick.setItemMeta(meta)
                        }
                        inventory.setItemInMainHand(magicStick)
                        if (!isSingleItem) {
                            sender.dropItem(true)
                            item.amount = item.amount - 1
                            inventory.setItemInMainHand(item)
                        }
                        sender.updateInventory()
                        sender.sendMessage(ChatColor.YELLOW.toString() + "Magic stick is got!")
                        sender.playSound(sender.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun isValidName(name: String): Boolean {
        return name.length >= 1 && name.length < 15
    }

    private fun parseLocationString(positionName: String): Location? {
        val arr = positionName.substring(1).split("|")
        if (arr.size != 4) {
            return null
        }
        return try {
            val worldName = arr[0]
            val posX = arr[1].toInt()
            val posY = arr[2].toInt()
            val posZ = arr[3].toInt()
            Location(Bukkit.getWorld(worldName), posX.toDouble(), posY.toDouble(), posZ.toDouble())
        } catch (e: Exception) {
            null
        }
    }

    private fun generateLocationString(location: Location): String {
        return "*" + location.world!!.name + "|" + location.blockX + "|" + location.blockY + "|" + location.blockZ
    }

    @EventHandler
    fun magicStickEvent(event: PlayerInteractEvent) {
        val action = event.action
        val player = event.player
        val item = player.inventory.itemInMainHand
        if (item.type != Material.STICK) return
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            val meta = item.itemMeta ?: return
            if (!meta.displayName.startsWith(TPOINT_STICK_PREFIX)) return
            if (!meta.hasLore()) return
            val locationString = meta.lore?.get(0) ?: return
            val location = parseLocationString(locationString)
            if (location == null) {
                player.sendMessage(ChatColor.RED.toString() + "Location is undefined for this player or something went wrong!")
                return
            }
            player.teleport(location)
            playTeleportSoundFor(player)
        }
    }

    companion object {
        private val TPOINT_STICK_PREFIX = ChatColor.YELLOW.toString() + "TPoint Stick: " + ChatColor.GREEN
        private fun playTeleportSoundFor(player: Player) {
            player.playSound(player.location, Sound.ENTITY_ENDER_DRAGON_SHOOT, 1f, 1f)
        }

        private fun getRequirementMultiplier(count: Int): Int {
            if (count < 5) {
                return 0
            }
            var mul = (count - 4) * 2
            if (mul > 20) mul = 20
            return mul
        }
    }
}