package com.sabastian.renamer_block_mod;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(RenamerBlockMod.MOD_ID)
public class RenamerBlockMod
{
    public static final String MOD_ID = "renamer_block_mod";

    //public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<CreativeModeTab> CTABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> blockSupplier)
    {
        RegistryObject<T> block = BLOCKS.register(name, blockSupplier);
        registerBlockItem(name, block);
        return block;
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block)
    {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static <T extends AbstractContainerMenu>RegistryObject<MenuType<T>> registerMenuType(String name, IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IForgeMenuType.create(factory));
    }

    public static final RegistryObject<Block> RENAMER_BLOCK = registerBlock("renamer_block", () -> new RenamerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.ANVIL)));

    public static final RegistryObject<BlockEntityType<RenamerBlockEntity>> RENAMER_BLOCKENTITY = BLOCK_ENTITIES.register("renamer_blockentity", () -> BlockEntityType.Builder.of(RenamerBlockEntity::new, RENAMER_BLOCK.get()).build(null));

    public static final RegistryObject<MenuType<RenamerMenu>> RENAMER_MENU = registerMenuType("renamer_menu", RenamerMenu::new);

    public static final RegistryObject<CreativeModeTab> RENAMER_MOD_TAB = CTABS.register("renamer_block_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(RENAMER_BLOCK.get())).title(Component.translatable("creativetab.renamer_block_tab")).displayItems((itemDisplayParameters, output) -> {output.accept(RENAMER_BLOCK.get());}).build());

    public static final String NETWORK_VERSION = String.valueOf(1);

    public static final SimpleChannel INSTANCE = NetworkRegistry.ChannelBuilder.named(
            new ResourceLocation(MOD_ID, "main"))
            .serverAcceptedVersions(NETWORK_VERSION::equals)
            .clientAcceptedVersions(NETWORK_VERSION::equals)
            .networkProtocolVersion(() -> NETWORK_VERSION)
            .simpleChannel();

    private static void initPackets() {
        int index = 0;
        //INSTANCE.registerMessage(index++, ServerboundRenamerSetPacket.class, ServerboundRenamerSetPacket::write, ServerboundRenamerSetPacket::new, ServerboundRenamerSetPacket::handle);

        INSTANCE.messageBuilder(ServerboundRenamerSetPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ServerboundRenamerSetPacket::write)
                .decoder(ServerboundRenamerSetPacket::new)
                .consumerMainThread((packet, contextSupplier) -> {
                    NetworkEvent.Context context = contextSupplier.get();
                    if (packet.handle(context)) {
                        context.setPacketHandled(true);
                    }
                })
                .add();

//        INSTANCE.messageBuilder(ClientboundRenamerUpdatePacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
//                .encoder(ClientboundRenamerUpdatePacket::write)
//                .decoder(ClientboundRenamerUpdatePacket::new)
//                .consumerMainThread(ClientboundRenamerUpdatePacket::handle)
//                .add();
    }

    public RenamerBlockMod()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        CTABS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCKS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        event.enqueueWork(RenamerBlockMod::initPackets);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {

    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            MenuScreens.register(RENAMER_MENU.get(), RenamerScreen::new);
        }
    }
}
