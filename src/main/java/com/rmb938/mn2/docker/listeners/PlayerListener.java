package com.rmb938.mn2.docker.listeners;

import com.rmb938.mn2.docker.MN2Bungee;
import com.rmb938.mn2.docker.db.entity.MN2Server;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.ArrayList;

public class PlayerListener implements Listener {

    private final MN2Bungee plugin;

    public PlayerListener(MN2Bungee plugin) {
        this.plugin = plugin;
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
    }

    @EventHandler
    public void onPing(ProxyPingEvent event) {
        ArrayList<MN2Server> servers = plugin.getServerLoader().getServers();
        int max = 0;
        int online = 0;
        for (MN2Server server : servers) {
            if (server.getPort() > 0 && server.getLastUpdate() > System.currentTimeMillis()-60000) {
                max += server.getServerType().getPlayers();
                online += server.getPlayers().size();
            }
        }
        ServerPing serverPing = new ServerPing();
        ServerPing.Players players = new ServerPing.Players(max, online, event.getResponse().getPlayers().getSample());
        serverPing.setPlayers(players);
        serverPing.setDescription(event.getResponse().getDescription());
        serverPing.setVersion(event.getResponse().getVersion());
        serverPing.setFavicon(event.getResponse().getFaviconObject());
        event.setResponse(serverPing);
    }
}
