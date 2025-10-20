package com.example.researchtable.progression;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ResearchProgressState extends PersistentState {
    private static final Type<ResearchProgressState> TYPE = new Type<>(ResearchProgressState::new, ResearchProgressState::fromNbt, null);

    private final Map<UUID, Map<Identifier, ResearchProgressEntry>> data = new HashMap<>();

    public static ResearchProgressState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE, "researchtable_progress");
    }

    private static ResearchProgressState fromNbt(NbtCompound nbt) {
        ResearchProgressState state = new ResearchProgressState();
        NbtList players = nbt.getList("Players", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < players.size(); i++) {
            NbtCompound playerTag = players.getCompound(i);
            UUID uuid = Uuids.fromString(playerTag.getString("Id"));
            NbtList entries = playerTag.getList("Entries", NbtElement.COMPOUND_TYPE);
            Map<Identifier, ResearchProgressEntry> map = new HashMap<>();
            for (int j = 0; j < entries.size(); j++) {
                NbtCompound entryTag = entries.getCompound(j);
                Identifier id = new Identifier(entryTag.getString("Enchant"));
                boolean discovered = entryTag.getBoolean("Discovered");
                int progress = entryTag.getInt("Progress");
                map.put(id, new ResearchProgressEntry(discovered, progress));
            }
            state.data.put(uuid, map);
        }
        return state;
    }

    public ResearchProgressState() {
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList players = new NbtList();
        for (Map.Entry<UUID, Map<Identifier, ResearchProgressEntry>> playerEntry : data.entrySet()) {
            NbtCompound playerTag = new NbtCompound();
            playerTag.putString("Id", playerEntry.getKey().toString());
            NbtList entries = new NbtList();
            for (Map.Entry<Identifier, ResearchProgressEntry> entry : playerEntry.getValue().entrySet()) {
                NbtCompound entryTag = new NbtCompound();
                entryTag.putString("Enchant", entry.getKey().toString());
                entryTag.putBoolean("Discovered", entry.getValue().discovered());
                entryTag.putInt("Progress", entry.getValue().progress());
                entries.add(entryTag);
            }
            playerTag.put("Entries", entries);
            players.add(playerTag);
        }
        nbt.put("Players", players);
        return nbt;
    }

    public ResearchProgressEntry getEntry(UUID playerId, Identifier enchantmentId) {
        return data.computeIfAbsent(playerId, id -> new HashMap<>()).getOrDefault(enchantmentId, ResearchProgressEntry.EMPTY);
    }

    public void addProgress(UUID playerId, Identifier enchantmentId, int progress) {
        Map<Identifier, ResearchProgressEntry> map = data.computeIfAbsent(playerId, id -> new HashMap<>());
        ResearchProgressEntry entry = map.getOrDefault(enchantmentId, ResearchProgressEntry.EMPTY);
        boolean discovered = entry.discovered() || progress > 0;
        int newProgress = Math.min(entry.progress() + progress, getMaxProgress(enchantmentId));
        map.put(enchantmentId, new ResearchProgressEntry(discovered, newProgress));
        markDirty();
    }

    public void unlock(UUID playerId, Identifier enchantmentId) {
        Map<Identifier, ResearchProgressEntry> map = data.computeIfAbsent(playerId, id -> new HashMap<>());
        ResearchProgressEntry entry = map.getOrDefault(enchantmentId, ResearchProgressEntry.EMPTY);
        map.put(enchantmentId, new ResearchProgressEntry(true, entry.progress()));
        markDirty();
    }

    public int getProgress(UUID playerId, Identifier enchantmentId) {
        return getEntry(playerId, enchantmentId).progress();
    }

    public boolean isDiscovered(UUID playerId, Identifier enchantmentId) {
        return getEntry(playerId, enchantmentId).discovered();
    }

    public int getUnlockedLevel(UUID playerId, Enchantment enchantment) {
        Identifier id = Registries.ENCHANTMENT.getId(enchantment);
        if (id == null) {
            return 0;
        }
        int progress = getProgress(playerId, id);
        return ResearchLogic.getUnlockedLevel(progress, enchantment);
    }

    public int getMaxProgress(Identifier enchantmentId) {
        Enchantment enchantment = Registries.ENCHANTMENT.get(enchantmentId);
        if (enchantment == null) {
            return 0;
        }
        return ResearchLogic.getMaxProgress(enchantment);
    }

    public record ResearchProgressEntry(boolean discovered, int progress) {
        private static final ResearchProgressEntry EMPTY = new ResearchProgressEntry(false, 0);
    }
}
