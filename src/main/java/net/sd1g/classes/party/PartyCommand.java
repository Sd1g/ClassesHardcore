package net.sd1g.classes.party;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class PartyCommand {
    public static void register(CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("party")
                .then(Commands.literal("create").executes(ctx -> {
                    PartyManager.createParty(ctx.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("invite")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    PartyManager.invitePlayer(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player"));
                                    return 1;
                                })))
                .then(Commands.literal("accept").executes(ctx -> {
                    PartyManager.acceptInvite(ctx.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("leave").executes(ctx -> {
                    PartyManager.leaveParty(ctx.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("info").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Party party = PartyManager.getParty(p.level().getServer(), p.getUUID());
                    if (party == null) {
                        p.sendSystemMessage(Component.literal("§cВы не в пати."));
                    } else {
                        p.sendSystemMessage(Component.literal("§e--- Информация о пати ---"));
                        p.sendSystemMessage(Component.literal("§aЛидер: §f" + p.level().getServer().getPlayerList().getPlayer(party.leader).getName().getString()));
                        p.sendSystemMessage(Component.literal("§aУчастники: §f" + party.members.size() + "/" + net.sd1g.classes.config.ModConfig.getInstance().getGlobalInt("party.max_size", 2)));
                        for (var m : party.members) {
                            var sp = p.level().getServer().getPlayerList().getPlayer(m);
                            if (sp != null) p.sendSystemMessage(Component.literal(" - " + sp.getName().getString()));
                        }
                    }
                    return 1;
                }))
        );
    }
}