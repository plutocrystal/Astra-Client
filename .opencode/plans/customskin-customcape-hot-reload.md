# CustomSkin / CustomCape 热加载：Reload 参数 + reload 指令

## 状态
- 用户已确认执行
- 会话处于计划模式，代码编辑被拦截；本文件为最终可执行方案

## 根因
1. `getCapes()/getSkins()` 仅在构造函数执行一次，运行期新增 PNG 不进列表
2. `loadExternal()` 以 `getTexture(loc) == null` 判断缓存，同名替换文件不重读
3. `ModeValue.modes` 为 `Arrays.asList` 固定长度列表，无法运行期刷新

---

## 改动 1：`src/main/java/dev/astra/value/impl/ModeValue.java`

替换类头部至 getModes 之前：

```java
import dev.astra.value.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModeValue extends Value<String> {
    private final List<String> modes = new ArrayList<>();

    public ModeValue(String name, String defaultValue, String... modes) {
        super(name, defaultValue);
        this.modes.addAll(Arrays.asList(modes));
    }

    public List<String> getModes() {
        return modes;
    }

    public void setModes(List<String> newModes) {
        String previous = getValue();
        modes.clear();
        modes.addAll(newModes);
        if (!modes.contains(previous) && !modes.isEmpty()) {
            setValue(modes.get(0));
        }
    }
```

（`is(String)`、`setMode(String)` 保持不变）

## 改动 2：`src/main/java/dev/astra/module/impl/render/CustomSkin.java`

### 新增 import
```java
import dev.astra.event.EventHandler;
import dev.astra.event.events.TickEvent;
```
（`java.util.HashSet`、`java.util.Set` 已有）

### 字段区新增
```java
    public BooleanValue reload;
    private final Set<ResourceLocation> loadedTextures = new HashSet<>();
```

### 构造函数 addValues 追加
```java
        reload = new BooleanValue("Reload", false);
        addValues(skinMode, model, allPlayers, reload);
```

### 新增方法（放在 loadExternal 之后）
```java
    @EventHandler
    public void onTick(TickEvent event) {
        if (reload.isEnabled()) {
            reload.setEnabled(false);
            doReload();
        }
    }

    public void doReload() {
        for (ResourceLocation loc : loadedTextures) {
            mc.getTextureManager().deleteTexture(loc);
        }
        loadedTextures.clear();

        List<String> skins = getSkins();
        skins.add(0, "None");
        skinMode.setModes(skins);
    }
```

### loadExternal() 成功分支记录缓存
原：
```java
                mc.getTextureManager().loadTexture(loc, new DynamicTexture(image));
```
改为：
```java
                mc.getTextureManager().loadTexture(loc, new DynamicTexture(image));
                loadedTextures.add(loc);
```

## 改动 3：`src/main/java/dev/astra/module/impl/render/CustomCape.java`

与改动 2 完全对称：
- import 增加 `dev.astra.event.EventHandler`、`dev.astra.event.events.TickEvent`
- 字段：`public BooleanValue reload;` + `private final Set<ResourceLocation> loadedTextures = new HashSet<>();`
- 构造函数：
```java
        capeMode = new ModeValue("Cape", "None", capes.toArray(new String[0]));
        reload = new BooleanValue("Reload", false);
        addValues(capeMode, reload);
```
- 新增 `onTick(TickEvent)` 与 `doReload()`（同上，内部用 capeMode/getCapes）
- `loadExternal()` 加载成功后 `loadedTextures.add(loc);`

## 改动 4：新建 `src/main/java/dev/astra/command/impl/ReloadCommand.java`

```java
package dev.astra.command.impl;

import dev.astra.Main;
import dev.astra.command.Command;
import dev.astra.module.impl.render.CustomCape;
import dev.astra.module.impl.render.CustomSkin;
import dev.astra.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReloadCommand implements Command {

    @Override
    public String[] getAliases() {
        return new String[]{"reload"};
    }

    @Override
    public void execute(String[] args) {
        String target = args.length == 0 ? "all" : args[0].toLowerCase();

        if (target.equals("skin") || target.equals("all")) {
            CustomSkin customSkin = Main.moduleManager.getModuleByClass(CustomSkin.class);
            if (customSkin != null) {
                customSkin.doReload();
            }
        }

        if (target.equals("cape") || target.equals("all")) {
            CustomCape customCape = Main.moduleManager.getModuleByClass(CustomCape.class);
            if (customCape != null) {
                customCape.doReload();
            }
        }

        if (target.equals("skin") || target.equals("cape") || target.equals("all")) {
            ChatUtil.sendMessage("Successfully reloaded: " + target);
        } else {
            ChatUtil.sendMessage("Usage: .reload <skin/cape/all>");
        }
    }

    @Override
    public List<String> tabComplete(String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(Arrays.asList("skin", "cape", "all"));
        }
        return new ArrayList<>();
    }
}
```

注意：需确认 `ModuleManager.getModuleByClass` 的公开性（AbstractClientPlayer 已有同样调用，可行）。

## 改动 5：`src/main/java/dev/astra/Main.java:41` 注册

在 `commandManager.registerCommand(new ConfigCommand());` 之后追加：
```java
        commandManager.registerCommand(new ReloadCommand());
```

## 改动 6（条件）：HelpCommand
若 HelpCommand 硬编码命令列表则补上 `.reload <skin/cape/all>`；若动态遍历 getCommands() 则无需改动。

## 验证
1. `./gradlew compileJava`
2. 进游戏：往 `Astra/CustomCape/` 添加新 PNG → ClickGUI 右键 CustomCape 点 "Reload" → 列表出现新名字，选中生效
3. 覆盖同名 PNG → 点 Reload 或输入 `.reload all` → 纹理即时刷新
4. 输入 `.reload skin` / `.reload cape` / `.reload abc` 验证指令反馈与 tab 补全
