package io.minestack.docker.commands;

import io.minestack.db.entity.MN2Player;
import io.minestack.db.entity.MN2Server;
import io.minestack.docker.MN2Bungee;
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
import java.util.stream.Collectors;

public class CommandList extends Command {

    private final MN2Bungee plugin;

    public CommandList(MN2Bungee plugin) {
        super("glist", "bungeecord.command.list");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        int online = 0;
        int onlineNetwork = 0;
        for (MN2Server server : plugin.getServerLoader().getServers()) {
            onlineNetwork += server.getPlayers().size();
        }
        for (ServerInfo server : ProxyServer.getInstance().getServers().values()) {
            if (!server.canAccess(sender)) {
                continue;
            }
            List<String> players = new ArrayList<>();
            Collections.sort(players, String.CASE_INSENSITIVE_ORDER);
            try {
                MN2Server mn2Server = plugin.getServerLoader().loadEntity(new ObjectId(server.getName()));
                if (mn2Server == null) {
                    throw new Exception();//to break out and show the regular server format
                }
                online += mn2Server.getPlayers().size();
                if (mn2Server.getServerType() != null) {
                    players.addAll(mn2Server.getPlayers().stream().map(MN2Player::getPlayerName).collect(Collectors.toList()));
                    sender.sendMessage(new TextComponent(ChatColor.GREEN + "[" + mn2Server.getServerType().getName() + "." + mn2Server.getNumber() + "] "+ChatColor.GOLD+"(" + mn2Server.getPlayers().size() + "): "+ChatColor.RESET + Util.format(players, ChatColor.RESET + ", ")));
                } else {
                    players.addAll(server.getPlayers().stream().map(ProxiedPlayer::getDisplayName).collect(Collectors.toList()));
                    sender.sendMessage(new TextComponent(ChatColor.GREEN + "[NULL." + mn2Server.getNumber() + "] "+ChatColor.GOLD+"(" + mn2Server.getPlayers().size() + "): "+ChatColor.RESET + Util.format(players, ChatColor.RESET + ", ")));
                }
            } catch (Exception ex) {
                online += server.getPlayers().size();
                sender.sendMessage(new TextComponent(ChatColor.GREEN + "[" + server.getName() + "] (" + server.getPlayers().size() + "): " + Util.format(players, ChatColor.RESET + ", ")));
            }
        }
        sender.sendMessage(new TextComponent("Total Players on Bungee: " + plugin.getProxy().getOnlineCount()));
        sender.sendMessage(new TextComponent("Total Players on Bungee Type: "+online));
        sender.sendMessage(new TextComponent("Total Players on Network: "+onlineNetwork));
    }
}