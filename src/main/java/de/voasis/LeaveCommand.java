package de.voasis;

import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

public class LeaveCommand extends Command {
    public LeaveCommand() {
        super("leave");
        setDefaultExecutor((sender, context) -> {
            if(sender instanceof Player) {
                Main.quitAll();
            }
        });
    }
}
