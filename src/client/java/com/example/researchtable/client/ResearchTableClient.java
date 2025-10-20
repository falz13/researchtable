package com.example.researchtable.client;

import com.example.researchtable.ResearchTableMod;
import com.example.researchtable.client.screen.ResearchTableScreen;
import com.example.researchtable.network.ResearchTableNetworking;
import com.example.researchtable.screen.ResearchTableScreenHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;

public class ResearchTableClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ScreenRegistry.register(ResearchTableMod.RESEARCH_TABLE_SCREEN_HANDLER, ResearchTableScreen::new);
        ResearchTableNetworking.registerClient();
    }
}
