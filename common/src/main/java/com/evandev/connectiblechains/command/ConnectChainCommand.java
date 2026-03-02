package com.evandev.connectiblechains.command;

import com.evandev.connectiblechains.entity.ChainKnotEntity;
import com.evandev.connectiblechains.entity.Chainable;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;

public class ConnectChainCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("connectchain")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("pos1", BlockPosArgument.blockPos())
                        .then(Commands.argument("dir1", StringArgumentType.word())
                                .then(Commands.argument("pos2", BlockPosArgument.blockPos())
                                        .then(Commands.argument("dir2", StringArgumentType.word())
                                                .executes(context -> {
                                                    BlockPos pos1 = BlockPosArgument.getLoadedBlockPos(context, "pos1");
                                                    Direction parsedDir1 = Direction.byName(StringArgumentType.getString(context, "dir1").toLowerCase());
                                                    final Direction dir1 = parsedDir1 != null ? parsedDir1 : Direction.UP;

                                                    BlockPos pos2 = BlockPosArgument.getLoadedBlockPos(context, "pos2");
                                                    Direction parsedDir2 = Direction.byName(StringArgumentType.getString(context, "dir2").toLowerCase());
                                                    final Direction dir2 = parsedDir2 != null ? parsedDir2 : Direction.UP;

                                                    ServerLevel level = context.getSource().getLevel();

                                                    ChainKnotEntity knot1 = ChainKnotEntity.getOrCreate(level, pos1, Items.CHAIN, dir1);
                                                    ChainKnotEntity knot2 = ChainKnotEntity.getOrCreate(level, pos2, Items.CHAIN, dir2);

                                                    if (knot1.equals(knot2)) {
                                                        context.getSource().sendFailure(Component.literal("Cannot connect a chain to the same block."));
                                                        return 0;
                                                    }

                                                    knot1.attachChain(new Chainable.ChainData(knot2, Items.CHAIN), null, true);

                                                    context.getSource().sendSuccess(() -> Component.literal(
                                                            "Successfully connected chain from " + pos1.toShortString() + " (" + dir1.getName() + ") to " + pos2.toShortString() + " (" + dir2.getName() + ")"
                                                    ), true);

                                                    return 1;
                                                })
                                        )
                                )
                        )
                )
        );
    }
}