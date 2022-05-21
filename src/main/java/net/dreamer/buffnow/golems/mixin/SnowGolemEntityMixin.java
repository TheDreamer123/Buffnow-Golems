package net.dreamer.buffnow.golems.mixin;

import net.dreamer.buffnow.golems.entity.SnowGolemSnowballEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.FrostWalkerEnchantment;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;
import java.util.UUID;

@Mixin(SnowGolemEntity.class)
public abstract class SnowGolemEntityMixin extends GolemEntity implements RangedAttackMob {
    private static final UUID HEALTH_MODIFIER_ID = UUID.fromString("1eaf83ff-7207-4596-b37a-d7a07b3ec4ce");
    private int snowStage;
    BlockPos pos;
    BlockView view;

    protected SnowGolemEntityMixin(EntityType<? extends GolemEntity> entityType,World world) {
        super(entityType,world);
    }

    @Inject(at = @At("HEAD"), method = "attack", cancellable = true)
    public void attackInject(LivingEntity target,float pullProgress,CallbackInfo info) {
        SnowGolemSnowballEntity snowballEntity = new SnowGolemSnowballEntity(this.world, this);
        double d = target.getEyeY() - 1.100000023841858D;
        double e = target.getX() - this.getX();
        double f = d - snowballEntity.getY();
        double g = target.getZ() - this.getZ();
        double h = Math.sqrt(e * e + g * g) * 0.20000000298023224D;
        snowballEntity.setItem(new ItemStack(Items.ICE));

        if(hasStatusEffect(StatusEffects.STRENGTH)) {
            snowballEntity.setDamage(3.0 * (Objects.requireNonNull(getStatusEffect(StatusEffects.STRENGTH)).getAmplifier() + 1));
        }

        snowballEntity.setVelocity(e, f + h, g, 1.6F, 12.0F);

        this.playSound(SoundEvents.ENTITY_SNOW_GOLEM_SHOOT, 1.0F, 0.4F / (this.random.nextFloat() * 0.4F + 0.8F));
        this.world.spawnEntity(snowballEntity);
        info.cancel();
    }

    @Inject(at = @At("HEAD"), method = "createSnowGolemAttributes", cancellable = true)
    private static void createSnowGolemAttributesInject(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        cir.setReturnValue(MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 4.0D * 10).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.20000000298023224D).add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0D));
    }

    @Override
    protected void applyMovementEffects(BlockPos pos) {
        super.applyMovementEffects(pos);
        FrostWalkerEnchantment.freezeWater(this, this.world, pos, 1);
    }

    @Inject(at = @At("HEAD"), method = "tickMovement", cancellable = true)
    public void tickMovementInject(CallbackInfo info) {
        super.tickMovement();
        if (!this.world.isClient) {
            if(this.random.nextInt(100) == 0) {
                heal(2.0F);
            }

            if(snowStage > 1) {
                if(random.nextInt(100) == 0) {
                    addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 60, random.nextInt(2)));
                }
                if(random.nextInt(100) == 0) {
                    addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 120, random.nextInt(3)));
                }
            }

            if (!this.world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
                return;
            }

            BlockState blockState = Blocks.SNOW.getDefaultState();

            for(int l = 0; l < 4; ++l) {
                int i = MathHelper.floor(this.getX() + (double)((float)(l % 2 * 2 - 1) * 0.25F));
                int j = MathHelper.floor(this.getY());
                int k = MathHelper.floor(this.getZ() + (double)((float)(l / 2 % 2 * 2 - 1) * 0.25F));
                BlockPos blockPos2 = new BlockPos(i, j, k);
                if (this.world.getBlockState(blockPos2).isAir() && blockState.canPlaceAt(this.world, blockPos2)) {
                    this.world.setBlockState(blockPos2, blockState);
                }
            }
        }
        info.cancel();
    }

    private void teleportTo(double x,double y,double z) {
        BlockPos.Mutable mutable = new BlockPos.Mutable(x, y, z);

        while(mutable.getY() > this.world.getBottomY() && !this.world.getBlockState(mutable).getMaterial().blocksMovement()) {
            mutable.move(Direction.DOWN);
        }

        BlockState blockState = this.world.getBlockState(mutable);
        boolean bl = blockState.getMaterial().blocksMovement();
        if (bl) {

            this.teleport(x,y,z,false);
        }
    }

    protected void teleportRandomly() {
        if (!this.world.isClient() && this.isAlive()) {
            double d = this.getX() + (this.random.nextDouble() - 0.5D) * 64.0D;
            double e = this.getY() + (double)(this.random.nextInt(64) - 32);
            double f = this.getZ() + (this.random.nextDouble() - 0.5D) * 64.0D;
            this.teleportTo(d,e,f);
        }
    }

    @Override
    public boolean damage(DamageSource source,float amount) {
        if (!source.isMagic() && source.getSource() instanceof LivingEntity livingEntity) {
            if (!source.isExplosive()) {
                livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE,100,2));
                livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,100,2));
                livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,100,2));
            }
        }
        if(source == DamageSource.IN_WALL) {
            this.teleportRandomly();
        }
        return super.damage(source,amount);
    }

    @Inject(at = @At("HEAD"), method = "hurtByWater", cancellable = true)
    public void hurtByWaterInject(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(at = @At("HEAD"), method = "initGoals", cancellable = true)
    public void initGoalsInject(CallbackInfo info) {
        this.goalSelector.add(1, new ProjectileAttackGoal(this, 1.25D, 5, 10.0F));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 1.0D, 1.0000001E-5F));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(4, new LookAroundGoal(this));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, MobEntity.class, 10, true, false, (entity) -> !(entity instanceof SnowGolemEntity)));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false, Objects::nonNull));
        this.goalSelector.add(0, new SwimGoal(this));
        info.cancel();
    }

    @Override
    public void onDeath(DamageSource source) {
        if(snowStage >= 3) {
            AreaEffectCloudEntity areaEffectCloudEntity = new AreaEffectCloudEntity(this.world, this.getX(), this.getY(), this.getZ());
            areaEffectCloudEntity.addEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 5));
            areaEffectCloudEntity.addEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 200, 5));
            areaEffectCloudEntity.addEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 200, 5));
            areaEffectCloudEntity.setRadius(2.5F);
            areaEffectCloudEntity.setRadiusOnUse(-0.5F);
            areaEffectCloudEntity.setWaitTime(10);
            areaEffectCloudEntity.setDuration(areaEffectCloudEntity.getDuration() / 2);
            areaEffectCloudEntity.setRadiusGrowth(-areaEffectCloudEntity.getRadius() / (float)areaEffectCloudEntity.getDuration());

            this.world.spawnEntity(areaEffectCloudEntity);
            this.world.spawnEntity(areaEffectCloudEntity);

            super.onDeath(source);
        } else {
            setHealth(this.getMaxHealth());
            snowStage++;
        }
        BlockState powderSnowPlacer = Blocks.POWDER_SNOW.getDefaultState();
        float f = (float)Math.min(16, 8);
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (BlockPos blockPos2 : BlockPos.iterate(getBlockPos().add(-f, world.getBottomY(),-f),getBlockPos().add(f, world.getTopY(),f))) {
            if (blockPos2.isWithinDistance(this.getPos(),f)) {
                mutable.set(blockPos2.getX(),blockPos2.getY() + 1,blockPos2.getZ());
                BlockState blockState2 = world.getBlockState(mutable);
                if (!blockState2.isSolidBlock(view, pos)) {
                    BlockState blockState3 = world.getBlockState(blockPos2);
                    if (blockState3.isSolidBlock(view, pos)) {
                        if (this.random.nextInt(5) < 1 + snowStage) {
                            world.setBlockState(blockPos2,powderSnowPlacer);
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Inject(at = @At("HEAD"), method = "writeCustomDataToNbt")
    public void writeCustomDataToNbtInject(NbtCompound nbt,CallbackInfo info) {
        nbt.putInt("snowStage", this.snowStage);
    }

    @Inject(at = @At("HEAD"), method = "readCustomDataFromNbt")
    public void readCustomDataFromNbtInject(NbtCompound nbt,CallbackInfo info) {
        this.snowStage = nbt.getInt("snowStage");
    }

    protected void removeHealthModifier() {
        EntityAttributeInstance entityAttributeInstance = this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (entityAttributeInstance != null) {
            if (entityAttributeInstance.getModifier(HEALTH_MODIFIER_ID) != null) {
                entityAttributeInstance.removeModifier(HEALTH_MODIFIER_ID);
            }
        }
    }

    protected void addHealthModifierIfNeeded() {
        if(snowStage == 1) {
            EntityAttributeInstance entityAttributeInstance = this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            if (entityAttributeInstance == null) {
                return;
            }
            entityAttributeInstance.addTemporaryModifier(new EntityAttributeModifier(HEALTH_MODIFIER_ID,"First snow stage", -(40.0D - 30),EntityAttributeModifier.Operation.ADDITION));
        } else if(snowStage == 2) {
            EntityAttributeInstance entityAttributeInstance = this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            if (entityAttributeInstance == null) {
                return;
            }
            entityAttributeInstance.addTemporaryModifier(new EntityAttributeModifier(HEALTH_MODIFIER_ID,"Second snow stage", -(40.0D - 20),EntityAttributeModifier.Operation.ADDITION));
        } else if(snowStage >= 3) {
            EntityAttributeInstance entityAttributeInstance = this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            if (entityAttributeInstance == null) {
                return;
            }
            entityAttributeInstance.addTemporaryModifier(new EntityAttributeModifier(HEALTH_MODIFIER_ID,"Third snow stage", -(40.0D - 10),EntityAttributeModifier.Operation.ADDITION));
        }
    }

    @Override
    protected void fall(double heightDifference,boolean onGround,BlockState landedState,BlockPos landedPosition) {
        super.fall(heightDifference,onGround,landedState,landedPosition);
        if (!this.world.isClient) {
            this.removeHealthModifier();
            this.addHealthModifierIfNeeded();
        }
    }
}
