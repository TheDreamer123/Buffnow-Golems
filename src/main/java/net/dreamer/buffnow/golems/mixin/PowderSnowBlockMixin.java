package net.dreamer.buffnow.golems.mixin;

import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PowderSnowBlock.class)
public class PowderSnowBlockMixin extends Block {
    public PowderSnowBlockMixin(Settings settings) {
        super(settings);
    }

    @Inject(at = @At("HEAD"), method = "canWalkOnPowderSnow", cancellable = true)
    private static void canWalkOnPowderSnow(Entity entity,CallbackInfoReturnable<Boolean> cir) {
        if(entity.getType() == EntityType.SNOW_GOLEM) {
            cir.setReturnValue(true);
        }
    }

    @Inject(at = @At("HEAD"), method = "getCollisionShape", cancellable = true)
    public void getCollisionShapeInject(BlockState state,BlockView world,BlockPos pos,ShapeContext context,CallbackInfoReturnable<VoxelShape> cir) {
        if (context instanceof EntityShapeContext entityShapeContext) {
            Entity entity = entityShapeContext.getEntity();
            if(entity != null) {
                if (entity.getType() == EntityType.SNOW_GOLEM) {
                    cir.setReturnValue(state.getOutlineShape(world, pos));
                }
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "onEntityCollision", cancellable = true)
    public void onEntityCollisionInject(BlockState state,World world,BlockPos pos,Entity entity, CallbackInfo info) {
        if(entity.getType() == EntityType.SNOW_GOLEM) {
            info.cancel();
        }
    }
}
