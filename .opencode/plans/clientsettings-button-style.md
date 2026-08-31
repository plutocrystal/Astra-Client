# Plan: Augustus 按钮默认颜色 + 圆角可配置

## 目标
1. 将 Augustus 按钮默认颜色从 `(67, 122, 163)` 改为 `(22, 155, 235)`
2. 为 Augustus 按钮添加圆角效果
3. 颜色和圆角半径可在 ClientSettings 中配置

## 改动文件

### 1. `src/main/java/dev/astra/module/impl/render/ClientSettings.java`

新增 4 个 NumberValue 设置项：
- `ButtonRed` — 默认值 22，范围 0-255，步进 1
- `ButtonGreen` — 默认值 155，范围 0-255，步进 1
- `ButtonBlue` — 默认值 235，范围 0-255，步进 1
- `ButtonCornerRadius` — 默认值 4.0，范围 0-10，步进 0.5

将这 4 个值加入构造函数的 `addValues()` 调用。

新增 3 个静态 getter 方法：
- `getButtonColor()` — 返回 `new Color(r, g, b, 255).getRGB()`
- `getButtonCornerRadius()` — 返回圆角半径 float 值
- （保留现有 `useAugustusStyle()` 不变）

### 2. `src/main/java/net/minecraft/client/gui/GuiButton.java`

- 删除硬编码的 `BUTTON_COLOR` 常量
- 在 `drawButton()` 的 Augustus 分支中：
  - 用 `RenderUtil.drawRoundedRect(float, float, float, float, float, int)` 替代 `Gui.drawRect()`
  - 颜色从 `ClientSettings.getButtonColor()` 获取
  - 圆角半径从 `ClientSettings.getButtonCornerRadius()` 获取
  - hover 效果保持现有的 HSB 亮度调整逻辑

需要新增 import：`dev.astra.util.RenderUtil`

## 验证
- `./gradlew compileJava` 编译通过
