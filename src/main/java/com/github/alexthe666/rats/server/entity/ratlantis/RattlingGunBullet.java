package com.github.alexthe666.rats.server.entity.ratlantis;

import com.github.alexthe666.rats.server.entity.ArrowlikeProjectile;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

public class RattlingGunBullet extends ArrowlikeProjectile {

	public RattlingGunBullet(EntityType<? extends ArrowlikeProjectile> type, Level level) {
		super(type, level);
		this.setBaseDamage(2.0F);
	}

	public RattlingGunBullet(EntityType<? extends ArrowlikeProjectile> type, Level level, LivingEntity shooter) {
		super(type, shooter, level);
		this.setBaseDamage(2.0F);
	}

	@Override
	public boolean isInWater() {
		return false;
	}

	public void tick() {
		float sqrt = (float) this.getDeltaMovement().length();
		if (!this.shouldRender(this.getX(), this.getY(), this.getZ()) || sqrt < 0.2F || this.inGround) {
			this.discard();
		}
		super.tick();
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		if (this.getOwner() instanceof TamedRat rat) {
			if (rat.shouldHuntMonster() && !(entity instanceof Enemy)) return false;
			if (rat.shouldHuntAnimal() && !(entity instanceof Animal)) return false;
			if (rat.getTarget() != entity) return false;
		}
		return this.getOwner() == null || !this.getOwner().isAlliedTo(entity);
	}

	@Override
	public boolean isNoGravity() {
		return true;
	}

	@Override
	public boolean explodesOnHit() {
		return false;
	}
}
