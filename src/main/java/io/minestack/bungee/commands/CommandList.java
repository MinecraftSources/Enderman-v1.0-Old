package io.minestack.bungee.commands;

import io.minestack.bungee.Titanium46;
import io.minestack.db.Uranium;
import io.minestack.db.entity.UPlayer;
import io.minestack.db.entity.UServer;
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

    private final Titanium46 plugin;

    public CommandList(Titanium46 plugin) {
        super("glist", "bungeecord.command.list");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        int online = 0;
        int onlineNetwork = 0;
        for (UServer server : Uranium.getServerLoader().getServers()) {
            onlineNetwork += server.getPlayers().size();
        }
        for (ServerInfo server : ProxyServer.getInstance().getServers().values()) {
            if (!server.canAccess(sender)) {
                continue;
            }
            List<String> players = new ArrayList<>();
            Collections.sort(players, String.CASE_INSENSITIVE_ORDER);
            try {
                UServer mn2Server = Uranium.getServerLoader().loadEntity(new ObjectId(server.getName()));
                if (mn2Server == null) {
                    throw new Exception();//to break out and show the regular server format
                }
                online += mn2Server.getPlayers().size();
                if (mn2Server.getServerType() != null) {
                    players.addAll(mn2Server.getPlayers().stream().map(UPlayer::getPlayerName).collect(Collectors.toList()));
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
