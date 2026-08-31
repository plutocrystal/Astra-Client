# Silent / Strict MoveFix 区分方案

## 背景
- 上一轮修复后，Silent 与 Strict 走了完全相同的管线：`fixStrafe` 吸附 + 物理 yaw 交换（两处都只判 `moveFix != None`），行为零差异。
- 目标：恢复两个模式的有意义区分，同时**两者都必须保持预测安全**（Tatako Speed (P) 不回退）。

## 最终设计

| 模式 | 输入处理 | 物理 yaw | 手感 |
|------|---------|----------|------|
| None | 原始按键 | 相机 yaw | 原版行为（会被预测反作弊 flag） |
| **Silent** | 意图方向吸附到发包 yaw 的合法 45° 网格（保留现状） | 发包 yaw | 正常操控自己想走的方向，最多偏 22.5°，适合绕圈走位 |
| **Strict** | **零补偿**：原始按键直接透传，不做任何重映射 | 发包 yaw | 移动严格跟随服务端朝向——Aura 锁定时按 W 即朝目标走；等价"真面朝该方向的原版玩家"，兼容性上限最高 |
| LockView | 原始按键 | 真实 = 发包 | 相机真实转向目标 |

两者均 100% 可被服务端模拟复现：
- Silent：离散吸附方向（相对发包 yaw 的 45° 网格）
- Strict：原始离散输入（相对发包 yaw）

## 改动清单（共一处逻辑）

### 1. `src/main/java/net/minecraft/client/entity/EntityPlayerSP.java`（updateEntityActionState 内，约 538 行）

```java
// 改前
&& RotationManager.getMoveFix() != RotationManager.MoveFix.None
```
改为：
```java
// 改后
&& RotationManager.getMoveFix() == RotationManager.MoveFix.Silent
```

效果：仅 Silent 执行 `MoveUtil.fixStrafe(RotationManager.getYaw())` 吸附；Strict 跳过吸附、原始输入透传。

### 2. `src/main/java/net/minecraft/entity/EntityLivingBase.java` —— 不动

- `moveFlyingWithRotFix()`：门槛保持 `getMoveFix() != MoveFix.None`（两种模式都需要物理 yaw 交换，这是 Speed (P) 修复的核心）
- `jump()` 冲刺跳加速：同上保持不变

### 3. 其余文件 —— 不动

- `RotationManager` / `MoveUtil` / KillAura / Deop 无需改动（Deop 共用同一枚举自动获得相同语义）

## 边界情况确认

- Strict 无吸附时：NoSlow/潜行的亚单位缩放经 movementInput 自然透传，moveFlying 线性处理，均为原版可复现状态 ✓
- Silent 分支的 `isForwardPressed()` 门必须保留（无按键时防止吸附出虚假输入）✓
- Velocity Jump / Eagle / MoreKB 不受影响 ✓

## 验证

1. `./gradlew build` 编译通过
2. Tatako 测试服：
   - Silent 绕圈走位 → Speed (P) buffer 不涨，移动方向跟随玩家意图（≤22.5° 偏差）
   - Strict 锁定绕圈 → Speed (P) buffer 不涨（offset 应更接近 0），按 W 实际朝准星/目标方向走
   - None 对照组 → 继续 flag（预期内）
