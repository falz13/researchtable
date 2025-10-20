package com.example.researchtable.network;

import com.example.researchtable.ResearchTableMod;
import com.example.researchtable.screen.ResearchTableScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class ResearchTableNetworking {
    public static final Identifier SYNC_PROGRESS = new Identifier(ResearchTableMod.MOD_ID, "sync_progress");
    public static final Identifier CONSUME_ITEM = new Identifier(ResearchTableMod.MOD_ID, "consume_item");
    public static final Identifier APPLY_ENCHANTMENTS = new Identifier(ResearchTableMod.MOD_ID, "apply_enchantments");

    private ResearchTableNetworking() {
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(CONSUME_ITEM, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                if (player.currentScreenHandler instanceof ResearchTableScreenHandler screenHandler) {
                    screenHandler.consumeResearchItem();
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(APPLY_ENCHANTMENTS, (server, player, handler, buf, responseSender) -> {
            List<EnchantmentSelection> selections = readSelections(buf);
            server.execute(() -> {
                if (player.currentScreenHandler instanceof ResearchTableScreenHandler screenHandler) {
                    screenHandler.applyEnchantments(selections);
                }
            });
        });
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(SYNC_PROGRESS, (client, handler, buf, responseSender) -> {
            ResearchTableScreenHandler.ProgressPayload payload = ResearchTableScreenHandler.ProgressPayload.read(buf);
            client.execute(() -> {
                if (client.player != null && client.player.currentScreenHandler instanceof ResearchTableScreenHandler screenHandler) {
                    screenHandler.applyPayload(payload);
                }
            });
        });
    }

    public static PacketByteBuf createConsumePacket() {
        return PacketByteBufs.create();
    }

    public static PacketByteBuf createApplyPacket(List<EnchantmentSelection> selections) {
        PacketByteBuf buf = PacketByteBufs.create();
        writeSelections(selections, buf);
        return buf;
    }

    public static void writeSelections(List<EnchantmentSelection> selections, PacketByteBuf buf) {
        buf.writeVarInt(selections.size());
        for (EnchantmentSelection selection : selections) {
            buf.writeIdentifier(selection.id());
            buf.writeVarInt(selection.level());
        }
    }

    public static List<EnchantmentSelection> readSelections(PacketByteBuf buf) {
        int size = buf.readVarInt();
        List<EnchantmentSelection> selections = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            selections.add(new EnchantmentSelection(buf.readIdentifier(), buf.readVarInt()));
        }
        return selections;
    }

    public record EnchantmentSelection(Identifier id, int level) {
    }
}
