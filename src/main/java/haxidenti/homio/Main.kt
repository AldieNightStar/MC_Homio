package haxidenti.homio

import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.block.Sign
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
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
                        config!!.giveMagicStickFor(sender, pointName, false)
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
            // If block is a sign - write coordinates to the sign
            val sign: Sign? = event.clickedBlock?.asSign()
            if (sign != null) {
                // Write coords
                val lines = sign.lines
                if (lines.size < 2) {
                    player.sendMessage(ChatColor.RED.toString() + "Lines should be 2 or more")
                    return
                }
                sign.setLine(0, TPOINT_SIGN_PREFIX + meta.displayName.substring(TPOINT_STICK_PREFIX.length))
                sign.setLine(1, locationString)
                sign.update()

                // Play sound
                player.location.world?.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 1F)
            } else {
                player.teleport(location)
                playTeleportSoundFor(player)
            }
        }
    }

    @EventHandler
    fun magicSignEvent(event: PlayerInteractEvent) {
        val action = event.action
        val inventory = event.player.inventory
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            val sign = event.clickedBlock?.asSign() ?: return
            if (sign.lines.size < 2) return
            if (!sign.lines[0].startsWith(TPOINT_SIGN_PREFIX)) return
            val location = parseLocationString(sign.lines[1])
            if (location !== null) {
                if (inventory.itemInMainHand.type == Material.STICK) return
                event.player.teleport(location)
                playTeleportSoundFor(event.player)
            }
        }
    }

    fun Block.asSign(): Sign? {
        if (state is Sign) {
            return state as Sign
        }
        return null
    }

    companion object {
        val TPOINT_STICK_PREFIX = ChatColor.YELLOW.toString() + "TPoint Stick: " + ChatColor.GREEN
        val TPOINT_SIGN_PREFIX = ChatColor.YELLOW.toString() + "T-Sign: " + ChatColor.GREEN

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