package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;

@ModuleInfo(
   name = "Sprint",
   description = "Automatically sprints",
   category = Category.MOVEMENT
)
public class Sprint extends Module {
   @EventTarget(0)
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         mc.f_91066_.f_92091_.m_7249_(true);
         mc.f_91066_.m_231832_().m_231514_(false);
      }

   }

   public void onDisable() {
      mc.f_91066_.f_92091_.m_7249_(false);
   }
}
