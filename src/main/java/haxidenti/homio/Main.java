package haxidenti.homio;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Main extends JavaPlugin implements Listener {

    private HomesConfig config;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        config = new HomesConfig(getDataFolder());

    }

    @Override
    public void onDisable() {
        config.save();
    }

    @Override
    public boolean onCommand(CommandSender sender,
                             Command command,
                             String label,
                             String[] args) {
        String name = command.getName();

        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (name.equalsIgnoreCase("home")) {
                Location location = config.getHomeFor(player.getName());
                if (location == null) {
                    sender.sendMessage(ChatColor.RED + "No home!");
                    return true;
                }
                player.teleport(location);
                playTeleportSoundFor(player);
                return true;
            } else if (name.equalsIgnoreCase("sethome")) {
                Location location = player.getLocation();
                config.setHomeFor(player.getName(), location);
                sender.sendMessage(ChatColor.YELLOW + "Welcome to your home!");
                return true;
            } else if (name.equalsIgnoreCase("tpoint")) {
                if (args.length < 2) {
                    try {
                        // Display Help and list of teleport points
                        Set<String> tpoints = config.getTPointListOf(player.getName());

                        sender.sendMessage(ChatColor.GRAY + "To create new: " + ChatColor.AQUA + "/tpoint new NAME");
                        sender.sendMessage(ChatColor.GRAY + "To remove: " + ChatColor.AQUA + "/tpoint rem NAME");
                        sender.sendMessage(ChatColor.GRAY + "To teleport: " + ChatColor.AQUA + "/tpoint tp NAME");
                        sender.sendMessage("");

                        StringBuilder sb = new StringBuilder();
                        tpoints.forEach(t -> sb.append(t).append(" "));

                        sender.sendMessage(ChatColor.YELLOW + "Teleport Points: " + ChatColor.GREEN + sb.toString());

                        return true;
                    } catch (Exception ignored) {
                    }
                } else {
                    String cmd = args[0];
                    String pointName = args[1];

                    if (cmd.equalsIgnoreCase("tp")) {
                        // =================
                        // TP to point
                        // =================
                        Location location = config.getTPointFor(player.getName(), pointName);
                        if (location == null) {
                            sender.sendMessage(ChatColor.RED + "No such point!");
                            return true;
                        }
                        player.teleport(location);
                        playTeleportSoundFor(player);
                        return true;
                    } else if (cmd.equalsIgnoreCase("new")) {
                        // =================
                        // New point
                        // =================
                        config.setTPointFor(player.getName(), pointName, player.getLocation());
                        sender.sendMessage(ChatColor.YELLOW + "New point set!");
                        return true;
                    } else if (cmd.equalsIgnoreCase("rem")) {
                        // =================
                        // Remove point
                        // =================
                        Location loc = config.getTPointFor(player.getName(), pointName);
                        if (loc != null) {
                            config.removeTPointFor(player.getName(), pointName);
                            sender.sendMessage(ChatColor.YELLOW + "Deleted point!");
                        } else {
                            sender.sendMessage(ChatColor.RED + "No such point");
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void playTeleportSoundFor(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 1f, 1f);
    }
}