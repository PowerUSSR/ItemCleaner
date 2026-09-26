package org.mineserver.itemcleaner;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedHashMap;
import java.util.Map;

// Раз в 2 часа убирает все брошенные предметы (Item-сущности) во всех мирах,
// чтобы они не копились и не грузили сервер. Перед очисткой предупреждает в чат.
public class ItemCleanerPlugin extends JavaPlugin {

    private static final int CYCLE_SECONDS = 2 * 60 * 60;

    // оставшиеся секунды до очистки -> текст предупреждения
    private static final Map<Integer, String> WARNINGS = new LinkedHashMap<>();
    static {
        WARNINGS.put(3600, "1 час");
        WARNINGS.put(1800, "30 минут");
        WARNINGS.put(900, "15 минут");
        WARNINGS.put(600, "10 минут");
        WARNINGS.put(300, "5 минут");
        WARNINGS.put(180, "3 минуты");
        WARNINGS.put(120, "2 минуты");
        WARNINGS.put(60, "1 минута");
    }

    private int remainingSeconds = CYCLE_SECONDS;
    private BukkitTask task;

    @Override
    public void onEnable() {
        remainingSeconds = CYCLE_SECONDS;
        task = Bukkit.getScheduler().runTaskTimer(this, this::tick, 20L, 20L);
        getLogger().info("ItemCleaner включён. Очистка предметов каждые 2 часа.");
    }

    @Override
    public void onDisable() {
        if (task != null) task.cancel();
    }

    private void tick() {
        remainingSeconds--;
        if (remainingSeconds <= 0) {
            int removed = cleanupItems();
            broadcast("§6[Очистка] §7Убрано §e" + removed + " §7брошенных предметов со всех миров.");
            remainingSeconds = CYCLE_SECONDS;
            announceCycleStart();
            return;
        }
        String label = WARNINGS.get(remainingSeconds);
        if (label != null) {
            broadcast("§6[Очистка] §7Брошенные предметы будут удалены через §e" + label + "§7.");
        }
    }

    private void announceCycleStart() {
        broadcast("§6[Очистка] §7Брошенные предметы будут удалены через §e2 часа§7.");
    }

    private int cleanupItems() {
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Item item : world.getEntitiesByClass(Item.class)) {
                item.remove();
                count++;
            }
        }
        return count;
    }

    private void broadcast(String message) {
        for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(message);
        getLogger().info(org.bukkit.ChatColor.stripColor(message));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("itemclean")) return false;

        if (!sender.hasPermission("itemclean.admin")) {
            sender.sendMessage("§cНедостаточно прав.");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("run")) {
            int removed = cleanupItems();
            sender.sendMessage("§a[ItemCleaner] §7Принудительная очистка: убрано §e" + removed + " §7предметов.");
            broadcast("§6[Очистка] §7Убрано §e" + removed + " §7брошенных предметов со всех миров.");
            remainingSeconds = CYCLE_SECONDS;
            announceCycleStart();
            return true;
        }

        sender.sendMessage("§a[ItemCleaner] §7До следующей очистки: §e" + formatDuration(remainingSeconds));
        return true;
    }

    private String formatDuration(int seconds) {
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;
        if (h > 0) return h + "ч " + m + "м " + s + "с";
        if (m > 0) return m + "м " + s + "с";
        return s + "с";
    }
}
