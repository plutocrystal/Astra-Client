# 修复：击退武器导致的回弹 + Grim "failed Simulation"

## 根因
`EntityPlayer.attackTargetEntityWithCurrentItem` 中（约 947-961 行），攻击者自减速被错误地门控在 `if (this.isSprinting())` 且叠加 KeepSprint factor：
```java
if (i > 0) {
    targetEntity.addVelocity(...);
    if (this.isSprinting()) {
        double factor = 0.6D;
        if (KeepSprint.ACTIVE && KeepSprint.slowDown != null) factor = 1.0D - KeepSprint.slowDown.getValue();
        this.motionX *= factor;
        this.motionZ *= factor;
        this.setSprinting(false);
    }
}
```
服务器/Grim（以及 vanilla/KevinClient/Moonlight）对带击退武器（每次命中 `i>0`）**无条件**执行
`motionX *= 0.6; motionZ *= 0.6; setSprinting(false)`。

结果：非冲刺 + 击退武器时，Astra 跳过自减速，但服务器仍减速 —— 客户端与服务器模拟不一致，
导致 "回弹" 与 "failed Simulation"（每 tick 极小偏移，VL 累积 99→105），且与 KillAura/AutoClick 无关。

## 改动（3 处）

### 1. 新建 `src/main/java/dev/astra/event/events/attack/KeepSprintEvent.java`
```java
package dev.astra.event.events.attack;

import dev.astra.event.Event;

public class KeepSprintEvent extends Event {
}
```

### 2. 改 `net/minecraft/entity/player/EntityPlayer.java`
- 删除 `import dev.astra.module.impl.movement.KeepSprint;`
- 将 `if (i > 0) { ... }` 内块替换为（Moonlight 模式，无条件 0.6，可被事件取消）：
```java
if (i > 0) {
    targetEntity.addVelocity(-MathHelper.sin(this.rotationYaw * (float) Math.PI / 180.0F) * (float) i * 0.5F, 0.1D, MathHelper.cos(this.rotationYaw * (float) Math.PI / 180.0F) * (float) i * 0.5F);
    dev.astra.event.events.attack.KeepSprintEvent keepSprintEvent = new dev.astra.event.events.attack.KeepSprintEvent();
    dev.astra.Main.eventBus.post(keepSprintEvent);
    if (!keepSprintEvent.isCancelled()) {
        this.motionX *= 0.6D;
        this.motionZ *= 0.6D;
        this.setSprinting(false);
    }
}
```

### 3. 改 `dev/astra/module/impl/movement/KeepSprint.java`
移除 `ACTIVE` 静态与 `slowDown` factor，改为事件取消：
```java
package dev.astra.module.impl.movement;

import dev.astra.event.EventHandler;
import dev.astra.event.events.attack.KeepSprintEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;

public class KeepSprint extends Module {
    public KeepSprint() {
        super("KeepSprint", 0, Category.MOVEMENT);
    }

    @EventHandler
    public void onKeepSprint(KeepSprintEvent event) {
        if (mc.thePlayer != null && mc.thePlayer.isSprinting()) event.setCancelled(true);
    }
}
```
（`SlowDown` 值在纯 keep-sprint 模式下无意义，一并移除。）

## 验证
`cd /root/Astra && ./gradlew build -q`

## 效果
- KeepSprint 关（默认）：客户端自减速 = 服务器 → 击退武器不再回弹/刷 Simulation。
- KeepSprint 开：取消自减速+停冲刺，保持冲刺（原特性保留）。
