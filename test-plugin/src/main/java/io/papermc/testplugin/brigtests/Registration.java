package io.papermc.testplugin.brigtests;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.RegistryArgumentExtractor;
import io.papermc.paper.command.brigadier.argument.range.DoubleRangeProvider;
import io.papermc.paper.command.brigadier.argument.resolvers.FinePositionResolver;
import io.papermc.paper.math.FinePosition;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.testplugin.brigtests.example.ExampleAdminCommand;
import io.papermc.testplugin.brigtests.example.MaterialArgumentType;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class Registration {

    private Registration() {
    }

    public static void registerViaOnEnable(final JavaPlugin plugin) {
        registerLegacyCommands(plugin);
        registerViaLifecycleEvents(plugin);
    }

    private static void registerViaLifecycleEvents(final JavaPlugin plugin) {
        final LifecycleEventManager<Plugin> lifecycleManager = plugin.getLifecycleManager();
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();

            commands.register(Commands.literal("ench")
                .then(Commands.argument("name", ArgumentTypes.resource(RegistryKey.ENCHANTMENT))
                    .executes(ctx -> sendMessage(ctx.getSource().getSender(), ctx.getArgument("name", Enchantment.class).toString()))
                ).build()
            );

            commands.register(Commands.literal("ench-key")
                .then(Commands.argument("key", ArgumentTypes.resourceKey(RegistryKey.ENCHANTMENT))
                    .executes(ctx -> {
                        final TypedKey<Enchantment> key = RegistryArgumentExtractor.getTypedKey(ctx, RegistryKey.ENCHANTMENT, "key");
                        return sendMessage(ctx.getSource().getSender(), key.toString());
                    })
                ).build()
            );

            commands.register(Commands.literal("fine-pos")
                .then(Commands.argument("pos", ArgumentTypes.finePosition(false))
                    .executes(ctx -> {
                        final FinePositionResolver position = ctx.getArgument("pos", FinePositionResolver.class);
                        return sendMessage(ctx.getSource().getSender(), "Position: " + position.resolve(ctx.getSource()));
                    })
                ).build()
            );

            commands.register(Commands.literal("tag")
                .executes(ctx -> sendMessage(ctx.getSource().getSender(), "overridden command"))
                .build(),
                null,
                Collections.emptyList()
            );
        });

        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS.newHandler(event -> {
            final Commands commands = event.registrar();

            commands.register(plugin.getPluginMeta(), Commands.literal("root_command")
                .then(Commands.literal("sub_command")
                    .requires(source -> source.getSender().hasPermission("testplugin.test"))
                    .executes(ctx -> sendMessage(ctx.getSource().getSender(), "root_command sub_command"))
                ).build(),
                null,
                Collections.emptyList()
            );

            commands.register(plugin.getPluginMeta(), "example", "test", Collections.emptyList(), new BasicCommand() {
                @Override
                public void execute(@NotNull final CommandSourceStack commandSourceStack, final @NotNull String @NotNull[] args) {
                    System.out.println(Arrays.toString(args));
                }

                @Override
                public @NotNull Collection<String> suggest(final @NotNull CommandSourceStack commandSourceStack, final @NotNull String @NotNull[] args) {
                    System.out.println(Arrays.toString(args));
                    return List.of("apple", "banana");
                }
            });

            commands.register(plugin.getPluginMeta(), Commands.literal("water")
                .requires(source -> {
                    System.out.println("isInWater check");
                    return source.getExecutor().isInWater();
                })
                .executes(ctx -> sendMessage(ctx.getSource().getExecutor(), "You are in water!"))
                .then(Commands.literal("lava")
                    .requires(source -> {
                        System.out.println("isInLava check");
                        return source.getExecutor() != null && source.getExecutor().isInLava();
                    })
                    .executes(ctx -> sendMessage(ctx.getSource().getExecutor(), "You are in lava!"))
                ).build(),
                null,
                Collections.emptyList()
            );

            ExampleAdminCommand.register(plugin, commands);
        }).priority(10));
    }

    private static void registerLegacyCommands(final JavaPlugin plugin) {
        registerLegacyCommand(plugin, "hi", "cool hi command", List.of("hialias"));
        registerLegacyCommand(plugin, "cooler-command", "cool hi command", List.of("cooler-command-alias"));

        plugin.getServer().getCommandMap().getKnownCommands().values().removeIf(command -> command.getName().equals("hi"));
    }

    private static void registerLegacyCommand(JavaPlugin plugin, String name, String description, List<String> aliases) {
        plugin.getServer().getCommandMap().register("fallback", new BukkitCommand(name, description, "<>", aliases) {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
                sender.sendMessage("hi");
                return true;
            }
        });
    }

    public static void registerViaBootstrap(final BootstrapContext context) {
        final LifecycleEventManager<BootstrapContext> lifecycleManager = context.getLifecycleManager();
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            commands.register(Commands.literal("material")
                .then(Commands.literal("item")
                    .then(Commands.argument("mat", MaterialArgumentType.item())
                        .executes(ctx -> sendMessage(ctx.getSource().getSender(), ctx.getArgument("mat", Material.class).name()))
                    )
                )
                .then(Commands.literal("block")
                    .then(Commands.argument("mat", MaterialArgumentType.block())
                        .executes(ctx -> sendMessage(ctx.getSource().getSender(), ctx.getArgument("mat", Material.class).name()))
                    )
                ).build(),
                null,
                Collections.emptyList()
            );
        });

        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS.newHandler(event -> {
            final Commands commands = event.registrar();
            commands.register(Commands.literal("heya")
                .then(Commands.argument("range", ArgumentTypes.doubleRange())
                    .executes(ctx -> sendMessage(ctx.getSource().getSender(), ctx.getArgument("range", DoubleRangeProvider.class).range().toString()))
                ).build(),
                null,
                Collections.emptyList()
            );
        }).priority(10));
    }

    private static int sendMessage(CommandSender sender, String message) {
        sender.sendMessage(message);
        return Command.SINGLE_SUCCESS;
    }

    private static int sendMessage(CommandSourceStack source, String message) {
        source.getSender().sendPlainMessage(message);
        return Command.SINGLE_SUCCESS;
    }
}
