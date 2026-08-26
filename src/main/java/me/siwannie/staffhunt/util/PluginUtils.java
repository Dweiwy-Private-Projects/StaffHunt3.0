package me.siwannie.staffhunt.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class PluginUtils {

    public static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static void sendMessage(CommandSender sender, FileConfiguration config, String message) {
        String prefix = config.getString("plugin-prefix", "&8[&6StaffHunt&8] &r");
        sender.sendMessage(colorize(prefix + message));
    }

    public static void broadcastMessage(FileConfiguration config, String message) {
        String prefix = config.getString("plugin-prefix", "&8[&6StaffHunt&8] &r");
        Bukkit.getServer().broadcastMessage(colorize(prefix + message));
    }

    // This is the robust, Reflection-based method for sending action bars
    public static void sendActionBar(Player player, String message) {
        try {
            Object chatComponent = getNMSClass("IChatBaseComponent$ChatSerializer")
                    .getMethod("a", String.class)
                    .invoke(null, "{\"text\":\"" + colorize(message) + "\"}");

            Object packet = getNMSClass("PacketPlayOutChat")
                    .getConstructor(getNMSClass("IChatBaseComponent"), byte.class)
                    .newInstance(chatComponent, (byte) 2);

            sendPacket(player, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper methods for Reflection (adapted from your Assassins plugin)
    private static void sendPacket(Player player, Object packet) throws Exception {
        Object handle = player.getClass().getMethod("getHandle").invoke(player);
        Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
        playerConnection.getClass().getMethod("sendPacket", getNMSClass("Packet")).invoke(playerConnection, packet);
    }

    private static Class<?> getNMSClass(String name) throws ClassNotFoundException {
        return Class.forName("net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + "." + name);
    }
}