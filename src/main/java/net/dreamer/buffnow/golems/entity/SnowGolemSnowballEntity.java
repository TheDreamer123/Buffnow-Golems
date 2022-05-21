package net.dreamer.buffnow.golems.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SnowGolemSnowballEntity extends SnowballEntity {
    private double damage;

    public SnowGolemSnowballEntity(World world,LivingEntity owner) {
        super(world,owner);
        this.damage = 0.0D;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        Entity entity = entityHitResult.getEntity();
        LivingEntity living = (LivingEntity) entity;

        double i = entity instanceof BlazeEntity ? 6 + damage : 3 + damage;

        living.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE,100,2));
        living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,100,2));
        living.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,100,2));
        entity.damage(DamageSource.thrownProjectile(this, this.getOwner()), (float)i);

        this.playSound(SoundEvents.BLOCK_GLASS_BREAK, 1.0F, 0.4F / (this.random.nextFloat() * 0.4F + 0.8F));
        if(this.random.nextInt(20) == 0) {
            BlockState icePlacer = Blocks.ICE.getDefaultState();
            float f = (float)Math.min(16, 3);
            BlockPos.Mutable mutable = new BlockPos.Mutable();
            for (BlockPos blockPos2 : BlockPos.iterate(getBlockPos().add(-f, world.getBottomY(),-f),getBlockPos().add(f, world.getTopY(),f))) {
                if (blockPos2.isWithinDistance(this.getPos(),f)) {
                    mutable.set(blockPos2.getX(),blockPos2.getY() + 1,blockPos2.getZ());
                    BlockState blockState2 = world.getBlockState(mutable);
                    if (blockState2.isAir()) {
                        world.setBlockState(blockPos2, icePlacer);
                    }
                }
            }
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
        this.playSound(SoundEvents.BLOCK_GLASS_BREAK, 1.0F, 0.4F / (this.random.nextFloat() * 0.4F + 0.8F));
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putDouble("damage", this.damage);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("damage", 99)) {
            this.damage = nbt.getDouble("damage");
        }
    }
}
