package dev.astra.module.impl.combat.killaura;

import dev.astra.Main;
import dev.astra.event.EventHandler;
import dev.astra.event.events.player.PreUpdateEvent;
import dev.astra.event.events.render.Render3DEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.module.impl.combat.killaura.attack.Attack;
import dev.astra.module.impl.combat.killaura.block.AutoBlock;
import dev.astra.module.impl.combat.killaura.render.AttackRangeRenderer;
import dev.astra.module.impl.combat.killaura.render.DotRenderer;
import dev.astra.module.impl.combat.killaura.rotation.Rotation;
import dev.astra.module.impl.combat.killaura.target.Target;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.ColorValue;
import dev.astra.value.impl.GroupValue;
import dev.astra.value.impl.ModeValue;
import dev.astra.value.impl.NumberValue;
import dev.astra.value.impl.RangeSelectionNumberValue;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import java.util.List;

public class KillAura extends Module {
    private static final double AIM_RANGE_HYSTERESIS = 0.5;

    public final ModeValue targetMode = new ModeValue("TargetMode", "Single", "Multi", "Switch", "Single");
    public final NumberValue switchDelay = new NumberValue("SwitchDelay", 500, 0, 5000, 50);
    public final ModeValue priority = new ModeValue("Priority", "Distance", "Distance", "Health", "Angle", "Armor");
    public final NumberValue range = new NumberValue("Range", 3.0, 1.0, 6.0, 0.1);
    public final NumberValue fov = new NumberValue("FOV", 360.0, 15.0, 360.0, 5.0);
    public final BooleanValue throughWalls = new BooleanValue("ThroughWalls", false);
    public final RangeSelectionNumberValue cps = new RangeSelectionNumberValue("CPS", 8.0, 12.0, 1.0, 20.0, 0.5);
    public final NumberValue swingRange = new NumberValue("SwingRange", 3.0, 1.0, 6.0, 0.1);
    public final BooleanValue rayCastCheck = new BooleanValue("RayCast", true);
    public final BooleanValue smartAttack = new BooleanValue("SmartAttack", false);
    public final ModeValue attackMode = new ModeValue("AttackMode", "Packet", "Packet", "Legit");
    public final ModeValue rotationMode = new ModeValue("RotationMode", "Silent", "None", "LockView", "Silent");
    public final RangeSelectionNumberValue yawTurnSpeed = new RangeSelectionNumberValue("YawTurnSpeed", 10.0, 15.0, 1.0, 360.0, 0.5);
    public final RangeSelectionNumberValue pitchTurnSpeed = new RangeSelectionNumberValue("PitchTurnSpeed", 10.0, 15.0, 1.0, 360.0, 0.5);
    public final NumberValue aimRange = new NumberValue("AimRange", 4.0, 1.0, 8.0, 0.1);
    public final ModeValue randomize = new ModeValue("Randomize", "Basic", "Basic", "Advanced");
    public final ModeValue autoBlockMode = new ModeValue("AutoBlock", "None", "None", "Fake");
    public final NumberValue blockRange = new NumberValue("BlockRange", 3.0, 1.0, 6.0, 0.1);
    public final ModeValue moveFix = new ModeValue("MoveFix", "Strict", "None", "Strict", "Silent");
    public final NumberValue hBoxSize = new NumberValue("HBoxSize", 0.0, 0.0, 1.0, 0.05);
    public final NumberValue vBoxSize = new NumberValue("VBoxSize", 0.0, 0.0, 1.0, 0.05);
    public final BooleanValue dot = new BooleanValue("Dot", false);
    public final ColorValue dotFillColor = new ColorValue("FillColor", 0xFFFFFFFF);
    public final ColorValue dotOutlineColor = new ColorValue("OutlineColor", 0xFFFFFFFF);
    public final NumberValue dotSize = new NumberValue("DotSize", 0.15, 0.05, 1.0, 0.01);
    public final NumberValue dotCornerRadius = new NumberValue("CornerRadius", 0.0, 0.0, 1.0, 0.01);
    public final ColorValue attackRangeOutlineColor = new ColorValue("AttackRangeOutline", 0xFFFF5555);
    public final ColorValue attackRangeFillColor = new ColorValue("AttackRangeFill", 0x40FF5555);
    public final BooleanValue bestHitVec = new BooleanValue("BestHitVec", true);
    public final BooleanValue smartAim = new BooleanValue("SmartAim", false);

    public final BooleanValue noiseEnabled = new BooleanValue("NoiseEnabled", true);
    public final ModeValue noiseMode = new ModeValue("NoiseMode", "Perlin", "None", "Gaussian", "Perlin", "Wave", "Hybrid", "Neural", "Brownian", "Spiral");
    public final NumberValue noiseBaseScale = new NumberValue("NoiseBaseScale", 0.5, 0.0, 5.0, 0.1);
    public final NumberValue noiseFatigueScale = new NumberValue("NoiseFatigueScale", 3.0, 0.0, 15.0, 0.1);
    public final NumberValue noisePitchRatio = new NumberValue("NoisePitchRatio", 0.7, 0.1, 2.0, 0.05);
    public final NumberValue noiseFrequency = new NumberValue("NoiseFrequency", 1.0, 0.01, 5.0, 0.01);
    public final NumberValue noiseFatigueRate = new NumberValue("NoiseFatigueRate", 0.005, 0.001, 0.05, 0.001);
    public final NumberValue noiseRecoveryRate = new NumberValue("NoiseRecoveryRate", 0.003, 0.001, 0.02, 0.001);
    public final NumberValue noiseYawBias = new NumberValue("NoiseYawBias", 0.0, -5.0, 5.0, 0.1);
    public final NumberValue noisePitchBias = new NumberValue("NoisePitchBias", 0.0, -5.0, 5.0, 0.1);
    public final BooleanValue noiseDistanceScale = new BooleanValue("NoiseDistanceScale", true);
    public final NumberValue noiseMicroJitter = new NumberValue("NoiseMicroJitter", 0.3, 0.0, 3.0, 0.1);
    public final BooleanValue noiseGcdQuantize = new BooleanValue("NoiseGcdQuantize", true);
    public final BooleanValue noiseClampToBox = new BooleanValue("NoiseClampToBox", true);

    public final NumberValue saInitTemp = new NumberValue("SAInitTemp", 3.0, 0.5, 20.0, 0.1);
    public final NumberValue saCoolingRate = new NumberValue("SACoolingRate", 0.95, 0.80, 0.99, 0.01);
    public final NumberValue saMinTemp = new NumberValue("SAMinTemp", 0.8, 0.0, 5.0, 0.1);
    public final NumberValue saIterations = new NumberValue("SAIterations", 20, 5, 50, 1);

    public final GroupValue targetParams = new GroupValue("Target",
            targetMode, switchDelay, priority, range, fov, throughWalls, aimRange);

    public final GroupValue clickParams = new GroupValue("Click",
            cps, swingRange, rayCastCheck, hBoxSize, vBoxSize, smartAttack, attackMode,
            autoBlockMode, blockRange);

    public final GroupValue rotationParams = new GroupValue("Rotation",
            rotationMode, yawTurnSpeed, pitchTurnSpeed, randomize, moveFix,
            bestHitVec, smartAim,
            noiseEnabled, noiseMode, noiseBaseScale, noiseFatigueScale, noisePitchRatio,
            noiseFrequency, noiseFatigueRate, noiseRecoveryRate, noiseYawBias, noisePitchBias,
            noiseDistanceScale, noiseMicroJitter, noiseGcdQuantize, noiseClampToBox,
            saInitTemp, saCoolingRate, saMinTemp, saIterations);

    public final GroupValue renderParams = new GroupValue("Render",
            dot, dotFillColor, dotOutlineColor, dotSize, dotCornerRadius,
            attackRangeOutlineColor, attackRangeFillColor);

    private final Target target = new Target(this);
    private final Rotation rotation = new Rotation(this);
    private final Attack attack = new Attack(this);
    private final AutoBlock autoBlock = new AutoBlock(this);
    private final DotRenderer dotRenderer = new DotRenderer(this);
    private final AttackRangeRenderer attackRangeRenderer = new AttackRangeRenderer(this);

    private List<EntityLivingBase> cachedTargets = null;
    private EntityLivingBase aimTarget = null;
    private long lastSelectionTick = 0L;

    public KillAura() {
        super("KillAura", Keyboard.KEY_NONE, Category.COMBAT);
        addValues(targetParams, clickParams, rotationParams, renderParams);
        dotCornerRadius.setBoundMax(dotSize);
        refreshVisibility();
    }

    @Override
    public void refreshVisibility() {
        dotFillColor.setVisible(dot.isEnabled());
        dotOutlineColor.setVisible(dot.isEnabled());
        dotSize.setVisible(dot.isEnabled());
        dotCornerRadius.setVisible(dot.isEnabled());
    }

    public static boolean shouldFakeBlock(ItemStack stack) {
        if (Main.moduleManager == null) return false;
        KillAura aura = Main.moduleManager.getModuleByClass(KillAura.class);
        return aura != null && aura.autoBlock.isFakeBlocking(stack);
    }

    public boolean isWithinAimRange(EntityLivingBase entity) {
        return mc.thePlayer.getDistanceToEntity(entity) <= aimRange.getValue() + AIM_RANGE_HYSTERESIS;
    }

    public EntityLivingBase getAimTarget() {
        return aimTarget;
    }

    public float getRotationYaw() {
        return rotation.getYaw();
    }

    public float getRotationPitch() {
        return rotation.getPitch();
    }

    public double getTargetScanRange() {
        double max = Math.max(range.getValue(), aimRange.getValue());
        return max + 2.0D;
    }

    @Override
    public String getStatus() {
        return String.valueOf(cachedTargets == null ? 0 : cachedTargets.size());
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        attackRangeRenderer.render(event.getPartialTicks());
        List<EntityLivingBase> targets = target.findTargets();
        if (targets.isEmpty()) {
            cachedTargets = null;
            aimTarget = null;
            autoBlock.update(false);
            rotation.clear();
            target.reset();
            return;
        }
        EntityLivingBase primary = targets.stream()
                .filter(this::isWithinAimRange)
                .findFirst()
                .orElse(null);
        if (primary == null) {
            cachedTargets = targets;
            aimTarget = null;
            autoBlock.update(false);
            rotation.clear();
            return;
        }
        cachedTargets = targets;
        if (targetMode.is("Multi")) {
            aimTarget = primary;
        } else {
            long now = System.currentTimeMillis();
            boolean selectionTick = now - lastSelectionTick >= 45L;
            boolean targetInvalid = aimTarget == null || !targets.contains(aimTarget);
            if (selectionTick || targetInvalid) {
                if (selectionTick) lastSelectionTick = now;
                aimTarget = target.selectTarget(targets);
            }
            if (aimTarget == null) {
                cachedTargets = null;
                return;
            }
        }
        rotation.update(aimTarget, event.getPartialTicks());
        dotRenderer.render(event.getPartialTicks());
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        List<EntityLivingBase> targets = cachedTargets;
        if (targets == null || targets.isEmpty() || aimTarget == null) return;
        boolean blockActive = targets.stream()
                .anyMatch(t -> mc.thePlayer.getDistanceToEntity(t) <= blockRange.getValue());
        autoBlock.update(blockActive);
        float yaw = rotation.getYaw();
        float pitch = rotation.getPitch();
        if (targetMode.is("Multi")) {
            for (EntityLivingBase target : targets) {
                attack.tryAttack(target, yaw, pitch);
            }
        } else {
            attack.tryAttack(aimTarget, yaw, pitch);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        cachedTargets = null;
        aimTarget = null;
        lastSelectionTick = 0L;
        attack.reset();
        autoBlock.reset();
        rotation.clear();
        target.reset();
    }
}