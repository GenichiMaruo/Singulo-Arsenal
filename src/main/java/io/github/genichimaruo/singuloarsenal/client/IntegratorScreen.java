package io.github.genichimaruo.singuloarsenal.client;

import io.github.genichimaruo.singuloarsenal.module.IntegratorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Drawn with plain fills: a gear bed in the middle, sockets around it, and lines showing which sockets this gear actually has. */
public final class IntegratorScreen extends AbstractContainerScreen<IntegratorMenu> {
    private static final int PANEL = 0xFF23212C, EDGE = 0xFF0F0E15, RIM = 0xFF4C4860, SLOT = 0xFF14131B, ACCENT = 0xFF7CE8FF, LOCKED = 0xFF2C2A36;

    public IntegratorScreen(IntegratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 73;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("screen.singulo_arsenal.integrate"), b -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }).bounds(leftPos + 116, topPos + 58, 52, 18).build());
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partial) {
        super.render(gui, mouseX, mouseY, partial);
        renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partial, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        gui.fill(x, y, x + imageWidth, y + imageHeight, EDGE);
        gui.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, RIM);
        gui.fill(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, PANEL);
        gui.fill(x + 3, y + 3, x + imageWidth - 3, y + 4, 0xFF6A6480);

        // Conduits from the bed to each socket, lit once the gear has that socket.
        int cx = x + IntegratorMenu.POSITIONS[0][0] + 8, cy = y + IntegratorMenu.POSITIONS[0][1] + 8;
        for (int socket = 0; socket < 3; socket++) {
            int[] p = IntegratorMenu.POSITIONS[socket + 1];
            int sx = x + p[0] + 8, sy = y + p[1] + 8;
            int color = menu.socketOpen(socket) ? (menu.working() ? 0xFFFFFFFF : ACCENT) : LOCKED;
            gui.fill(Math.min(cx, sx) - 1, Math.min(cy, sy) - 1, Math.max(cx, sx) + 1, Math.max(cy, sy) + 1, color);
        }
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            boolean locked = i >= 1 && i <= 3 && !menu.socketOpen(i - 1);
            gui.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, i == 0 ? ACCENT : i <= 3 && !locked ? 0xFF5FB9D0 : RIM);
            gui.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, locked ? LOCKED : SLOT);
        }
        // Stored FE on the left, integration progress under the bed.
        int barX = x + 12, barTop = y + 20, barHeight = 50;
        gui.fill(barX - 1, barTop - 1, barX + 9, barTop + barHeight + 1, RIM);
        gui.fill(barX, barTop, barX + 8, barTop + barHeight, SLOT);
        int filled = barHeight * menu.energy() / 100;
        gui.fill(barX, barTop + barHeight - filled, barX + 8, barTop + barHeight, 0xFFFFC36B);
        int px = x + 44, py = y + 20;
        gui.fill(px, py, px + 88, py + 3, SLOT);
        gui.fill(px, py, px + 88 * menu.progress() / 100, py + 3, ACCENT);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(font, title, titleLabelX, titleLabelY, 0xFFE9EEF2, false);
        gui.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFB9B5C8, false);
    }
}
