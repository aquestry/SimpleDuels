package de.voasis;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.scoreboard.Sidebar;

public class NebulaAPI {
    public NebulaAPI() {
        Main.globalEventHandler.addListener(PlayerDisconnectEvent.class, event -> event.getPlayer().getPassengers().forEach(Entity::remove));
        Main.globalEventHandler.addListener(PlayerPluginMessageEvent.class, event -> {
            String identifier = event.getIdentifier();
            String message = event.getMessageString();
            System.out.println("Channel: " + identifier + " Message: " + message);
            if(identifier.equals("nebula:main")) {
                String playerName = message.split(":")[0];
                Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUsername(playerName);
                if(player != null && player.getPassengers().isEmpty()) {
                    String newName = message.split(":")[1].split("#")[2] + player.getUsername();
                    System.out.println("Player: " + playerName + " New Name: " + newName);
                    createNametag(player, newName);
                }
            }
            if (event.getIdentifier().equals("nebula:scoreboard")) {
                String[] parts = message.split(":");
                if (parts.length >= 3) {
                    String username = parts[0];
                    String title = parts[1];
                    String[] lines = parts[2].split("#");
                    System.out.println("Lines: " + parts[2]);
                    Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUsername(username);
                    if (player != null) {
                        Sidebar sidebar = new Sidebar(MiniMessage.miniMessage().deserialize(title));
                        for (int i = 0; i < lines.length; i++) {
                            System.out.println("Line: " + lines[i]);
                            sidebar.createLine(new Sidebar.ScoreboardLine(
                                    "line_" + i,
                                    MiniMessage.miniMessage().deserialize(lines[i]),
                                    lines.length - i,
                                    Sidebar.NumberFormat.blank()
                            ));
                        }
                        sidebar.addViewer(player);
                        System.out.println("Scoreboard updated for player: " + username);
                    } else {
                        System.err.println("Player not found: " + username);
                    }
                } else {
                    System.err.println("Invalid scoreboard message format: " + message);
                }
            }
        });
    }
    public void createNametag(Object entityHolder, String name) {
        Component displayName = Main.mm.deserialize(name);
        Entity entity = new Entity(EntityType.TEXT_DISPLAY);
        TextDisplayMeta meta = (TextDisplayMeta) entity.getEntityMeta();
        meta.setText(displayName);
        meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
        meta.setBackgroundColor(0x00000000);
        meta.setShadow(true);
        meta.setTranslation(new Vec(0, 0.3, 0));
        if (entityHolder instanceof Player player) {
            player.addPassenger(entity);
            player.setDisplayName(displayName);
        }
    }
}