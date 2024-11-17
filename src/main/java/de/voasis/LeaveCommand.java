package de.voasis;

import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;

import java.nio.charset.StandardCharsets;

public class LeaveCommand extends Command {
    public LeaveCommand() {
        super("leave");
        setDefaultExecutor((sender, context) -> {
            if(sender instanceof Player player) {
                String message = "lobby:" + player.getUsername();
                PluginMessagePacket packet = new PluginMessagePacket(
                        "nebula:main",
                        message.getBytes(StandardCharsets.UTF_8)
                );
                player.sendPacket(packet);
            }
        });
    }
}
