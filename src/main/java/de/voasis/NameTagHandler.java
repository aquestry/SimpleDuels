package de.voasis;

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

public class NameTagHandler {
    public NameTagHandler() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerDisconnectEvent.class, event -> event.getPlayer().getPassengers().forEach(Entity::remove));
        MinecraftServer.getGlobalEventHandler().addListener(PlayerPluginMessageEvent.class, event -> {
            String identifier = event.getIdentifier();
            String message = event.getMessageString();
            if(!identifier.equals("nebula:main")) { return; }
            System.out.println("Channel: " + identifier + " Message:");
            System.out.println(message);
            String playerName = message.split(":")[0];
            Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUsername(playerName);
            if(player != null && player.getPassengers().isEmpty()) {
                player.setDisplayName(MiniMessage.miniMessage().deserialize(message.split(":")[1].split("#")[2] + player.getUsername()));
                Entity entity = new Entity(EntityType.TEXT_DISPLAY);
                TextDisplayMeta meta = (TextDisplayMeta) entity.getEntityMeta();
                meta.setText(player.getDisplayName());
                meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
                meta.setBackgroundColor(0x00000000);
                meta.setShadow(true);
                meta.setTranslation(new Vec(0, 0.3, 0));
                player.addPassenger(entity);
            }
        });
    }
}