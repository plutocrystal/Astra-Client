# FullBright: Add Entity Full-Bright Toggle

## Objective
Add an **entity full-bright toggle** to the existing `FullBright` module. When enabled,
all rendered entities are forced to maximum brightness (fully lit), independent of
ambient/block light. This is a hard on/off toggle (no slider) — "只要开关(满亮)".

## Background / Pipeline
Entity brightness is controlled by the **lightmap UV** set before rendering via
`OpenGlHelper.setLightmapTextureCoords(lightmapTexUnit, j, k)`, where `j` = block light
and `k` = sky light (each 0..240). Setting them to 240 forces a fully-lit entity
(the same value `RenderItem` already uses for full-bright items).

The single centralized hook for **all entities** is `RenderManager.renderEntity()`
(lines 239–247). `RendererLivingEntity.renderModel` draws living-model geometry against
this same lightmap, and `RenderLiving.setLightmap` is only used by special "eyes" layers
(dragon/enderman/spider), so `RenderManager` is the effective, sufficient hook.

## Changes

### 1. `src/main/java/dev/astra/module/impl/render/FullBright.java`
- Add `public static FullBright INSTANCE;` (pattern already used by `NoRender`/`Chams`).
- Add `public BooleanValue entityBrightness = new BooleanValue("EntityBrightness", false);`
- Register it: `addValues(mode, entityBrightness);`
- Assign `INSTANCE = this;` in constructor.
- Add static accessor:
  ```java
  public static boolean shouldBrightenEntities() {
      return INSTANCE != null && INSTANCE.isToggle() && INSTANCE.entityBrightness.isEnabled();
  }
  ```
- Add `import dev.astra.value.impl.BooleanValue;`

### 2. `src/main/java/net/minecraft/client/renderer/entity/RenderManager.java`
In `renderEntity()` (around lines 239–247), after computing `j`/`k` (and after
`isBurning()` override), force full brightness when the toggle is on:
```java
int j = i % 65536;
int k = i / 65536;
if (dev.astra.module.impl.render.FullBright.shouldBrightenEntities()) {
    j = 240;
    k = 240;
}
OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) j, (float) k);
```
Apply the same in `renderWitherSkull()` (lines 259–262) for consistency.

## Verification
- `cd /root/Astra && ./gradlew compileJava --offline` (Java 8 — previously BUILD SUCCESSFUL).

## Caveats
- Hard full-bright (no adjustable slider), as requested.
- Affects all entities rendered through `RenderManager` (mobs, players, items, vehicles).
- Independent of FullBright's Gamma mode (scene-wide); this is entity-only.
- In-game live toggling works since it is checked each render pass.
- With shaders ON behavior may differ, but current setup has shaders off (`Shaders: ~~NULL~~`).
