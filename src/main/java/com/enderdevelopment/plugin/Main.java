package com.enderdevelopment.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Team;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Main extends JavaPlugin {
    private final Map<String, Integer> scores = new HashMap<>();
    private boolean eventActive;
    private long eventEndsAt;
    private int eventTask = -1;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadState();
        getCommand("landrace").setExecutor(this);
        getCommand("raceclaim").setExecutor(this);
        scheduleWeeklyStart();
        if (eventActive) {
            long remaining = eventEndsAt - System.currentTimeMillis();
            if (remaining <= 0) {
                finishEvent();
            } else {
                scheduleEnd(remaining / 50L);
                announce(Component.text("The weekly land race is active again after restart.", NamedTextColor.GREEN));
            }
        }
        getLogger().info("Weekly land race enabled.");
    }

    @Override
    public void onDisable() {
        saveState();
        if (eventTask != -1) Bukkit.getScheduler().cancelTask(eventTask);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("raceclaim")) {
            claim(sender);
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            showStatus(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("top")) {
            showTop(sender);
            return true;
        }
        if (!sender.hasPermission("landrace.admin")) {
            sender.sendMessage(Component.text("You do not have permission to manage the race.", NamedTextColor.RED));
            return true;
        }
        if (args[0].equalsIgnoreCase("start")) {
            if (eventActive) sender.sendMessage(Component.text("A land race is already active.", NamedTextColor.RED));
            else startEvent();
            return true;
        }
        if (args[0].equalsIgnoreCase("end")) {
            if (!eventActive) sender.sendMessage(Component.text("No land race is active.", NamedTextColor.RED));
            else finishEvent();
            return true;
        }
        sender.sendMessage(Component.text("Usage: /landrace [start|end|status|top]", NamedTextColor.YELLOW));
        return true;
    }

    private void claim(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can register a chunk.", NamedTextColor.RED));
            return;
        }
        if (!eventActive) {
            player.sendMessage(Component.text("There is no active land race. Use /landrace status to check the schedule.", NamedTextColor.RED));
            return;
        }
        Team team = player.getScoreboard().getEntryTeam(player.getName());
        if (team == null) {
            player.sendMessage(Component.text("You must belong to a scoreboard team to compete.", NamedTextColor.RED));
            return;
        }
        Chunk chunk = player.getLocation().getChunk();
        World world = chunk.getWorld();
        String key = world.getUID() + ":" + chunk.getX() + ":" + chunk.getZ();
        if (scores.containsKey(key)) {
            player.sendMessage(Component.text("This chunk has already been registered in the race.", NamedTextColor.RED));
            return;
        }
        scores.put(key, 0);
        scores.merge(team.getName(), 1, Integer::sum);
        saveState();
        player.sendMessage(Component.text("Your team registered this chunk! Team score: " + scores.get(team.getName()), NamedTextColor.GREEN));
        announce(Component.text(team.getName() + " claimed a race chunk. Score: " + scores.get(team.getName()), NamedTextColor.GOLD));
    }

    private void startEvent() {
        scores.clear();
        eventActive = true;
        long duration = getConfig().getLong("event-duration-hours", 48);
        if (duration < 1) duration = 48;
        eventEndsAt = System.currentTimeMillis() + Duration.ofHours(duration).toMillis();
        saveState();
        scheduleEnd(Duration.ofHours(duration).toMillis() / 50L);
        announce(Component.text("The weekly land race has started! Use /raceclaim in an unclaimed chunk to score for your scoreboard team.", NamedTextColor.GREEN));
    }

    private void finishEvent() {
        eventActive = false;
        eventEndsAt = 0;
        if (eventTask != -1) Bukkit.getScheduler().cancelTask(eventTask);
        eventTask = -1;
        saveState();
        announce(Component.text("The weekly land race has ended.", NamedTextColor.GOLD));
        showTop(Bukkit.getConsoleSender());
    }

    private void scheduleEnd(long ticks) {
        if (eventTask != -1) Bukkit.getScheduler().cancelTask(eventTask);
        eventTask = Bukkit.getScheduler().runTaskLater(this, this::finishEvent, Math.max(1, ticks)).getTaskId();
    }

    private void scheduleWeeklyStart() {
        String zoneName = getConfig().getString("time-zone", "UTC");
        ZoneId zone;
        try {
            zone = ZoneId.of(zoneName);
        } catch (Exception exception) {
            getLogger().warning("Invalid time-zone in config.yml; using UTC.");
            zone = ZoneId.of("UTC");
        }
        int day = getConfig().getInt("start-day", 1);
        if (day < 1 || day > 7) day = 1;
        int hour = getConfig().getInt("start-hour", 18);
        if (hour < 0 || hour > 23) hour = 18;
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime next = now.with(DayOfWeek.of(day)).with(LocalTime.of(hour, 0));
        if (!next.isAfter(now)) next = next.plusWeeks(1);
        long delay = Math.max(1, Duration.between(now, next).toMillis() / 50L);
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!eventActive) startEvent();
            scheduleWeeklyStart();
        }, delay);
    }

    private void showStatus(CommandSender sender) {
        if (eventActive) {
            long minutes = Math.max(0, (eventEndsAt - System.currentTimeMillis()) / 60000L);
            sender.sendMessage(Component.text("Land race active. Time remaining: " + minutes / 60 + "h " + minutes % 60 + "m.", NamedTextColor.GREEN));
            showTop(sender);
        } else {
            sender.sendMessage(Component.text("No land race is active. Weekly schedule: day " + getConfig().getInt("start-day", 1) + " at " + getConfig().getInt("start-hour", 18) + ":00 " + getConfig().getString("time-zone", "UTC") + ".", NamedTextColor.YELLOW));
        }
    }

    private void showTop(CommandSender sender) {
        sender.sendMessage(Component.text("Land race leaderboard:", NamedTextColor.GOLD));
        if (scores.isEmpty()) {
            sender.sendMessage(Component.text("No teams have scored yet.", NamedTextColor.GRAY));
            return;
        }
        scores.entrySet().stream().filter(entry -> !entry.getKey().contains(":"))
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
                .limit(10).forEach(entry -> sender.sendMessage(Component.text(entry.getKey() + ": " + entry.getValue() + " chunks", NamedTextColor.YELLOW)));
    }

    private void announce(Component message) {
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(message));
        Bukkit.getConsoleSender().sendMessage(message);
    }

    private void loadState() {
        scores.clear();
        for (String key : getConfig().getConfigurationSection("scores") == null
                ? java.util.Set.<String>of() : getConfig().getConfigurationSection("scores").getKeys(false)) {
            scores.put(key, getConfig().getInt("scores." + key));
        }
        eventActive = getConfig().getBoolean("event-active", false);
        eventEndsAt = getConfig().getLong("event-ends-at", 0);
    }

    private void saveState() {
        getConfig().set("scores", scores);
        getConfig().set("event-active", eventActive);
        getConfig().set("event-ends-at", eventEndsAt);
        saveConfig();
    }
}
