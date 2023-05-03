package com.bonker.amongus_potion;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.brewing.IBrewingRecipe;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

@Mod(AmongusPotionMod.MODID)
public class AmongusPotionMod {
    public static final String MODID = "amongus_potion";

    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MODID);
    public static final RegistryObject<MobEffect> AMONGUS_EFFECT = MOB_EFFECTS.register("amongus",
            () -> new AmongusMobEffect(MobEffectCategory.NEUTRAL, 0xFF0000));

    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(ForgeRegistries.POTIONS, MODID);
    public static final RegistryObject<Potion> AMONGUS_POTION = POTIONS.register("amongus_potion",
            () -> new Potion(new MobEffectInstance(AMONGUS_EFFECT.get(), 3600, 0, true, true, true)));

    public AmongusPotionMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        MOB_EFFECTS.register(bus);
        POTIONS.register(bus);

        registerPackets();
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModCommonEvents {
        @SubscribeEvent
        public static void commonSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(() -> {
                BrewingRecipeRegistry.addRecipe(new IBrewingRecipe() {
                    @Override
                    public boolean isInput(ItemStack input) {return PotionUtils.getPotion(input) == AMONGUS_POTION.get();}
                    @Override
                    public boolean isIngredient(ItemStack ingredient) {return ingredient.is(Items.SUSPICIOUS_STEW);}
                    @Override
                    public ItemStack getOutput(ItemStack input, ItemStack ingredient) {return PotionUtils.setPotion(input.copy(), AMONGUS_POTION.get());}
                });
            });
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeCommonEvents {
        @SubscribeEvent
        public static void mobEffectAdded(MobEffectEvent.Added event) {
            if (event.getEntity().level.isClientSide) return;
            if (event.getEffectInstance().getEffect() == AMONGUS_EFFECT.get() && !(event.getEntity() instanceof Player)) {
                sendPacketToAll(new ClientboundAmongusSyncPacket(event.getEntity().getId(), true));
            }
        }

        @SubscribeEvent
        public static void mobEffectRemoved(MobEffectEvent.Remove event) {
            if (event.getEffectInstance() == null) return;
            if (event.getEntity().level.isClientSide) return;
            if (event.getEffectInstance().getEffect() == AMONGUS_EFFECT.get() && !(event.getEntity() instanceof Player)) {
                sendPacketToAll(new ClientboundAmongusSyncPacket(event.getEntity().getId(), false));
            }
        }

        @SubscribeEvent
        public static void mobEffectExpired(MobEffectEvent.Expired event) {
            if (event.getEffectInstance() == null) return;
            if (event.getEntity().level.isClientSide) return;
            if (event.getEffectInstance().getEffect() == AMONGUS_EFFECT.get() && !(event.getEntity() instanceof Player)) {
                sendPacketToAll(new ClientboundAmongusSyncPacket(event.getEntity().getId(), false));
            }
        }
    }

    public static class ClientboundAmongusSyncPacket {
        public final int entityId;
        public final boolean isAmongus;

        public ClientboundAmongusSyncPacket(int entityId, boolean isAmongus) {
            this.entityId = entityId;
            this.isAmongus = isAmongus;
        }

        public void encoder(FriendlyByteBuf buf) {
            buf.writeInt(entityId);
            buf.writeBoolean(isAmongus);
        }

        public static ClientboundAmongusSyncPacket decoder(FriendlyByteBuf buf) {
            return new ClientboundAmongusSyncPacket(buf.readInt(), buf.readBoolean());
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> AmongusPotionClient.handlePacket(this, ctx))
            );
            ctx.get().setPacketHandled(true);
        }
    }

    public static final SimpleChannel NETWORKING_CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "main"),
            () -> "1",
            "1"::equals,
            "1"::equals
    );

    public static void sendPacketToAll(Object packet) {
        NETWORKING_CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }

    public static void registerPackets() {
        NETWORKING_CHANNEL.messageBuilder(ClientboundAmongusSyncPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientboundAmongusSyncPacket::encoder)
                .decoder(ClientboundAmongusSyncPacket::decoder)
                .consumerMainThread(ClientboundAmongusSyncPacket::handle)
                .add();
    }

    public static class AmongusMobEffect extends MobEffect {
        protected AmongusMobEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
