package com.rmb938.mn2.docker.commands;

import com.rmb938.mn2.docker.MN2Bungee;
import com.rmb938.mn2.docker.db.entity.MN2Server;
import net.md_5.bungee.Util;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandList extends Command {

    private final MN2Bungee plugin;

    public CommandList(MN2Bungee plugin) {
        super("glist", "bungeecord.command.list");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        int online = 0;
        for (ServerInfo server : ProxyServer.getInstance().getServers().values()) {
            if (!server.canAccess(sender)) {
                continue;
            }
            List<String> players = new ArrayList<>();
            for (ProxiedPlayer player : server.getPlayers()) {
                players.add(player.getDisplayName());
            }
            Collections.sort(players, String.CASE_INSENSITIVE_ORDER);
            try {
                MN2Server mn2Server = plugin.getServerLoader().loadEntity(new ObjectId(server.getName()));
                if (mn2Server == null) {
                    throw new Exception("");//to break out and show the regular server format
                }
                online += mn2Server.getPlayers().size();
                if (mn2Server.getServerType() != null) {
                    sender.sendMessage(new TextComponent(ChatColor.GREEN + "[" + mn2Server.getServerType().getName() + "." + mn2Server.getNumber() + "] "+ChatColor.GOLD+"(" + mn2Server.getPlayers().size() + "): "+ChatColor.RESET + Util.format(players, ChatColor.RESET + ", ")));
                } else {
                    sender.sendMessage(new TextComponent(ChatColor.GREEN + "[NULL." + mn2Server.getNumber() + "] "+ChatColor.GOLD+"(" + mn2Server.getPlayers().size() + "): "+ChatColor.RESET + Util.format(players, ChatColor.RESET + ", ")));
                }
            } catch (Exception ex) {
                sender.sendMessage(new TextComponent(ChatColor.GREEN + "[" + server.getName() + "] (" + server.getPlayers().size() + "): " + Util.format(players, ChatColor.RESET + ", ")));
            }
        }
        sender.sendMessage(new TextComponent("Total Players on Bungee: " + plugin.getProxy().getOnlineCount()));
        sender.sendMessage(new TextComponent("Total Players on Bungee Type: "+online));
    }
}
