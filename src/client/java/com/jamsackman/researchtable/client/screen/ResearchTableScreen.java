package com.jamsackman.researchtable.client.screen;

import com.jamsackman.researchtable.network.ResearchTableNetworking;
import com.jamsackman.researchtable.progression.ResearchLogic;
import com.jamsackman.researchtable.screen.ResearchTableScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ResearchTableScreen extends HandledScreen<ResearchTableScreenHandler> {
    private static final Text TITLE = Text.translatable("screen.researchtable.title");
    private static final Text TAB_ENCHANTMENTS = Text.translatable("screen.researchtable.tab.enchantments");
    private static final Text TAB_RESEARCH = Text.translatable("screen.researchtable.tab.research");
    private static final Text TAB_CRAFTING = Text.translatable("screen.researchtable.tab.crafting");
    private static final Text BUTTON_RESEARCH = Text.translatable("screen.researchtable.button.research");
    private static final Text BUTTON_APPLY = Text.translatable("screen.researchtable.button.apply");

    private final List<ButtonWidget> tabButtons = new ArrayList<>();
    private final Map<Identifier, Integer> selectedLevels = new HashMap<>();
    private final List<OptionControl> optionControls = new ArrayList<>();

    private ButtonWidget researchButton;
    private ButtonWidget applyButton;

    private int activeTab = 0;
    private int progressScroll = 0;
    private int optionScroll = 0;

    private int costXp = 0;
    private int costLapis = 0;

    public ResearchTableScreen(ResearchTableScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 248;
        this.backgroundHeight = 220;
        handler.setClientUpdateListener(this::onDataUpdated);
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(TITLE)) / 2;
        createTabButtons();
        createActionButtons();
        setActiveTab(0);
        onDataUpdated();
    }

    private void createTabButtons() {
        tabButtons.clear();
        int tabWidth = 80;
        int spacing = 4;
        int xStart = this.x + 8;
        int yPos = this.y - 24;
        tabButtons.add(addDrawableChild(ButtonWidget.builder(TAB_ENCHANTMENTS, button -> setActiveTab(0)).dimensions(xStart, yPos, tabWidth, 20).build()));
        tabButtons.add(addDrawableChild(ButtonWidget.builder(TAB_RESEARCH, button -> setActiveTab(1)).dimensions(xStart + tabWidth + spacing, yPos, tabWidth, 20).build()));
        tabButtons.add(addDrawableChild(ButtonWidget.builder(TAB_CRAFTING, button -> setActiveTab(2)).dimensions(xStart + (tabWidth + spacing) * 2, yPos, tabWidth, 20).build()));
    }

    private void createActionButtons() {
        researchButton = addDrawableChild(ButtonWidget.builder(BUTTON_RESEARCH, button -> ClientPlayNetworking.send(ResearchTableNetworking.CONSUME_ITEM, ResearchTableNetworking.createConsumePacket())).dimensions(this.x + this.backgroundWidth - 96, this.y + this.backgroundHeight - 30, 88, 20).build());
        applyButton = addDrawableChild(ButtonWidget.builder(BUTTON_APPLY, button -> submitSelections()).dimensions(this.x + this.backgroundWidth - 96, this.y + this.backgroundHeight - 30, 88, 20).build());
    }

    private void setActiveTab(int tab) {
        this.activeTab = tab;
        updateTabVisuals();
        researchButton.visible = tab == 1;
        applyButton.visible = tab == 2;
        updateOptionButtonPositions();
    }

    private void updateTabVisuals() {
        for (int i = 0; i < tabButtons.size(); i++) {
            ButtonWidget button = tabButtons.get(i);
            boolean selected = i == activeTab;
            button.active = !selected;
            button.setMessage(getTabLabel(i, selected));
        }
    }

    private Text getTabLabel(int index, boolean selected) {
        Text base = switch (index) {
            case 0 -> TAB_ENCHANTMENTS;
            case 1 -> TAB_RESEARCH;
            case 2 -> TAB_CRAFTING;
            default -> Text.empty();
        };
        return selected ? Text.literal("[ ").append(base).append(" ]") : base;
    }

    private void submitSelections() {
        List<ResearchTableNetworking.EnchantmentSelection> selections = new ArrayList<>();
        handler.getClientState().getOptions().forEach(option -> {
            int level = selectedLevels.getOrDefault(option.id(), option.currentLevel());
            selections.add(new ResearchTableNetworking.EnchantmentSelection(option.id(), level));
        });
        ClientPlayNetworking.send(ResearchTableNetworking.APPLY_ENCHANTMENTS, ResearchTableNetworking.createApplyPacket(selections));
    }

    private void onDataUpdated() {
        handler.getClientState().getOptions().forEach(option -> selectedLevels.putIfAbsent(option.id(), option.currentLevel()));
        selectedLevels.keySet().removeIf(id -> handler.getClientState().getOptions().stream().noneMatch(option -> option.id().equals(id)));
        rebuildOptionControls();
        updateCostSummary();
    }

    private void rebuildOptionControls() {
        for (OptionControl control : optionControls) {
            remove(control.button());
        }
        optionControls.clear();
        for (ResearchTableScreenHandler.OptionEntry option : handler.getClientState().getOptions()) {
            Identifier id = option.id();
            List<Integer> values = new ArrayList<>();
            for (int level = 0; level <= option.maxLevel(); level++) {
                values.add(level);
            }
            int baseLevel = option.currentLevel();
            int current = selectedLevels.getOrDefault(id, baseLevel);
            current = MathHelper.clamp(current, baseLevel, option.maxLevel());
            selectedLevels.put(id, current);
            CyclingButtonWidget<Integer> button = CyclingButtonWidget.builder(value -> Text.literal("Lv " + value))
                    .values(values)
                    .initially(current)
                    .build(this.x + this.backgroundWidth - 90, this.y + 40, 72, 20, Text.translatable("screen.researchtable.level_button"), (btn, value) -> {
                        selectedLevels.put(id, value);
                        updateCostSummary();
                    });
            optionControls.add(new OptionControl(id, addDrawableChild(button)));
        }
        optionScroll = MathHelper.clamp(optionScroll, 0, Math.max(0, optionControls.size() - 6));
        updateOptionButtonPositions();
    }

    private void updateOptionButtonPositions() {
        int visible = 6;
        int start = Math.min(optionScroll, Math.max(0, optionControls.size() - visible));
        int yBase = this.y + 40;
        int index = 0;
        for (int i = 0; i < optionControls.size(); i++) {
            OptionControl control = optionControls.get(i);
            boolean visibleRow = activeTab == 2 && i >= start && index < visible;
            control.button().visible = visibleRow;
            if (visibleRow) {
                int y = yBase + index * 24;
                control.button().setPosition(this.x + this.backgroundWidth - 96, y);
                index++;
            }
        }
    }

    private void updateCostSummary() {
        costXp = 0;
        costLapis = 0;
        for (ResearchTableScreenHandler.OptionEntry option : handler.getClientState().getOptions()) {
            Identifier id = option.id();
            Enchantment enchantment = Registries.ENCHANTMENT.get(id);
            if (enchantment == null) {
                continue;
            }
            int current = option.currentLevel();
            int desired = MathHelper.clamp(selectedLevels.getOrDefault(id, current), 0, option.maxLevel());
            if (desired > current) {
                costXp += ResearchLogic.getXpCost(enchantment, desired);
                costLapis += ResearchLogic.getLapisCost(enchantment, desired);
            }
        }
        boolean hasSelection = costXp > 0 || costLapis > 0;
        boolean canAfford = true;
        if (this.client != null && this.client.player != null) {
            if (costXp > this.client.player.experienceLevel) {
                canAfford = false;
            }
        }
        ItemStack lapisStack = handler.getSlot(2).getStack();
        if (costLapis > lapisStack.getCount()) {
            canAfford = false;
        }
        applyButton.active = activeTab == 2 && hasSelection && canAfford;
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        if (researchButton != null) {
            ItemStack stack = handler.getSlot(0).getStack();
            researchButton.active = activeTab == 1 && !stack.isEmpty() && !EnchantmentHelper.get(stack).isEmpty();
        }
        updateCostSummary();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (activeTab == 0) {
            int total = handler.getClientState().getProgress().size();
            int visible = 9;
            int max = Math.max(0, total - visible);
            progressScroll = MathHelper.clamp(progressScroll - (int) Math.signum(amount), 0, max);
            return true;
        } else if (activeTab == 2) {
            int total = handler.getClientState().getOptions().size();
            int visible = 6;
            int max = Math.max(0, total - visible);
            optionScroll = MathHelper.clamp(optionScroll - (int) Math.signum(amount), 0, max);
            updateOptionButtonPositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int left = this.x;
        int top = this.y;
        context.fill(left, top, left + this.backgroundWidth, top + this.backgroundHeight, 0xFF101010);
        context.fill(left + 8, top + 24, left + this.backgroundWidth - 8, top + this.backgroundHeight - 8, 0xFF1C1C1C);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        drawForeground(context, mouseX, mouseY);
    }

    private void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, TITLE, this.x + (this.backgroundWidth - this.textRenderer.getWidth(TITLE)) / 2, this.y + 8, 0xFFFFFF, false);
        switch (activeTab) {
            case 0 -> renderProgressTab(context);
            case 1 -> renderResearchTab(context);
            case 2 -> renderCraftingTab(context);
        }
    }

    private void renderProgressTab(DrawContext context) {
        List<ResearchTableScreenHandler.ProgressEntry> entries = handler.getClientState().getProgress();
        int visible = 9;
        progressScroll = Math.min(progressScroll, Math.max(0, entries.size() - visible));
        int start = progressScroll;
        int yBase = this.y + 32;
        for (int i = 0; i < visible && start + i < entries.size(); i++) {
            ResearchTableScreenHandler.ProgressEntry entry = entries.get(start + i);
            int y = yBase + i * 18;
            String name = entry.discovered() ? getEnchantmentName(entry.id()) : "???";
            context.drawText(textRenderer, name, this.x + 16, y, entry.discovered() ? 0xFFFFFF : 0xAAAAAA, false);
            int barX = this.x + 140;
            int barWidth = 90;
            int barY = y + 4;
            context.fill(barX, barY, barX + barWidth, barY + 8, 0xFF303030);
            if (entry.maxProgress() > 0) {
                float ratio = Math.min(1.0f, (float) entry.progress() / (float) entry.maxProgress());
                context.fill(barX, barY, barX + (int) (barWidth * ratio), barY + 8, 0xFF4A76E8);
            }
            String progressText = entry.progress() + "/" + entry.maxProgress();
            context.drawText(textRenderer, progressText, barX + 2, barY - 9, 0xBBBBBB, false);
            if (entry.unlockedLevel() > 0) {
                context.drawText(textRenderer, Text.translatable("screen.researchtable.level", entry.unlockedLevel()), barX + barWidth + 6, barY, 0x55FF55, false);
            }
        }
    }

    private void renderResearchTab(DrawContext context) {
        int textX = this.x + 16;
        int textY = this.y + 32;
        context.drawText(textRenderer, Text.translatable("screen.researchtable.research_hint"), textX, textY, 0xFFFFFF, false);
        ItemStack stack = handler.getSlot(0).getStack();
        if (!stack.isEmpty()) {
            context.drawItem(stack, this.x + this.backgroundWidth - 40, this.y + 40);
            Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(stack);
            int row = 0;
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                Enchantment enchantment = entry.getKey();
                int level = entry.getValue();
                int reward = ResearchLogic.getProgressReward(enchantment, level);
                Text line = Text.translatable("screen.researchtable.research_item", getEnchantmentName(Objects.requireNonNull(Registries.ENCHANTMENT.getId(enchantment))), level, reward);
                context.drawText(textRenderer, line, textX, textY + 20 + row * 12, 0xA0A0A0, false);
                row++;
            }
        }
    }

    private void renderCraftingTab(DrawContext context) {
        List<ResearchTableScreenHandler.OptionEntry> options = handler.getClientState().getOptions();
        int visible = 6;
        optionScroll = Math.min(optionScroll, Math.max(0, options.size() - visible));
        int start = optionScroll;
        int yBase = this.y + 40;
        for (int i = 0; i < visible && start + i < options.size(); i++) {
            ResearchTableScreenHandler.OptionEntry option = options.get(start + i);
            int y = yBase + i * 24;
            String name = getEnchantmentName(option.id());
            int selectedLevel = selectedLevels.getOrDefault(option.id(), option.currentLevel());
            context.drawText(textRenderer, name, this.x + 16, y, 0xFFFFFF, false);
            context.drawText(textRenderer, Text.translatable("screen.researchtable.current_level", option.currentLevel(), option.maxLevel()), this.x + 16, y + 12, 0x888888, false);
            context.drawText(textRenderer, Text.translatable("screen.researchtable.selected_level", selectedLevel), this.x + 180, y, 0xA8FF8A, false);
        }
        context.drawText(textRenderer, Text.translatable("screen.researchtable.cost_summary", costXp, costLapis), this.x + 16, this.y + this.backgroundHeight - 28, 0xFFFFFF, false);
    }

    private String getEnchantmentName(Identifier id) {
        Enchantment enchantment = Registries.ENCHANTMENT.get(id);
        if (enchantment == null) {
            return id.toString();
        }
        return Text.translatable(enchantment.getTranslationKey()).getString();
    }

    @Override
    public void close() {
        handler.setClientUpdateListener(null);
        super.close();
    }

    private record OptionControl(Identifier id, CyclingButtonWidget<Integer> button) {
    }
}
