package io.minestack.bungee.listeners;

import io.minestack.bungee.Enderman;
import io.minestack.bungee.ReconnectHandler;
import io.minestack.db.DoubleChest;
import io.minestack.db.entity.DCServer;
import io.minestack.db.entity.DCServerType;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.bson.types.ObjectId;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.logging.Level;

public class PluginListener implements Listener {

    private final Enderman plugin;

    public PluginListener(Enderman plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (event.getReceiver() instanceof ProxiedPlayer == false) {
            return;
        }
        if (event.getTag().equalsIgnoreCase("BungeeCord")) {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()));
            try {
                String subchannel = in.readUTF();

                if (subchannel.equalsIgnoreCase("connect")) {
                    String serverId = in.readUTF();
                    String serverTypeName = null;
                    ObjectId _serverId = null;
                    try {
                        _serverId = new ObjectId(serverId);
                    } catch (Exception ex) {
                        serverTypeName = serverId;
                    }
                    if (_serverId != null) {
                        ServerInfo serverInfo = plugin.getProxy().getServerInfo(_serverId.toString());
                        if (serverInfo != null) {
                            ProxiedPlayer player = (ProxiedPlayer) event.getReceiver();
                            player.connect(serverInfo);
                        }
                    } else {
                        DCServerType serverType = DoubleChest.getServerTypeLoader().getType(serverTypeName);
                        if (serverType != null) {
                            DCServer server = ReconnectHandler.getServerWithRoom(plugin, serverType);
                            if (server != null) {
                                ServerInfo serverInfo = plugin.getProxy().getServerInfo(server.get_id().toString());
                                if (serverInfo != null) {
                                    ProxiedPlayer player = (ProxiedPlayer) event.getReceiver();
                                    player.connect(serverInfo);
                                }
                            }
                        }
                    }

                    event.setCancelled(true);
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, null, e);
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, null, e);
                }
            }
        }
    }

}
