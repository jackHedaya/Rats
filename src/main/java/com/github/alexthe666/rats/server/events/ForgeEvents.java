package com.github.alexthe666.rats.server.events;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.data.tags.RatsEntityTags;
import com.github.alexthe666.rats.registry.*;
import com.github.alexthe666.rats.registry.worldgen.RatlantisDimensionRegistry;
import com.github.alexthe666.rats.server.entity.misc.PlagueDoctor;
import com.github.alexthe666.rats.server.entity.projectile.PlagueShot;
import com.github.alexthe666.rats.server.entity.rat.Rat;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.items.RatSackItem;
import com.github.alexthe666.rats.server.items.RatStaffItem;
import com.github.alexthe666.rats.server.message.*;
import com.github.alexthe666.rats.server.misc.PlagueDoctorTrades;
import com.github.alexthe666.rats.server.misc.RatUpgradeUtils;
import com.github.alexthe666.rats.server.misc.RatUtils;
import com.github.alexthe666.rats.server.world.PlagueDoctorSpawner;
import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.VanillaGameEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mod.EventBusSubscriber(modid = RatsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEvents {

	@SubscribeEvent
	public static void onPlayerInteractWithEntity(PlayerInteractEvent.EntityInteract event) {
		ItemStack heldItem = event.getEntity().getItemInHand(event.getHand());
		if (event.getTarget() instanceof Sheep sheep && sheep.getColor() != DyeColor.WHITE && event.getEntity().getItemInHand(event.getHand()).is(RatsBlockRegistry.DYE_SPONGE.get().asItem())) {
			sheep.setColor(DyeColor.WHITE);
			for (int i = 0; i < 8; i++) {
				double d0 = sheep.getRandom().nextGaussian() * 0.02D;
				double d1 = sheep.getRandom().nextGaussian() * 0.02D;
				double d2 = sheep.getRandom().nextGaussian() * 0.02D;
				sheep.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(RatsBlockRegistry.DYE_SPONGE.get())), sheep.getX() + (double) (sheep.getRandom().nextFloat() * sheep.getBbWidth() * 2.0F) - (double) sheep.getBbWidth(), sheep.getY() + (double) (sheep.getRandom().nextFloat() * sheep.getBbHeight() * 2.0F) - (double) sheep.getBbHeight(), sheep.getZ() + (double) (sheep.getRandom().nextFloat() * sheep.getBbWidth() * 2.0F) - (double) sheep.getBbWidth(), d0, d1, d2);
			}
			sheep.playSound(RatsSoundRegistry.DYE_SPONGE_USED.get(), 1.0F, 1.0F);

		}
		if (heldItem.is(RatsItemRegistry.PLAGUE_DOCTORATE.get())) {
			if (event.getTarget() instanceof Villager villager && !villager.isBaby() && (villager.getVillagerData().getProfession() == VillagerProfession.NITWIT || villager.getVillagerData().getProfession() == VillagerProfession.NONE)) {
				PlagueDoctor doctor = villager.convertTo(RatsEntityRegistry.PLAGUE_DOCTOR.get(), true);
				if (doctor != null) {
					doctor.setWillDespawn(false);
					event.getEntity().swing(event.getHand());
					event.getLevel().playSound(null, doctor.blockPosition(), RatsSoundRegistry.PLAGUE_DOCTOR_SUMMON.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
					if (!event.getEntity().isCreative()) {
						heldItem.shrink(1);
					}
				}
			}
		}
		if (event.getTarget() instanceof LivingEntity living) {
			maybeAddAndSyncPlague(event.getEntity(), living, 6000, 0);
		}
	}

	public static int getProtectorCount(LivingEntity entity) {
		int protectors = 0;
		if (entity.getItemBySlot(EquipmentSlot.HEAD).is(RatlantisItemRegistry.RATLANTIS_HELMET.get())) {
			protectors++;
		}
		if (entity.getItemBySlot(EquipmentSlot.CHEST).is(RatlantisItemRegistry.RATLANTIS_CHESTPLATE.get())) {
			protectors++;
		}
		if (entity.getItemBySlot(EquipmentSlot.LEGS).is(RatlantisItemRegistry.RATLANTIS_LEGGINGS.get())) {
			protectors++;
		}
		if (entity.getItemBySlot(EquipmentSlot.FEET).is(RatlantisItemRegistry.RATLANTIS_BOOTS.get())) {
			protectors++;
		}
		return protectors;
	}

	@SubscribeEvent
	public static void piglinsDontAttackGoldRatsEver(LivingChangeTargetEvent event) {
		if (event.getEntity() instanceof Piglin) {
			if (event.getNewTarget() instanceof TamedRat rat && RatUpgradeUtils.hasUpgrade(rat, RatsItemRegistry.RAT_UPGRADE_IDOL.get())) {
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public static void sculkedRatsDontEmitVibrations(VanillaGameEvent event) {
		if (event.getCause() instanceof TamedRat rat && RatUpgradeUtils.hasUpgrade(rat, RatsItemRegistry.RAT_UPGRADE_SCULKED.get())) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void spawnAngelRat(LivingDeathEvent event) {
		if (event.getEntity() instanceof TamedRat rat && RatUpgradeUtils.hasUpgrade(rat, RatsItemRegistry.RAT_UPGRADE_ANGEL.get())) {
			event.setCanceled(true);
			rat.spawnAngelCopy();
			rat.playSound(RatsSoundRegistry.RAT_DIE.get());
			if (rat.getOwner() instanceof Player player) {
				player.sendSystemMessage(Component.translatable("entity.rats.rat.respawned_angel", rat.getName().getString()));
			}
			rat.discard();
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void ejectRatsOutOfSack(ItemExpireEvent event) {
		if (event.getEntity().getItem().is(RatsItemRegistry.RAT_SACK.get()) && RatSackItem.getRatsInSack(event.getEntity().getItem()) > 0) {
			RatSackItem.ejectRatsFromSack(event.getEntity().getItem(), event.getEntity().level(), event.getEntity().blockPosition());
		}
	}

	//complete hack workaround for rats not cooking food dropped by victims if they kill them in 1 shot
	@SubscribeEvent
	public static void hackySetFireFix(LivingAttackEvent event) {
		if (!event.getEntity().fireImmune()) {
			if (event.getSource().getEntity() instanceof TamedRat rat && RatUpgradeUtils.hasUpgrade(rat, RatsItemRegistry.RAT_UPGRADE_DEMON.get())) {
				event.getEntity().setSecondsOnFire(1);
			}
		}
	}

	@SubscribeEvent
	public static void checkIfPlagueCanApplyToMob(MobEffectEvent.Applicable event) {
		if (event.getEffectInstance().getEffect() == RatsEffectRegistry.PLAGUE.get() && event.getEntity().getType().is(RatsEntityTags.PLAGUE_IMMUNE)) {
			event.setResult(Event.Result.DENY);
		}
	}

	@SubscribeEvent
	public static void onHitEntity(LivingAttackEvent event) {
		if (event.getSource().getDirectEntity() instanceof LivingEntity living) {
			maybeAddAndSyncPlague(living, event.getEntity(), 6000, 0);
		}
		int protectors = getProtectorCount(event.getEntity());
		if (protectors > 0) {
			if (event.getSource() != null && event.getSource().getEntity() != null) {
				Entity trueSource = event.getSource().getEntity();
				if (trueSource.distanceTo(event.getEntity()) < 4.0D) {
					trueSource.hurt(event.getEntity().damageSources().magic(), 0.75F * protectors);
					Vec3 vec3d = trueSource.getDeltaMovement();
					double strength = 0.15D * protectors;
					Vec3 vec3d1 = (new Vec3(event.getEntity().getX() - trueSource.getX(), 0.0D, event.getEntity().getZ() - trueSource.getZ())).normalize().scale(strength);
					trueSource.setDeltaMovement(vec3d.x / 2.0D - vec3d1.x, trueSource.onGround() ? Math.min(0.4D, vec3d.y / 2.0D + strength) : vec3d.y, vec3d.z / 2.0D - vec3d1.z);
				}
			}
		}
	}

	@SubscribeEvent
	public static void addPlagueDoctorSpawningAndCacheMobs(LevelEvent.Load event) {
		if (event.getLevel() instanceof ServerLevel server && server.dimension().equals(Level.OVERWORLD)) {
			List<CustomSpawner> spawners = new ArrayList<>(server.customSpawners);
			spawners.add(new PlagueDoctorSpawner(server));
			server.customSpawners = spawners;
		}

		if (event.getLevel().isClientSide()) {
			if (RatsMod.MOB_CACHE.isEmpty()) {
				List<Pair<String, Component>> unsortedCache = new ArrayList<>();
				ForgeRegistries.ENTITY_TYPES.getEntries().stream().filter(entry -> entry.getValue().create((Level) event.getLevel()) instanceof Mob || entry.getValue() == EntityType.PLAYER).forEach(entry ->
						unsortedCache.add(Pair.of(entry.getKey().location().toString(), entry.getValue().getDescription())));

				RatsMod.MOB_CACHE.addAll(unsortedCache.stream().sorted(Comparator.comparing(o -> o.getSecond().getString())).toList());
				RatsMod.LOGGER.debug("Cached {} mob ids for later use.", RatsMod.MOB_CACHE.size());
			}
		}
	}

	@SubscribeEvent
	public static void maybeSendPlayerWarning(PlayerEvent.PlayerLoggedInEvent event) {
		if (!event.getEntity().level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
			CompoundTag playerData = event.getEntity().getPersistentData();
			CompoundTag data = playerData.getCompound(Player.PERSISTED_NBT_TAG);
			if (data != null && !data.getBoolean("rats_griefing_warning")) {
				event.getEntity().displayClientMessage(Component.translatable("entity.rats.rat.griefing_off").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
				data.putBoolean("rats_griefing_warning", true);
				playerData.put(Player.PERSISTED_NBT_TAG, data);
			}
		}
	}

	@SubscribeEvent
	public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
		if (event.getEntity() != null && event.getEntity() instanceof IronGolem golem && RatConfig.golemsTargetRats) {
			golem.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(golem, Rat.class, true, RatUtils.UNTAMED_RAT_SELECTOR));
		}

		if (event.getEntity() != null && RatUtils.isPredator(event.getEntity()) && event.getEntity() instanceof Animal animal) {
			animal.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(animal, Rat.class, true, RatUtils.UNTAMED_RAT_SELECTOR));
		}

		if (event.getEntity() != null && event.getEntity() instanceof Husk husk) {
			if (husk.getRandom().nextFloat() < 0.12F) {
				husk.setItemSlot(EquipmentSlot.HEAD, new ItemStack(RatsItemRegistry.ARCHEOLOGIST_HAT.get()));
				husk.setDropChance(EquipmentSlot.HEAD, 1.0F);
			}
		}
		if (event.getEntity() != null && event.getEntity() instanceof AbstractSkeleton skele && event.getEntity().level().getBiome(event.getEntity().blockPosition()).is(BiomeTags.IS_JUNGLE)) {
			if (skele.getRandom().nextFloat() < 0.25F) {
				skele.setItemSlot(EquipmentSlot.HEAD, new ItemStack(RatsItemRegistry.ARCHEOLOGIST_HAT.get()));
				skele.setDropChance(EquipmentSlot.HEAD, 1.0F);
			}
		}
	}

	@SubscribeEvent
	public static void ratlantisFishing(ItemFishedEvent event) {
		FishingHook hook = event.getHookEntity();
		Player player = event.getEntity();
		Level level = player.level();
		if (RatsMod.RATLANTIS_DATAPACK_ENABLED && level.dimension().equals(RatlantisDimensionRegistry.DIMENSION_KEY)) {
			event.setCanceled(true);
			LootParams.Builder builder = (new LootParams.Builder((ServerLevel) level))
					.withParameter(LootContextParams.ORIGIN, hook.position())
					//TODO this is a horrible assumption but it works for now I guess
					.withParameter(LootContextParams.TOOL, player.getMainHandItem())
					.withParameter(LootContextParams.KILLER_ENTITY, player)
					.withParameter(LootContextParams.THIS_ENTITY, hook)
					.withLuck((float) hook.luck + player.getLuck());
			LootTable loottable = level.getServer().getLootData().getLootTable(RatsLootRegistry.RATLANTIS_FISH);
			List<ItemStack> list = loottable.getRandomItems(builder.create(LootContextParamSets.FISHING));

			for (ItemStack itemstack : list) {
				ItemEntity itementity = new ItemEntity(level, hook.getX(), hook.getY(), hook.getZ(), itemstack);
				double d0 = player.getX() - hook.getX();
				double d1 = player.getY() - hook.getY();
				double d2 = player.getZ() - hook.getZ();
				itementity.setDeltaMovement(d0 * 0.1D, d1 * 0.1D + Math.sqrt(Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2)) * 0.08D, d2 * 0.1D);
				level.addFreshEntity(itementity);
				if (!(player instanceof FakePlayer)) {
					level.addFreshEntity(new ExperienceOrb(level, player.getX(), player.getY() + 0.5D, player.getZ() + 0.5D, player.getRandom().nextInt(6) + 1));
					if (itemstack.is(ItemTags.FISHES)) {
						player.awardStat(Stats.FISH_CAUGHT, 1);
					}
				}
			}
		}
	}

	@SubscribeEvent
	public static void initVillagerTrades(VillagerTradesEvent event) {
		//1-2, 5-10, 10-20, 15, 30
		if (event.getType() == RatsVillagerRegistry.PET_SHOP_OWNER.get()) {
			event.getTrades().get(1).add(new PlagueDoctorTrades.ItemsToEmeralds(Items.BONE, 12, 12, 1));
			event.getTrades().get(1).add(new PlagueDoctorTrades.ItemsToEmeralds(Items.ROTTEN_FLESH, 11, 12, 1));
			event.getTrades().get(1).add(new PlagueDoctorTrades.ItemsToEmeralds(Items.COD, 10, 12, 1));
			event.getTrades().get(1).add(new PlagueDoctorTrades.ItemsToEmeralds(Items.STRING, 12, 12, 1));
			event.getTrades().get(1).add(new PlagueDoctorTrades.ItemsToEmeralds(Items.EGG, 16, 12, 1));
			event.getTrades().get(1).add(new PlagueDoctorTrades.ItemsToEmeralds(RatsItemRegistry.RAW_RAT.get(), 11, 12, 1));
			event.getTrades().get(1).add(new PlagueDoctorTrades.EmeraldsToItems(RatsItemRegistry.CHEESE.get(), 1, 5, 1));

			event.getTrades().get(2).add(new PlagueDoctorTrades.EmeraldsToItems(RatsItemRegistry.COOKED_RAT.get(), 1, 5, 5));
			event.getTrades().get(2).add(new PlagueDoctorTrades.ItemsAndEmeraldsToItems(RatsBlockRegistry.GARBAGE_PILE.get(), 10, 3, RatsItemRegistry.PLASTIC_WASTE.get(), 10, 12, 5));
			event.getTrades().get(2).add(new PlagueDoctorTrades.EmeraldsToItems(RatsBlockRegistry.MARBLED_CHEESE_RAW.get(), 1, 8, 5));
			event.getTrades().get(2).add(new PlagueDoctorTrades.EmeraldsToItems(RatsBlockRegistry.GARBAGE_PILE.get(), 1, 6, 5));
			event.getTrades().get(2).add(new PlagueDoctorTrades.ItemsToEmeralds(RatsItemRegistry.RAW_PLASTIC.get(), 5, 12, 5));
			event.getTrades().get(2).add(new PlagueDoctorTrades.ItemsToEmeralds(RatsItemRegistry.RAT_FLUTE.get(), 1, 12, 5));

			event.getTrades().get(3).add(new PlagueDoctorTrades.EmeraldsToItems(RatsItemRegistry.ARCHEOLOGIST_HAT.get(), 12, 1, 10));
			event.getTrades().get(3).add(new PlagueDoctorTrades.EmeraldsToItems(RatsItemRegistry.SANTA_HAT.get(), 20, 1, 15));
			event.getTrades().get(3).add(new PlagueDoctorTrades.EmeraldsToItems(RatsItemRegistry.PARTY_HAT.get(), 20, 1, 15));
			event.getTrades().get(3).add(new PlagueDoctorTrades.EmeraldsToItems(RatsItemRegistry.HALO_HAT.get(), 8, 1, 10));
			event.getTrades().get(3).add(new PlagueDoctorTrades.EmeraldsToItems(RatsItemRegistry.EXTERMINATOR_HAT.get(), 14, 1, 10));

			event.getTrades().get(4).add(new PlagueDoctorTrades.EmeraldsToItems(RatsItemRegistry.RAT_UPGRADE_BASIC.get(), 5, 1, 20));
			event.getTrades().get(4).add(new PlagueDoctorTrades.EmeraldsToItems(RatsItemRegistry.RAT_UPGRADE_JURY_RIGGED.get(), 16, 1, 20));
			event.getTrades().get(4).add(new PlagueDoctorTrades.EmeraldsToItems(RatsItemRegistry.RAT_UPGRADE_BLACKLIST.get(), 8, 1, 20));
			event.getTrades().get(4).add(new PlagueDoctorTrades.EmeraldsToItems(RatsItemRegistry.RAT_UPGRADE_WHITELIST.get(), 8, 1, 20));
			event.getTrades().get(4).add(new PlagueDoctorTrades.EmeraldsToItems(RatsItemRegistry.RAT_UPGRADE_SPEED.get(), 10, 1, 20));
			event.getTrades().get(4).add(new PlagueDoctorTrades.EmeraldsToItems(RatsItemRegistry.RAT_UPGRADE_HEALTH.get(), 10, 1, 20));
			event.getTrades().get(4).add(new PlagueDoctorTrades.EmeraldsToItems(RatsItemRegistry.RAT_UPGRADE_ARMOR.get(), 10, 1, 20));

			event.getTrades().get(5).add(new PlagueDoctorTrades.EmeraldsToItems(RatsItemRegistry.PLAGUE_DOCTORATE.get(), 10, 1, 30));
			event.getTrades().get(5).add(new PlagueDoctorTrades.EmeraldsToItems(RatsItemRegistry.PLAGUE_TOME.get(), 32, 1, 30));
			event.getTrades().get(5).add(new PlagueDoctorTrades.EmeraldsToItems(RatsItemRegistry.RAT_PAPERS.get(), 5, 1, 30));
			event.getTrades().get(5).add(new PlagueDoctorTrades.EmeraldsToItems(RatsItemRegistry.CHARGED_CREEPER_CHUNK.get(), 3, 1, 30));
			event.getTrades().get(5).add(new PlagueDoctorTrades.EmeraldsToItems(RatsItemRegistry.TINY_COIN.get(), 1, 8, 30));

		}
	}

	@SubscribeEvent
	public static void onPlayerLeftClick(PlayerInteractEvent.LeftClickEmpty event) {
		if (event.getLevel().isClientSide()) {
			if (event.getEntity().isShiftKeyDown() && !event.getEntity().getPassengers().isEmpty()) {
				for (Entity passenger : event.getEntity().getPassengers()) {
					if (passenger instanceof TamedRat) {
						passenger.stopRiding();
						Vec3 dismountPos = passenger.getDismountLocationForPassenger(event.getEntity());
						passenger.setPos(dismountPos.x(), dismountPos.y(), dismountPos.z());
						RatsNetworkHandler.CHANNEL.sendToServer(new DismountRatPacket(passenger.getId()));
					}
				}
			}
			handleArmSwing(event.getItemStack(), event.getEntity());
			RatsNetworkHandler.CHANNEL.sendToServer(new SyncArmSwingPacket(event.getItemStack()));
		}
	}

	@SubscribeEvent
	public static void onLivingHurt(LivingHurtEvent event) {
		if (event.getEntity() instanceof Player) {
			List<TamedRat> list = event.getEntity().level().getEntitiesOfClass(TamedRat.class, event.getEntity().getBoundingBox().inflate(RatConfig.ratVoodooDistance), rat -> rat.isTame() && rat.isOwnedBy(event.getEntity()) && !rat.isInvulnerable() && !rat.isInvulnerableTo(event.getSource()) && RatUpgradeUtils.hasUpgrade(rat, RatsItemRegistry.RAT_UPGRADE_VOODOO.get()));
			if (!list.isEmpty()) {
				float damage = event.getAmount() / list.size();
				event.setCanceled(true);
				for (TamedRat rat : list) {
					rat.hurt(event.getSource(), damage);
				}
			}
		}
	}

	@SubscribeEvent
	public static void onLivingHeal(LivingHealEvent event) {
		if (event.getEntity().hasEffect(RatsEffectRegistry.PLAGUE.get())) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onLivingUpdate(LivingEvent.LivingTickEvent event) {
		if (event.getEntity().level().isClientSide() && event.getEntity().hasEffect(RatsEffectRegistry.PLAGUE.get())) {
			RandomSource rand = event.getEntity().getRandom();
			if (rand.nextInt(4) == 0) {
				int entitySize = 1;
				if (event.getEntity().getBoundingBox().getSize() > 0) {
					entitySize = Math.max(1, (int) event.getEntity().getBoundingBox().getSize());
				}
				for (int i = 0; i < entitySize; i++) {
					float motionX = rand.nextFloat() * 0.1F - 0.05F;
					float motionZ = rand.nextFloat() * 0.1F - 0.05F;

					event.getEntity().level().addParticle(RatsParticleRegistry.FLEA.get(),
							event.getEntity().getX() + (double) (rand.nextFloat() * event.getEntity().getBbWidth() * 2F) - (double) event.getEntity().getBbWidth(),
							event.getEntity().getY() + (double) (rand.nextFloat() * event.getEntity().getBbHeight()),
							event.getEntity().getZ() + (double) (rand.nextFloat() * event.getEntity().getBbWidth() * 2F) - (double) event.getEntity().getBbWidth(),
							motionX, 0.0F, motionZ);
				}
			}
		}
	}

	@SubscribeEvent
	public static void onPlayerInteract(PlayerInteractEvent.RightClickBlock event) {
		ItemStack stack = event.getEntity().getItemInHand(event.getHand());
		if (stack.getItem() instanceof RatStaffItem staff) {
			event.setUseBlock(Event.Result.DENY);
			event.setCancellationResult(InteractionResult.FAIL);
			event.setCanceled(true);
			TamedRat rat = null;
			if (event.getEntity().getCapability(RatsCapabilityRegistry.SELECTED_RAT).resolve().isPresent()) {
				rat = event.getEntity().getCapability(RatsCapabilityRegistry.SELECTED_RAT).resolve().get().getSelectedRat();
			}
			if (rat != null) {
				event.getEntity().swing(event.getHand());
				if (!event.getLevel().isClientSide()) {
					RatsNetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getEntity()), new ManageRatStaffPacket(rat.getId(), event.getPos(), event.getFace().ordinal(), false, true, staff.getStaff(stack)));
				}
			} else {
				event.getEntity().displayClientMessage(Component.literal("Staff is not bound to a rat!").withStyle(ChatFormatting.RED), true);
			}
		}
	}

	public static void maybeAddAndSyncPlague(@Nullable LivingEntity attacker, LivingEntity victim, int duration, int amplifier) {
		if (RatConfig.plagueSpread && !victim.level().isClientSide()) {
			if (attacker == null || attacker.hasEffect(RatsEffectRegistry.PLAGUE.get())) {
				MobEffectInstance plague = new MobEffectInstance(RatsEffectRegistry.PLAGUE.get(), duration, amplifier);
				if (!victim.hasEffect(RatsEffectRegistry.PLAGUE.get()) && victim.canBeAffected(plague)) {
					victim.addEffect(plague);
					victim.playSound(RatsSoundRegistry.PLAGUE_SPREAD.get(), 1.0F, 1.0F);
					RatsNetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> victim), new SyncPlaguePacket(victim.getId(), plague));
				}
			}
		}
	}

	public static void handleArmSwing(ItemStack stack, Player player) {
		if (stack.is(RatsItemRegistry.PLAGUE_SCYTHE.get())) {
			if (player.swingTime == 0 && !player.isSpectator()) {
				Multimap<Attribute, AttributeModifier> dmg = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
				double totalDmg = 0;
				for (AttributeModifier modifier : dmg.get(Attributes.ATTACK_DAMAGE)) {
					totalDmg += modifier.getAmount();
				}
				player.playSound(RatsSoundRegistry.PLAGUE_CLOUD_SHOOT.get(), 1, 1);
				PlagueShot shot = new PlagueShot(RatsEntityRegistry.PLAGUE_SHOT.get(), player.level(), player, totalDmg * 0.5F);
				Vec3 vec3 = player.getViewVector(1.0F);

				shot.shoot(vec3.x(), vec3.y(), vec3.z(), 1.0F, 0.5F);
				player.level().addFreshEntity(shot);
			}
		}
	}

}
