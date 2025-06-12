package com.vitor;

import org.bukkit.Color;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;

public final class vExp extends JavaPlugin implements Listener, CommandExecutor {

    public FileConfiguration messages;

    private Set<UUID> pendingConfirmations = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        File messageFile = new File(getDataFolder(), "messages.yml");
        messages = YamlConfiguration.loadConfiguration(messageFile);

        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getPluginManager().registerEvents(vExp.this, vExp.this);
                getCommand("xp").setExecutor(vExp.this);
                getCommand("frasco").setExecutor(vExp.this);

                System.out.println("vExp loaded successfully!");
                System.out.println("Worlds found: " + Bukkit.getWorlds());
            }
        }.runTaskLater(this, 10L);
    }

    @Override
    public void onDisable() {
        System.out.println("vExp unloaded!");
    }

    private String getMessage(String path) {
        if (!messages.isSet(path)) return "§cMensagem '" + path + "' não encontrada.";
        return ChatColor.translateAlternateColorCodes('&', messages.getString(path));
    }

    private List<String> getMessageList(String path) {
        List<String> lines = messages.getStringList(path);
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            result.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        return result;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("frasco")) {
            return handleFrascoCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("xp")) {
            return handleXpCommand(sender, args);
        }
        return false;
    }

    private boolean handleFrascoCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vexp.use")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("only-players-bottle"));
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 1 && args[0].equalsIgnoreCase("all")) {
            int totalXp = getTotalXP(player);
            if (totalXp <= 0) {
                player.sendMessage(getMessage("not-enough-xp").replace("%xp%", String.valueOf(totalXp)));
                return true;
            }
            createXpBottle(player, totalXp);
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(getMessage("usage-frasco"));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[0]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage(getMessage("invalid-number"));
            return true;
        }

        int totalXp = getTotalXP(player);
        if (totalXp < amount) {
            player.sendMessage(getMessage("not-enough-xp").replace("%xp%", String.valueOf(totalXp)));
            return true;
        }

        createXpBottle(player, amount);
        player.sendMessage(getMessage("bottle-created").replace("%xp%", String.valueOf(amount)));
        return true;
    }

    private boolean handleXpCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vexp.use")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            return handleXpSelf(sender);
        }

        switch (args[0].toLowerCase()) {
            case "give":
                return handleXpGive(sender, args);
            case "clear":
                return handleXpClear(sender, args, false);
            case "confirm":
                return handleXpClear(sender, args, true);
            case "set":
                return handleXpSet(sender, args);
            default:
                return handleXpCheck(sender, args);
        }
    }

    private boolean handleXpSelf(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("only-players"));
            return true;
        }
        Player p = (Player) sender;
        int expToNext = (int) Math.round(p.getExpToLevel() * (1.0 - p.getExp()));

        for (String line : getMessageList("self-xp")) {
            p.sendMessage(line
                    .replace("%nivel%", String.valueOf(p.getLevel()))
                    .replace("%xp%", String.valueOf(getTotalXP(p)))
                    .replace("%faltam%", String.valueOf(expToNext)));
        }
        return true;
    }

    private boolean handleXpCheck(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(getMessage("usage"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(getMessage("player-not-found"));
            return true;
        }
        int expToNext = (int) Math.round(target.getExpToLevel() * (1.0 - target.getExp()));
        for (String line : getMessageList("target-xp")) {
            sender.sendMessage(line
                    .replace("%jogador%", target.getName())
                    .replace("%nivel%", String.valueOf(target.getLevel()))
                    .replace("%xp%", String.valueOf(getTotalXP(target)))
                    .replace("%faltam%", String.valueOf(expToNext)));
        }
        return true;
    }

    private boolean handleXpGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vexp.staff")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length != 4) {
            sender.sendMessage(getMessage("usage-xp-give"));
            return true;
        }

        String targetName = args[1];
        String amountStr = args[2];
        String type = args[3];

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(getMessage("player-not-found"));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(amountStr);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage(getMessage("invalid-number"));
            return true;
        }

        if (type.equalsIgnoreCase("level")) {
            target.giveExpLevels(amount);
            sender.sendMessage(getMessage("xp-given-levels")
                    .replace("%jogador%", target.getName())
                    .replace("%quantia%", String.valueOf(amount)));
            target.sendMessage(getMessage("xp-received-levels")
                    .replace("%quantia%", String.valueOf(amount)));
        } else if (type.equalsIgnoreCase("points")) {
            target.giveExp(amount);
            sender.sendMessage(getMessage("xp-given-points")
                    .replace("%jogador%", target.getName())
                    .replace("%quantia%", String.valueOf(amount)));
            target.sendMessage(getMessage("xp-received-points")
                    .replace("%quantia%", String.valueOf(amount)));
        } else {
            sender.sendMessage(getMessage("invalid-type"));
            return true;
        }
        return true;
    }

    private boolean handleXpSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vexp.staff")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length != 4) {
            sender.sendMessage(getMessage("xp-set-usage"));
            return true;
        }

        String targetName = args[1];
        String amountStr = args[2];
        String type = args[3].toLowerCase();

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(getMessage("player-not-found"));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(amountStr);
            if (amount < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage(getMessage("invalid-number"));
            return true;
        }

        if (type.equals("level")) {
            target.setLevel(amount);
            target.setExp(0.0f);
        } else if (type.equals("points")) {
            setTotalXP(target, amount);
        } else {
            sender.sendMessage(getMessage("invalid-type"));
            return true;
        }

        sender.sendMessage(getMessage("xp-set-success")
                .replace("%jogador%", target.getName())
                .replace("%quantia%", String.valueOf(amount))
                .replace("%tipo%", type));
        return true;
    }

    private boolean handleXpClear(CommandSender sender, String[] args, boolean confirmed) {
        if (!sender.hasPermission("vexp.staff")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(getMessage("usage-xp-clear"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(getMessage("player-not-found"));
            return true;
        }

        if (!confirmed) {
            pendingConfirmations.add(target.getUniqueId());
            sender.sendMessage(getMessage("xp-clear-confirm"));
            Bukkit.getScheduler().runTaskLater(this, () -> {
                pendingConfirmations.remove(target.getUniqueId());
            }, 600);

            return true;
        }

        if (!pendingConfirmations.contains(target.getUniqueId())) {
            sender.sendMessage(getMessage("xp-clear-no-pending"));
            return true;
        }

        clearPlayerXP(target);
        pendingConfirmations.remove(target.getUniqueId());
        sender.sendMessage(getMessage("xp-cleared").replace("%jogador%", target.getName()));
        return true;
    }

    private void clearPlayerXP(Player player) {
        player.setExp(0);
        player.setLevel(0);
    }

    private void createXpBottle(Player player, int amount) {
        ItemStack bottle = new ItemStack(Material.EXP_BOTTLE);
        ItemMeta meta = bottle.getItemMeta();

        String displayName = getMessage("bottle-displayname");
        meta.setDisplayName(displayName);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(getMessage("bottle-lore-value").replace("%amount%", String.valueOf(amount)));

        if (getConfig().getBoolean("allownicknames", false)) {
            lore.add(getMessage("bottle-lore-created-by").replace("%player%", player.getName()));
        }

        meta.setLore(lore);
        bottle.setItemMeta(meta);
        player.getInventory().addItem(bottle);

        int currentTotal = getTotalXP(player);
        int newTotal = currentTotal - amount;
        setTotalXP(player, newTotal);

        player.sendMessage(getMessage("bottle-created").replace("%xp%", String.valueOf(amount)));
    }

    private int getTotalXP(Player player) {
        int level = player.getLevel();
        float progress = player.getExp();
        int expToNextLevel = getExpToLevel(level);
        int expFromProgress = Math.round(progress * expToNextLevel);

        int totalExp = expFromProgress;
        for (int i = 0; i < level; i++) {
            totalExp += getExpToLevel(i);
        }
        return totalExp;
    }

    private int getExpToLevel(int level) {
        if (level >= 0 && level <= 15) {
            return 2 * level + 7;
        } else if (level >= 16 && level <= 30) {
            return 5 * level - 38;
        } else {
            return 9 * level - 158;
        }
    }

    private void setTotalXP(Player player, int totalXP) {
        int level = 0;
        int remaining = totalXP;
        while (true) {
            int xpToNext = getExpToLevel(level);
            if (remaining >= xpToNext) {
                remaining -= xpToNext;
                level++;
            } else {
                break;
            }
        }
        float progress = (remaining == 0) ? 0.0f : (float) remaining / (float) getExpToLevel(level);
        player.setLevel(level);
        player.setExp(progress);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.hasItem()) return;
        ItemStack item = event.getItem();
        if (item.getType() != Material.EXP_BOTTLE) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;
        if (!meta.getDisplayName().equals(getMessage("bottle-displayname"))) return;

        int xpAmount = 0;
        if (meta.hasLore()) {
            String valuePrefix = ChatColor.stripColor(getMessage("bottle-lore-value").split("%amount%")[0]);

            for (String line : meta.getLore()) {
                if (ChatColor.stripColor(line).startsWith(valuePrefix)) {
                    String numberStr = ChatColor.stripColor(line).replace(valuePrefix, "").trim();
                    xpAmount = Integer.parseInt(numberStr);
                }
            }

            /* Old Code Lines!
            for (String line : meta.getLore()) {
                String plain = ChatColor.stripColor(line);
                if (plain.startsWith("Quantia armazenada:")) {
                    try {
                        xpAmount = Integer.parseInt(plain.replace("Quantia armazenada:", "").trim());
                        break;
                    } catch (NumberFormatException ignored) {}
                }
            } */
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        if (xpAmount > 0) {
            int currentTotal = getTotalXP(player);
            int newTotal = currentTotal + xpAmount;
            setTotalXP(player, newTotal);

            player.sendMessage(getMessage("bottle-used").replace("%xp%", String.valueOf(xpAmount)));
            ItemStack[] contents = player.getInventory().getContents();
            for (int i = 0; i < contents.length; i++) {
                ItemStack invItem = contents[i];
                if (invItem != null && invItem.isSimilar(item)) {
                    if (invItem.getAmount() > 1) {
                        invItem.setAmount(invItem.getAmount() - 1);
                    } else {
                        player.getInventory().setItem(i, null);
                    }
                    break;
                }
            }
            player.updateInventory();
        } else {
            player.sendMessage(getMessage("bottle-invalid"));
        }
    }

    @EventHandler
    public void vanillaXPBottle(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getItemInHand();

        if (item == null || item.getType() != Material.EXP_BOTTLE) {
            return;
        }

        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().getDisplayName().equals(getMessage("bottle-displayname"))) {
            return;
        }

        event.setCancelled(true);

        int xpToGive = (int) (Math.random() * 8) + 3;

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.setItemInHand(null);
        }

        player.giveExp(xpToGive);
    }

    @EventHandler
    public void onPlayerDeathPermission(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!player.hasPermission("vexp.safexp")) return;
        event.setKeepLevel(true);
        event.setDroppedExp(0);
    }

    @EventHandler
    public void onPlayerDeathWorld(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String worldName = player.getWorld().getName();
        List<String> safeWorlds = getConfig().getStringList("worlds");

        if (safeWorlds.contains(worldName)) {
            event.setKeepLevel(true);
            event.setDroppedExp(0);
        }
    }
}