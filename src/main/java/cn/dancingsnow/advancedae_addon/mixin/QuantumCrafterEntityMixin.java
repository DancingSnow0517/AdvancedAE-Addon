package cn.dancingsnow.advancedae_addon.mixin;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.MachineUpgradesChanged;
import appeng.blockentity.grid.AENetworkedPoweredBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.helpers.MultiCraftingTracker;
import appeng.util.inv.AppEngInternalInventory;
import com.google.common.collect.ImmutableSet;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.pedroksl.advanced_ae.common.entities.QuantumCrafterEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mixin(QuantumCrafterEntity.class)
public abstract class QuantumCrafterEntityMixin extends AENetworkedPoweredBlockEntity implements ICraftingRequester {
    @Unique
    private static final String ADVANCEDAE_ADDON_INPUT_CRAFTING_TAG = "advancedaeAddonInputCrafting";

    @Unique
    private static final int ADVANCEDAE_ADDON_PATTERN_SLOTS = 9;

    @Unique
    private static final int ADVANCEDAE_ADDON_MAX_INPUTS_PER_PATTERN = 9;

    @Final
    @Shadow
    private IUpgradeInventory upgrades;

    @Final
    @Shadow
    private List<QuantumCrafterEntity.CraftingJob> craftingJobs;

    @Final
    @Shadow
    private List<Boolean> invalidPatternSlots;

    @Final
    @Shadow
    private List<Boolean> enabledPatternSlots;

    @Final
    @Shadow
    private IActionSource mySrc;

    @Final
    @Shadow
    private AppEngInternalInventory outputInv;

    @Shadow
    protected abstract boolean isEnabled();

    @Shadow
    protected abstract boolean hasAvailableOutputStorage(QuantumCrafterEntity.CraftingJob job);

    @Shadow
    protected abstract int maximumCraftableAmount(QuantumCrafterEntity.CraftingJob job);

    @Unique
    private MultiCraftingTracker advancedae_addon$inputCraftingTracker;

    public QuantumCrafterEntityMixin(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState blockState) {
        super(blockEntityType, pos, blockState);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void advancedae_addon$initInputCraftingTracker(BlockEntityType<?> type, BlockPos pos, BlockState blockState, CallbackInfo ci) {
        this.advancedae_addon$inputCraftingTracker = new MultiCraftingTracker(
            this,
            ADVANCEDAE_ADDON_PATTERN_SLOTS * ADVANCEDAE_ADDON_MAX_INPUTS_PER_PATTERN
        );
        this.getMainNode().addService(ICraftingRequester.class, this);
    }

    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lappeng/api/upgrades/UpgradeInventories;forMachine(Lnet/minecraft/world/level/ItemLike;ILappeng/api/upgrades/MachineUpgradesChanged;)Lappeng/api/upgrades/IUpgradeInventory;"))
    private IUpgradeInventory wrapUpgradesSize(ItemLike machineType, int maxUpgrades, MachineUpgradesChanged changeCallback, Operation<IUpgradeInventory> original) {
        return original.call(machineType, maxUpgrades + 1, changeCallback);
    }

    @Inject(method = "hasCraftWork", at = @At("RETURN"))
    private void advancedae_addon$requestMissingInputsWhenBlocked(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            return;
        }

        Level level = this.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        this.advancedae_addon$requestMissingInputs(level);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void advancedae_addon$saveInputCraftingTracker(CompoundTag data, HolderLookup.Provider registries, CallbackInfo ci) {
        CompoundTag trackerTag = new CompoundTag();
        this.advancedae_addon$inputCraftingTracker.writeToNBT(trackerTag);
        data.put(ADVANCEDAE_ADDON_INPUT_CRAFTING_TAG, trackerTag);
    }

    @Inject(method = "loadTag", at = @At("TAIL"))
    private void advancedae_addon$loadInputCraftingTracker(CompoundTag data, HolderLookup.Provider registries, CallbackInfo ci) {
        if (data.contains(ADVANCEDAE_ADDON_INPUT_CRAFTING_TAG, Tag.TAG_COMPOUND)) {
            this.advancedae_addon$inputCraftingTracker.readFromNBT(data.getCompound(ADVANCEDAE_ADDON_INPUT_CRAFTING_TAG));
        }
    }

    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return this.advancedae_addon$inputCraftingTracker.getRequestedJobs();
    }

    @Override
    public long insertCraftedItems(ICraftingLink link, AEKey what, long amount, Actionable mode) {
        if (!this.advancedae_addon$inputCraftingTracker.getRequestedJobs().contains(link)) {
            return 0;
        }

        IGridNode node = this.getActionableNode();
        if (node == null) {
            return 0;
        }

        return node.getGrid().getStorageService().getInventory().insert(what, amount, mode, this.mySrc);
    }

    @Override
    public void jobStateChange(ICraftingLink link) {
        this.advancedae_addon$inputCraftingTracker.jobStateChange(link);
        this.saveChanges();
    }

    @Override
    public IGridNode getActionableNode() {
        return this.getMainNode().getNode();
    }

    @Unique
    private void advancedae_addon$requestMissingInputs(Level level) {
        if (!this.upgrades.isInstalled(AEItems.CRAFTING_CARD) || !this.isEnabled()) {
            return;
        }

        IGridNode node = this.getActionableNode();
        if (node == null) {
            return;
        }

        IGrid grid = node.getGrid();
        MEStorage inventory = grid.getStorageService().getInventory();
        ICraftingService craftingService = grid.getCraftingService();
        Set<AEKey> requestedThisPass = new HashSet<>();
        int craftsPerTick = this.advancedae_addon$getCraftsPerTick();

        for (int patternSlot = 0; patternSlot < this.craftingJobs.size(); patternSlot++) {
            QuantumCrafterEntity.CraftingJob job = this.craftingJobs.get(patternSlot);
            if (job == null || job.pattern == null) {
                continue;
            }
            if (Boolean.TRUE.equals(this.invalidPatternSlots.get(patternSlot))
                || !Boolean.TRUE.equals(this.enabledPatternSlots.get(patternSlot))) {
                continue;
            }
            if (!this.hasAvailableOutputStorage(job) || this.maximumCraftableAmount(job) > 0) {
                continue;
            }

            int craftsToRequest = this.advancedae_addon$getCraftsToRequest(job, inventory, craftsPerTick);
            if (craftsToRequest <= 0) {
                continue;
            }

            this.advancedae_addon$requestMissingInputsForJob(
                level,
                craftingService,
                inventory,
                job,
                patternSlot,
                craftsToRequest,
                requestedThisPass);
        }
    }

    @Unique
    private int advancedae_addon$getCraftsToRequest(
        QuantumCrafterEntity.CraftingJob job,
        MEStorage inventory,
        int craftsPerTick
    ) {
        long maxStock = job.limitMaxOutput;
        if (maxStock <= 0) {
            return craftsPerTick;
        }

        List<GenericStack> outputs = job.pattern.getOutputs();
        if (outputs.isEmpty()) {
            return 0;
        }

        GenericStack output = outputs.getFirst();
        long stored = inventory.extract(output.what(), maxStock, Actionable.SIMULATE, this.mySrc);
        long amountInOutput = 0;
        for (int x = 0; x < this.outputInv.size(); x++) {
            ItemStack stack = this.outputInv.getStackInSlot(x);
            if (stack.is(output.what().wrapForDisplayOrFilter().getItem())) {
                amountInOutput += stack.getCount();
            }
        }

        long producedAmount = job.outputAmountPerCraft(output);
        if (producedAmount <= 0) {
            return 0;
        }

        long remainingCapacity = maxStock - stored - amountInOutput;
        if (remainingCapacity <= 0) {
            return 0;
        }

        return (int) Math.min(craftsPerTick, remainingCapacity / producedAmount);
    }

    @Unique
    private void advancedae_addon$requestMissingInputsForJob(
        Level level,
        ICraftingService craftingService,
        MEStorage inventory,
        QuantumCrafterEntity.CraftingJob job,
        int patternSlot,
        int craftsToRequest,
        Set<AEKey> requestedThisPass
    ) {
        IPatternDetails.IInput[] inputs = job.pattern.getInputs();
        int maxInputs = Math.min(inputs.length, ADVANCEDAE_ADDON_MAX_INPUTS_PER_PATTERN);
        for (int inputIndex = 0; inputIndex < maxInputs; inputIndex++) {
            int trackerSlot = patternSlot * ADVANCEDAE_ADDON_MAX_INPUTS_PER_PATTERN + inputIndex;
            this.advancedae_addon$requestMissingInput(
                level,
                craftingService,
                inventory,
                job,
                inputs[inputIndex],
                trackerSlot,
                craftsToRequest,
                requestedThisPass);
        }
    }

    @Unique
    private void advancedae_addon$requestMissingInput(
        Level level,
        ICraftingService craftingService,
        MEStorage inventory,
        QuantumCrafterEntity.CraftingJob job,
        IPatternDetails.IInput input,
        int trackerSlot,
        int craftsToRequest,
        Set<AEKey> requestedThisPass
    ) {
        for (GenericStack possibleInput : input.getPossibleInputs()) {
            long required = job.requiredInputTotal(possibleInput, craftsToRequest);
            if (job.isInputConsumed(possibleInput)) {
                required += job.minimumInputToKeep(input);
            }
            if (required <= 0) {
                return;
            }

            AEKey what = possibleInput.what();
            long available = inventory.extract(what, required, Actionable.SIMULATE, this.mySrc);
            if (available >= required) {
                return;
            }

            long alreadyRequested = craftingService.getRequestedAmount(what);
            if (available + alreadyRequested >= required || requestedThisPass.contains(what)) {
                return;
            }

            requestedThisPass.add(what);
            this.advancedae_addon$inputCraftingTracker.handleCrafting(
                trackerSlot,
                what,
                required - available - alreadyRequested,
                level,
                craftingService,
                this.mySrc);
            return;
        }
    }

    @Unique
    private int advancedae_addon$getCraftsPerTick() {
        return switch (this.upgrades.getInstalledUpgrades(AEItems.SPEED_CARD)) {
            case 1 -> 8;
            case 2 -> 16;
            case 3 -> 32;
            case 4 -> 64;
            default -> 1;
        };
    }
}
