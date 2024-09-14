package net.cavoj.skinpls;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
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

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("skinpls")
                .then(literal("mineskin")
                    .then(argument("uuid", StringArgumentType.greedyString())
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
        String skin_uuid = getString(context, "uuid");
        UUID player_uuid = context.getSource().getPlayerOrThrow().getUuid();
        executor.execute(() -> {
            InputStream inp;
            try {
                URL url = new URL("https://api.mineskin.org/get/uuid/%s".formatted(skin_uuid));
                inp = url.openStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            JsonElement root = JsonParser.parseReader(new InputStreamReader(inp));
            JsonObject obj = root.getAsJsonObject().getAsJsonObject("data").getAsJsonObject("texture");
            String value = obj.getAsJsonPrimitive("value").getAsString();
            String signature = obj.getAsJsonPrimitive("signature").getAsString();
            DataManager.writeData(player_uuid, value, signature);

            // TODO can I do this from a random thread?
            context.getSource().sendFeedback(() -> Text.of("Successfully fetched skin data. Please relog."), false);
        });
        return 1;
    }

    private int executeMojang(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String username = getString(context, "username");
        UUID uuid = context.getSource().getPlayerOrThrow().getUuid();
        executor.execute(() -> {
            String targetUuid;
            {
                InputStream inp;
                try {
                    URL url = new URL("https://api.mojang.com/users/profiles/minecraft/%s".formatted(username));
                    inp = url.openStream();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                JsonElement root = JsonParser.parseReader(new InputStreamReader(inp));
                targetUuid = root.getAsJsonObject().getAsJsonPrimitive("id").getAsString();
            }
            {
                InputStream inp;
                try {
                    URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false".formatted(targetUuid));
                    inp = url.openStream();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                JsonElement root = JsonParser.parseReader(new InputStreamReader(inp));
                for (JsonElement e : root.getAsJsonObject().getAsJsonArray("properties")) {
                    JsonObject obj = e.getAsJsonObject();
                    if (obj.getAsJsonPrimitive("name").getAsString().equals("textures")) {
                        String value, signature;
                        value = obj.getAsJsonPrimitive("value").getAsString();
                        signature = obj.getAsJsonPrimitive("signature").getAsString();
                        DataManager.writeData(uuid, value, signature);
                        context.getSource().sendFeedback(() -> Text.of("Successfully fetched skin data. Please relog."), false);
                        break;
                    }
                }
            }
        });
        return 1;
    }
}