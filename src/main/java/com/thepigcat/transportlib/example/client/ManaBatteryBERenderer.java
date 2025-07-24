package com.thepigcat.transportlib.example.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thepigcat.transportlib.example.ManaBatteryBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FastColor;

public class ManaBatteryBERenderer implements BlockEntityRenderer<ManaBatteryBlockEntity> {
    public ManaBatteryBERenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ManaBatteryBlockEntity manaBatteryBlockEntity, float v, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, int i1) {
        BlockPos blockPos = manaBatteryBlockEntity.getBlockPos();
        poseStack.pushPose();
        {
            poseStack.scale(2, 2, 2);
            DebugRenderer.renderFloatingText(poseStack, multiBufferSource, "Mana: " + manaBatteryBlockEntity.manaStored, blockPos.getX() + 0.5f, blockPos.getY() + 2, blockPos.getZ() + 0.5f, FastColor.ARGB32.color(0, 0, 0));
        }
        poseStack.popPose();
    }
}
