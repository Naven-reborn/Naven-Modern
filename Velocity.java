package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.mixin.O.accessors.LocalPlayerAccessor;
import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventHandlePacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRespawn;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.LongJump;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Scaffold;
import com.heypixel.heypixelmod.obsoverlay.utils.BlockUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.PlayerUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.Rotation;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationManager;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import java.util.concurrent.LinkedBlockingDeque;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.EnchantmentTableBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FletchingTableBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SmithingTableBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(
   name = "Velocity",
   description = "Reduces knockback.",
   category = Category.MOVEMENT
)
public class Velocity extends Module {
   LinkedBlockingDeque<Packet<ClientGamePacketListener>> inBound = new LinkedBlockingDeque();
   public static Velocity.Stage stage;
   public static int grimTick;
   public static int debugTick;
   public BooleanValue log = ValueBuilder.create(this, "Logging").setDefaultBooleanValue(false).build().getBooleanValue();
   Packet velocityPacket;
   private BlockHitResult result = null;
   Scaffold.BlockPosWithFacing pos;

   private boolean shouldAvoidInteraction(Block block) {
      return block instanceof ChestBlock || block instanceof CraftingTableBlock || block instanceof FurnaceBlock || block instanceof EnderChestBlock || block instanceof BarrelBlock || block instanceof ShulkerBoxBlock || block instanceof AnvilBlock || block instanceof EnchantmentTableBlock || block instanceof BrewingStandBlock || block instanceof BeaconBlock || block instanceof HopperBlock || block instanceof DispenserBlock || block instanceof DropperBlock || block instanceof LecternBlock || block instanceof CartographyTableBlock || block instanceof FletchingTableBlock || block instanceof SmithingTableBlock || block instanceof StonecutterBlock || block instanceof LoomBlock || block instanceof GrindstoneBlock || block instanceof ComposterBlock || block instanceof CauldronBlock || block instanceof BedBlock || block instanceof DoorBlock || block instanceof TrapDoorBlock || block instanceof FenceGateBlock || block instanceof ButtonBlock || block instanceof LeverBlock || block instanceof NoteBlock;
   }

   public void reset() {
      if (mc.m_91403_() != null) {
         stage = Velocity.Stage.IDLE;
         grimTick = -1;
         debugTick = 0;
         this.processPackets();
      }
   }

   public void processPackets() {
      ClientPacketListener connection = mc.m_91403_();
      if (connection == null) {
         this.inBound.clear();
      } else {
         Packet packet;
         while((packet = (Packet)this.inBound.poll()) != null) {
            try {
               packet.m_5797_(connection);
            } catch (Exception var4) {
               var4.printStackTrace();
               this.inBound.clear();
               break;
            }
         }

      }
   }

   public Direction checkBlock(Vec3 baseVec, BlockPos bp) {
      if (!(mc.f_91073_.m_8055_(bp).m_60734_() instanceof AirBlock)) {
         return null;
      } else {
         Vec3 center = new Vec3((double)bp.m_123341_() + 0.5D, (double)((float)bp.m_123342_() + 0.5F), (double)bp.m_123343_() + 0.5D);
         Direction sbface = Direction.DOWN;
         Vec3 hit = center.m_82549_(new Vec3((double)sbface.m_122436_().m_123341_() * 0.5D, (double)sbface.m_122436_().m_123342_() * 0.5D, (double)sbface.m_122436_().m_123343_() * 0.5D));
         Vec3i baseBlock = bp.m_121955_(sbface.m_122436_());
         BlockPos po = new BlockPos(baseBlock.m_123341_(), baseBlock.m_123342_(), baseBlock.m_123343_());
         if (!mc.f_91073_.m_8055_(po).m_60638_(mc.f_91073_, po, mc.f_91074_, sbface)) {
            return null;
         } else {
            Vec3 relevant = hit.m_82546_(baseVec);
            if (relevant.m_82556_() <= 20.25D && relevant.m_82541_().m_82526_(Vec3.m_82528_(sbface.m_122436_()).m_82541_()) >= 0.0D) {
               this.pos = new Scaffold.BlockPosWithFacing(new BlockPos(baseBlock), sbface.m_122424_());
               return sbface.m_122424_();
            } else {
               return null;
            }
         }
      }
   }

   public void onEnable() {
      this.reset();
   }

   public void onDisable() {
      this.reset();
   }

   private void log(String message) {
      if (this.log.getCurrentValue()) {
         ChatUtils.addChatMessage(message);
      }

   }

   @EventTarget
   public void onWorld(EventRespawn eventRespawn) {
      this.reset();
   }

   @EventTarget
   public void onTick(EventRunTicks eventRunTicks) {
      if (mc.f_91074_ != null && mc.m_91403_() != null && mc.f_91072_ != null && eventRunTicks.getType() != EventType.POST) {
         if (!Naven.getInstance().getModuleManager().getModule(LongJump.class).isEnabled()) {
            if (mc.f_91074_.m_21224_() || !mc.f_91074_.m_6084_() || mc.f_91074_.m_21223_() <= 0.0F || mc.f_91080_ instanceof ProgressScreen || mc.f_91080_ instanceof DeathScreen) {
               this.reset();
            }

            if (debugTick > 0) {
               --debugTick;
               if (debugTick == 0) {
                  this.processPackets();
                  stage = Velocity.Stage.IDLE;
               }
            } else {
               stage = Velocity.Stage.IDLE;
            }

            if (grimTick > 0) {
               --grimTick;
            }

            float yaw = RotationManager.rotations.getX();
            float pitch = 89.79F;
            BlockHitResult blockRayTraceResult = (BlockHitResult)PlayerUtils.pickCustom(3.700000047683716D, yaw, pitch);
            if (stage == Velocity.Stage.TRANSACTION && grimTick == 0 && blockRayTraceResult != null && !BlockUtils.isAirBlock(blockRayTraceResult.m_82425_()) && mc.f_91074_.m_20191_().m_82381_(new AABB(blockRayTraceResult.m_82425_().m_7494_()))) {
               Block targetBlock = mc.f_91073_.m_8055_(blockRayTraceResult.m_82425_()).m_60734_();
               if (targetBlock instanceof ChestBlock || targetBlock instanceof CraftingTableBlock || targetBlock instanceof FurnaceBlock || targetBlock instanceof EnchantmentTableBlock || targetBlock instanceof AnvilBlock || targetBlock instanceof BarrelBlock || targetBlock instanceof ShulkerBoxBlock) {
                  return;
               }

               this.result = new BlockHitResult(blockRayTraceResult.m_82450_(), blockRayTraceResult.m_82434_(), blockRayTraceResult.m_82425_(), false);
               ((LocalPlayerAccessor)mc.f_91074_).setYRotLast(yaw);
               ((LocalPlayerAccessor)mc.f_91074_).setXRotLast(pitch);
               RotationManager.setRotations((new Rotation(yaw, pitch)).toVec2f());
               if (Aura.rotation != null) {
                  Aura.rotation = (new Rotation(yaw, pitch)).toVec2f();
               }

               this.processPackets();
               mc.f_91074_.f_108617_.m_104955_(new Rot(yaw, pitch, mc.f_91074_.m_20096_()));
               mc.f_91072_.m_233732_(mc.f_91074_, InteractionHand.MAIN_HAND, this.result);
               Naven.skipTasks.add(() -> {
               });

               for(int i = 2; i <= 100; ++i) {
                  Naven.skipTasks.add(() -> {
                     EventMotion event1 = new EventMotion(EventType.PRE, mc.f_91074_.m_20182_().f_82479_, mc.f_91074_.m_20182_().f_82480_, mc.f_91074_.m_20182_().f_82481_, yaw, pitch, mc.f_91074_.m_20096_());
                     Naven.getInstance().getRotationManager().onPre(event1);
                     if (event1.getYaw() != yaw || event1.getPitch() != pitch) {
                        mc.f_91074_.f_108617_.m_104955_(new Rot(event1.getYaw(), event1.getPitch(), mc.f_91074_.m_20096_()));
                     }

                  });
               }

               debugTick = 20;
               stage = Velocity.Stage.BLOCK;
               grimTick = 0;
            }

         }
      }
   }

   @EventTarget
   public void onPacket(EventHandlePacket e) {
      if (mc.f_91074_ != null && mc.m_91403_() != null && mc.f_91072_ != null && !mc.f_91074_.m_6117_()) {
         if (!Naven.getInstance().getModuleManager().getModule(LongJump.class).isEnabled()) {
            if (mc.f_91074_.f_19797_ < 20) {
               this.reset();
            } else if (!mc.f_91074_.m_21224_() && mc.f_91074_.m_6084_() && !(mc.f_91074_.m_21223_() <= 0.0F) && !(mc.f_91080_ instanceof ProgressScreen) && !(mc.f_91080_ instanceof DeathScreen)) {
               Packet<?> packet = e.getPacket();
               if (packet instanceof ClientboundLoginPacket) {
                  this.reset();
               } else {
                  if (debugTick > 0 && mc.f_91074_.f_19797_ > 20) {
                     if (stage == Velocity.Stage.BLOCK && packet instanceof ClientboundBlockUpdatePacket) {
                        ClientboundBlockUpdatePacket cbu = (ClientboundBlockUpdatePacket)packet;
                        if (this.result != null && this.result.m_82425_().equals(cbu.m_131749_())) {
                           this.processPackets();
                           Naven.skipTasks.clear();
                           debugTick = 0;
                           this.result = null;
                           return;
                        }
                     }

                     if (!(packet instanceof ClientboundSystemChatPacket) && !(packet instanceof ClientboundSetTimePacket)) {
                        e.setCancelled(true);
                        this.inBound.add(packet);
                        return;
                     }
                  }

                  if (packet instanceof ClientboundSetEntityMotionPacket) {
                     ClientboundSetEntityMotionPacket packetEntityVelocity = (ClientboundSetEntityMotionPacket)packet;
                     if (packetEntityVelocity.m_133192_() != mc.f_91074_.m_19879_()) {
                        return;
                     }

                     if (packetEntityVelocity.m_133196_() < 0 || mc.f_91074_.m_21205_().m_41720_() instanceof EnderpearlItem) {
                        e.setCancelled(false);
                        return;
                     }

                     grimTick = 2;
                     debugTick = 100;
                     stage = Velocity.Stage.TRANSACTION;
                     e.setCancelled(true);
                  }

               }
            } else {
               this.reset();
            }
         }
      }
   }

   static {
      stage = Velocity.Stage.IDLE;
      grimTick = -1;
      debugTick = 10;
   }

   public static enum Stage {
      TRANSACTION,
      ROTATION,
      BLOCK,
      IDLE;

      // $FF: synthetic method
      private static Velocity.Stage[] $values() {
         return new Velocity.Stage[]{TRANSACTION, ROTATION, BLOCK, IDLE};
      }
   }
}
