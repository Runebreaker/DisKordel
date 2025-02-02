package de.jagenka.mixin;

import com.mojang.authlib.GameProfile;
import de.jagenka.MinecraftHandler;
import de.jagenka.PlayerStatManager;
import de.jagenka.UserRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.ServerStatHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin
{
    ExecutorService discordExecutor = Executors.newSingleThreadExecutor();
    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "createStatHandler", at = @At("RETURN"))
    void saveStatHandlerToCache(PlayerEntity player, CallbackInfoReturnable<ServerStatHandler> cir)
    {
        PlayerStatManager.INSTANCE.updateStatHandler(player.getUuid(), cir.getReturnValue());
    }

    @Inject(method = "onPlayerConnect", at = @At("HEAD"))
    void saveNewPlayersProfileToCache(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci)
    {
        GameProfile profile = player.getGameProfile();

        discordExecutor.submit(() ->
        {
            assert profile != null;
            UserRegistry.INSTANCE.saveToCache(profile);
        });
    }

    @Inject(method = "broadcast(Lnet/minecraft/network/message/SignedMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/network/message/MessageType$Parameters;)V", at = @At("HEAD"))
    void handleSayCommand(SignedMessage message, Predicate<ServerPlayerEntity> shouldSendFiltered, @Nullable ServerPlayerEntity sender, MessageType.Parameters params, CallbackInfo ci)
    {
        MinecraftHandler.INSTANCE.handleSayCommand(message, params);
    }
}
