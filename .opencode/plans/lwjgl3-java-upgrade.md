# LWJGL 2 → 3 迁移 + Gradle 9.7.1 升级（对齐 Unfair 方案）

状态：已确认待执行
参考：github.com/UnfairGaming/Unfair（OpenMyau 血统，LWJGL 3.4.2 + org.lwjgl v2 兼容层）

## 调研结论

- 本仓库 src 含完整 1.8.9 客户端源码（1613 文件，含 OptiFine），结构同 MCP
- LWJGL2 使用面统计：input 73 文件；Display/Sys/GLContext/PixelFormat ~20 文件；
  util.glu 10 文件；util.vector 16 文件；BufferUtils 7 文件；openal 0（声音走二进制 jar）
- Unfair 方案 = 自带 `org.lwjgl` v2 兼容层（~90 文件）+ lwjgl3 3.4.2 六件套，
  MC 源码 import 零修改；声音系统靠 v2 风格 openal shim 让 paulscode 二进制继续工作
- Gradle 最新 9.7.1；Shadow 必须换 `com.gradleup.shadow` 9.6.1（旧 id 不兼容 Gradle 9）
- 用户决策：Win+Linux+macOS x64 三平台 natives；一次到位执行

## 阶段 A — 构建升级（保持 lwjgl2 编译绿）

1. Wrapper 升级：`./gradlew wrapper --gradle-version 9.7.1 --distribution-type bin`
   （distributionUrl 保持腾讯镜像域名，仅改版本号）
2. build.gradle：
   - `id "com.gradleup.shadow" version "9.6.1"` 替换旧插件
   - `java { sourceCompatibility = JavaVersion.VERSION_1_8 }` +
     `tasks.withType(JavaCompile) { options.release = 8 }`（产物 Java 8 字节码；
     构建机需 JDK17+ 跑 Gradle 9；8/17/21/25 JRE 均可运行产物）
   - shadowJar/jar/runClient 任务语法核对（Shadow 9.x 中
     `configurations = [project.configurations.runtimeClasspath]` 仍有效）
3. 门禁：`./gradlew compileJava build` 通过

## 阶段 B — LWJGL3 落地

4. 依赖调整：
   - 删除 libs/minecraft 中：lwjgl-2.9.4-nightly.jar、lwjgl_platform-natives-windows、
     lwjgl_util-2.9.4-nightly.jar、jinput-2.0.5(+platform)、jutils、librarylwjglopenal
     （删除前 grep 确认 org.lwjgl.util.* 非 vector/glu 引用面）
   - mavenCentral 引入（platform BOM 3.4.2）：
     org.lwjgl:lwjgl / lwjgl.glfw / lwjgl.openal / lwjgl.opengl / lwjgl.stb / lwjgl.jemalloc
     + runtimeOnly natives classifiers: windows-x64, linux-x64, macos-x64（各模块）
5. 新增兼容层 `src/main/java/org/lwjgl/**`（从 Unfair raw.githubusercontent 拉取，
   OpenMyau/lwjglx 血统 BSD 许可）：
   - 直接采用：Sys、LWJGLException、BufferChecks、MemoryUtil、LWJGLUtil、Sync、
     input/{Keyboard,Mouse,KeyCodes,Cursor,Controllers,EventQueue}、
     opengl/{Display,DisplayMode,PixelFormat,ContextAttribs,ContextCapabilities,GLContext,
     SharedDrawable,Pbuffer,...}、util/glu/*、util/vector/*、openal/{AL10,AL,ALCdevice,
     ALCcontext,EFX10,...}
   - 决策点（编译驱动）：opengl/GL11~GL20 与 ARB*/EXT* —— 先尝试直接用 lwjgl3 原生类
     （签名几乎一致），编译报错处再决定引入对应 v2 shim（内部委托 *C 类）；
     vendor 的 GL12C/GL13C 等若与 lwjgl3 jar 同名冲突则弃用 shim 版
6. 声音系统：不改动任何 paulscode 调用；v2 openal shim 使 soundsystem +
   librarylwjglopenal…（librarylwjglopenal 已删）→ 由 shim 的 org.lwjgl.openal.*
   提供 v2 API，paulscode soundsystem jar 内部 LibraryLWJGLOpenAL 直接链接成功
   （Unfair 同款做法，其 librarylwjglopenal 仍在 libs——若编译期需要则保留该 jar，
   仅移除 lwjgl2 本体）
7. Twitch/stream 包：twitch jar 若存在 lwjgl2 链接需求，运行时不触发即可；
   编译失败则 stub net/minecraft/client/stream 相关入口
8. 门禁：`./gradlew compileJava` 全绿（迭代修复）

## 阶段 C — 收尾

9. runClient：移除 `-Djava.library.path=natives/`；注释说明 macOS 需 `-XstartOnFirstThread`
10. 验证 shadowJar：`unzip -l | grep lwjgl` 确认 lwjgl3 六件套+三平台 natives 打入
11. `./gradlew build` 全量通过；输出变更说明

## 风险与限制

- 本环境无显示设备：只能做编译级验证；实际启动需用户本地 `gradlew runClient`
  （JDK17+ 构建后产物仍可在 Java 8 运行）
- OptiFine Shaders.java 的 ARB/ext 调用面广，是最可能的编译修复重灾区（Unfair 的
  ARB shim 即为此准备）
- 兼容层文件量大但机械性强，来源为已验证的开源实现，风险集中在少数签名差异

## 验证清单

- [ ] gradle 9.7.1 wrapper 生效
- [ ] compileJava / build 绿
- [ ] 无 org.lwjgl v2 二进制残留依赖（lwjgl2/jinput/librarylwjglopenal 已移除或仅存必要项）
- [ ] shadowJar 含 lwjgl3 + windows/linux/macos natives
- [ ] 用户本地 runClient 实测（主菜单渲染、键盘鼠标输入、声音、全屏切换）
