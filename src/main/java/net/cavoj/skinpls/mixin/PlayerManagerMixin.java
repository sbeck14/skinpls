package net.cavoj.skinpls.mixin;

import com.mojang.authlib.properties.PropertyMap;
import net.cavoj.skinpls.DataManager;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @Inject(method = "onPlayerConnect", at = @At("HEAD"))
    void onConnect(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
        PropertyMap map = player.getGameProfile().getProperties();
        DataManager.getTextures(player.getUuid()).ifPresent(property -> {
            map.removeAll("textures");
            map.put("textures", property);
        });
    }
}
