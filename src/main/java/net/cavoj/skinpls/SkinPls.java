package net.cavoj.skinpls;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

public class SkinPls implements DedicatedServerModInitializer {
    ExecutorService executor;

    @Override
    public void onInitializeServer() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            this.executor = Executors.newSingleThreadExecutor();
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            this.executor.shutdownNow();
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(literal("skinpls")
                .then(literal("mineskin")
                    .then(argument("id", IntegerArgumentType.integer())
                        .executes(this::executeMineskin)
                    )
                ).then(literal("mojang")
                    .then(argument("username", StringArgumentType.greedyString())
                        .executes(this::executeMojang)
                    )
                )
            );
        });
    }

    private int executeMineskin(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int id = getInteger(context, "id");
        UUID uuid = context.getSource().getPlayer().getUuid();
        executor.execute(() -> {
            InputStream inp;
            try {
                URL url = new URL("https://api.mineskin.org/get/id/%d".formatted(id));
                inp = url.openConnection().getInputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            JsonElement root = new JsonParser().parse(new InputStreamReader(inp));
            JsonObject obj = root.getAsJsonObject().getAsJsonObject("data").getAsJsonObject("texture");
            String value = obj.getAsJsonPrimitive("value").getAsString();
            String signature = obj.getAsJsonPrimitive("signature").getAsString();
            DataManager.writeData(uuid, value, signature);

            // TODO can I do this from a random thread?
            context.getSource().sendFeedback(Text.of("Successfully fetched skin data. Please relog."), false);
        });
        return 1;
    }

    private int executeMojang(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        context.getSource().sendFeedback(Text.of("This is not implemented yet."), false);
        return 1;
    }
}