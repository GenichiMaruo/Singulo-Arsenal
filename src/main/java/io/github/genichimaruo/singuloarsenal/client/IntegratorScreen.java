package io.github.genichimaruo.singuloarsenal.client;

import io.github.genichimaruo.singuloarsenal.ArsenalItems;
import io.github.genichimaruo.singuloarsenal.armor.ArmorSet;
import io.github.genichimaruo.singuloarsenal.core.Rules;
import io.github.genichimaruo.singuloarsenal.core.Tips;
import io.github.genichimaruo.singuloarsenal.module.IntegratorBlockEntity;
import io.github.genichimaruo.singuloarsenal.module.IntegratorMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

/** Laid out by Singulo's rules: header with a status chip, the machine's own part, a groove, the inventory. Numbers are icons and values; sentences live in tooltips. */
public final class IntegratorScreen extends AbstractContainerScreen<IntegratorMenu> {
    private static final int BAR_X = 11, BAR_Y = 24, BAR_W = 6, BAR_H = 58;
    private static final int ARROW_X = IntegratorMenu.ARROW_X, ARROW_Y = IntegratorMenu.ARROW_Y;
    private static final int BUTTON_X = 96, BUTTON_Y = 62, BUTTON_W = 54, BUTTON_H = 15;
    private static final int STAT_Y = 72, COST_X = 26, TIME_X = 62;
    private final ItemStack ghost;

    public IntegratorScreen(IntegratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = IntegratorMenu.WIDTH;
        imageHeight = IntegratorMenu.HEIGHT;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = IntegratorMenu.INVENTORY_X;
        inventoryLabelY = imageHeight - 93;
        ghost = new ItemStack(ArsenalItems.SUITS.get(ArmorSet.COLLAPSAR).get(ArmorItem.Type.CHESTPLATE).get());
    }

    private record State(GuiKit.Status icon, int color, String key) {}

    private State state() {
        if (menu.working()) {
            return menu.energyStored() >= Rules.INTEGRATE_FE_PER_TICK ? new State(GuiKit.Status.RUN, GuiKit.OK, "running") : new State(GuiKit.Status.NO_POWER, GuiKit.WARN, "no_power");
        }
        if (menu.gear().isEmpty()) return new State(GuiKit.Status.IDLE, GuiKit.IDLE, "empty");
        if (!menu.valid()) return new State(GuiKit.Status.WARN, GuiKit.BAD, "invalid");
        return new State(GuiKit.Status.IDLE, GuiKit.IDLE, "ready");
    }

    private static MutableComponent line(String key, Object... args) {
        return Component.translatable("screen.singulo_arsenal." + key, args);
    }

    private GuiKit.Press buttonState(int mouseX, int mouseY) {
        if (!menu.canStart()) return GuiKit.Press.DISABLED;
        return GuiKit.inside(mouseX, mouseY, leftPos + BUTTON_X, topPos + BUTTON_Y, BUTTON_W, BUTTON_H) ? GuiKit.Press.HOVER : GuiKit.Press.NORMAL;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && menu.canStart() && GuiKit.inside(mouseX, mouseY, leftPos + BUTTON_X, topPos + BUTTON_Y, BUTTON_W, BUTTON_H)) {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partial) {
        super.render(gui, mouseX, mouseY, partial);
        renderTooltip(gui, mouseX, mouseY);
        List<Component> tip = hovered(mouseX, mouseY);
        if (!tip.isEmpty() && hoveredSlot == null) GuiKit.tooltip(gui, font, tip, 200, mouseX, mouseY);
    }

    private List<Component> hovered(int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        if (GuiKit.inside(mouseX, mouseY, x + BAR_X - 1, y + BAR_Y - 1, BAR_W + 2, BAR_H + 2)) {
            List<Component> lines = new ArrayList<>();
            lines.add(line("energy", Tips.energy(menu.energyStored()), Tips.energy(Rules.INTEGRATOR_CAPACITY)));
            if (!Tips.detail()) Tips.hint(lines);
            return lines;
        }
        int chipRight = imageWidth - 6;
        if (GuiKit.inside(mouseX, mouseY, x + chipRight - GuiKit.CHIP_WIDTH, y + GuiKit.POWER_Y, GuiKit.CHIP_WIDTH, 11)) {
            State state = state();
            return List.of(line("status." + state.key).withStyle(s -> s.withColor(state.color)));
        }
        if (GuiKit.inside(mouseX, mouseY, x + COST_X, y + STAT_Y - 1, 34, 10)) return List.of(line("cost"));
        if (GuiKit.inside(mouseX, mouseY, x + TIME_X, y + STAT_Y - 1, 24, 10)) return List.of(line("time"));
        return List.of();
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partial, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        GuiKit.frame(gui, x, y, imageWidth, imageHeight);
        GuiKit.header(gui, x, y, imageWidth);
        GuiKit.groove(gui, x + 7, y + imageHeight - 96, imageWidth - 14);

        GuiKit.gauge(gui, x + BAR_X, y + BAR_Y, BAR_W, BAR_H, (double) menu.energyStored() / Rules.INTEGRATOR_CAPACITY, GuiKit.GLOW);
        GuiKit.arrow(gui, x + ARROW_X, y + ARROW_Y, menu.progress() / 100D);

        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            int sx = x + slot.x, sy = y + slot.y;
            boolean socket = i >= IntegratorBlockEntity.FIRST_SOCKET && i < IntegratorBlockEntity.SLOTS;
            if (socket && !menu.socketOpen(i - IntegratorBlockEntity.FIRST_SOCKET) && !slot.hasItem()) {
                GuiKit.lockedSlot(gui, sx, sy);
            } else {
                GuiKit.slot(gui, sx, sy);
                if (socket && slot.hasItem() && !menu.socketHolds(i - IntegratorBlockEntity.FIRST_SOCKET)) gui.renderOutline(sx - 1, sy - 1, 18, 18, GuiKit.BAD);
            }
        }
        if (!menu.slots.get(IntegratorBlockEntity.GEAR).hasItem()) {
            Slot bed = menu.slots.get(IntegratorBlockEntity.GEAR);
            gui.pose().pushPose();
            gui.renderItem(ghost, x + bed.x, y + bed.y);
            gui.pose().translate(0, 0, 200);
            gui.fill(x + bed.x, y + bed.y, x + bed.x + 16, y + bed.y + 16, 0xA05A646E);
            gui.pose().popPose();
        }

        GuiKit.labelButton(gui, font, x + BUTTON_X, y + BUTTON_Y, BUTTON_W, BUTTON_H, line("integrate"), buttonState(mouseX, mouseY));

        GuiKit.bolt(gui, x + COST_X, y + STAT_Y, GuiKit.WARN);
        gui.drawString(font, Rules.compactEnergy((long) Rules.INTEGRATE_TICKS * Rules.INTEGRATE_FE_PER_TICK), x + COST_X + 10, y + STAT_Y, GuiKit.TEXT, false);
        GuiKit.hourglass(gui, x + TIME_X, y + STAT_Y, GuiKit.DIM);
        gui.drawString(font, Rules.INTEGRATE_TICKS / 20 + "s", x + TIME_X + 10, y + STAT_Y, GuiKit.TEXT, false);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(font, title, titleLabelX, titleLabelY, GuiKit.TEXT, false);
        gui.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiKit.TEXT, false);
        State state = state();
        GuiKit.chip(gui, imageWidth - 6, GuiKit.POWER_Y, state.icon, state.color);
    }
}
