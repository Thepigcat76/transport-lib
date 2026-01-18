package com.thepigcat.transportlib.client.debug;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.thepigcat.transportlib.TransportLib;
import com.thepigcat.transportlib.api.NetworkNode;
import com.thepigcat.transportlib.impl.NetworkNodeImpl;
import com.thepigcat.transportlib.api.TransportNetwork;
import com.thepigcat.transportlib.client.ClientNodes;
import com.thepigcat.transportlib.client.TLRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.Collections;

public final class TransportNetworkRenderer {
    public static NetworkNodeImpl<?> selectedNode;

    public static void renderNetworkNodes(RenderLevelStageEvent event) {
        Vec3 cameraPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = event.getLevelRenderer().renderBuffers.bufferSource();

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            for (TransportNetwork<?> network : TransportLib.NETWORK_REGISTRY) {
                for (NetworkNode<?> node : ClientNodes.NODES.getOrDefault(network, Collections.emptyMap()).values()) {
                    render(node, poseStack, bufferSource, cameraPos);
                }

                for (BlockPos interactor : ClientNodes.INTERACTORS.getOrDefault(network, Collections.emptySet())) {
                    renderInteractor(interactor, poseStack, bufferSource, cameraPos);
                }
            }

            if (selectedNode != null) {
                render(selectedNode, poseStack, bufferSource, cameraPos);
            }
        }
    }

    private static void renderInteractor(BlockPos interactorPos, PoseStack poseStack, MultiBufferSource bufferSource, Vec3 cameraPos) {
        RenderSystem.disableDepthTest();    // Don't test depth
        RenderSystem.depthMask(false); // Don't write to depth buffer
        RenderSystem.disableCull();
        VertexConsumer consumer = bufferSource.getBuffer(TLRenderTypes.TEST_RENDER_TYPE);

        poseStack.pushPose();
        {
            poseStack.translate((double) interactorPos.getX() - cameraPos.x(), (double) interactorPos.getY() - cameraPos.y(), (double) interactorPos.getZ() - cameraPos.z());
            poseStack.scale(1.25f, 1.25f, 1.25f);
            poseStack.translate(-0.1, -0.1, -0.1);
            renderCube(consumer, poseStack.last().pose(), 255, 0, 255, 70);
        }
        poseStack.popPose();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }

    private static void render(NetworkNode<?> node, PoseStack poseStack, MultiBufferSource bufferSource, Vec3 cameraPos) {
        RenderSystem.disableDepthTest();    // Don't test depth
        RenderSystem.depthMask(false); // Don't write to depth buffer
        RenderSystem.disableCull();
        VertexConsumer consumer = bufferSource.getBuffer(TLRenderTypes.TEST_RENDER_TYPE);

        poseStack.pushPose();
        {
            poseStack.translate((double) node.getPos().getX() - cameraPos.x(), (double) node.getPos().getY() - cameraPos.y(), (double) node.getPos().getZ() - cameraPos.z());
            int r = 0;
            int g = 255;
            int b = 0;
            if (node == selectedNode) {
                r = 100;
                b = 100;
            } else if (node.isDead()) {
                r = 255;
                g = 0;
            } else if (node.hasInteractorConnections()) {
                r = 255;
                g = 0;
                b = 255;
            }

            if (!node.hasInteractorConnections()) {
                renderCube(consumer, poseStack.last().pose(), r, g, b, 70);
            } else {
                for (Direction connection : Direction.values()) {
                    if (node.hasInteractorConnection(connection)) {
                        renderLine(poseStack, connection, consumer, r, g, b, 70);
                    }
                }
            }

            VertexConsumer consumer2 = bufferSource.getBuffer(TLRenderTypes.TEST_RENDER_TYPE);
            if (node.hasNextNodes()) {
                for (Direction direction : Direction.values()) {
                    NetworkNode<?> nextNode = node.getNextNode(direction);
                    if (nextNode != null) {
                        poseStack.pushPose();
                        {
                            renderLine(poseStack, direction, consumer2, 255, 0, 0, 200);
                        }
                        poseStack.popPose();
                    }
                }
            }
        }
        poseStack.popPose();

        if (node == selectedNode) {
            if (node.hasNextNodes()) {
                for (Direction direction : Direction.values()) {
                    NetworkNode<?> nextNode = node.getNextNode(direction);
                    if (nextNode != null) {
                        BlockPos pos = nextNode.getPos();
                        poseStack.pushPose();
                        {
                            poseStack.translate((double) pos.getX() - cameraPos.x(), (double) pos.getY() - cameraPos.y(), (double) pos.getZ() - cameraPos.z());
                            renderCube(consumer, poseStack.last().pose(), 0, 0, 0, 200);
                        }
                        poseStack.popPose();
                    }
                }
            }
        }

        int i = 0;
        if (node.hasNextNodes()) {
            for (Direction direction : Direction.values()) {
                NetworkNode<?> nextNode = node.getNextNode(direction);
                if (nextNode != null) {
                    poseStack.pushPose();
                    {
                        poseStack.translate(0.5, 0.5, 0.5);
                        BlockPos pos = nextNode.getPos();
                        DebugRenderer.renderFloatingText(poseStack, bufferSource, String.format("%s: %d, %d, %d", direction.toString(), pos.getX(), pos.getY(), pos.getZ()), node.getPos().getX(), node.getPos().getY() + (3f - i / 2f), node.getPos().getZ(), -1);
                    }
                    poseStack.popPose();
                    i++;
                }
            }
        }

        DebugRenderer.renderFloatingText(poseStack, bufferSource, String.format("Transporting: %s", node.getTransporting().toString()), node.getPos().getX(), node.getPos().getY() + (3f - i / 2f) + 0.5f, node.getPos().getZ(), -1);

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }

    private static void renderLine(PoseStack poseStack, Direction direction, VertexConsumer consumer2, int r, int g, int b, int a) {
        poseStack.pushPose();
        {
            poseStack.translate(-0.5, 0, -0.5);
            if (direction == Direction.UP) {
                poseStack.translate(0, 0.75, 0);
            } else if (direction == Direction.DOWN) {
                poseStack.translate(0, 0.25, 2);
            } else if (direction.getAxis() == Direction.Axis.X) {
                if (direction == Direction.WEST) {
                    poseStack.translate(0.75, 1.5, 0);
                } else {
                    poseStack.translate(1.25, 1.5, 2);
                }
            } else if (direction.getAxis() == Direction.Axis.Z) {
                if (direction == Direction.SOUTH) {
                    poseStack.translate(0, 1.5, 1.25);
                } else {
                    poseStack.translate(2, 1.5, 0.75);
                }
            }
            poseStack.mulPose(direction.getRotation());
            renderLine(consumer2, poseStack.last().pose(), r, g, b, a, direction);
        }
        poseStack.popPose();
    }

    private static void renderCube(VertexConsumer consumer, Matrix4f matrix, int r, int g, int b, int a) {
        consumer.addVertex(matrix, 0, 1, 0).setColor(r, g, b, a).setNormal(0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix, 0, 1, 1).setColor(r, g, b, a).setNormal(0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix, 1, 1, 1).setColor(r, g, b, a).setNormal(0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix, 1, 1, 0).setColor(r, g, b, a).setNormal(0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix, 0, 0, 0).setColor(r, g, b, a).setNormal(0.0F, -1.0F, 0.0F);
        consumer.addVertex(matrix, 1, 0, 0).setColor(r, g, b, a).setNormal(0.0F, -1.0F, 0.0F);
        consumer.addVertex(matrix, 1, 0, 1).setColor(r, g, b, a).setNormal(0.0F, -1.0F, 0.0F);
        consumer.addVertex(matrix, 0, 0, 1).setColor(r, g, b, a).setNormal(0.0F, -1.0F, 0.0F);
        consumer.addVertex(matrix, 0, 0, 1).setColor(r, g, b, a).setNormal(0.0F, 0.0F, 1.0F);
        consumer.addVertex(matrix, 1, 0, 1).setColor(r, g, b, a).setNormal(0.0F, 0.0F, 1.0F);
        consumer.addVertex(matrix, 1, 1, 1).setColor(r, g, b, a).setNormal(0.0F, 0.0F, 1.0F);
        consumer.addVertex(matrix, 0, 1, 1).setColor(r, g, b, a).setNormal(0.0F, 0.0F, 1.0F);
        consumer.addVertex(matrix, 0, 0, 0).setColor(r, g, b, a).setNormal(0.0F, 0.0F, -1.0F);
        consumer.addVertex(matrix, 0, 1, 0).setColor(r, g, b, a).setNormal(0.0F, 0.0F, -1.0F);
        consumer.addVertex(matrix, 1, 1, 0).setColor(r, g, b, a).setNormal(0.0F, 0.0F, -1.0F);
        consumer.addVertex(matrix, 1, 0, 0).setColor(r, g, b, a).setNormal(0.0F, 0.0F, -1.0F);
        consumer.addVertex(matrix, 0, 0, 0).setColor(r, g, b, a).setNormal(-1.0F, 0.0F, 0.0F);
        consumer.addVertex(matrix, 0, 0, 1).setColor(r, g, b, a).setNormal(-1.0F, 0.0F, 0.0F);
        consumer.addVertex(matrix, 0, 1, 1).setColor(r, g, b, a).setNormal(-1.0F, 0.0F, 0.0F);
        consumer.addVertex(matrix, 0, 1, 0).setColor(r, g, b, a).setNormal(-1.0F, 0.0F, 0.0F);
        consumer.addVertex(matrix, 1, 0, 0).setColor(r, g, b, a).setNormal(1.0F, 0.0F, 0.0F);
        consumer.addVertex(matrix, 1, 1, 0).setColor(r, g, b, a).setNormal(1.0F, 0.0F, 0.0F);
        consumer.addVertex(matrix, 1, 1, 1).setColor(r, g, b, a).setNormal(1.0F, 0.0F, 0.0F);
        consumer.addVertex(matrix, 1, 0, 1).setColor(r, g, b, a).setNormal(1.0F, 0.0F, 0.0F);
    }

    private static void renderLine(VertexConsumer consumer, Matrix4f matrix, int r, int g, int b, int a, Direction direction) {
        float x = 1f;
        float z = 1f;
        float halfSize = 0.025f; // half of thickness
        float yBottom = -0.25f;
        float yTop = 0.5f;

        // FRONT FACE
        consumer.addVertex(matrix, x - halfSize, yBottom, z - halfSize).setColor(r, g, b, a).setNormal(0, 0, -1);
        consumer.addVertex(matrix, x + halfSize, yBottom, z - halfSize).setColor(r, g, b, a).setNormal(0, 0, -1);
        consumer.addVertex(matrix, x + halfSize, yTop, z - halfSize).setColor(r, g, b, a).setNormal(0, 0, -1);
        consumer.addVertex(matrix, x - halfSize, yTop, z - halfSize).setColor(r, g, b, a).setNormal(0, 0, -1);

        // BACK FACE
        consumer.addVertex(matrix, x + halfSize, yBottom, z + halfSize).setColor(r, g, b, a).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x - halfSize, yBottom, z + halfSize).setColor(r, g, b, a).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x - halfSize, yTop, z + halfSize).setColor(r, g, b, a).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x + halfSize, yTop, z + halfSize).setColor(r, g, b, a).setNormal(0, 0, 1);

        // LEFT FACE
        consumer.addVertex(matrix, x - halfSize, yBottom, z + halfSize).setColor(r, g, b, a).setNormal(-1, 0, 0);
        consumer.addVertex(matrix, x - halfSize, yBottom, z - halfSize).setColor(r, g, b, a).setNormal(-1, 0, 0);
        consumer.addVertex(matrix, x - halfSize, yTop, z - halfSize).setColor(r, g, b, a).setNormal(-1, 0, 0);
        consumer.addVertex(matrix, x - halfSize, yTop, z + halfSize).setColor(r, g, b, a).setNormal(-1, 0, 0);

        // RIGHT FACE
        consumer.addVertex(matrix, x + halfSize, yBottom, z - halfSize).setColor(r, g, b, a).setNormal(1, 0, 0);
        consumer.addVertex(matrix, x + halfSize, yBottom, z + halfSize).setColor(r, g, b, a).setNormal(1, 0, 0);
        consumer.addVertex(matrix, x + halfSize, yTop, z + halfSize).setColor(r, g, b, a).setNormal(1, 0, 0);
        consumer.addVertex(matrix, x + halfSize, yTop, z - halfSize).setColor(r, g, b, a).setNormal(1, 0, 0);

        // TOP FACE
        consumer.addVertex(matrix, x - halfSize, yTop, z - halfSize).setColor(r, g, b, a).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x + halfSize, yTop, z - halfSize).setColor(r, g, b, a).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x + halfSize, yTop, z + halfSize).setColor(r, g, b, a).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x - halfSize, yTop, z + halfSize).setColor(r, g, b, a).setNormal(0, 1, 0);

        // BOTTOM FACE
        consumer.addVertex(matrix, x - halfSize, yBottom, z + halfSize).setColor(r, g, b, a).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x + halfSize, yBottom, z + halfSize).setColor(r, g, b, a).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x + halfSize, yBottom, z - halfSize).setColor(r, g, b, a).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x - halfSize, yBottom, z - halfSize).setColor(r, g, b, a).setNormal(0, -1, 0);

//        consumer.addVertex(matrix, 1, 0, 1).setColor(r, g, b, a).setNormal(1, 0, 0);
//        consumer.addVertex(matrix, 1, 0.5f, 1).setColor(r, g, b, a).setNormal(1, 0, 0);

    }
}
