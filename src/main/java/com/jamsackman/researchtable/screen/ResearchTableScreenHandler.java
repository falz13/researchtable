package com.jamsackman.researchtable.screen;

import com.jamsackman.researchtable.ResearchTableMod;
import com.jamsackman.researchtable.block.entity.ResearchTableBlockEntity;
import com.jamsackman.researchtable.network.ResearchTableNetworking;
import com.jamsackman.researchtable.progression.ResearchLogic;
import com.jamsackman.researchtable.progression.ResearchProgressState;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ResearchTableScreenHandler extends ScreenHandler {
    private final ResearchTableBlockEntity blockEntity;
    private final PlayerEntity player;

    private final ClientState clientState = new ClientState();
    private Runnable clientUpdateListener;

    public ResearchTableScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory, getBlockEntity(playerInventory.player, buf.readBlockPos()));
    }

    public ResearchTableScreenHandler(int syncId, PlayerInventory playerInventory, ResearchTableBlockEntity blockEntity) {
        super(ResearchTableMod.RESEARCH_TABLE_SCREEN_HANDLER, syncId);
        this.blockEntity = blockEntity;
        this.player = playerInventory.player;

        checkSize(blockEntity, 3);

        this.addSlot(new Slot(blockEntity, 0, 44, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return hasEnchantments(stack);
            }
        });

        this.addSlot(new Slot(blockEntity, 1, 98, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return !stack.isEmpty();
            }
        });

        this.addSlot(new Slot(blockEntity, 2, 26, 53) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(Items.LAPIS_LAZULI);
            }
        });

        int m;
        int l;
        for (m = 0; m < 3; ++m) {
            for (l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + m * 9 + 9, 8 + l * 18, 84 + m * 18));
            }
        }
        for (m = 0; m < 9; ++m) {
            this.addSlot(new Slot(playerInventory, m, 8 + m * 18, 142));
        }

        if (player instanceof ServerPlayerEntity serverPlayer) {
            syncToClient(serverPlayer);
        }
    }

    private static ResearchTableBlockEntity getBlockEntity(PlayerEntity player, BlockPos pos) {
        if (player.getWorld().getBlockEntity(pos) instanceof ResearchTableBlockEntity tableBlockEntity) {
            return tableBlockEntity;
        }
        throw new IllegalStateException("Missing research table block entity at " + pos);
    }

    private static boolean hasEnchantments(ItemStack stack) {
        return !EnchantmentHelper.get(stack).isEmpty();
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return blockEntity.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasStack()) {
            ItemStack stack = slot.getStack();
            itemStack = stack.copy();
            if (index < 3) {
                if (!this.insertItem(stack, 3, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (hasEnchantments(stack)) {
                if (!this.insertItem(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.isOf(Items.LAPIS_LAZULI)) {
                if (!this.insertItem(stack, 2, 3, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(stack, 1, 2, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }

            if (stack.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTakeItem(player, stack);
        }

        return itemStack;
    }

    @Override
    public void onContentChanged(net.minecraft.inventory.Inventory inventory) {
        super.onContentChanged(inventory);
        if (player instanceof ServerPlayerEntity serverPlayer) {
            syncToClient(serverPlayer);
        }
    }

    public void consumeResearchItem() {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        ItemStack stack = blockEntity.getStack(0);
        if (stack.isEmpty()) {
            return;
        }
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(stack);
        if (enchantments.isEmpty()) {
            return;
        }

        ResearchProgressState state = ResearchProgressState.get(serverPlayer.getServerWorld());
        UUID uuid = serverPlayer.getUuid();

        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            Enchantment enchantment = entry.getKey();
            int level = entry.getValue();
            Identifier id = Registries.ENCHANTMENT.getId(enchantment);
            if (id == null) {
                continue;
            }
            state.addProgress(uuid, id, ResearchLogic.getProgressReward(enchantment, level));
        }

        stack.decrement(1);
        if (stack.isEmpty()) {
            blockEntity.setStack(0, ItemStack.EMPTY);
        }
        blockEntity.markDirty();
        syncToClient(serverPlayer);
        sendContentUpdates();
    }

    public void applyEnchantments(List<ResearchTableNetworking.EnchantmentSelection> selections) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        ItemStack target = blockEntity.getStack(1);
        if (target.isEmpty()) {
            return;
        }

        ResearchProgressState state = ResearchProgressState.get(serverPlayer.getServerWorld());
        UUID uuid = serverPlayer.getUuid();

        Map<Enchantment, Integer> existing = new HashMap<>(EnchantmentHelper.get(target));
        Map<Enchantment, Integer> result = new HashMap<>(existing);

        int totalXpCost = 0;
        int totalLapisCost = 0;

        for (ResearchTableNetworking.EnchantmentSelection selection : selections) {
            if (selection.level() <= 0) {
                continue;
            }
            Enchantment enchantment = Registries.ENCHANTMENT.get(selection.id());
            if (enchantment == null) {
                continue;
            }
            if (!enchantment.isAcceptableItem(target)) {
                continue;
            }
            int unlocked = state.getUnlockedLevel(uuid, enchantment);
            if (selection.level() > unlocked) {
                continue;
            }
            int maxLevel = enchantment.getMaxLevel();
            int desiredLevel = Math.min(selection.level(), maxLevel);

            if (!isCompatible(result, enchantment)) {
                continue;
            }

            int currentLevel = result.getOrDefault(enchantment, 0);
            if (desiredLevel <= currentLevel) {
                continue;
            }

            result.put(enchantment, desiredLevel);
            totalXpCost += ResearchLogic.getXpCost(enchantment, desiredLevel);
            totalLapisCost += ResearchLogic.getLapisCost(enchantment, desiredLevel);
        }

        if (totalXpCost <= 0 && totalLapisCost <= 0) {
            return;
        }

        ItemStack lapis = blockEntity.getStack(2);
        if (lapis.isEmpty() || !lapis.isOf(Items.LAPIS_LAZULI) || lapis.getCount() < totalLapisCost) {
            return;
        }
        if (serverPlayer.experienceLevel < totalXpCost) {
            return;
        }

        lapis.decrement(totalLapisCost);
        if (lapis.isEmpty()) {
            blockEntity.setStack(2, ItemStack.EMPTY);
        }
        serverPlayer.addExperienceLevels(-totalXpCost);
        EnchantmentHelper.set(result, target);
        blockEntity.markDirty();
        syncToClient(serverPlayer);
        sendContentUpdates();
    }

    private boolean isCompatible(Map<Enchantment, Integer> existing, Enchantment enchantment) {
        for (Enchantment other : existing.keySet()) {
            if (other == enchantment) {
                continue;
            }
            if (!enchantment.canAccept(other)) {
                return false;
            }
        }
        return true;
    }

    private void syncToClient(ServerPlayerEntity player) {
        ProgressPayload payload = ProgressPayload.create(player, this.blockEntity, player.getUuid());
        PacketByteBuf buf = PacketByteBufs.create();
        payload.write(buf);
        ServerPlayNetworking.send(player, ResearchTableNetworking.SYNC_PROGRESS, buf);
    }

    public ClientState getClientState() {
        return clientState;
    }

    public void applyPayload(ProgressPayload payload) {
        this.clientState.progress = payload.progressEntries();
        this.clientState.options = payload.optionEntries();
        if (this.clientUpdateListener != null) {
            this.clientUpdateListener.run();
        }
    }

    public void setClientUpdateListener(@Nullable Runnable listener) {
        this.clientUpdateListener = listener;
    }

    public record ProgressEntry(Identifier id, boolean discovered, int progress, int maxProgress, int unlockedLevel) {
    }

    public record OptionEntry(Identifier id, boolean discovered, int currentLevel, int maxLevel) {
    }

    public static class ProgressPayload {
        private final List<ProgressEntry> progressEntries;
        private final List<OptionEntry> optionEntries;

        public ProgressPayload(List<ProgressEntry> progressEntries, List<OptionEntry> optionEntries) {
            this.progressEntries = progressEntries;
            this.optionEntries = optionEntries;
        }

        public static ProgressPayload create(ServerPlayerEntity player, ResearchTableBlockEntity blockEntity, UUID uuid) {
            ResearchProgressState state = ResearchProgressState.get(player.getServerWorld());
            List<ProgressEntry> progress = new ArrayList<>();
            Registries.ENCHANTMENT.stream().sorted(Comparator.comparing(enchantment -> Registries.ENCHANTMENT.getId(enchantment).toString())).forEach(enchantment -> {
                Identifier id = Registries.ENCHANTMENT.getId(enchantment);
                if (id == null) {
                    return;
                }
                int currentProgress = state.getProgress(uuid, id);
                int max = state.getMaxProgress(id);
                boolean discovered = state.isDiscovered(uuid, id);
                int unlockedLevel = ResearchLogic.getUnlockedLevel(currentProgress, enchantment);
                progress.add(new ProgressEntry(id, discovered, currentProgress, max, unlockedLevel));
            });

            List<OptionEntry> options = new ArrayList<>();
            ItemStack target = blockEntity.getStack(1);
            if (!target.isEmpty()) {
                Map<Enchantment, Integer> existing = EnchantmentHelper.get(target);
                Registries.ENCHANTMENT.stream().forEach(enchantment -> {
                    if (!enchantment.isAcceptableItem(target)) {
                        return;
                    }
                    Identifier id = Registries.ENCHANTMENT.getId(enchantment);
                    if (id == null) {
                        return;
                    }
                    if (!state.isDiscovered(uuid, id)) {
                        return;
                    }
                    int unlockedLevel = state.getUnlockedLevel(uuid, enchantment);
                    if (unlockedLevel <= 0) {
                        return;
                    }
                    int current = existing.getOrDefault(enchantment, 0);
                    options.add(new OptionEntry(id, true, current, Math.min(unlockedLevel, enchantment.getMaxLevel())));
                });
            }

            return new ProgressPayload(progress, options);
        }

        public void write(PacketByteBuf buf) {
            buf.writeVarInt(progressEntries.size());
            for (ProgressEntry entry : progressEntries) {
                buf.writeIdentifier(entry.id());
                buf.writeBoolean(entry.discovered());
                buf.writeVarInt(entry.progress());
                buf.writeVarInt(entry.maxProgress());
                buf.writeVarInt(entry.unlockedLevel());
            }
            buf.writeVarInt(optionEntries.size());
            for (OptionEntry option : optionEntries) {
                buf.writeIdentifier(option.id());
                buf.writeBoolean(option.discovered());
                buf.writeVarInt(option.currentLevel());
                buf.writeVarInt(option.maxLevel());
            }
        }

        public static ProgressPayload read(PacketByteBuf buf) {
            int progressSize = buf.readVarInt();
            List<ProgressEntry> progressEntries = new ArrayList<>(progressSize);
            for (int i = 0; i < progressSize; i++) {
                progressEntries.add(new ProgressEntry(buf.readIdentifier(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
            }
            int optionSize = buf.readVarInt();
            List<OptionEntry> options = new ArrayList<>(optionSize);
            for (int i = 0; i < optionSize; i++) {
                options.add(new OptionEntry(buf.readIdentifier(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt()));
            }
            return new ProgressPayload(progressEntries, options);
        }

        public List<ProgressEntry> progressEntries() {
            return progressEntries;
        }

        public List<OptionEntry> optionEntries() {
            return optionEntries;
        }
    }

    public static class ClientState {
        private List<ProgressEntry> progress = List.of();
        private List<OptionEntry> options = List.of();

        public List<ProgressEntry> getProgress() {
            return progress;
        }

        public List<OptionEntry> getOptions() {
            return options;
        }
    }
}
