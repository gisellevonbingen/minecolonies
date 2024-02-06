package com.minecolonies.core;

import com.ldtteam.common.config.Configurations;
import com.ldtteam.common.language.LanguageHandler;
import com.ldtteam.structurize.storage.SurvivalBlueprintHandlers;
import com.minecolonies.api.MinecoloniesAPIProxy;
import com.minecolonies.api.advancements.AdvancementTriggers;
import com.minecolonies.api.colony.IChunkmanagerCapability;
import com.minecolonies.api.colony.IColonyTagCapability;
import com.minecolonies.api.configuration.ClientConfiguration;
import com.minecolonies.api.configuration.CommonConfiguration;
import com.minecolonies.api.configuration.ServerConfiguration;
import com.minecolonies.api.crafting.CountedIngredient;
import com.minecolonies.api.creativetab.ModCreativeTabs;
import com.minecolonies.api.enchants.ModEnchants;
import com.minecolonies.api.entity.ModEntities;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.mobs.AbstractEntityRaiderMob;
import com.minecolonies.api.entity.mobs.RaiderMobUtils;
import com.minecolonies.api.items.ModBannerPatterns;
import com.minecolonies.api.items.ModTags;
import com.minecolonies.api.loot.ModLootConditions;
import com.minecolonies.api.sounds.ModSoundEvents;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.apiimp.ClientMinecoloniesAPIImpl;
import com.minecolonies.apiimp.CommonMinecoloniesAPIImpl;
import com.minecolonies.apiimp.initializer.*;
import com.minecolonies.core.colony.IColonyManagerCapability;
import com.minecolonies.core.colony.requestsystem.init.RequestSystemInitializer;
import com.minecolonies.core.colony.requestsystem.init.StandardFactoryControllerInitializer;
import com.minecolonies.core.entity.mobs.EntityMercenary;
import com.minecolonies.core.event.*;
import com.minecolonies.core.loot.SupplyLoot;
import com.minecolonies.core.placementhandlers.PlacementHandlerInitializer;
import com.minecolonies.core.placementhandlers.main.SuppliesHandler;
import com.minecolonies.core.placementhandlers.main.SurvivalHandler;
import com.minecolonies.core.recipes.FoodIngredient;
import com.minecolonies.core.recipes.PlantIngredient;
import com.minecolonies.core.structures.MineColoniesStructures;
import net.minecraft.core.registries.Registries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.DistExecutor;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.fml.javafmlmod.FMLModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.capabilities.Capability;
import net.neoforged.neoforge.common.capabilities.CapabilityManager;
import net.neoforged.neoforge.common.capabilities.CapabilityToken;
import net.neoforged.neoforge.common.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.crafting.CraftingHelper;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

@Mod(Constants.MOD_ID)
public class MineColonies
{
    public static final Capability<IChunkmanagerCapability> CHUNK_STORAGE_UPDATE_CAP = CapabilityManager.get(new CapabilityToken<>() {});

    public static final Capability<IColonyManagerCapability> COLONY_MANAGER_CAP = CapabilityManager.get(new CapabilityToken<>() {});

    /**
     * The config instance.
     */
    private static Configurations<ClientConfiguration, ServerConfiguration, CommonConfiguration> config;

    public MineColonies(final FMLModContainer modContainer, final Dist dist)
    {
        final IEventBus modBus = modContainer.getEventBus();
        final IEventBus forgeBus = NeoForge.EVENT_BUS;
    
        config = new Configurations<>(modContainer, modBus, ClientConfiguration::new, ServerConfiguration::new, CommonConfiguration::new);
        LanguageHandler.loadLangPath("assets/minecolonies/lang/%s.json");

        TileEntityInitializer.BLOCK_ENTITIES.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModEnchants.ENCHANTMENTS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModContainerInitializers.CONTAINERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModBuildingsInitializer.DEFERRED_REGISTER.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModFieldsInitializer.DEFERRED_REGISTER.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModGuardTypesInitializer.DEFERRED_REGISTER.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModColonyEventDescriptionTypeInitializer.DEFERRED_REGISTER.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModResearchRequirementInitializer.DEFERRED_REGISTER.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModRecipeSerializerInitializer.RECIPE_SERIALIZER.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModRecipeSerializerInitializer.RECIPE_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModColonyEventTypeInitializer.DEFERRED_REGISTER.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModCraftingTypesInitializer.DEFERRED_REGISTER.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModJobsInitializer.DEFERRED_REGISTER.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModRecipeTypesInitializer.DEFERRED_REGISTER.register(FMLJavaModLoadingContext.get().getModEventBus());
        RaiderMobUtils.ATTRIBUTES.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModSoundEvents.SOUND_EVENTS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModInteractionsInitializer.DEFERRED_REGISTER.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModResearchEffectInitializer.DEFERRED_REGISTER.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModResearchCostTypeInitializer.DEFERRED_REGISTER.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModLootConditions.DEFERRED_REGISTER.register(FMLJavaModLoadingContext.get().getModEventBus());
        SupplyLoot.GLM.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModBannerPatterns.BANNER_PATTERNS.register(FMLJavaModLoadingContext.get().getModEventBus());

        ModQuestInitializer.DEFERRED_REGISTER_OBJECTIVE.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModQuestInitializer.DEFERRED_REGISTER_TRIGGER.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModQuestInitializer.DEFERRED_REGISTER_REWARD.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModQuestInitializer.DEFERRED_REGISTER_ANSWER_RESULT.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModHappinessFactorTypeInitializer.DEFERRED_REGISTER_HAPPINESS_FACTOR.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModHappinessFactorTypeInitializer.DEFERRED_REGISTER_HAPPINESS_FUNCTION.register(FMLJavaModLoadingContext.get().getModEventBus());

        ModCreativeTabs.TAB_REG.register(FMLJavaModLoadingContext.get().getModEventBus());

        ModEnchantInitializer.init();

        Consumer<TagsUpdatedEvent> onTagsLoaded = (event) -> ModTags.tagsLoaded = true;
        NeoForge.EVENT_BUS.addListener(onTagsLoaded);

        Mod.EventBusSubscriber.Bus.FORGE.bus().get().register(EventHandler.class);
        Mod.EventBusSubscriber.Bus.FORGE.bus().get().register(FMLEventHandler.class);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Mod.EventBusSubscriber.Bus.FORGE.bus().get().register(ClientEventHandler.class));
        Mod.EventBusSubscriber.Bus.FORGE.bus().get().register(DataPackSyncEventHandler.ServerEvents.class);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Mod.EventBusSubscriber.Bus.FORGE.bus().get().register(DataPackSyncEventHandler.ClientEvents.class));

        Mod.EventBusSubscriber.Bus.MOD.bus().get().addListener(GatherDataHandler::dataGeneratorSetup);

        Mod.EventBusSubscriber.Bus.FORGE.bus().get().register(this.getClass());
        Mod.EventBusSubscriber.Bus.MOD.bus().get().register(this.getClass());
        Mod.EventBusSubscriber.Bus.MOD.bus().get().register(ClientRegistryHandler.class);
        Mod.EventBusSubscriber.Bus.MOD.bus().get().register(ModCreativeTabs.class);

        InteractionValidatorInitializer.init();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MinecoloniesAPIProxy.getInstance().setApiInstance(new ClientMinecoloniesAPIImpl()));
        DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> () -> MinecoloniesAPIProxy.getInstance().setApiInstance(new CommonMinecoloniesAPIImpl()));

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MineColoniesStructures.DEFERRED_REGISTRY_STRUCTURE.register(modEventBus);

        SurvivalBlueprintHandlers.registerHandler(new SurvivalHandler());
        SurvivalBlueprintHandlers.registerHandler(new SuppliesHandler());
    }

    @SubscribeEvent
    public static void registerNewRegistries(final NewRegistryEvent event)
    {
        MinecoloniesAPIProxy.getInstance().onRegistryNewRegistry(event);
    }

    /**
     * Event handler for forge pre init event.
     *
     * @param event the forge pre init event.
     */
    @SubscribeEvent
    public static void preInit(@NotNull final FMLCommonSetupEvent event)
    {
        Network.getNetwork().registerCommonMessages();

        AdvancementTriggers.preInit();

        StandardFactoryControllerInitializer.onPreInit();

        event.enqueueWork(ModLootConditions::init);
        event.enqueueWork(ModTags::init);
    }

    @SubscribeEvent
    public static void registerCaps(final RegisterCapabilitiesEvent event)
    {
        event.register(IColonyTagCapability.class);
        event.register(IChunkmanagerCapability.class);
        event.register(IColonyManagerCapability.class);
    }

    @SubscribeEvent
    public static void createEntityAttribute(final EntityAttributeCreationEvent event)
    {
        event.put(ModEntities.CITIZEN, AbstractEntityCitizen.getDefaultAttributes().build());
        event.put(ModEntities.VISITOR, AbstractEntityCitizen.getDefaultAttributes().build());
        event.put(ModEntities.MERCENARY, EntityMercenary.getDefaultAttributes().build());
        event.put(ModEntities.BARBARIAN, AbstractEntityRaiderMob.getDefaultAttributes().build());
        event.put(ModEntities.ARCHERBARBARIAN, AbstractEntityRaiderMob.getDefaultAttributes().build());
        event.put(ModEntities.CHIEFBARBARIAN, AbstractEntityRaiderMob.getDefaultAttributes().build());
        event.put(ModEntities.PHARAO, AbstractEntityRaiderMob.getDefaultAttributes().build());
        event.put(ModEntities.MUMMY, AbstractEntityRaiderMob.getDefaultAttributes().build());
        event.put(ModEntities.ARCHERMUMMY, AbstractEntityRaiderMob.getDefaultAttributes().build());
        event.put(ModEntities.PIRATE, AbstractEntityRaiderMob.getDefaultAttributes().build());
        event.put(ModEntities.ARCHERPIRATE, AbstractEntityRaiderMob.getDefaultAttributes().build());
        event.put(ModEntities.CHIEFPIRATE, AbstractEntityRaiderMob.getDefaultAttributes().build());
        event.put(ModEntities.AMAZON, AbstractEntityRaiderMob.getDefaultAttributes().build());
        event.put(ModEntities.AMAZONSPEARMAN, AbstractEntityRaiderMob.getDefaultAttributes().build());
        event.put(ModEntities.AMAZONCHIEF, AbstractEntityRaiderMob.getDefaultAttributes().build());
        event.put(ModEntities.NORSEMEN_ARCHER, AbstractEntityRaiderMob.getDefaultAttributes().build());
        event.put(ModEntities.NORSEMEN_CHIEF, AbstractEntityRaiderMob.getDefaultAttributes().build());
        event.put(ModEntities.SHIELDMAIDEN, AbstractEntityRaiderMob.getDefaultAttributes().build());
    }

    @SubscribeEvent
    public static void registerRecipeSerializers(final RegisterEvent event)
    {
        if (event.getRegistryKey().equals(Registries.RECIPE_SERIALIZER))
        {
            CraftingHelper.register(CountedIngredient.ID, CountedIngredient.Serializer.getInstance());
            CraftingHelper.register(FoodIngredient.ID, FoodIngredient.Serializer.getInstance());
            CraftingHelper.register(PlantIngredient.ID, PlantIngredient.Serializer.getInstance());
        }
    }

    /**
     * Called when MC loading is about to finish.
     *
     * @param event event
     */
    @SubscribeEvent
    public static void onLoadComplete(final FMLLoadCompleteEvent event)
    {
        PlacementHandlerInitializer.initHandlers();
        RequestSystemInitializer.onPostInit();
    }

    /**
     * Get the config handler.
     *
     * @return the config handler.
     */
    public static Configurations<ClientConfiguration, ServerConfiguration, CommonConfiguration> getConfig()
    {
        return config;
    }
}
