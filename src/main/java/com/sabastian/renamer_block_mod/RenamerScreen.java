package com.sabastian.renamer_block_mod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class RenamerScreen extends AbstractContainerScreen<RenamerMenu> {

    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(RenamerBlockMod.MOD_ID, "textures/gui/renamer_gui.png");

    private EditBox name;

    private String renameTo = "";

    public RenamerScreen(RenamerMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float v, int i, int i1) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
        //(((RenamerMenu)this.menu).getSlot(0).hasItem() ? 0 : 16)
        guiGraphics.blit(GUI_TEXTURE, this.leftPos + 33, this.topPos + 54, 0, this.imageHeight, 110, 16);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, pMouseX, pMouseY, pPartialTick);
        this.name.render(guiGraphics, pMouseX, pMouseY, pPartialTick);
        renderTooltip(guiGraphics, pMouseX, pMouseY);
    }

    @Override
    protected void init() {
        super.init();
        this.renameTo = this.menu.blockEntity.renameTo;

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        this.name = new EditBox(this.font, x + 37, y + 59, 103, 12, Component.translatable("block.renamer_block_mod.renamer_block"));
        this.name.setCanLoseFocus(false);
        this.name.setTextColor(-1);
        this.name.setTextColorUneditable(-1);
        this.name.setBordered(false);
        this.name.setMaxLength(40);
        this.name.setValue(this.renameTo);
        this.name.setResponder(this::onNameChanged);
        this.addWidget(this.name);
        this.setInitialFocus(this.name);
        this.name.setEditable(true);
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    private void onNameChanged(String newValue) {
        if (this.renameTo == newValue)
            return;

        this.renameTo = newValue;
        RenamerBlockMod.INSTANCE.sendToServer(new ServerboundRenamerSetPacket(newValue, this.menu.blockEntity.getBlockPos()));
    }

    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (pKeyCode == 256) {
            this.minecraft.player.closeContainer();
        }

        return !this.name.keyPressed(pKeyCode, pScanCode, pModifiers) && !this.name.canConsumeInput() ? super.keyPressed(pKeyCode, pScanCode, pModifiers) : true;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.name.tick();
    }
}