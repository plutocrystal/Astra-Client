# ChestStealer 配合修复 + 随机顺序 + Delay 统一

状态：待执行（编辑权限受限，计划已确认）

## 背景 / 根因

1. **小偷拿走背包整理要扔的东西**：`ChestStealer.getItemsToSteal()` 虽然调用了
   `cleaner.isStackUseful(stack, stacks, ...)` 过滤，但传入的 `stacks` 只有箱子内容
   （packetStacks）。`isStackUseful` 内部的装备对比（hasBestParameters）、食物/方块计数
   全部只在箱内互相比较，玩家背包已有的更好物品不参与判断。
   例：背包有钻石剑 → 箱内铁剑"最强" → 拿走 → InventoryCleaner 又扔掉。

2. **顺序不够随机**：`RandomSlot` 的 shuffle 被"盔甲优先"、"SmartOrder 确定性最近邻"
   排序覆盖；SmartOrder 每轮固定同一路线。

3. **Delay 不统一**：SmartDelay 分支 = `√槽距 × 固定 multiplier + nextLong(t, t+20)`，
   完全没用 Delay 设置的 min/max；SimulateShortStop 硬编码 150~500ms；
   `Math.random()` 与 RandomUtils 混用。

## 用户已确认的决策

- SmartDelay / DelayMultiplier / SimulateShortStop **保留**，但计算结果 clamp 进
  Delay 设置的 min/max 区间后再随机化。
- 顺序随机化采用**局部随机**：随机化最近邻（距离相近候选中随机挑），盔甲优先保留。

## 修改文件

仅 `src/main/java/dev/astra/module/impl/player/ChestStealer.java`

### 1. 评估列表纳入玩家背包（getItemsToSteal）

新增 `buildEvalStacks()`：

```java
private List<ItemStack> buildEvalStacks() {
    if (mc.thePlayer != null && mc.thePlayer.openContainer != null) {
        List<ItemStack> live = mc.thePlayer.openContainer.getInventory();
        if (live != null && live.size() > 36 && live.size() == packetStacks.length) {
            return live;
        }
    }
    List<ItemStack> fallback = new ArrayList<>();
    Collections.addAll(fallback, packetStacks);
    return fallback;
}
```

`getItemsToSteal()` 开头由 `new ArrayList + addAll(packetStacks)` 改为
`List<ItemStack> stacks = buildEvalStacks();`

- 实时容器布局（箱子区在前 + 玩家36格在后）与 packetStacks 对齐，索引即槽位号，
  `identityIndexOf` 引用匹配、`canBeSortedTo(index, size)` 映射均保持正确。
- 长度不一致或容器不可用时回退旧行为。
- `stacks` 在方法内只读（仅 get/size），无需拷贝。
- packetStacks 原有职责（receivedId 同步、debug 输出）不变。

### 2. SmartOrder 局部随机化（sortBasedOnOptimumPath 整体替换）

```java
private void sortBasedOnOptimumPath(List<ItemTakeRecord> itemsToSteal) {
    for (int i = 0; i < itemsToSteal.size(); i++) {
        int minDistance = Integer.MAX_VALUE;
        for (int j = i + 1; j < itemsToSteal.size(); j++) {
            int distance = squaredDistanceOfSlots(itemsToSteal.get(i).index, itemsToSteal.get(j).index);
            if (distance < minDistance) {
                minDistance = distance;
            }
        }
        double threshold = Math.max(minDistance * 1.6D, 1.0D);
        List<Integer> candidates = new ArrayList<>();
        for (int j = i + 1; j < itemsToSteal.size(); j++) {
            int distance = squaredDistanceOfSlots(itemsToSteal.get(i).index, itemsToSteal.get(j).index);
            if (distance <= threshold) {
                candidates.add(j);
            }
        }
        if (candidates.isEmpty()) continue;
        Integer pickIndex = RandomUtils.getRandomElement(candidates);
        if (pickIndex == null || pickIndex.intValue() == i + 1) continue;
        ItemTakeRecord picked = itemsToSteal.get(pickIndex.intValue());
        itemsToSteal.set(pickIndex.intValue(), itemsToSteal.get(i + 1));
        itemsToSteal.set(i + 1, picked);
    }
}
```

- 阈值作用于平方距离（≈真实距离 ×1.26 的邻域窗口），每步从窗口内随机选。
- `finishRecords` 中盔甲优先/sortableTo 优先排序保持不动。

### 3. Delay 统一 clamp 进范围 + RandomUtils

新增两个辅助方法（放在 `sampleRange` 之后）：

```java
private long sampleSmartDelay(int fromIndex, int toIndex) {
    double min = Math.max(0.0D, delay.getMinValue());
    double max = Math.max(min, delay.getMaxValue());
    int dist = squaredDistanceOfSlots(fromIndex, toIndex);
    long base = (long) (Math.sqrt(dist) * multiplier.getValue());
    long spread = Math.max(1L, base / 4L);
    long low = Math.max((long) min, Math.min((long) max, base - spread));
    long high = Math.max((long) min, Math.min((long) max, base + spread));
    return RandomUtils.nextLong(low, high);
}

private long delayMaxValue() {
    double min = Math.max(0.0D, delay.getMinValue());
    return (long) Math.max(min, delay.getMaxValue());
}
```

ITERATE 分支替换：

```java
long stealingDelay;
if (smartDelay.isEnabled() && iterateCursor + 1 < itemsToSteal.size()) {
    stealingDelay = sampleSmartDelay(record.index,
            itemsToSteal.get(iterateCursor + 1).index);
} else {
    stealingDelay = sampleRange(delay);
}
```

SimulateShortStop 替换（同时去掉 Math.random()）：

```java
long totalDelay = stealingDelay;
if (simulateShortStop.isEnabled() && RandomUtils.nextDouble() > 0.75D) {
    long pauseBase = delayMaxValue();
    totalDelay += RandomUtils.nextLong(pauseBase, pauseBase * 4L);
}
LBInventoryManager.delay(totalDelay);
```

- StartDelay / CloseDelay / 普通 Delay 分支已是 RandomUtils(min,max)，不动。
- 设置项无增删，refreshVisibility 无需改动。

## 验证

- `./gradlew compileJava --offline -q` 编译通过。
- 复查点：
  - buildEvalStacks 回退路径在主菜单/容器未同步时不 NPE；
  - nextLong(low, high) 在 low==high 时安全（RandomUtils 已处理 range<=0）；
  - finishRecords 各排序与 ITERATE 回调对 record.stack 引用的使用不受评估列表替换影响
    （record.stack 现在来自实时列表引用，仅用于 instanceof/getDisplayName 判断，安全）。
