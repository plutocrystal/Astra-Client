/*
 * ============================================================================
 *  Aura (KillAura) — 已解混淆可读版
 * ============================================================================
 *  来源 : net.nonemc.leaf.module.modules.combat.Aura  (Leaf client, Kotlin)
 *  说明 : 这是对 ProGuard 混淆字节码的人工逆向“可读翻译”，聚焦【转头(aim/rotation)】
 *         核心算法。并非字节级等价源码，而是以“读懂算法”为目标的重写版本：
 *         - 配置项字段已按 @Metadata 还原为真实属性名（orbid 原名在注释中）。
 *         - 局部变量按语义重新命名；仍不确定处保留注释标记。
 *         - 方法名 proguard/jar/ant/retrace 是 ProGuard 复用名，此处按各自真实角色
 *           拆分命名（它们是同一个字节码名字代表多个逻辑实体，无法自动全局改名）。
 *
 *  主流程:
 *     onUpdate                        每 tick 选目标、算瞄准点
 *       ├─ selectTarget()             过滤 + 选最近可见目标
 *       ├─ aimPoint()/visible()       计算可命中瞄准点（实体包围盒取点 + 射线检测）
 *       └─ smoothYaw()/smoothPitch()  转头核心（偏航 PID+贝塞尔+噪声；俯仰）
 *  ============================================================================
 */

package net.nonemc.leaf.module.modules.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.nonemc.leaf.libs.rotation.Rotation;
import net.nonemc.leaf.libs.rotation.RotationLib;

/**
 * 说明：为便于阅读，将原混淆的成员按角色重命名
 *  - proguard(...) 多种重载：
 *        float proguard(float cur,float tgt,Entity) = smoothPitch()
 *        float proguard(float min,float max)        = random(min,max)
 *        float proguard(float x)                    = fract(x)
 *        float proguard(float t,int oct,float pers) = fractalNoise()
 *        Pair<Boolean,Vec3> proguard(EntityPlayer,Entity) = aimPointVisible()
 *  - jar(...)
 *        float jar(float cur,float tgt,Entity)      = smoothYaw()
 *  - ant() = resetInputs() 之类的次要函数已省略
 *  - noise 数组: nlPhase=proguard(float[]), nlState=jar(float[])
 */
public final class Aura /* extends Module */ {

    /* ========================================================================
     * 1) 状态字段（原混淆单字母，按真实名称还原）
     *    —— 其中“时间戳”类字段大多投影在 basicSimulationOutOfView* 等上
     * ======================================================================== */
    private static float yawVelocity;          // 偏航平滑速度/速度累积 (c)
    private static float integral;             // PID 积分项累积 (j)
    private static float lastError;            // 上一次误差 (i = basicSimulationLastError)

    private static long   humanReactionUntil;  // 人为反应延迟截止时间 (d -> humanReactionUntil)
    private static float  humanCurvatureSeed;   // (m -> humanCurvatureSeed)
    private static float  noiseTime;            // (g -> basicSimulationNoiseTime)
    private static long   lagActiveUntil;      // (h)
    private static long   lagCooldownUntil;    // (i)
    private static float  lagSlowFactor;       // (o)
    private static long   aheadActiveUntil;    // (j)
    private static long   aheadCooldownUntil;  // (k)
    private static float  aheadOffset;         // (p)

    private static float  nextTargetYaw;       // 下一个目标偏航 (q -> nextTargetYaw)
    private static boolean fovActive;          // 是否已经进入视野内 (g -> getFov)

    // -- 额外运行时状态（本次还原所用） --
    private static boolean firstTargetActive;          // 首次目标“已咬住”
    private static Timer   firstTargetTimer;           // 首次目标等待计时 (clickDelay/其它 Timer)
    private static float   lastYaw;                    // 上一帧 adjusted target, 用于反应突变检测
    private static float   lastDerivative;             // 上一帧导数值 (PID-D 平滑)
    private static float   localZ;                     // 落地微调整值
    private static long    landingOffsetUntil;         // 落地抑制截止
    private static long    microCooldownUntil;         // 末端加速冷却
    private static Timer   adjustTimer;                // 最终吸附计时

    // -- 俯仰状态 --
    private static float   targetPitch;                // 俯仰输出 (原 static 复用 float)
    private static int     jitterIndex;                // DataSwim 数据池游标 (x)
    private static java.util.List<Float> dataPool;     // DataSwim 数据池 (proguard Value<List?> 投影)
    private static Value<String> pitchMode;            // 俯仰模式 (retrace Value<String>)

    /* ========================================================================
     * 2) 配置项 —— 用到的 PID / 噪声 / 行为参数（原 Value.get() 引用已还原）
     *    每个均来自 @Metadata 真实属性名，混淆代号放在末尾括号里
     * ======================================================================== */
    // -- PID 基础 --                                                (混淆代号)
    private static Value<Float> pidPBase;            // (E)
    private static Value<Float> pidPAttenuation;     // (F)
    private static Value<Float> pidPDynamicGain;     // (G)
    private static Value<Float> pidPNonlinearity;    // (H)
    private static Value<Float> pidPResponseCurve;   // (I)
    private static Value<Float> pidIBase;            // (J)
    private static Value<Float> pidIAttenuation;     // (K)
    private static Value<Float> pidIAdaptive;        // (L)
    private static Value<Float> pidINonlinearDecay;  // (M)
    private static Value<Float> pidIAntiWindup;      // (N)
    private static Value<Float> pidIDynamicClamp;    // (O)
    private static Value<Float> pidDBase;            // (P)
    private static Value<Float> pidDAttenuation;     // (Q)
    private static Value<Float> pidDNoiseFilter;     // (R)
    private static Value<Float> pidDNonlinearBoost;  // (S)
    private static Value<Float> pidDErrorSensitivity;// (T)
    private static Value<Float> pidErrorDeadzone;    // (U)
    private static Value<Float> pidErrorSaturation;  // (V)
    private static Value<Float> pidDynamicCoupling;  // (W)

    // -- 速度/加速度 --                                                 (混淆代号)
    private static Value<Float> speedBase;           // (v)
    private static Value<Float> speedMultBase;       // (w)
    private static Value<Float> speedMultLarge;      // (x, delta>90)
    private static Value<Float> speedMultMedium;     // (y, delta>30)
    private static Value<Float> speedMultSmall;      // (z, else)
    private static Value<Float> accelBase;           // (A)
    private static Value<Float> accelMultLarge;      // (B)
    private static Value<Float> accelMultMedium;     // (C)
    private static Value<Float> accelMultSmall;      // (D)

    // -- 抖动/噪声 --                                                    (混淆代号)
    private static Value<Float> jitterHigh;          // (X)
    private static Value<Float> jitterLow;           // (Y)
    private static Value<Float> dampingFactor;       // (Z)
    private static Value<Float> fatigueDecay;        // (aa)
    private static Value<Float> fatigueRecover;      // (ab)
    private static Value<Float> microJitter;         // (ac)
    private static Value<Float> microSpeedJitter;    // (ad)
    private static Value<Float> overshootDecay;      // (ae)
    private static Value<Float> overshootThreshold;  // (af)
    private static Value<Float> fineTuningThreshold; // (ag)
    private static Value<Float> reactionLagChance;   // (ah)
    private static Value<Float> reactionLagFactor;   // (ai)
    private static Value<Float> reactionBoostChance; // (aj)
    private static Value<Float> reactionBoostFactor; // (ak)

    // -- 延迟/回弹 (lag & ahead) --                                     (混淆代号)
    private static Value<Float> lagMinDelta;         // (al)
    private static Value<Float> lagChance;           // (am)
    private static Value<Float> lagDurMin;           // (an)
    private static Value<Float> lagDurMax;           // (ao)
    private static Value<Float> lagCdMin;            // (ap)
    private static Value<Float> lagCdMax;            // (aq)
    private static Value<Float> lagSlowMin;          // (ar)
    private static Value<Float> lagSlowMax;          // (as)
    private static Value<Float> aheadMinDelta;       // (at)
    private static Value<Float> aheadChance;         // (au)
    private static Value<Float> aheadDurMin;         // (av)
    private static Value<Float> aheadDurMax;         // (aw)
    private static Value<Float> aheadCdMin;          // (ax)
    private static Value<Float> aheadCdMax;          // (ay)
    private static Value<Float> aheadAmtMin;         // (az)
    private static Value<Float> aheadAmtMax;         // (aA)

    // -- 游泳/轨迹 --                                                   (混淆代号)
    private static Value<Float> swimSpeed;           // (aB)
    private static Value<Float> swimDeltaLimit;      // (aC)
    private static Value<Float> swimMaxAmp;          // (aD)
    private static Value<Float> trajectoryFactor;    // (aE)
    private static Value<Float> trajectoryLowFreq;   // (aF)
    private static Value<Float> trajectoryHighFreq;  // (aG)
    private static Value<Float> trajectoryOvershoot; // (aH)

    // -- 微调/瞄准吸附 --                                              (混淆代号)
    private static Value<Float> adjustMinDelta;      // (aI)
    private static Value<Float> adjustMaxDelta;      // (aJ)
    private static Value<Float> adjustTime;          // (aK)
    private static Value<Float> adjustMaxRandom;     // (aL)
    private static Value<Float> adjustMinRandom;     // (aM)
    private static Value<Float> breathingPeriod;     // (aN)
    private static Value<Float> breathingAmplitude;  // (aO)
    private static Value<Float> rhythmMinDur;        // (aP)
    private static Value<Float> rhythmMaxDur;        // (aQ)
    private static Value<Float> rhythmAmplitude;     // (aR)
    private static Value<Float> dpiSensitivity;      // (aS)
    private static Value<Float> dpiBase;             // (aT)
    private static Value<Float> dpiJitterAmp;        // (aU)

    // -- 基础噪声叠加层 (nl = noise layer) --                           (混淆代号)
    private static Value<Float> noiseAmplitude;          // (aV)
    private static Value<Float> noiseFrequency;          // (aW)
    private static Value<Float> noiseOctaves;            // (aX)
    private static Value<Float> noisePersistence;        // (aY)
    private static Value<Float> noiseSmoothness;         // (aZ)
    private static Value<Float> noiseSmoothnessAmplitude;// (ba)
    private static Value<Float> nlGlobalMix;             // (bb)
    private static Value<Float> nlGlobalErrorRef;        // (bc)
    private static Value<Float> nlGlobalErrorCurve;      // (bd)
    private static Value<Float> nlGlobalSpeedRef;        // (be)
    private static Value<Float> nlGlobalSpeedCurve;      // (bf)
    private static Value<Float> nl1Amp;  private static Value<Float> nl1Freq;  private static Value<Float> nl1Seed;   // (bg bh bi)
    private static Value<Float> nl2Amp;  private static Value<Float> nl2Freq;  private static Value<Float> nl2Seed;   // (bj bk bl)
    private static Value<Float> nl3Amp;  private static Value<Float> nl3Freq;  private static Value<Float> nl3Seed;   // (bm bn bp, octaves=bo)
    private static Value<Float> nl4Amp;  private static Value<Float> nl4Freq;  private static Value<Float> nl4Seed;   // (bq br bs)
    private static Value<Float> nl5Amp;  private static Value<Float> nl5Step;  private static Value<Float> nl5Seed;   // (bt bu bw, stiff=bv)
    private static Value<Float> nl6Amp;  private static Value<Float> nl6Freq;  private static Value<Float> nl6Seed;   // (bx by bz)
    private static Value<Float> nl7Amp;  private static Value<Float> nl7Freq;  private static Value<Float> nl7Seed;   // (bA bB bC)
    private static Value<Float> nl8Amp;  private static Value<Float> nl8Alpha; private static Value<Float> nl8Seed;   // (bD bE bF)
    private static Value<Float> nl9Amp;  private static Value<Float> nl9Freq;  private static Value<Float> nl9Seed;   // (bG bH bJ, jitter=bI)
    private static Value<Float> nl10Amp; private static Value<Float> nl10Freq; private static Value<Float> nl10Seed;  // (bK bL bM)

    // -- 出视野/落地/回弹 --                                           (混淆代号)
    private static Value<Float> outOfViewFov;                  // (bT)
    private static Value<Float> outOfViewPhase1Min;            // (bU)
    private static Value<Float> outOfViewPhase1Max;            // (bV)
    private static Value<Float> outOfViewCooldownMin;          // (bW)
    private static Value<Float> outOfViewCooldownMax;          // (bX)
    private static Value<Float> outOfViewNearbyOffsetRange;    // (bY)
    private static Value<Float> outOfViewFineOffset;           // (bZ)
    private static Value<Float> outOfViewSpeedMult;            // (ca)
    private static Value<Float> landingAmplitude;              // (bO)
    private static Value<Float> landingChance;                 // (bP)
    private static Value<Float> landingDuration;               // (bQ)
    private static Value<Float> landingActivateDelta;          // (bR)
    private static Value<Float> landingScaleWithRange;         // (bS)

    // -- 额外引用到的配置（按真实名） --
    private static Value<Float> basicSimulationSpeedMult;      // (cb)
    private static Value<Float> basicSimulationNoiseSmoothnessAmplitude;
    private static Value<Integer> nl3Octaves;                  // (bo)
    private static Value<Float> nl9Jitter;                     // (bI)
    private static Value<Float> nl10SpeedCouple;               // (bN)
    private static Value<Float> linearAimingSpeed;             // (c -> FloatValue)
    private static Value<Boolean> aimPointMode;                // (p) 是否选包围盒角点
    private static Value<Double> playerPosYOffset;             // (e)
    private static Value<Boolean> serverRotationEnabled;       // (t) 用服务器视角旋转
    private static Value<Float> airRangeMin;                   // (c) 空气减伤范围下限
    private static Value<Float> airRangeMax;                   // (b)
    private static Value<Float> minAttackRange;                // (a)
    private static Value<Float> maxAttackRange;                // (retrace)
    private static Value<Float> minThroughWallsRange;          // (s)
    private static Value<Float> maxThroughWallsRange;          // (r)
    private static Value<Integer> firstTargetRealFovThreshold; // (cq)
    private static Value<Integer> firstTargetFakeFovThreshold; // (cr)
    private static Value<Float> firstTargetMinOffset;          // (co)
    private static Value<Float> firstTargetMaxOffset;          // (cp)
    private static Value<Integer> firstTargetWaitDelay;        // (cs)
    private static Value<Float> microJitterCfg;                // (ad)

    // -- 状态噪声相位数组 (原 proguard/nlPhase 与 jar/nlState) --
    private static float[] nlPhase = new float[10];   // (proguard 字段): 各层相位累计
    private static float[] nlState = new float[10];   // (jar 字段)      : 各层状态/滤波

    private static long   lastYawTime;   // (a) 上一帧时间

    /* ========================================================================
     * 3) 噪声/随机 基础原语（还原 proguard/jar/ant 的重载调用）
     * ======================================================================== */
    /** random(a,b) —— 原混淆名 proguard(float,float) */
    private static float random(float min, float max) {
        return min + (float) Math.random() * (max - min);
    }

    /** fract(x) —— 原混淆名 proguard(float) */
    private static float fract(float x) {
        return x - (float) Math.floor(x);
    }

    /** 哈希噪声: fract(sin(x*12.9898+78.233)*43758.5453) —— 原 jar(float) */
    private static float hashNoise(float x) {
        float s = (float) Math.sin(x * 12.9898f + 78.233f) * 43758.547f;
        return fract(s);
    }

    /** 值噪声（在两个相邻哈希间平滑插值）—— 原 ant(float) */
    private static float valueNoise(float x) {
        int xi = (int) Math.floor(x);
        float xf = x - xi;
        float h0 = hashNoise(xi);
        float h1 = hashNoise(xi + 1);
        float u = xf * xf * (3.0f - 2.0f * xf);   // smoothstep
        return h0 * (1 - u) + h1 * u;
    }

    /** fBm 分形值噪声 —— 原 proguard(float t,int octaves,float persistence) */
    private static float fractalNoise(float t, int octaves, float persistence) {
        float total = 0f, freq = 1f, amp = 1f, maxV = 0f;
        for (int i = 0; i < octaves; i++) {
            total += valueNoise(t * freq) * amp;
            maxV += amp;
            amp *= persistence;
            freq *= 2.0f;
        }
        return total / maxV;
    }

    /** 基于种子的值噪声 —— 原 retrace(float) */
    private static float seededNoise(float t) {
        float t0 = (float) Math.floor(t), t1 = t0 + 1.0f;
        float v0 = (float) (new java.util.Random((long) t0).nextDouble() * 2 - 1);
        float v1 = (float) (new java.util.Random((long) t1).nextDouble() * 2 - 1);
        float fade = t - t0;
        float s = fade * fade * (3 - 2 * fade);
        return v0 + (v1 - v0) * s;
    }

    /** 1D 柏林噪声 —— 原 a(float)，基于 fade/grad1D/lerp */
    private static float noise1D(float x) {
        int xi = (int) Math.floor(x) & 0xFF;
        float xf = x - (float) Math.floor(x);
        float u = fade(xf);
        float a = grad1D(xi, xf);
        float b = grad1D(xi + 1, xf - 1.0f);
        return lerp(a, b, u);
    }

    /** 平滑插值用的缓动 —— fade(t) */
    public static float fade(float t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    /** 1D 梯度 —— grad1D(hash,x) */
    public static float grad1D(int hash, float x) {
        int h = hash & 0xF;
        int grad = 1 + (h & 7);
        if ((h & 8) != 0) grad = -grad;
        return grad * x;
    }

    /** 线性插值 —— lerp(a,b,f) */
    public static float lerp(float a, float b, float f) {
        return a + f * (b - a);
    }

    /* ========================================================================
     * 4) 转头核心 —— 偏航：PID + 贝塞尔 + 多层噪声   （原 jar(cur,tgt,target)）
     * ========================================================================
     *  输入 currentYaw/targetYaw，返回应设置的 newYaw（模拟真人转向）。
     *  结构：
     *     a) out-of-view(目标在视野外)阶段：快速切向逼近
     *     b) PID 三环 (P/I/D) 计算期望速度 desiredSpeed
     *     c) 人为因素：反应延迟、呼吸、微抖动、DPI、游泳
     *     d) 延迟(lag) 与 提前(ahead) 回弹
     *     e) 贝塞尔曲线平滑 + 多层噪声叠加(nl1..nl10) + 落地/连杆抑制
     */
    private static float smoothYaw(float currentYaw, float targetYaw, Entity target) {
        // 仅当 rotationMode == "BasicSimulation" 时才走这套仿真
        // if (!rotationMode.get().equals("BasicSimulation")) return currentYaw;
        float r = 0.5f;

        float rawD = wrapAngle(targetYaw - currentYaw);
        float fov = Math.abs(rawD);

        // ---------- a) 目标“首次”出现在默认瞄准视野时的快速逼近 ----------
        //   (getFov: 目标首次出现标记; firstTargetActive: 已正式咬住目标)
        float q = targetYaw;
        if (getFov || fov >= firstTargetRealFovThreshold.get().intValue()) {
            getFov = true;
            if (firstTargetActive) {   // 尚未进入所谓“假视野”，先做一个随机偏移的大致瞄准
                q = targetYaw + random(firstTargetMinOffset.get(), firstTargetMaxOffset.get());
            } else {
                // 进入“假视野”：若当前角差已足够小且等待时间到，则正式咬住目标
                float newFov = Math.abs(wrapAngle(q - currentYaw));
                if (newFov <= firstTargetFakeFovThreshold.get().intValue()
                        && firstTargetTimer.hasTimePassed(firstTargetWaitDelay.get().intValue())) {
                    q = targetYaw;
                    firstTargetActive = true;
                    getFov = false;
                }
            }
        } else {
            q = targetYaw;
            firstTargetActive = true;
            getFov = false;
        }

        long now = System.currentTimeMillis();
        float dt = lastYawTime == 0L ? 0.05f
                : clamp((now - lastYawTime) / 1000.0f, 0.001f, 0.1f);

        float deltaWrapped = wrapAngle(q - currentYaw);
        float adjustedTargetYaw = currentYaw + deltaWrapped;

        float fovHalf = outOfViewFov.get() * r;
        boolean inView = Math.abs(deltaWrapped) <= fovHalf;

        float outOfViewFalloff = 0f;   // (bY/bZ)
        boolean outOfViewFast = false;
        if (!inView) {
            if (now > basicSimulationOutOfViewActiveUntil && now > basicSimulationOutOfViewCooldownUntil) {
                basicSimulationOutOfViewPhase = 1;
                outOfViewFalloff = random(-outOfViewNearbyOffsetRange.get(), outOfViewNearbyOffsetRange.get());
                int dur1 = randInt(outOfViewPhase1Min.get(), outOfViewPhase1Max.get());
                basicSimulationOutOfViewActiveUntil = now + dur1;
                basicSimulationOutOfViewCooldownUntil = now + dur1 + randInt(outOfViewCooldownMin.get(), outOfViewCooldownMax.get());
            }
            if (now <= basicSimulationOutOfViewActiveUntil && basicSimulationOutOfViewPhase == 1) {
                float approachFraction = 0.9f + (float) Math.random() * 0.6f;
                adjustedTargetYaw = currentYaw + sign(deltaWrapped) * (fovHalf * approachFraction) + outOfViewFalloff;
                outOfViewFast = true;
            } else {
                basicSimulationOutOfViewPhase = 0;
                adjustedTargetYaw = q + random(-outOfViewFineOffset.get(), outOfViewFineOffset.get());
            }
        }

        // ---------- 落地激活/微调(local) ----------
        if (now > landingOffsetUntil /*retrace*/) {
            if (Math.abs(adjustedTargetYaw - currentYaw) <= landingActivateDelta.get()
                    && Math.random() < landingChance.get()) {
                float err = Math.abs(adjustedTargetYaw - currentYaw);
                float fineThr = fineTuningThreshold.get();
                float errScale = fineThr > 0 ? 1 - clamp(err / fineThr, 0, 1) : 1;
                float globalScale = landingScaleWithRange.get();
                float amp = landingAmplitude.get() * (0.35f + 0.65f * errScale) * globalScale;
                localZ = random(-amp, amp);
                landingOffsetUntil = now + landingDuration.get().longValue();
            } else {
                localZ = 0f;
            }
        }
        if (now <= landingOffsetUntil) adjustedTargetYaw += localZ;

        // ---------- PID ----------
        float delta = adjustedTargetYaw - currentYaw;
        // 反应延迟
        if (isNaN(lastYaw) || Math.abs(adjustedTargetYaw - lastYaw) > 2.5f) {
            humanReactionUntil = now + (60 + randInt(0, 160));
            humanCurvatureSeed = ((float) Math.random() - r) * 0.8f;
        }
        lastYaw = adjustedTargetYaw;
        boolean inReaction = now < humanReactionUntil;
        float reactionSuppression = inReaction ? 0.12f + (float) Math.random() * 0.25f : 1.0f;

        float baseMaxSpeed = speedBase.get() * speedMultBase.get()
                * (Math.abs(delta) > 90f ? speedMultLarge.get()
                 : Math.abs(delta) > 30f ? speedMultMedium.get() : speedMultSmall.get());
        float maxSpeed = baseMaxSpeed * (outOfViewFast ? outOfViewSpeedMult.get() : 1f);
        float baseAccel = accelBase.get()
                * (Math.abs(delta) > 90f ? accelMultLarge.get()
                 : Math.abs(delta) > 30f ? accelMultMedium.get() : accelMultSmall.get());
        float acceleration = baseAccel * (outOfViewFast ? outOfViewSpeedMult.get() : 1f);

        // 误差死区 / 饱和（非线性 P 项输入）
        float error = Math.abs(delta) < pidErrorDeadzone.get() ? 0f
                : Math.abs(delta) > pidErrorSaturation.get()
                      ? pidErrorSaturation.get() * sign(delta) : delta;

        float pNonlinear = 1f - (float) Math.exp(-Math.abs(error) / (30f * pidPResponseCurve.get()));
        float adaptiveP = pidPBase.get() * (pidPDynamicGain.get() + pidPNonlinearity.get() * pNonlinear);

        float decay = Math.abs(error) > pidIAdaptive.get()
                ? 1f - pidINonlinearDecay.get() * (Math.abs(error) - pidIAdaptive.get()) / pidIAdaptive.get()
                : 1f;
        integral = integral * decay
                + error * dt * r * (1f - pidIAntiWindup.get() * (float) Math.tanh(Math.abs(integral) / pidIDynamicClamp.get()));
        integral = clamp(integral, -pidIDynamicClamp.get(), pidIDynamicClamp.get());

        // (原处有一段空转代码，等价于 0，已省略)

        float derivative = dt > 0 ? (error - lastError) / dt : 0f;
        float filteredDerivative = pidDNoiseFilter.get() * derivative
                + (1f - pidDNoiseFilter.get()) * lastDerivative;
        float dBoost = 1f + pidDNonlinearBoost.get() * (float) Math.tanh(Math.abs(error) * pidDErrorSensitivity.get());

        float p = adaptiveP * (1f - pidPAttenuation.get() * (float) Math.exp(-Math.abs(delta) / 30.0));
        float i = pidIBase.get() * (1f - pidIAttenuation.get() * (float) Math.exp(-Math.abs(delta) / 15.0));
        float d = pidDBase.get() * dBoost * (1f - pidDAttenuation.get() * (float) Math.exp(-Math.abs(delta) / 20.0));
        float dynamicCoupling = 1f + pidDynamicCoupling.get() * (1f - (float) Math.exp(-(Math.abs(delta) / 180.0)));

        float output = (p * error + i * integral + d * filteredDerivative) * dynamicCoupling;
        float desiredSpeed = clamp(output, -maxSpeed, maxSpeed);
        float speedNorm = clamp(Math.abs(desiredSpeed) / Math.max(maxSpeed, 1f), 0f, 1f);
        // 原 proguardgui(speedNorm)：平滑 tanh 缓动曲线，再加一点随机增益
        float easedGain = (float) (Math.tanh(speedNorm * 2f - 1f) * 0.5 + 0.5) * (0.85f + 0.3f * (float) Math.random());
        desiredSpeed *= easedGain;

        // ---------- 呼吸（周期性波动） ----------
        float breath = (float) Math.sin((now % breathingPeriod.get().longValue()) / (double) breathingPeriod.get().intValue() * Math.PI * 2);
        float breathingFactor = 1f + breath * breathingAmplitude.get();
        float closeFactor = 1f - clamp(Math.abs(delta) / fineTuningThreshold.get(), 0f, 1f);
        float microSpeedJitter = ((float) Math.random() - r) * microJitterCfg.get() * Math.abs((desiredSpeed *= reactionSuppression) / maxSpeed);
        float adaptiveMicro = microSpeedJitter * (1f + closeFactor * 2f);

        // ---------- 反应停滞 / 增益 ----------
        float reactionLag = 1f;
        if (Math.random() < reactionLagChance.get()) reactionLag = reactionLagFactor.get();
        else if (Math.random() < reactionBoostChance.get()) reactionLag = reactionBoostFactor.get();
        // 节奏子循环
        if (now - lastRhythmChangeTime > currentRhythmDuration) {
            lastRhythmChangeTime = now;
            currentRhythmDuration = randInt(rhythmMinDur.get(), rhythmMaxDur.get());
            currentRhythmFactor = 1f - rhythmAmplitude.get() + (float) Math.random() * (rhythmAmplitude.get() * 2);
        }
        desiredSpeed *= breathingFactor * reactionLag * currentRhythmFactor;
        desiredSpeed += adaptiveMicro;

        // ---------- DPI 灵敏度 ----------
        float angleFactor = clamp(Math.abs(delta) / 180f, 0f, 1f);
        float dpiFactor = 1f + (float) Math.pow(angleFactor, 1.5) * dpiSensitivity.get() * dpiBase.get() / 800f;
        float dpiJitter = ((float) Math.random() - r) * dpiJitterAmp.get() * dpiFactor;
        desiredSpeed = desiredSpeed * dpiFactor + dpiJitter;

        // ---------- 延迟(lag)回弹 ----------
        if (now > lagActiveUntil && now > lagCooldownUntil && Math.abs(delta) >= lagMinDelta.get()
                && Math.random() < lagChance.get()) {
            int dur = randInt(lagDurMin.get(), lagDurMax.get());
            lagActiveUntil = now + dur;
            lagCooldownUntil = now + dur + randInt(lagCdMin.get(), lagCdMax.get());
            lagSlowFactor = random(lagSlowMin.get(), lagSlowMax.get());
        }
        if (now <= lagActiveUntil) desiredSpeed *= lagSlowFactor;

        // ---------- 提前(ahead)回弹 ----------
        if (now > aheadActiveUntil && now > aheadCooldownUntil && Math.abs(delta) >= aheadMinDelta.get()
                && Math.random() < aheadChance.get()) {
            int dur = randInt(aheadDurMin.get(), aheadDurMax.get());
            aheadActiveUntil = now + dur;
            aheadCooldownUntil = now + dur + randInt(aheadCdMin.get(), aheadCdMax.get());
            aheadOffset = random(aheadAmtMin.get(), aheadAmtMax.get());
        }

        // ---------- 速度累积 + 阻尼 + 贝塞尔 ----------
        float maxAcceleration = acceleration * dt;
        float rawChange = desiredSpeed - yawVelocity;
        float softened = clamp(rawChange, -maxAcceleration, maxAcceleration);
        float responsiveness = clamp(0.3f + 0.7f * (1f - Math.abs(delta) / 180f), 0.2f, 1f);
        yawVelocity += softened * responsiveness;
        float easing = (float) (Math.sin((Math.abs(delta) / 180f) * Math.PI) * 0.5 + 0.5);
        yawVelocity *= easing;

        float damp = 1f - dampingFactor.get() * (float) Math.exp(-Math.abs(error) / 45.0);
        float newYaw = currentYaw + (yawVelocity *= damp) * dt + delta * basicSimulationSpeedMult.get();

        if (now <= aheadActiveUntil) newYaw += aheadOffset;

        // 末端轻微“按到底”触感
        if (Math.random() < 0.008 && Math.abs(delta) < 20f && now > microCooldownUntil) {
            yawVelocity *= 1.25f;
            microCooldownUntil = now + 250 + randInt(0, 400);
        }

        // 贝塞尔曲线插入（让人看起来是平滑圆弧而不是直线）
        float grip = clamp(Math.abs(delta) / 180f, 0f, 1f);
        float lateralMax = Math.max(5f * (1f - grip), 0.2f);
        float lateralSign = delta > 0 ? 1f : -1f;
        float lateralBias = lateralSign * lateralMax * humanCurvatureSeed
                * (1f - clamp(Math.abs(delta) / 90f, 0f, 1f));
        float controlPoint = currentYaw + delta * 0.45f + lateralBias;
        float t = clamp(Math.abs(yawVelocity) / Math.max(maxSpeed, 1f), 0f, 1f) * 0.35f;
        float bezierYaw = (1 - t) * (1 - t) * currentYaw + 2 * (1 - t) * t * controlPoint + t * t * adjustedTargetYaw;
        newYaw += (bezierYaw - currentYaw) * (0.25f + 0.6f * (1f - grip));

        // 高频/低频噪声
        float coarse = clamp(Math.abs(delta) / 60f, 0f, 1f);
        float humanNoiseScale = 1f * (1f - 0.6f * coarse) + closeFactor * 0.6f;
        double highFreq = (Math.random() - 0.5) * jitterHigh.get() * (Math.abs(yawVelocity) / maxSpeed) * (0.6 + 0.4 * humanNoiseScale);
        double lowFreq  = Math.sin(now / 250.0) * jitterLow.get() * (r + r * humanNoiseScale);
        float newDelta = adjustedTargetYaw - (newYaw += (float) (highFreq + lowFreq));

        // 接近目标时收尾
        if (Math.abs(newDelta) < Math.abs(error) * overshootThreshold.get())
            yawVelocity *= Math.max(overshootDecay.get(), Math.abs(newDelta) / 15f);
        if (Math.abs(newDelta) < fineTuningThreshold.get()) {
            newYaw += ((float) Math.random() - r) * microJitter.get() * (1f + closeFactor * 2f);
            yawVelocity *= 0.16f;
        }

        // 疲劳衰减
        yawVelocity = fatigueDecay.get() * yawVelocity
                + (1f - fatigueDecay.get()) * (1f - fatigueRecover.get() * (float) Math.tanh(dt * r));
        if (Math.random() < 0.01 && Math.abs(delta) > 5f) yawVelocity *= r;

        // 最终瞄准“目标点随机吸附”（调整幅度/时机）
        if ((delta >= adjustMinDelta.get() && delta <= adjustMaxDelta.get())
                && adjustTimer.hasTimePassed(adjustTime.get().intValue())) {
            newYaw = adjustedTargetYaw + (float) random(adjustMinRandom.get(), adjustMaxRandom.get());
            adjustTimer.reset();
        }

        // ---------- 游泳摆动 ----------
        float swimAmp = swimMaxAmp.get() * (1f - clamp(Math.abs(delta) / swimDeltaLimit.get(), 0f, 1f));
        float swimOffset = swimAmp * (float) Math.sin((now % 100000L) / 1000.0 * swimSpeed.get() * Math.PI * 2);
        newYaw += swimOffset;

        // ---------- 基础噪声（一阶：值噪声 * 幅度，随接近而增强/减弱） ----------
        float baseNoise = fractalNoise(noiseTime += dt * noiseFrequency.get(),
                                        noiseOctaves.get().intValue(), noisePersistence.get());
        newYaw += baseNoise * noiseAmplitude.get() * (0.6f + 0.4f * closeFactor);
        newYaw += valueNoise(dt * noiseSmoothness.get()) * noiseSmoothnessAmplitude.get();

        // ---------- 多层噪声叠加 nl1..nl10（用 nlPhase/nlState 数组累计） ----------
        // 噪声包络：随误差与速度按指数衰减（原 errFactor/spdFactor * nlGlobalMix）
        float errFactor  = (float) Math.pow(Math.exp(-(Math.abs(delta) / nlGlobalErrorRef.get())), nlGlobalErrorCurve.get());
        float spdFactor  = (float) Math.pow(Math.exp(-(Math.abs(yawVelocity) / nlGlobalSpeedRef.get())), nlGlobalSpeedCurve.get());
        float env = clamp(nlGlobalMix.get() * errFactor * spdFactor, 0f, 2f);
        float seed = 0f;   // 每层把输出注入下一层种子
        final float noiseEnvSum = 2.0f;    // 原 gdccALP5（part1+..+part5+2）恒等于 2

        // nl1. 正弦+余弦混合层
        nlPhase[0] += dt * (nl1Freq.get() * (1 + nl1Seed.get() * Math.abs(seed)));
        float nl1 = env * nl1Amp.get() * (0.6f * (float) Math.sin(nlPhase[0] + seed)
                                        + 0.4f * (float) Math.cos(1.7f * nlPhase[0] - seed));
        newYaw += nl1; seed = nl1;

        // nl2. 1D 柏林噪声层
        nlPhase[1] += dt * (nl2Freq.get() * (1 + nl2Seed.get() * Math.abs(seed)));
        float nl2 = env * nl2Amp.get() * noise1D(nlPhase[1] + seed * 0.77f);
        newYaw += nl2; seed = nl2;

        // nl3. 分形值噪声层（fBm，octaves=bo）
        nlPhase[2] += dt * (nl3Freq.get() * (1 + nl3Seed.get() * Math.abs(seed)));
        float nl3 = env * nl3Amp.get() * fractalNoise(nlPhase[2] + seed * 0.53f, nl3Octaves, r);
        newYaw += nl3; seed = nl3;

        // nl4. 值噪声带符号层
        nlPhase[3] += dt * (nl4Freq.get() * (1 + nl4Seed.get() * Math.abs(seed)));
        float nl4v = nlPhase[3] * 1.3f + seed * 1.1f;
        float nl4 = env * nl4Amp.get() * (valueNoise(nl4v) * 2f - 1f);
        newYaw += nl4; seed = nl4;

        // nl5. 随机游走层
        nlState[4] += dt * (1 + nl5Step.get() * Math.abs(seed));
        float walk = ((float) Math.random() - r) * 2f * nl5Step.get();
        nlState[4] = clamp(nlState[4] + walk - nl5Stiffness.get() * nlState[4], -1.5f, 1.5f);
        float nl5 = env * nl5Amp.get() * nlState[4];
        newYaw += nl5; seed = nl5;

        // nl6. 细胞/Voronoi 式抖动层
        nlPhase[5] += dt * (nl6Freq.get() * (1 + nl6Seed.get() * Math.abs(seed)));
        int cell = (int) Math.floor(nlPhase[5] + seed * 2.3f);
        float center = cell + r + (hashNoise(cell) - r) * 0.3f;
        float dist = Math.abs(nlPhase[5] + seed * 2.3f - center);
        float nl6 = env * nl6Amp.get() * clamp(r - dist, -r, r) * 2f;
        newYaw += nl6; seed = nl6;

        // nl7. 噪声绝对值调制层
        nlPhase[6] += dt * (nl7Freq.get() * (1 + nl7Seed.get() * Math.abs(seed)));
        float base7 = noise1D(nlPhase[6] + seed * 0.91f);
        float nl7 = env * nl7Amp.get() * (Math.abs(base7) * noiseEnvSum - 1f);
        newYaw += nl7; seed = nl7;

        // nl8. 低通滤波白噪声层
        nlPhase[7] += dt * (1 + nl8Alpha.get() * Math.abs(seed));
        float white = ((float) Math.random() - r) * noiseEnvSum;
        nlState[7] = (1f - nl8Alpha.get()) * nlState[7] + nl8Alpha.get() * white;
        float nl8 = env * nl8Amp.get() * nlState[7];
        newYaw += nl8; seed = nl8;

        // nl9. 锯齿波层
        nlPhase[8] += dt * (nl9Freq.get() * (1 + nl9Seed.get() * Math.abs(seed))
                * (1 + ((float) Math.random() - r) * nl9Jitter.get()));
        float twoPi = (float) Math.PI * 2;
        float phaseNorm = nlPhase[8] / twoPi - (float) Math.floor(nlPhase[8] / twoPi + r);
        float saw = 2f * phaseNorm;
        float nl9 = env * nl9Amp.get() * saw;
        newYaw += nl9; seed = nl9;

        // nl10. 平滑噪声（smoothstep 调制）
        nlPhase[9] += dt * (nl10Freq.get() * (1 + nl10Seed.get() * Math.abs(seed)));
        float x10raw = r + r * (float) Math.sin(nlPhase[9] + seed * 0.37f);
        float x10 = x10raw * x10raw * (3f - noiseEnvSum * x10raw);
        float nl10 = env * nl10Amp.get() * (x10 * 2f - 1f);
        newYaw += nl10;
        // 第十层反过来影响速度
        float speedGain = clamp(1f + nl10 * nl10SpeedCouple.get(), 0.8f, 1.2f);
        desiredSpeed *= speedGain;

        // ---------- 对玩家的“断开/猛甩”(disconnect) 与 猛吸(snap) ----------
        if (target instanceof EntityPlayer) {
            final double disconProb = 0.06;
            final int disconMin = 80, disconMax = 450;
            final float disconMinOff = -22f, disconMaxOff = 22f;
            final double snapProb = 0.02;
            if (now > playerDisconCooldownUntil && Math.random() < disconProb) {
                int dur = randInt(disconMin, disconMax);
                playerDisconActiveUntil = now + dur;
                playerDisconCooldownUntil = now + dur + randInt(150, 1200);
                playerDisconTargetOffset = random(disconMinOff, disconMaxOff);
            }
            if (now <= playerDisconActiveUntil) {
                newYaw += playerDisconTargetOffset;
                yawVelocity *= 0.08f;
                newYaw += ((float) Math.random() - r) * 6f;
            }
            if (now > playerDisconCooldownUntil && Math.random() < snapProb) {
                newYaw = adjustedTargetYaw + random(-6f, 6f);
                yawVelocity *= 0.2f;                    // (c = d(...)) 近似置零
                playerDisconCooldownUntil = now + 300 + randInt(0, 900);
            }
            if (((EntityPlayer) target).hurtTime > 0 && Math.abs(delta) < 25f) {
                newYaw += random(-4f, 4f);
                yawVelocity *= 0.3f;
            }
        }

        lastError = error;
        lastYawTime = now;
        return newYaw;
    }

    /* ========================================================================
     * 5) 转头核心 —— 俯仰（原 proguard(cur,tgt,targetPlayer)）
     * ======================================================================== */
    private static float smoothPitch(float current, float target, Entity targetPlayer) {
        String mode = pitchMode.get();                      // retrace(Value<String>)
        if ("LinearAiming".equals(mode)) {
            float delta = target - current;
            targetPitch += delta * linearAimingSpeed.get();       // (c<->FloatValue)
        } else if ("DataSwim".equals(mode)) {
            if (jitterIndex < dataPool.size()) {            // (x<proguard.size())
                int n = jitterIndex;
                jitterIndex = n + 1;
                targetPitch += dataPool.get(jitterIndex);
            } else {
                jitterIndex = 0;
                targetPitch = 0.0f;
            }
        }
        return current;
    }

    /* ========================================================================
     * 6) 瞄准点 / 可见性（原 proguard(EntityPlayer,Entity)）
     *    从目标包围盒的 9 个候选点(含中心+8角)中，
     *    找第一个“从玩家眼睛出发射线不命中方块”的点作为可命中瞄准点。
     * ======================================================================== */
    private static Pair<Boolean, Vec3> aimPointVisible(EntityPlayer player, Entity target) {
        Vec3 playerEye = new Vec3(player.posX, player.posY + player.eyeHeight, player.posZ);
        WorldClient world = mc.theWorld;

        double minX = target.boundingBox.minX, maxX = target.boundingBox.maxX;
        double minY = target.boundingBox.minY, maxY = target.boundingBox.maxY;
        double minZ = target.boundingBox.minZ, maxZ = target.boundingBox.maxZ;

        List<Vec3> candidates;
        if (aimPointMode.get()) {   // (p) 选角点模式
            candidates = listOf(
                new Vec3(target.posX, target.posY, target.posZ), // 中心
                new Vec3(maxX, minY, maxZ), new Vec3(minX, minY, minZ),
                new Vec3(maxX, minY, minZ), new Vec3(minX, minY, maxZ),
                new Vec3(maxX, maxY, maxZ), new Vec3(minX, maxY, minZ),
                new Vec3(maxX, maxY, minZ), new Vec3(minX, maxY, maxZ));
        } else {
            candidates = listOf(new Vec3(target.posX, target.posY, target.posZ));
        }

        for (Vec3 corner : candidates) {
            MovingObjectPosition hit = world.rayTrace(playerEye, corner, false, false, false);
            if (hit == null || hit.typeOfHit != MovingObjectType.MISS) continue;
            return new Pair<>(true, corner);   // 可见且可命中
        }
        return new Pair<>(false, Vec3.zeroVec);
    }

    /* ========================================================================
     * 7) onUpdate 骨架 —— 目标选择
     * ======================================================================== */
    public void onUpdate(UpdateEvent event) {
        EntityPlayer player = mc.thePlayer;
        if (player == null) return;

        // 范围在冲刺/落地时的随机缩减
        double sprintReduce = player.isSprinting()
            ? randomDouble(minAttackRange/*(a)*/, maxAttackRange/*(retrace)*/) : 0.0;
        double airReduce = !player.onGround
            ? randomDouble(airRangeMin/*(c)*/, airRangeMax/*(b)*/) : 0.0;
        currentRange = randomDouble(minAttackRange/*(proguardgui)*/, maxAttackRange/*(ant)*/)
            - sprintReduce - airReduce;
        maxThroughWallsRange = randomDouble(minThroughWallsRange/*(s)*/, maxThroughWallsRange/*(r)*/);

        // 选目标：实体 -> 过滤(友/敌/中立/隐身/死亡/可见) -> 取“能打中”最近者
        Entity target = world.playerEntities.stream()
            .filter(e -> e != player)
            .filter(e -> EntityLib.INSTANCE.isSelected(e))
            .filter(e -> e instanceof EntityLivingBase)
            .filter(e -> aimPointVisible(player, (Entity) e).getFirst())   // 可见
            .filter(e -> hurtTimeRange(e))                                 // d..e 范围
            .filter(e -> distToBox(player, e) <= currentRange)
            .filter(e -> targetFilters(e))                                 // g/f/b/c/d/e 开关
            .min(byDistancePlayer)
            .orElse(null);

        if (target == null) return;

        // 处理攻击/阻挡(block)/帧，然后：
        // 计算瞄准点 targetVec，用 RotationLib 得到 rotation，
        // 最后: currentYaw/currentPitch = smoothYaw / smoothPitch(...)
        Vec3 aim = aimPointVisible(player, target).getSecond();
        Vec3 targetVec = new Vec3(aim.xCoord , aim.yCoord, aim.zCoord);
        Vec3 playerVec = new Vec3(player.posX, player.posY + playerPosYOffset.get(), player.posZ);
        Rotation rot = RotationLib.Companion.getRotation(playerVec, targetVec);

        currentYaw = (serverRotationEnabled/*(t)*/)
            ? fixRotation(currentServerYaw, smoothYaw(currentServerYaw, rot.getYaw(), target))
            : rot.getYaw();
        currentPitch = smoothPitch(currentPitch, rot.getPitch(), target);
        // 位置被 applied 到视角
        RotationLib.targetRotation = new Rotation(currentYaw, currentPitch);
    }

    // 简化辅助（原库）
    private static float wrapAngle(float d) { while (d < -180) d += 360; while (d > 180) d -= 360; return d; }
    private static int randInt(int min, int max) { return min + (int) (Math.random() * (max - min + 1)); }
    private static double randomDouble(double min, double max) { return min + Math.random() * (max - min); }
    private static float clamp(float v, float lo, float hi) { return v < lo ? lo : v > hi ? hi : v; }
    private static float sign(float v) { return v > 0 ? 1f : -1f; }
}
