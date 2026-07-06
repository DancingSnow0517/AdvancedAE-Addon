package cn.dancingsnow.advancedae_addon;

import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.pedroksl.advanced_ae.common.definitions.AAEBlocks;
import org.slf4j.Logger;

@Mod(AdvancedAEAddon.MOD_ID)
public class AdvancedAEAddon {
    public static final String MOD_ID = "advancedae_addon";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AdvancedAEAddon() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        eventBus.addListener(AdvancedAEAddon::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Upgrades.add(AEItems.CRAFTING_CARD, AAEBlocks.QUANTUM_CRAFTER, 1);
        });
    }
}
