package com.jamsackman.researchtable;

import com.jamsackman.researchtable.block.ResearchTableBlock;
import com.jamsackman.researchtable.block.entity.ResearchTableBlockEntity;
import com.jamsackman.researchtable.network.ResearchTableNetworking;
import com.jamsackman.researchtable.screen.ResearchTableScreenHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroups;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ResearchTableMod implements ModInitializer {
    public static final String MOD_ID = "researchtable";

    public static final Identifier RESEARCH_TABLE_ID = new Identifier(MOD_ID, "research_table");

    public static final Block RESEARCH_TABLE_BLOCK = new ResearchTableBlock(FabricBlockSettings.copyOf(Blocks.ENCHANTING_TABLE));
    public static final Item RESEARCH_TABLE_ITEM = new BlockItem(RESEARCH_TABLE_BLOCK, new Item.Settings());
    public static final BlockEntityType<ResearchTableBlockEntity> RESEARCH_TABLE_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(MOD_ID, "research_table"),
            FabricBlockEntityTypeBuilder.create(ResearchTableBlockEntity::new, RESEARCH_TABLE_BLOCK).build()
    );
    public static final ScreenHandlerType<ResearchTableScreenHandler> RESEARCH_TABLE_SCREEN_HANDLER = ScreenHandlerRegistry.registerExtended(
            new Identifier(MOD_ID, "research_table"),
            ResearchTableScreenHandler::new
    );

    @Override
    public void onInitialize() {
        Registry.register(Registries.BLOCK, RESEARCH_TABLE_ID, RESEARCH_TABLE_BLOCK);
        Registry.register(Registries.ITEM, RESEARCH_TABLE_ID, RESEARCH_TABLE_ITEM);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> entries.addAfter(Blocks.ENCHANTING_TABLE, new ItemStack(RESEARCH_TABLE_ITEM)));

        ResearchTableNetworking.registerServer();
    }
}
