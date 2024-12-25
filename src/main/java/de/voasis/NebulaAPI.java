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
import net.minestom.server.network.ConnectionState;
import net.minestom.server.scoreboard.Sidebar;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;

public class NebulaAPI {
    public NebulaAPI() {
        Main.globalEventHandler.addListener(PlayerDisconnectEvent.class, event -> event.getPlayer().getPassengers().forEach(Entity::remove));
        Main.globalEventHandler.addListener(PlayerPluginMessageEvent.class, event -> {
            String identifier = event.getIdentifier();
            String message = event.getMessageString();
            Player player = event.getPlayer();
            if (!identifier.equals("nebula:main") && !identifier.equals("nebula:scoreboard")) { return; }
            System.out.println("Channel: " + identifier + " Message: " + message);
            final int[] attempts = {0};
            final Task[] taskHolder = new Task[1];
            taskHolder[0] = MinecraftServer.getSchedulerManager().buildTask(() -> {
                if (player.getPlayerConnection().getConnectionState().equals(ConnectionState.PLAY) && player.getInstance() != null) {
                    switch (identifier) {
                        case "nebula:main" -> handleNametagEvent(message);
                        case "nebula:scoreboard" -> handleScoreboardEvent(message);
                    }
                    taskHolder[0].cancel();
                } else {
                    attempts[0]++;
                    if (attempts[0] >= 10) {
                        taskHolder[0].cancel();
                        System.err.println("Failed to process plugin message after 10 attempts: " + identifier);
                    }
                }
            }).repeat(TaskSchedule.seconds(1)).delay(TaskSchedule.seconds(1)).schedule();
        });
    }

    private void handleNametagEvent(String message) {
        try {
            String[] parts = message.split(":");
            String playerName = parts[0];
            String newName = parts[1].split("#")[2] + playerName;
            Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUsername(playerName);
            if (player != null && player.getPassengers().isEmpty()) {
                System.out.println("Player: " + playerName + " received name: " + newName);
                createNametag(player, newName);
            }
        } catch (Exception e) {
            System.err.println("Error handling nametag event: " + e.getMessage());
        }
    }

    private void handleScoreboardEvent(String message) {
        try {
            String[] parts = message.split("&");
            if (parts.length < 3) {
                System.err.println("Invalid Scoreboard message format: " + message);
                return;
            }
            String username = parts[0];
            String title = parts[1];
            String[] lines = parts[2].split("#");
            Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUsername(username);
            if (player != null) {
                Sidebar sidebar = new Sidebar(MiniMessage.miniMessage().deserialize(title));
                for (int i = 0; i < lines.length; i++) {
                    String lineId = "line_" + i;
                    String lineText = lines[i];
                    sidebar.createLine(new Sidebar.ScoreboardLine(
                            lineId,
                            MiniMessage.miniMessage().deserialize(lineText),
                            (lines.length - i),
                            Sidebar.NumberFormat.blank()
                    ));
                }
                sidebar.addViewer(player);
                System.out.println("Scoreboard successfully updated for: " + username);
            } else {
                System.err.println("Player not found: " + username);
            }
        } catch (Exception e) {
            System.err.println("Error handling scoreboard event: " + e.getMessage());
        }
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