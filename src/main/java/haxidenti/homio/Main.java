package haxidenti.homio;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.regex.Pattern;

public class Main extends JavaPlugin implements Listener {

    private HomesConfig config;

    private static final String MAGIC_STICK_PREFIX = ChatColor.YELLOW + "Magic stick: " + ChatColor.GREEN;

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
                // ====================
                // HOME Command
                // ====================
                Location location = config.getHomeFor(player.getName());
                if (location == null) {
                    sender.sendMessage(ChatColor.RED + "No home!");
                    return true;
                }
                player.teleport(location);
                playTeleportSoundFor(player);
                return true;
            } else if (name.equalsIgnoreCase("sethome")) {
                // ====================
                // SETHOME Command
                // ====================
                Location location = player.getLocation();
                config.setHomeFor(player.getName(), location);
                sender.sendMessage(ChatColor.YELLOW + "Welcome to your home!");
                return true;
            } else if (name.equalsIgnoreCase("tpoint")) {
                if (args.length < 2) {
                    try {
                        // ========================================
                        // Display Help and list of teleport points
                        // ========================================
                        Set<String> tpoints = config.getTPointListOf(player.getName());

                        sender.sendMessage(ChatColor.GRAY + "To create/modify: " + ChatColor.AQUA + "/tpoint set NAME");
                        sender.sendMessage(ChatColor.GRAY + "To remove: " + ChatColor.AQUA + "/tpoint rem NAME");
                        sender.sendMessage(ChatColor.GRAY + "To teleport: " + ChatColor.AQUA + "/tpoint tp NAME");
                        sender.sendMessage(ChatColor.GRAY + "Magic stick: " + ChatColor.AQUA + "/tpoint stick NAME" + ChatColor.GRAY + " or " + ChatColor.AQUA + "/tpoint stick *");
                        sender.sendMessage(ChatColor.GRAY + "Next portal LVL cost: " + ChatColor.GOLD + getRequirementMultiplier(tpoints.size()));
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

                    if (!isValidName(pointName)) {
                        sender.sendMessage(ChatColor.RED + "Point name is invalid!");
                        return true;
                    }

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
                    } else if (cmd.equalsIgnoreCase("set")) {
                        // =================
                        // New point
                        // =================
                        int count = config.getTPointListOf(player.getName()).size();

                        Location prevPoint = config.getTPointFor(player.getName(), pointName);

                        if (prevPoint == null) {
                            // ============================
                            // NEW POINT
                            // ============================
                            int levelRequired = getRequirementMultiplier(count);
                            int playerLevel = player.getLevel();
                            if (playerLevel < levelRequired) {
                                sender.sendMessage(ChatColor.RED + "Level is too low. Required level: " + ChatColor.GREEN + levelRequired);
                                sender.sendMessage(ChatColor.RED + "Point cannot be set!");
                                return true;
                            }
                            playerLevel -= levelRequired;
                            player.setLevel(playerLevel);
                            config.setTPointFor(player.getName(), pointName, player.getLocation());
                            sender.sendMessage(ChatColor.YELLOW + "New point set!");
                            return true;
                        } else {
                            // ============================
                            // MODIFY POINT
                            // ============================
                            config.setTPointFor(player.getName(), pointName, player.getLocation());
                            sender.sendMessage(ChatColor.YELLOW + "Point updated!");
                        }
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
                    } else if (cmd.equalsIgnoreCase("stick")) {
                        // =================
                        // Give magic stick
                        // =================
                        PlayerInventory inventory = player.getInventory();

                        ItemStack item = inventory.getItemInMainHand();
                        if (!item.getType().equals(Material.STICK)) {
                            sender.sendMessage(ChatColor.RED + "You need to keep STICK in your main hand!");
                            return true;
                        }

                        boolean isSingleItem = item.getAmount() == 1;

                        Location loc = config.getTPointFor(player.getName(), pointName);
                        if (pointName.equals("*")) {
                            loc = player.getLocation();
                        }
                        if (loc == null) {
                            sender.sendMessage(ChatColor.RED + "No such point!");
                            return true;
                        }

                        ItemStack magicStick = new ItemStack(Material.STICK);
                        {
                            ItemMeta meta = magicStick.getItemMeta();
                            if (pointName.equals("*")) {
                                meta.setDisplayName(MAGIC_STICK_PREFIX + generateLocationString(loc));
                            } else {
                                meta.setDisplayName(MAGIC_STICK_PREFIX + pointName);
                            }
                            meta.addEnchant(Enchantment.BINDING_CURSE, 1, false);
                            magicStick.setItemMeta(meta);
                        }

                        inventory.setItemInMainHand(magicStick);
                        if (!isSingleItem) {
                            player.dropItem(true);
                            item.setAmount(item.getAmount() - 1);
                            inventory.setItemInMainHand(item);
                        }
                        player.updateInventory();

                        sender.sendMessage(ChatColor.YELLOW + "Magic stick is got!");

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

    private static int getRequirementMultiplier(int count) {
        if (count < 5) {
            return 0;
        }
        int mul = (count - 4) * 2;
        if (mul > 20) mul = 20;
        return mul;
    }

    private boolean isValidName(String name) {
        return name.length() >= 1 && name.length() < 15;
    }

    private Location parseLocationString(String playerName, String positionName) {
        if (positionName.startsWith("*")) {
            String[] arr = positionName.substring(1).split(Pattern.quote("|"));
            if (arr.length != 4) {
                return null;
            }
            try {
                String worldName = arr[0];
                int posX = Integer.parseInt(arr[1]);
                int posY = Integer.parseInt(arr[2]);
                int posZ = Integer.parseInt(arr[3]);

                return new Location(Bukkit.getWorld(worldName), posX, posY, posZ);
            } catch (Exception e) {
                return null;
            }
        } else {
            return config.getTPointFor(playerName, positionName);
        }
    }

    private String generateLocationString(Location location) {
        return "*" + location.getWorld().getName() + "|" + location.getBlockX() + "|" + location.getBlockY() + "|" + location.getBlockZ();
    }

    @EventHandler
    public void magicStickEvent(PlayerInteractEvent event) {
        Action action = event.getAction();
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!item.getType().equals(Material.STICK)) return;
        if (action.equals(Action.RIGHT_CLICK_AIR) || action.equals(Action.RIGHT_CLICK_BLOCK)) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;
            String displayName = meta.getDisplayName();

            if (!displayName.startsWith(MAGIC_STICK_PREFIX)) return;
            String locationString = displayName.substring(MAGIC_STICK_PREFIX.length());

            Location location = parseLocationString(player.getName(), locationString);
            if (location == null) {
                player.sendMessage(ChatColor.RED + "Location is undefined for this player or something went wrong!");
                return;
            }

            player.teleport(location);
            playTeleportSoundFor(player);
        }
    }

}