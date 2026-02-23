package com.evandev.connectiblechains.command;

import com.evandev.connectiblechains.entity.ChainKnotEntity;
import com.evandev.connectiblechains.entity.Chainable;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;

public class ConnectChainCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("connectchain")
                .requires(source -> source.hasPermission(2)) // Require OP permissions
                .then(Commands.argument("pos1", BlockPosArgument.blockPos())
                        .then(Commands.argument("pos2", BlockPosArgument.blockPos())
                                .executes(context -> {
                                    BlockPos pos1 = BlockPosArgument.getLoadedBlockPos(context, "pos1");
                                    BlockPos pos2 = BlockPosArgument.getLoadedBlockPos(context, "pos2");
                                    ServerLevel level = context.getSource().getLevel();

                                    ChainKnotEntity knot1 = ChainKnotEntity.getOrCreate(level, pos1, Items.CHAIN);
                                    ChainKnotEntity knot2 = ChainKnotEntity.getOrCreate(level, pos2, Items.CHAIN);

                                    if (knot1.equals(knot2)) {
                                        context.getSource().sendFailure(Component.literal("Cannot connect a chain to the same block."));
                                        return 0;
                                    }

                                    knot1.attachChain(new Chainable.ChainData(knot2, Items.CHAIN), null, true);

                                    context.getSource().sendSuccess(() -> Component.literal(
                                            "Successfully connected chain from " + pos1.toShortString() + " to " + pos2.toShortString()
                                    ), true);

                                    return 1;
                                })
                        )
                )
        );
    }
}