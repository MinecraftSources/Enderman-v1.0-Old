package io.minestack.bungee.listeners;

import io.minestack.bungee.Enderman;
import io.minestack.bungee.ReconnectHandler;
import io.minestack.db.DoubleChest;
import io.minestack.db.entity.DCServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.ArrayList;

public class PlayerListener implements Listener {

    private final Enderman plugin;

    public PlayerListener(Enderman plugin) {
        this.plugin = plugin;
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
    }

    @EventHandler
    public void onPing(ProxyPingEvent event) {
        ArrayList<DCServer> servers = DoubleChest.getServerLoader().getServers();
        int max = 0;
        int online = 0;
        for (DCServer server : servers) {
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

    @EventHandler
    public void onServerKick(ServerKickEvent event) {
        plugin.getLogger().info("Server Kick");
        ServerInfo newServer = ((ReconnectHandler)plugin.getProxy().getReconnectHandler()).getSimilarServer(event.getPlayer(), event.getKickedFrom());

        if (newServer != null) {
            event.getPlayer().sendMessage(event.getKickReasonComponent());
        } else {
            event.setKickReasonComponent(event.getKickReasonComponent());
            return;
        }

        plugin.getLogger().info("New Server "+newServer);
        event.setCancelled(true);
        event.setCancelServer(newServer);
    }

    @EventHandler
    public void onServerDisconnect(ServerDisconnectEvent event) {
        plugin.getLogger().info("Server Disconnect");
    }

    @EventHandler
    public void onServerSwitch(ServerSwitchEvent event) {
        plugin.getLogger().info("Server Switch");
    }
}
