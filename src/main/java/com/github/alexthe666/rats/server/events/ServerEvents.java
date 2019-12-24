package com.github.alexthe666.rats.server.events;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.blocks.RatsBlockRegistry;
import com.github.alexthe666.rats.server.entity.EntityIllagerPiper;
import com.github.alexthe666.rats.server.entity.EntityPlagueDoctor;
import com.github.alexthe666.rats.server.entity.EntityRat;
import com.github.alexthe666.rats.server.entity.RatUtils;
import com.github.alexthe666.rats.server.items.RatsItemRegistry;
import com.github.alexthe666.rats.server.message.MessageRatDismount;
import com.github.alexthe666.rats.server.message.MessageSwingArm;
import com.google.common.base.Predicate;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.state.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.Hand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootFunction;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.conditions.RandomChance;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.GetCollisionBoxesEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber
public class ServerEvents {

    Predicate<Entity> UNTAMED_RAT_SELECTOR = new Predicate<Entity>() {
        public boolean apply(@Nullable Entity p_apply_1_) {
            return p_apply_1_ instanceof EntityRat && !((EntityRat) p_apply_1_).isTamed();
        }
    };

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent.RightClickBlock event) {
        if (event.getPlayerEntity().getHeldItem(Hand.MAIN_HAND).getItem() == RatsItemRegistry.CHEESE_STICK || event.getPlayerEntity().getHeldItem(Hand.OFF_HAND).getItem() == RatsItemRegistry.CHEESE_STICK) {
            event.setUseBlock(Event.Result.DENY);
        }
        if (event.getPlayerEntity().getHeldItem(Hand.MAIN_HAND).getItem() == RatsItemRegistry.CHUNKY_CHEESE_TOKEN || event.getPlayerEntity().getHeldItem(Hand.OFF_HAND).getItem() == RatsItemRegistry.CHUNKY_CHEESE_TOKEN) {
            if (!RatConfig.disableRatlantis) {
                if (!event.getPlayerEntity().isCreative()) {
                    event.getItemStack().shrink(1);
                }
                boolean canBuild = true;
                BlockPos pos = event.getPos().offset(event.getFace());
                for (int i = 0; i < 4; i++) {
                    BlockState state = event.getWorld().getBlockState(pos.up(i));
                    if (state.getBlockHardness(event.getWorld(), pos.up(i)) == -1.0F) {
                        canBuild = false;
                    }
                }
                if (canBuild) {
                    event.getPlayerEntity().playSound(SoundEvents.BLOCK_END_PORTAL_SPAWN, 1, 1);
                    event.getWorld().setBlockState(pos, RatsBlockRegistry.MARBLED_CHEESE_RAW.getDefaultState());
                    event.getWorld().setBlockState(pos.up(), RatsBlockRegistry.RATLANTIS_PORTAL.getDefaultState());
                    event.getWorld().setBlockState(pos.up(2), RatsBlockRegistry.RATLANTIS_PORTAL.getDefaultState());
                    event.getWorld().setBlockState(pos.up(3), RatsBlockRegistry.MARBLED_CHEESE_RAW.getDefaultState());
                }
            }
        }
        if (RatConfig.cheesemaking && event.getWorld().getBlockState(event.getPos()).getBlock() == Blocks.CAULDRON && isMilk(event.getItemStack())) {
            if (event.getWorld().getBlockState(event.getPos()).getValue(BlockCauldron.LEVEL) == 0) {
                event.getWorld().setBlockState(event.getPos(), RatsBlockRegistry.MILK_CAULDRON.getDefaultState());
                if (!event.getWorld().isRemote) {
                    CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayerEntity) event.getPlayerEntity(), event.getPos(), new ItemStack(RatsBlockRegistry.MILK_CAULDRON));
                }
                event.getPlayerEntity().playSound(SoundEvents.ITEM_BUCKET_EMPTY, 1, 1);
                if (!event.getPlayerEntity().isCreative()) {
                    if (event.getItemStack().getItem() == Items.MILK_BUCKET) {
                        event.getItemStack().shrink(1);
                        event.getPlayerEntity().addItemStackToInventory(new ItemStack(Items.BUCKET));
                    } else if (isMilk(event.getItemStack())) {
                        IFluidHandlerItem fluidHandler = FluidUtil.getFluidHandler(event.getItemStack());
                        fluidHandler.drain(1000, true);
                    }
                }
                event.setCanceled(true);
            }
        }
    }

    private boolean isMilk(ItemStack stack) {
        if (stack.getItem() == Items.MILK_BUCKET) {
            return true;
        }
        FluidStack fluidStack = FluidUtil.getFluidContained(stack);
        return fluidStack != null && fluidStack.amount >= 1000 && (fluidStack.getFluid().getUnlocalizedName().contains("milk") || fluidStack.getFluid().getUnlocalizedName().contains("Milk"));
    }

    @SubscribeEvent
    public void onPlayerInteractWithEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof EntityOcelot) {
            EntityOcelot ocelot = (EntityOcelot) event.getTarget();
            Item heldItem = event.getPlayerEntity().getHeldItem(event.getHand()).getItem();
            Random random = event.getWorld().rand;
            if (ocelot.getHealth() < ocelot.getMaxHealth()) {
                if (heldItem == RatsItemRegistry.RAW_RAT) {
                    ocelot.heal(4);
                    event.getWorld().playSound(null, ocelot.posX, ocelot.posY, ocelot.posZ, SoundEvents.ENTITY_LLAMA_EAT, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                    event.getWorld().playSound(null, ocelot.posX, ocelot.posY, ocelot.posZ, SoundEvents.ENTITY_CAT_AMBIENT, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                    for (int i = 0; i < 3; i++) {
                        event.getWorld().spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, ocelot.posX + random.nextDouble() - random.nextDouble(), ocelot.posY + 0.5 + random.nextDouble() - random.nextDouble(), ocelot.posZ + random.nextDouble() - random.nextDouble(), 0, 0, 0);
                    }
                }
                if (heldItem == RatsItemRegistry.COOKED_RAT) {
                    ocelot.heal(8);
                    event.getWorld().playSound(null, ocelot.posX, ocelot.posY, ocelot.posZ, SoundEvents.ENTITY_LLAMA_EAT, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                    event.getWorld().playSound(null, ocelot.posX, ocelot.posY, ocelot.posZ, SoundEvents.ENTITY_CAT_AMBIENT, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                    for (int i = 0; i < 3; i++) {
                        event.getWorld().spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, ocelot.posX + random.nextDouble() - random.nextDouble(), ocelot.posY + 0.5 + random.nextDouble() - random.nextDouble(), ocelot.posZ + random.nextDouble() - random.nextDouble(), 0, 0, 0);
                    }
                }
            }
        }
        if (event.getTarget() instanceof EntityVillager) {
            ItemStack heldItem = event.getPlayerEntity().getHeldItem(event.getHand());
            if (heldItem.getItem() == RatsItemRegistry.PLAGUE_DOCTORATE && !((EntityVillager) event.getTarget()).isChild()) {
                EntityVillager villager = (EntityVillager) event.getTarget();
                EntityPlagueDoctor doctor = new EntityPlagueDoctor(event.getWorld());
                doctor.copyLocationAndAnglesFrom(villager);
                villager.setDead();
                doctor.onInitialSpawn(event.getWorld().getDifficultyForLocation(event.getPos()), null);
                if (!event.getWorld().isRemote) {
                    event.getWorld().addEntity(doctor);
                }
                doctor.setNoAI(villager.isAIDisabled());
                if (villager.hasCustomName()) {
                    doctor.setCustomNameTag(villager.getCustomNameTag());
                    doctor.setAlwaysRenderNameTag(villager.getAlwaysRenderNameTag());
                }
                event.getPlayerEntity().swingArm(event.getHand());
                if (!event.getPlayerEntity().isCreative()) {
                    heldItem.shrink(1);
                }
            }
        }
        if (event.getPlayerEntity().isPotionActive(RatsMod.PLAGUE_POTION) && RatConfig.plagueSpread && !(event.getTarget() instanceof EntityRat)) {
            if (event.getTarget() instanceof LivingEntity && !((LivingEntity) event.getTarget()).isPotionActive(RatsMod.PLAGUE_POTION)) {
                ((LivingEntity) event.getTarget()).addPotionEffect(new PotionEffect(RatsMod.PLAGUE_POTION, 6000));
                event.getTarget().playSound(SoundEvents.ENTITY_ZOMBIE_INFECT, 1.0F, 1.0F);
            }
        }
    }

    @SubscribeEvent
    public void onHitEntity(LivingAttackEvent event) {
        if (event.getSource().getImmediateSource() instanceof LivingEntity && RatConfig.plagueSpread) {
            LivingEntity attacker = (LivingEntity) event.getSource().getImmediateSource();
            if (attacker.isPotionActive(RatsMod.PLAGUE_POTION) && !(event.getLivingEntity() instanceof EntityRat)) {
                if (!event.getLivingEntity().isPotionActive(RatsMod.PLAGUE_POTION)) {
                    event.getLivingEntity().addPotionEffect(new PotionEffect(RatsMod.PLAGUE_POTION, 6000));
                    event.getLivingEntity().playSound(SoundEvents.ENTITY_ZOMBIE_INFECT, 1.0F, 1.0F);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerPunch(AttackEntityEvent event) {
        ItemStack itemstack = event.getPlayerEntity().getHeldItem(Hand.MAIN_HAND);
        TinkersCompatBridge.onPlayerSwing(event.getPlayerEntity(), itemstack);
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.getEntity() != null && event.getEntity() instanceof EntityIronGolem && RatConfig.golemsTargetRats) {
            EntityIronGolem golem = (EntityIronGolem) event.getEntity();
            golem.targetSelector.addGoal(4, new EntityAINearestAttackableTarget(golem, EntityRat.class, 10, false, false, UNTAMED_RAT_SELECTOR));
        }
        if (event.getEntity() != null && RatUtils.isPredator(event.getEntity()) && event.getEntity() instanceof EntityAnimal) {
            EntityAnimal animal = (EntityAnimal) event.getEntity();
            animal.targetSelector.addGoal(5, new EntityAINearestAttackableTarget(animal, EntityRat.class, true));
        }
        if (event.getEntity() != null && event.getEntity() instanceof EntityHusk) {
            if (((EntityHusk) event.getEntity()).getRNG().nextFloat() < RatConfig.archeologistHatSpawnRate) {
                event.getEntity().setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(RatsItemRegistry.ARCHEOLOGIST_HAT));
                ((LivingEntity) event.getEntity()).setDropChance(EntityEquipmentSlot.HEAD, 0.5F);
            }
        }
        if (event.getEntity() != null && (event.getEntity() instanceof AbstractSkeleton || event.getEntity() instanceof EntityZombie) && BiomeDictionary.hasType(event.getWorld().getBiome(event.getEntity().getPosition()), BiomeDictionary.Type.JUNGLE)) {
            if (((LivingEntity) event.getEntity()).getRNG().nextFloat() < RatConfig.archeologistHatSpawnRate) {
                event.getEntity().setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(RatsItemRegistry.ARCHEOLOGIST_HAT));
                ((LivingEntity) event.getEntity()).setDropChance(EntityEquipmentSlot.HEAD, 0.5F);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLeftClick(PlayerInteractEvent.LeftClickEmpty event) {
        ItemStack itemstack = event.getPlayerEntity().getHeldItem(Hand.MAIN_HAND);
        if (TinkersCompatBridge.onPlayerSwing(event.getPlayerEntity(), itemstack)) {
            RatsMod.NETWORK_WRAPPER.sendToServer(new MessageSwingArm());
        }
        if (event.getPlayerEntity().isSneaking() && !event.getPlayerEntity().getPassengers().isEmpty()) {
            for (Entity passenger : event.getPlayerEntity().getPassengers()) {
                if (passenger instanceof EntityRat) {
                    passenger.dismountRidingEntity();
                    passenger.setPosition(event.getPlayerEntity().posX, event.getPlayerEntity().posY, event.getPlayerEntity().posZ);
                    RatsMod.NETWORK_WRAPPER.sendToServer(new MessageRatDismount(passenger.getEntityId()));
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        ItemStack itemstack = event.getPlayerEntity().getHeldItem(Hand.MAIN_HAND);
        TinkersCompatBridge.onPlayerSwing(event.getPlayerEntity(), itemstack);
    }

    @SubscribeEvent
    public void onGatherCollisionBoxes(GetCollisionBoxesEvent event) {
        if (event.getEntity() instanceof EntityRat) {
            event.getCollisionBoxesList().removeIf(aabb -> ((EntityRat) event.getEntity()).canPhaseThroughBlock(event.getWorld(), new BlockPos(aabb.minX, aabb.minY, aabb.minZ)));
        }
    }

    @SubscribeEvent
    public void onDrops(LivingDropsEvent event) {
        if (event.getLivingEntity() instanceof EntityIllagerPiper && event.getSource().getTrueSource() instanceof PlayerEntity && event.getLivingEntity().world.rand.nextFloat() < RatConfig.piperHatDropRate + (RatConfig.piperHatDropRate / 2) * event.getLootingLevel()) {
            event.getDrops().add(new ItemEntity(event.getEntity().world, event.getLivingEntity().posX, event.getLivingEntity().posY, event.getLivingEntity().posZ, new ItemStack(RatsItemRegistry.PIPER_HAT)));
        }
        if (event.getLivingEntity() instanceof EntityCreeper && ((EntityCreeper) event.getLivingEntity()).getPowered()) {
            event.getDrops().add(new ItemEntity(event.getEntity().world, event.getLivingEntity().posX, event.getLivingEntity().posY, event.getLivingEntity().posZ, new ItemStack(RatsItemRegistry.CHARGED_CREEPER_CHUNK, event.getLootingLevel() + 1 + event.getLivingEntity().world.rand.nextInt(2))));
        }
        if (event.getSource().getTrueSource() instanceof EntityRat && ((EntityRat) event.getSource().getTrueSource()).hasUpgrade(RatsItemRegistry.RAT_UPGRADE_ARISTOCRAT)) {
            event.getDrops().add(new ItemEntity(event.getEntity().world, event.getLivingEntity().posX, event.getLivingEntity().posY, event.getLivingEntity().posZ, new ItemStack(RatsItemRegistry.TINY_COIN)));
        }
    }

    @SubscribeEvent
    public void onLivingHeal(LivingHealEvent event) {
        if (event.getLivingEntity().getActivePotionEffect(RatsMod.PLAGUE_POTION) != null) {
            PotionEffect effect = event.getLivingEntity().getActivePotionEffect(RatsMod.PLAGUE_POTION);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getLivingEntity() instanceof PlayerEntity) {
            AxisAlignedBB axisalignedbb = event.getLivingEntity().getBoundingBox().grow(RatConfig.ratVoodooDistance, RatConfig.ratVoodooDistance, RatConfig.ratVoodooDistance);
            List<EntityRat> list = event.getLivingEntity().world.getEntitiesWithinAABB(EntityRat.class, axisalignedbb);
            List<EntityRat> voodooRats = new ArrayList<>();
            int capturedRat = 0;
            if (!list.isEmpty()) {
                for (EntityRat rat : list) {
                    if (rat.isTamed() && (rat.isOwner(event.getLivingEntity())) && rat.hasUpgrade(RatsItemRegistry.RAT_UPGRADE_VOODOO)) {
                        voodooRats.add(rat);
                    }
                }
                if (!voodooRats.isEmpty()) {
                    float damage = event.getAmount() / Math.max(1, voodooRats.size());
                    event.setCanceled(true);
                    for (EntityRat rat : voodooRats) {
                        rat.attackEntityFrom(event.getSource(), damage);
                    }
                }

            }
        }
    }


    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (event.getLivingEntity().world.isRemote && (event.getLivingEntity().isPotionActive(RatsMod.PLAGUE_POTION) || event.getLivingEntity() instanceof EntityRat && ((EntityRat) event.getLivingEntity()).hasPlague())) {
            Random rand = event.getLivingEntity().getRNG();
            if (rand.nextInt(4) == 0) {
                int entitySize = 1;
                if (event.getLivingEntity().getBoundingBox().getAverageEdgeLength() > 0) {
                    entitySize = Math.max(1, (int) event.getLivingEntity().getBoundingBox().getAverageEdgeLength());
                }
                for (int i = 0; i < entitySize; i++) {
                    float motionX = rand.nextFloat() * 0.2F - 0.1F;
                    float motionZ = rand.nextFloat() * 0.2F - 0.1F;
                    RatsMod.PROXY.spawnParticle("flea", event.getLivingEntity().posX + (double) (rand.nextFloat() * event.getLivingEntity().width * 2F) - (double) event.getLivingEntity().width,
                            event.getLivingEntity().posY + (double) (rand.nextFloat() * event.getLivingEntity().height),
                            event.getLivingEntity().posZ + (double) (rand.nextFloat() * event.getLivingEntity().width * 2F) - (double) event.getLivingEntity().width,
                            motionX, 0.0F, motionZ);
                }
            }
        }
    }

    @SubscribeEvent
    public void onChestGenerated(LootTableLoadEvent event) {
        if (RatConfig.addLoot) {
            if (event.getName().equals(LootTableList.CHESTS_SIMPLE_DUNGEON) || event.getName().equals(LootTableList.CHESTS_ABANDONED_MINESHAFT)
                    || event.getName().equals(LootTableList.CHESTS_DESERT_PYRAMID) || event.getName().equals(LootTableList.CHESTS_JUNGLE_TEMPLE)
                    || event.getName().equals(LootTableList.CHESTS_STRONGHOLD_CORRIDOR) || event.getName().equals(LootTableList.CHESTS_STRONGHOLD_CROSSING)
                    || event.getName().equals(LootTableList.CHESTS_IGLOO_CHEST) || event.getName().equals(LootTableList.CHESTS_WOODLAND_MANSION)
                    || event.getName().equals(LootTableList.CHESTS_VILLAGE_BLACKSMITH)) {
                LootCondition chance = new RandomChance(0.4f);
                LootEntryItem item = new LootEntryItem(RatsItemRegistry.CONTAMINATED_FOOD, 20, 1, new LootFunction[0], new LootCondition[0], "rats:contaminated_food");
                LootPool pool = new LootPool(new LootEntry[]{item}, new LootCondition[]{chance}, new RandomValueRange(1, 5), new RandomValueRange(0, 3), "rats:contaminated_food");
                event.getTable().addPool(pool);
            }
            if (event.getName().equals(LootTableList.CHESTS_SIMPLE_DUNGEON) || event.getName().equals(LootTableList.CHESTS_ABANDONED_MINESHAFT)
                    || event.getName().equals(LootTableList.CHESTS_DESERT_PYRAMID) || event.getName().equals(LootTableList.CHESTS_JUNGLE_TEMPLE)
                    || event.getName().equals(LootTableList.CHESTS_STRONGHOLD_CORRIDOR) || event.getName().equals(LootTableList.CHESTS_STRONGHOLD_CROSSING)) {
                LootCondition chance = new RandomChance(0.2f);
                LootEntryItem item = new LootEntryItem(RatsItemRegistry.TOKEN_FRAGMENT, 8, 10, new LootFunction[0], new LootCondition[0], "rats:token_fragment");
                LootPool pool = new LootPool(new LootEntry[]{item}, new LootCondition[]{chance}, new RandomValueRange(1, 1), new RandomValueRange(0, 1), "token_fragment");
                event.getTable().addPool(pool);
            }
            if (event.getName().equals(LootTableList.CHESTS_SIMPLE_DUNGEON) || event.getName().equals(LootTableList.CHESTS_ABANDONED_MINESHAFT)
                    || event.getName().equals(LootTableList.CHESTS_DESERT_PYRAMID) || event.getName().equals(LootTableList.CHESTS_JUNGLE_TEMPLE)
                    || event.getName().equals(LootTableList.CHESTS_STRONGHOLD_CORRIDOR) || event.getName().equals(LootTableList.CHESTS_STRONGHOLD_CROSSING)) {
                LootCondition chance = new RandomChance(0.05f);
                LootEntryItem item = new LootEntryItem(RatsItemRegistry.RAT_UPGRADE_BASIC, 3, 8, new LootFunction[0], new LootCondition[0], "rats:rat_upgrade_basic");
                LootPool pool = new LootPool(new LootEntry[]{item}, new LootCondition[]{chance}, new RandomValueRange(1, 1), new RandomValueRange(0, 0), "rat_upgrade_basic");
                event.getTable().addPool(pool);
            }
        }
    }

}