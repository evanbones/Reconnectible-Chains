package com.evandev.connectiblechains.command;

import com.evandev.connectiblechains.entity.ChainKnotEntity;
import com.evandev.connectiblechains.entity.Chainable;
import com.evandev.connectiblechains.tag.ModTagRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class ConnectChainCommand {

    private static final SuggestionProvider<CommandSourceStack> CATENARY_ITEM_SUGGESTIONS = (context, builder) -> {
        List<String> suggestions = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (new ItemStack(item).is(ModTagRegistry.CATENARY_ITEMS)) {
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
                suggestions.add(key.toString());
            }
        }
        return SharedSuggestionProvider.suggest(suggestions, builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("connectchain")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("pos1", BlockPosArgument.blockPos())
                        .then(Commands.argument("dir1", StringArgumentType.word())
                                .then(Commands.argument("pos2", BlockPosArgument.blockPos())
                                        .then(Commands.argument("dir2", StringArgumentType.word())
                                                .executes(context -> executeConnect(context, Items.CHAIN))
                                                .then(Commands.argument("item", ResourceLocationArgument.id())
                                                        .suggests(CATENARY_ITEM_SUGGESTIONS)
                                                        .executes(context -> {
                                                            ResourceLocation id = ResourceLocationArgument.getId(context, "item");
                                                            Item item = BuiltInRegistries.ITEM.get(id);
                                                            if (item == Items.AIR) {
                                                                context.getSource().sendFailure(Component.literal("Unknown item: " + id));
                                                                return 0;
                                                            }
                                                            if (!new ItemStack(item).is(ModTagRegistry.CATENARY_ITEMS)) {
                                                                context.getSource().sendFailure(Component.literal("'" + id + "' is not a valid chain item."));
                                                                return 0;
                                                            }
                                                            return executeConnect(context, item);
                                                        })
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static int executeConnect(CommandContext<CommandSourceStack> context, Item chainItem) throws CommandSyntaxException {
        BlockPos pos1 = BlockPosArgument.getLoadedBlockPos(context, "pos1");
        Direction parsedDir1 = Direction.byName(StringArgumentType.getString(context, "dir1").toLowerCase());
        final Direction dir1 = parsedDir1 != null ? parsedDir1 : Direction.UP;

        BlockPos pos2 = BlockPosArgument.getLoadedBlockPos(context, "pos2");
        Direction parsedDir2 = Direction.byName(StringArgumentType.getString(context, "dir2").toLowerCase());
        final Direction dir2 = parsedDir2 != null ? parsedDir2 : Direction.UP;

        ServerLevel level = context.getSource().getLevel();

        ChainKnotEntity knot1 = ChainKnotEntity.getOrCreate(level, pos1, chainItem, dir1);
        ChainKnotEntity knot2 = ChainKnotEntity.getOrCreate(level, pos2, chainItem, dir2);

        if (knot1.equals(knot2)) {
            context.getSource().sendFailure(Component.literal("Cannot connect a chain to the same block."));
            return 0;
        }

        knot1.attachChain(new Chainable.ChainData(knot2, chainItem), null, true);

        context.getSource().sendSuccess(() -> Component.literal(
                "Successfully connected chain from " + pos1.toShortString() + " (" + dir1.getName() + ") to " + pos2.toShortString() + " (" + dir2.getName() + ")"
        ), true);

        return 1;
    }
}
