package com.jamsackman.researchtable.client;

import com.jamsackman.researchtable.ResearchTableMod;
import com.jamsackman.researchtable.client.screen.ResearchTableScreen;
import com.jamsackman.researchtable.network.ResearchTableNetworking;
import com.jamsackman.researchtable.screen.ResearchTableScreenHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;

public class ResearchTableClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ScreenRegistry.register(ResearchTableMod.RESEARCH_TABLE_SCREEN_HANDLER, ResearchTableScreen::new);
        ResearchTableNetworking.registerClient();
    }
}
