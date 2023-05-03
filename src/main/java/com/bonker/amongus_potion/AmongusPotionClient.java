package com.bonker.amongus_potion;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AmongusPotionClient {
    @Mod.EventBusSubscriber(modid = AmongusPotionMod.MODID, value = Dist.CLIENT)
    public static class ForgeClientEvents {

        @SubscribeEvent
        public static void renderLivingEntity(RenderLivingEvent.Pre<?,?> event) {
            MobEffectInstance amongusEffect = event.getEntity().getEffect(AmongusPotionMod.AMONGUS_EFFECT.get());
            if (amongusEffect != null) {
                event.setCanceled(true);
                AmongusRenderer.INSTANCE.render(event.getEntity(), 0.0F, event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
            }
        }

        @SubscribeEvent
        public static void renderPlayerHand(RenderHandEvent event) {
            if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.getEffect(AmongusPotionMod.AMONGUS_EFFECT.get()) != null) {
                event.setCanceled(true);
            }
        }
    }

    @Mod.EventBusSubscriber(modid = AmongusPotionMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModClientEvents {

        @SubscribeEvent
        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(EntityType.PLAYER, AmongusRenderer::new);
        }

        @SubscribeEvent
        public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(AmongusRenderer.LAYER_LOCATION, AmongusModel::createBodyLayer);
        }
    }

    public static void handlePacket(AmongusPotionMod.ClientboundAmongusSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        Entity entity = level.getEntity(packet.entityId);
        if (entity instanceof LivingEntity livingEntity) {
            if (packet.isAmongus) {
                livingEntity.forceAddEffect(new MobEffectInstance(AmongusPotionMod.AMONGUS_EFFECT.get()), null);
            } else {
                livingEntity.removeEffect(AmongusPotionMod.AMONGUS_EFFECT.get());
            }
        }
    }

    public static class AmongusRenderer extends EntityRenderer<LivingEntity> {
        private static final ResourceLocation AMONGUS_LOCATION = new ResourceLocation(AmongusPotionMod.MODID, "textures/entity/amongus.png");
        public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(AMONGUS_LOCATION, "main");
        private final AmongusModel model;

        public static AmongusRenderer INSTANCE;

        public AmongusRenderer(EntityRendererProvider.Context context) {
            super(context);
            this.model = new AmongusModel(context.bakeLayer(LAYER_LOCATION));
            INSTANCE = this;
        }

        @Override
        public void render(LivingEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
            super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
            model.setupAnim(entity, 0, 0, 0, 0, 0);
            model.renderToBuffer(poseStack, bufferSource.getBuffer(model.renderType(AMONGUS_LOCATION)), packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 0.15F);
        }

        @Override
        public ResourceLocation getTextureLocation(LivingEntity entity) {
            return AMONGUS_LOCATION;
        }
    }

    public static class AmongusModel extends EntityModel<LivingEntity> {
        private final ModelPart rightLeg;
        private final ModelPart leftLeg;
        private final ModelPart body;
        private final Iterable<ModelPart> parts;

        public AmongusModel(ModelPart root) {
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
            body = root.getChild("body");
            parts = ImmutableList.of(rightLeg, leftLeg, body);
        }

        public static LayerDefinition createBodyLayer() {
            MeshDefinition meshdefinition = new MeshDefinition();
            PartDefinition partdefinition = meshdefinition.getRoot();

            partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(36, 0).addBox(1.0F, -4.0F, -2.0F, 4.0F, 4.0F, 4.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));
            partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 38).addBox(-5.0F, -4.0F, -2.0F, 4.0F, 4.0F, 4.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));
            partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -19.0F, -4.0F, 14.0F, 15.0F, 8.0F, CubeDeformation.NONE)
                    .texOffs(32, 23).addBox(-5.0F, -22.0F, -3.0F, 10.0F, 3.0F, 6.0F, CubeDeformation.NONE)
                    .texOffs(32, 32).addBox(-5.0F, -18.0F, 4.0F, 10.0F, 6.0F, 4.0F, CubeDeformation.NONE)
                    .texOffs(0, 23).addBox(-6.0F, -18.0F, -8.0F, 12.0F, 11.0F, 4.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

            return LayerDefinition.create(meshdefinition, 64, 64);
        }

        @Override
        public void setupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            boolean gliding = entity.getFallFlyingTicks() > 4;
            boolean swimming = entity.isVisuallySwimming();

            parts.forEach((part) -> {
                part.xRot = 0;
                part.yRot = entity.getYRot() * (float) (Math.PI / 180);
            });

//        rightLeg.xRot = Mth.cos(limbSwing) * 1.4F;
//        leftLeg.xRot = Mth.cos(limbSwing + (float)Math.PI) * 1.4F;

            if (gliding || swimming) {
                parts.forEach((part) -> part.xRot += 80);
            }
            body.xRot += entity.getXRot() * (float) (Math.PI / 180) * -0.5F;
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
            poseStack.pushPose();

            poseStack.translate(0, 1.5, 0);
            poseStack.mulPose(Axis.XP.rotationDegrees(180));
            poseStack.mulPose(Axis.YP.rotationDegrees(180));

            rightLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
            leftLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
            body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
            poseStack.popPose();
        }
    }
}
