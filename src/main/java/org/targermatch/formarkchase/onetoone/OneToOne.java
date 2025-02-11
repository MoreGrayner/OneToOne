package org.targermatch.formarkchase.onetoone;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class OneToOne extends JavaPlugin implements Listener {
    private final List<Player> players = new ArrayList<>();
    private final List<List<Player>> groups = new ArrayList<>();
    private Lock[] slotLocks;

    @Override
    public void onEnable() {
        slotLocks = new Lock[41];
        for (int i = 0; i < slotLocks.length; i++) {
            slotLocks[i] = new ReentrantLock();
        }
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("north").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("north")) {
            createGroups();
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        players.add(event.getPlayer());
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        players.remove(player);
        for (List<Player> group : groups) {
            if (group.contains(player)) {
                group.remove(player);
                break;
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        List<Player> group = getGroupByPlayer(player);
        if (group != null) {
            dropRandomItems(player, event);
        }
    }

    private void createGroups() {
        Random rand = new Random();
        List<Player> tempPlayers = new ArrayList<>(players);
        while (tempPlayers.size() >= 5) {
            List<Player> group = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                int index = rand.nextInt(tempPlayers.size());
                group.add(tempPlayers.remove(index));
            }
            groups.add(group);
            shareInventory(group);
        }
        if (!tempPlayers.isEmpty()) {
            groups.add(tempPlayers);
            shareInventory(tempPlayers);
        }
    }

    private void shareInventory(List<Player> group) {
        PlayerInventory firstPlayerInventory = group.get(0).getInventory();

        for (Player player : group) {
            PlayerInventory playerInventory = player.getInventory();

            for (int i = 0; i < 36; i++) {
                Lock slotLock = slotLocks[i];
                slotLock.lock();
                try {
                    playerInventory.setItem(i, firstPlayerInventory.getItem(i));
                } finally {
                    slotLock.unlock();
                }
            }


            for (int i = 36; i < 40; i++) {
                Lock slotLock = slotLocks[i];
                slotLock.lock();
                try {
                    playerInventory.setArmorContents(firstPlayerInventory.getArmorContents());
                } finally {
                    slotLock.unlock();
                }
            }

            Lock offHandLock = slotLocks[40];
            offHandLock.lock();
            try {
                playerInventory.setItemInOffHand(firstPlayerInventory.getItemInOffHand());
            } finally {
                offHandLock.unlock();
            }
        }
    }


    private List<Player> getGroupByPlayer(Player player) {
        for (List<Player> group : groups) {
            if (group.contains(player)) {
                return group;
            }
        }
        return null;
    }

    private void dropRandomItems(Player player, PlayerDeathEvent event) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                items.add(item);
            }
        }

        Random rand = new Random();
        for (int i = 0; i < 10 && !items.isEmpty(); i++) {
            int index = rand.nextInt(items.size());
            ItemStack item = items.remove(index);
            event.getDrops().add(item);
        }
    }
}

