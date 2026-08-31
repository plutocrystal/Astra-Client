package loftily.module.impl.combat;

import com.google.common.base.Function;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import java.lang.invoke.LambdaMetafactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import loftily.Client;
import loftily.core.AbstractModule;
import loftily.event.impl.packet.PacketSendEvent;
import loftily.event.impl.player.AttackEvent;
import loftily.event.impl.player.motion.MotionEvent;
import loftily.event.impl.render.Render3DEvent;
import loftily.event.impl.world.PreUpdateEvent;
import loftily.handlers.impl.client.TargetsHandler;
import loftily.handlers.impl.player.RotationHandler;
import loftily.module.Module;
import loftily.module.ModuleCategory;
import loftily.module.ModuleInfo;
import loftily.utils.client.PacketUtils;
import loftily.utils.math.CalculateUtils;
import loftily.utils.math.RandomUtils;
import loftily.utils.math.Rotation;
import loftily.utils.player.PlayerUtils;
import loftily.utils.player.RayCastUtils;
import loftily.utils.player.RotationUtils;
import loftily.utils.timer.DelayTimer;
import loftily.value.impl.BooleanValue;
import loftily.value.impl.MultiBooleanValue;
import loftily.value.impl.NumberValue;
import loftily.value.impl.RangeSelectionNumberValue;
import loftily.value.impl.mode.Mode;
import loftily.value.impl.mode.ModeValue;
import loftily.value.impl.mode.StringMode;
import lombok.Generated;
import lombok.NonNull;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemShield;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketPlayerTryUseItem;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name="KillAura", key=19, category=ModuleCategory.COMBAT)
public class KillAura
extends Module {
    private final ModeValue attackTimeMode = new ModeValue("AttackTimeMode", "Tick", (AbstractModule)this, new Mode[]{new StringMode("Tick"), new StringMode("Pre"), new StringMode("Post")});
    private final ModeValue attackMode = new ModeValue("AttackMode", "Packet", (AbstractModule)this, new Mode[]{new StringMode("Packet"), new StringMode("Legit")});
    private final ModeValue swingMode = new ModeValue("SwingMode", "Vanilla", (AbstractModule)this, new Mode[]{new StringMode("Vanilla"), new StringMode("Packet"), new StringMode("NoPacket")});
    private final ModeValue cpsMode = new ModeValue("CPSMode", "Normal", (AbstractModule)this, new Mode[]{new StringMode("Normal"), new StringMode("Gaussian"), new StringMode("Jitter")});
    private final RangeSelectionNumberValue jitterPercent = new RangeSelectionNumberValue("JitterPercent", 0.1, 0.5, 0.0, 1.0, 0.01);
    private final NumberValue gaussianSigma = (NumberValue)new NumberValue("Gaussian-Sigma", 2.5, 0.0, 10.0, 0.01).setVisible(() -> this.cpsMode.is("Gaussian"));
    private final RangeSelectionNumberValue cpsValue = new RangeSelectionNumberValue("CPS", 8.0, 15.0, 0.0, 20.0, 0.1);
    private final BooleanValue fastOnFirstHit = new BooleanValue("FastOnFirstHit", Boolean.valueOf((boolean)false));
    private final ModeValue noDoubleHit = new ModeValue("NoDoubleHit", "Cancel", (AbstractModule)this, new Mode[]{new StringMode("Cancel"), new StringMode("NextHit"), new StringMode("None")});
    private final NumberValue hurtTime = new NumberValue("HurtTime", 0.0, 0.0, 20.0);
    private final BooleanValue rayCast = new BooleanValue("RayCast", Boolean.valueOf((boolean)false));
    private final BooleanValue rayCastThroughWalls = new BooleanValue("RayCastThroughWalls", Boolean.valueOf((boolean)false));
    private final BooleanValue rayCastOnlyTarget = new BooleanValue("RayCastOnlyTarget", Boolean.valueOf((boolean)false));
    private final ModeValue keepSprintMode = new ModeValue("KeepSprintMode", "None", (AbstractModule)this, new Mode[]{new StringMode("None"), new StringMode("Always"), new StringMode("WhenNotHurt")});
    private final BooleanValue noInventory = new BooleanValue("NoInventory", Boolean.valueOf((boolean)false));
    private final BooleanValue noGui = new BooleanValue("NoGui", Boolean.valueOf((boolean)false));
    private final NumberValue rotationRange;
    private final NumberValue swingRange;
    private final NumberValue attackRange;
    private final NumberValue throughWallAttackRange;
    private final ModeValue targetSortingMode = new ModeValue("TargetSortingMode", "Distance", (AbstractModule)this, new Mode[]{new StringMode("Distance"), new StringMode("HurtTime"), new StringMode("Health"), new StringMode("Angle"), new StringMode("Random")});
    private final ModeValue mode = new ModeValue("Mode", "Single", (AbstractModule)this, new Mode[]{new StringMode("Single"), new StringMode("Switch")});
    private final NumberValue switchDelay = (NumberValue)new NumberValue("SwitchDelay", 200.0, 0.0, 2000.0).setVisible(() -> this.mode.is("Switch"));
    private final ModeValue rotationMode = new ModeValue("RotationMode", "LockCenter", (AbstractModule)this, new Mode[]{new StringMode("LockCenter"), new StringMode("LockHead"), new StringMode("NearestCenter"), new StringMode("Lower"), new StringMode("Normal"), new StringMode("Advance"), new StringMode("Test"), new StringMode("Proportion"), new StringMode("None")});
    private final RangeSelectionNumberValue distancePro = (RangeSelectionNumberValue)new RangeSelectionNumberValue("DistanceProportion", 0.0, 1.0, 0.0, 1.0, 0.01).setVisible(() -> this.rotationMode.is("Proportion"));
    private final RangeSelectionNumberValue anglePro = (RangeSelectionNumberValue)new RangeSelectionNumberValue("AngleProportion", 0.0, 1.0, 0.0, 1.0, 0.01).setVisible(() -> this.rotationMode.is("Proportion"));
    private final RangeSelectionNumberValue yawTurnSpeed = new RangeSelectionNumberValue("YawTurnSpeed", 100.0, 150.0, 0.0, 360.0, 0.1);
    private final RangeSelectionNumberValue pitchTurnSpeed = new RangeSelectionNumberValue("PitchTurnSpeed", 100.0, 150.0, 0.0, 360.0, 0.1);
    private final RangeSelectionNumberValue keepTicks = new RangeSelectionNumberValue("KeepRotationTicks", 1.0, 2.0, 0.0, 20.0);
    private final RangeSelectionNumberValue backTicks = new RangeSelectionNumberValue("ReverseTicks", 1.0, 2.0, 0.0, 20.0);
    private final BooleanValue silentRotation = new BooleanValue("SilentRotation", Boolean.valueOf((boolean)false));
    private final BooleanValue throughWallsAim = new BooleanValue("ThroughWallsAim", Boolean.valueOf((boolean)false));
    private final BooleanValue noRotationWhenFaceTarget = new BooleanValue("NoRotationWhenFaceTarget", Boolean.valueOf((boolean)false));
    private final BooleanValue delayRotation = new BooleanValue("DelayWhenTargetCantBeSeen", Boolean.valueOf((boolean)false));
    private final RangeSelectionNumberValue rotationDelay = (RangeSelectionNumberValue)new RangeSelectionNumberValue("RotationDelay", 100.0, 150.0, 0.0, 1000.0).setVisible(() -> ((BooleanValue)this.delayRotation).getValue());
    private final BooleanValue predictAimPlayer = new BooleanValue("PlayerPredict", Boolean.valueOf((boolean)false));
    private final RangeSelectionNumberValue horizontalMultiplierPlayer = (RangeSelectionNumberValue)new RangeSelectionNumberValue("PlayerHMultiplier", 0.1, 0.8, -4.0, 4.0, 0.01).setVisible(() -> ((BooleanValue)this.predictAimPlayer).getValue());
    private final RangeSelectionNumberValue verticalMultiplierPlayer = (RangeSelectionNumberValue)new RangeSelectionNumberValue("PlayerVMultiplier", 0.1, 0.8, -4.0, 4.0, 0.01).setVisible(() -> ((BooleanValue)this.predictAimPlayer).getValue());
    private final BooleanValue predictAimTarget = new BooleanValue("TargetPredict", Boolean.valueOf((boolean)false));
    private final RangeSelectionNumberValue horizontalMultiplierTarget = (RangeSelectionNumberValue)new RangeSelectionNumberValue("TargetHMultiplier", 0.1, 0.8, -4.0, 4.0, 0.01).setVisible(() -> ((BooleanValue)this.predictAimTarget).getValue());
    private final RangeSelectionNumberValue verticalMultiplierTarget = (RangeSelectionNumberValue)new RangeSelectionNumberValue("TargetVMultiplier", 0.1, 0.8, -4.0, 4.0, 0.01).setVisible(() -> ((BooleanValue)this.predictAimTarget).getValue());
    private final BooleanValue boxExpand = new BooleanValue("TargetBoxExpand", Boolean.valueOf((boolean)false));
    private final NumberValue expandSizeH = (NumberValue)new NumberValue("BoxHExpandSize", 0.1, -1.5, 1.5, 0.01).setVisible(() -> ((BooleanValue)this.boxExpand).getValue());
    private final NumberValue expandSizeV = (NumberValue)new NumberValue("BoxVExpandSize", 0.1, -1.5, 1.5, 0.01).setVisible(() -> ((BooleanValue)this.boxExpand).getValue());
    private final BooleanValue randomize = new BooleanValue("Randomize", Boolean.valueOf((boolean)false));
    private final MultiBooleanValue randomizeTime = new MultiBooleanValue("RandomizeTime").add("Always", true).add("Attack", false).add("Movement", false).add("Rotation", false);
    private final ModeValue randomizeMode = (ModeValue)new ModeValue("RandomizeMode", "Normal", (AbstractModule)this, new Mode[]{new StringMode("Normal"), new StringMode("Gaussian"), new StringMode("Noise")}).setVisible(() -> ((BooleanValue)this.randomize).getValue());
    private final RangeSelectionNumberValue randomizeDelay = (RangeSelectionNumberValue)new RangeSelectionNumberValue("RandomizeDelay", 0.0, 50.0, 0.0, 1000.0).setVisible(() -> ((BooleanValue)this.randomize).getValue());
    private final RangeSelectionNumberValue randomizeYaw = (RangeSelectionNumberValue)new RangeSelectionNumberValue("RandomizeYaw", 0.0, 10.0, -60.0, 60.0, 0.01).setVisible(() -> ((BooleanValue)this.randomize).getValue());
    private final RangeSelectionNumberValue randomizePitch = (RangeSelectionNumberValue)new RangeSelectionNumberValue("RandomizePitch", 0.0, 10.0, -60.0, 60.0, 0.01).setVisible(() -> ((BooleanValue)this.randomize).getValue());
    private final NumberValue gaussianAmplitude = (NumberValue)new NumberValue("GaussianAmplitude", 1.0, 0.0, 5.0, 0.1).setVisible(() -> this.randomizeMode.is("Gaussian"));
    private final NumberValue gaussianMean = (NumberValue)new NumberValue("GaussianMean", 0.0, 0.0, 5.0, 0.1).setVisible(() -> this.randomizeMode.is("Gaussian"));
    private final NumberValue gaussianDeviation = (NumberValue)new NumberValue("GaussianDeviation", 1.0, 0.0, 5.0, 0.1).setVisible(() -> this.randomizeMode.is("Gaussian"));
    private final NumberValue noiseSigma = (NumberValue)new NumberValue("Noise-Sigma", 2.5, 0.0, 10.0, 0.01).setVisible(() -> this.randomizeMode.is("Noise"));
    private final ModeValue moveFixMode = new ModeValue("MoveFixMode", "None", (AbstractModule)this, new Mode[]{new StringMode("None"), new StringMode("Strict"), new StringMode("Silent")});
    private final ModeValue autoBlockMode = new ModeValue("AutoBlockMode", "None", (AbstractModule)this, new Mode[]{new StringMode("HoldKey"), new StringMode("AfterTick"), new StringMode("Packet"), new StringMode("Fake"), new StringMode("Matrix"), new StringMode("Verus"), new StringMode("Attack"), new StringMode("None")});
    private final ModeValue blockTiming = new ModeValue("AutoBlockTiming", "Normal", (AbstractModule)this, new Mode[]{new StringMode("Normal"), new StringMode("Tick"), new StringMode("Pre"), new StringMode("Post"), new StringMode("AfterAttack")});
    private final BooleanValue noPacketShielded = (BooleanValue)new BooleanValue("NoPacketWhileShielding", Boolean.valueOf((boolean)false)).setVisible(() -> this.autoBlockMode.is("AfterTick"));
    private final BooleanValue onlyWhileKeyBinding = new BooleanValue("OnlyWhileKeyBinding", Boolean.valueOf((boolean)false));
    private final BooleanValue sendInteractPacket = new BooleanValue("InteractPacket", Boolean.valueOf((boolean)false));
    private final List<EntityLivingBase> targets = new ArrayList();
    private final DelayTimer attackTimer = new DelayTimer();
    private final DelayTimer targetTimer = new DelayTimer();
    private final DelayTimer randomizeTimer = new DelayTimer();
    private int randomDelay = 0;
    public EntityLivingBase target = null;
    private int attackDelay = 0;
    private int canAttackTimes = 0;
    private int curRotationDelay = 0;
    private DelayTimer rotationTimer = new DelayTimer();
    public boolean blockingTick = false;
    private boolean blockingStatus = false;

    public KillAura() {
        this.rotationRange = new NumberValue("RotationRange", 6.0, 0.0, 10.0, 0.1);
        this.swingRange = new NumberValue("SwingRange", 6.0, 0.0, 10.0, 0.1).setMaxWith(this.rotationRange);
        this.attackRange = new NumberValue("AttackRange", 6.0, 0.0, 10.0, 0.1).setMaxWith(this.swingRange);
        this.throughWallAttackRange = new NumberValue("ThroughWallAttackRange", 6.0, 0.0, 10.0, 0.1).setMaxWith(this.attackRange);
        this.attackRange.setMinWith(this.throughWallAttackRange);
        this.swingRange.setMinWith(this.attackRange);
        this.rotationRange.setMinWith(this.swingRange);
    }

    public AxisAlignedBB getTargetBox(EntityLivingBase target) {
        double vertical;
        double horizontal;
        AxisAlignedBB basicBox = target.getBox();
        if (((Boolean)this.boxExpand.getValue()).booleanValue()) {
            basicBox = basicBox.expand(((Double)this.expandSizeH.getValue()).doubleValue(), ((Double)this.expandSizeV.getValue()).doubleValue(), ((Double)this.expandSizeH.getValue()).doubleValue());
        }
        if (((Boolean)this.predictAimPlayer.getValue()).booleanValue()) {
            horizontal = RandomUtils.randomDouble((double)this.horizontalMultiplierPlayer.getFirst(), (double)this.horizontalMultiplierPlayer.getSecond());
            vertical = RandomUtils.randomDouble((double)this.verticalMultiplierPlayer.getFirst(), (double)this.verticalMultiplierPlayer.getSecond());
            basicBox = basicBox.offset(KillAura.mc.player.motionX * horizontal, KillAura.mc.player.motionY * vertical, KillAura.mc.player.motionZ * horizontal);
        }
        if (((Boolean)this.predictAimTarget.getValue()).booleanValue()) {
            horizontal = RandomUtils.randomDouble((double)this.horizontalMultiplierTarget.getFirst(), (double)this.horizontalMultiplierTarget.getSecond());
            vertical = RandomUtils.randomDouble((double)this.verticalMultiplierTarget.getFirst(), (double)this.verticalMultiplierTarget.getSecond());
            basicBox = basicBox.offset((target.posX - target.lastTickPosX) * horizontal, (target.posY - target.lastTickPosY) * vertical, (target.posZ - target.lastTickPosZ) * horizontal);
        }
        return basicBox;
    }

    public void rotation(EntityLivingBase target) {
        if (target == null) {
            this.rotationTimer.reset();
            return;
        }
        float horizonSpeed = (float)RandomUtils.randomDouble((double)this.yawTurnSpeed.getFirst(), (double)this.yawTurnSpeed.getSecond());
        float pitchSpeed = (float)RandomUtils.randomDouble((double)this.pitchTurnSpeed.getFirst(), (double)this.pitchTurnSpeed.getSecond());
        int keepTicks = RandomUtils.randomInt((int)((int)Math.round((double)this.keepTicks.getFirst())), (int)((int)Math.round((double)this.keepTicks.getSecond())));
        int reverseTicks = RandomUtils.randomInt((int)((int)Math.round((double)this.backTicks.getFirst())), (int)((int)Math.round((double)this.backTicks.getSecond())));
        if (!this.rotationMode.is("None")) {
            Rotation rotation = this.calculateRotation(target);
            Rotation calculateRot = RotationUtils.smoothRotation((Rotation)RotationHandler.getRotation(), (Rotation)(rotation == null ? new Rotation(KillAura.mc.player.rotationYaw, KillAura.mc.player.rotationPitch) : rotation), (float)horizonSpeed, (float)pitchSpeed).fixedSensitivity(0.0f);
            if (((Boolean)this.silentRotation.getValue()).booleanValue()) {
                RotationHandler.setClientRotation((Rotation)calculateRot, (Integer)keepTicks, (Integer)reverseTicks, (String)((Mode)this.moveFixMode.getValue()).getName());
            } else {
                KillAura.mc.player.rotationYaw = calculateRot.yaw;
                KillAura.mc.player.rotationPitch = calculateRot.pitch;
            }
        }
    }

    
    public Rotation calculateRotation(EntityLivingBase target) {
        block52: {
            block51: {
                block50: {
                    center = null;
                    currentRotation = null;
                    targetBox = this.getTargetBox(target);
                    var5_5 = ((Mode)this.rotationMode.getValue()).getName();
                    var6_7 = -1;
                    switch (var5_5.hashCode()) {
                        case 533086306: {
                            if (!var5_5.equals((Object)"Advance")) break;
                            var6_7 = 0;
                            break;
                        }
                        case -1881109120: {
                            if (!var5_5.equals((Object)"LockCenter")) break;
                            var6_7 = 1;
                            break;
                        }
                        case 73612001: {
                            if (!var5_5.equals((Object)"Lower")) break;
                            var6_7 = 2;
                            break;
                        }
                        case 1973607883: {
                            if (!var5_5.equals((Object)"LockHead")) break;
                            var6_7 = 3;
                            break;
                        }
                        case 2603186: {
                            if (!var5_5.equals((Object)"Test")) break;
                            var6_7 = 4;
                            break;
                        }
                        case -2114987469: {
                            if (!var5_5.equals((Object)"NearestCenter")) break;
                            var6_7 = 5;
                            break;
                        }
                        case -1955878649: {
                            if (!var5_5.equals((Object)"Normal")) break;
                            var6_7 = 6;
                            break;
                        }
                        case 1353703610: {
                            if (!var5_5.equals((Object)"Proportion")) break;
                            var6_7 = 7;
                            break;
                        }
                    }
                    switch (var6_7) {
                        case 0: {
                            break;
                        }
                        case 1: {
                            center = targetBox.lerpWith(0.5, 0.5, 0.5);
                            break;
                        }
                        case 2: {
                            block31: for (x = 0.4; x <= 0.6; x += 0.025) {
                                y = 0.2;
lbl48:
                                

                                while (true) {
                                    if (!(y <= 0.6)) continue block31;
                                    break block50;
                                    break;
                                }
                            }
                            break;
                        }
                        case 3: {
                            for (x = 0.3; x <= 0.8; x += 0.1) {
                                for (z = 0.3; z <= 0.8; z += 0.1) {
                                    preCenter = targetBox.lerpWith(x, 0.9, z);
                                    if (((Boolean)this.rayCast.getValue()).booleanValue() && !((Boolean)this.rayCastThroughWalls.getValue()).booleanValue()) {
                                        rotation = RotationUtils.toRotation((Vec3d)preCenter, (Entity)KillAura.mc.player);
                                        entity = RayCastUtils.raycastEntity((double)((Double)this.rotationRange.getValue()), (float)rotation.yaw, (float)rotation.pitch, (Boolean)((Boolean)this.rayCastThroughWalls.getValue()), (Function)(Function)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Ljava/lang/Object;, lambda.calculateRotation.10(net.minecraft.entity.Entity ), (Lnet/minecraft/entity/Entity;)Ljava/lang/Boolean;)());
                                        if (entity == null || entity != target && ((Boolean)this.rayCastOnlyTarget.getValue()).booleanValue()) continue;
                                    }
                                    if (!CalculateUtils.isVisible((Vec3d)preCenter).booleanValue() && !((Boolean)this.throughWallsAim.getValue()).booleanValue() || center != null && !(RotationUtils.getRotationDifference((Rotation)RotationUtils.toRotation((Vec3d)preCenter, (Entity)KillAura.mc.player), (Rotation)RotationHandler.getRotation()) < RotationUtils.getRotationDifference((Rotation)RotationUtils.toRotation((Vec3d)center, (Entity)KillAura.mc.player), (Rotation)RotationHandler.getRotation()))) continue;
                                    center = preCenter;
                                }
                            }
                            break;
                        }
                        case 4: {
                            center = CalculateUtils.getClosestPoint((Vec3d)KillAura.mc.player.getPositionEyes(2.1478f).addVector(0.0, -0.199, 0.0), (AxisAlignedBB)targetBox.offset(-KillAura.mc.player.motionX - KillAura.mc.player.posX + KillAura.mc.player.prevPosX, -KillAura.mc.player.motionY - KillAura.mc.player.posY + KillAura.mc.player.prevPosY, -KillAura.mc.player.motionZ - KillAura.mc.player.posZ + KillAura.mc.player.prevPosZ));
                            break;
                        }
                        case 5: {
                            center = CalculateUtils.getClosestPoint((Vec3d)KillAura.mc.player.getEyes(), (AxisAlignedBB)targetBox);
                            break;
                        }
                        case 6: {
                            block35: for (x = 0.2; x <= 0.8; x += 0.1) {
                                y = 0.2;
lbl74:
                                

                                while (true) {
                                    if (!(y <= 0.8)) continue block35;
                                    break block51;
                                    break;
                                }
                            }
                            break;
                        }
                        case 7: {
                            d = RandomUtils.randomDouble((double)this.distancePro.getFirst(), (double)this.distancePro.getSecond());
                            a = RandomUtils.randomDouble((double)this.anglePro.getFirst(), (double)this.anglePro.getSecond());
                            lastD = 0.0;
                            lastA = 0.0;
                            block37: for (x = 0.2; x <= 0.8; x += 0.1) {
                                y = 0.2;
lbl85:
                                

                                while (true) {
                                    if (!(y <= 0.8)) continue block37;
                                    break block52;
                                    break;
                                }
                            }
                            break;
                        }
                    }
                    if (center != null && (((Boolean)this.throughWallsAim.getValue()).booleanValue() || CalculateUtils.isVisible((Vec3d)center).booleanValue())) {
                        currentRotation = RotationUtils.toRotation((Vec3d)center, (Entity)KillAura.mc.player);
                    }
                    var5_5 = ((Mode)this.rotationMode.getValue()).getName();
                    var6_7 = -1;
                    switch (var5_5.hashCode()) {
                        case -2114987469: {
                            if (!var5_5.equals((Object)"NearestCenter")) break;
                            var6_7 = 0;
                            break;
                        }
                        case -1955878649: {
                            if (!var5_5.equals((Object)"Normal")) break;
                            var6_7 = 1;
                            break;
                        }
                        case 1973607883: {
                            if (!var5_5.equals((Object)"LockHead")) break;
                            var6_7 = 2;
                            break;
                        }
                        case -1881109120: {
                            if (!var5_5.equals((Object)"LockCenter")) break;
                            var6_7 = 3;
                            break;
                        }
                        case 533086306: {
                            if (!var5_5.equals((Object)"Advance")) break;
                            var6_7 = 4;
                            break;
                        }
                    }
                    switch (var6_7) {
                        case 0: 
                        case 1: 
                        case 2: 
                        case 3: {
                            break;
                        }
                        case 4: {
                            currentRotation = RotationUtils.findBestRotationSimulatedAnnealing((Entity)KillAura.mc.player, (Entity)target);
                            break;
                        }
                    }
                    if (currentRotation == null) return currentRotation;
                    this.randomizeRotation(currentRotation);
                    if (((Boolean)this.noRotationWhenFaceTarget.getValue()).booleanValue() && (entity = RayCastUtils.raycastEntity((double)((Double)this.rotationRange.getValue()), (float)RotationHandler.getCurrentRotation().yaw, (float)RotationHandler.getCurrentRotation().pitch, (Boolean)((Boolean)this.rayCastThroughWalls.getValue()), (Function)(Function)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Ljava/lang/Object;, lambda.calculateRotation.13(net.minecraft.entity.Entity ), (Lnet/minecraft/entity/Entity;)Ljava/lang/Boolean;)())) != null && (!((Boolean)this.rayCastOnlyTarget.getValue()).booleanValue() || entity == target)) {
                        currentRotation = RotationHandler.getCurrentRotation();
                    }
                    if ((Boolean)this.delayRotation.getValue() == false) return currentRotation;
                    v0 = canBeSeen = RotationUtils.getRotationDifference((Rotation)currentRotation, (Rotation)RotationHandler.getCurrentRotation()) <= KillAura.mc.gameSettings.fovSetting;
                    if (canBeSeen) {
                        this.rotationTimer.reset();
                        return currentRotation;
                    }
                    if (!this.rotationTimer.hasTimeElapsed(this.curRotationDelay)) {
                        return RotationHandler.getCurrentRotation();
                    }
                    this.rotationTimer.reset();
                    this.curRotationDelay = RandomUtils.randomInt((int)((int)this.rotationDelay.getFirst()), (int)((int)this.rotationDelay.getSecond()));
                    return currentRotation;
                }
                for (z = 0.4; z <= 0.6; z += 0.025) {
                    preCenter = targetBox.lerpWith(x, y, z);
                    if (((Boolean)this.rayCast.getValue()).booleanValue() && !((Boolean)this.rayCastThroughWalls.getValue()).booleanValue()) {
                        rotation = RotationUtils.toRotation((Vec3d)preCenter, (Entity)KillAura.mc.player);
                        entity = RayCastUtils.raycastEntity((double)((Double)this.rotationRange.getValue()), (float)rotation.yaw, (float)rotation.pitch, (Boolean)((Boolean)this.rayCastThroughWalls.getValue()), (Function)(Function)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Ljava/lang/Object;, lambda.calculateRotation.9(net.minecraft.entity.Entity ), (Lnet/minecraft/entity/Entity;)Ljava/lang/Boolean;)());
                        if (entity == null || entity != target && ((Boolean)this.rayCastOnlyTarget.getValue()).booleanValue()) continue;
                    }
                    if (!CalculateUtils.isVisible((Vec3d)preCenter).booleanValue() && !((Boolean)this.throughWallsAim.getValue()).booleanValue() || center != null && !(RotationUtils.getRotationDifference((Rotation)RotationUtils.toRotation((Vec3d)preCenter, (Entity)KillAura.mc.player), (Rotation)RotationHandler.getRotation()) < RotationUtils.getRotationDifference((Rotation)RotationUtils.toRotation((Vec3d)center, (Entity)KillAura.mc.player), (Rotation)RotationHandler.getRotation()))) continue;
                    center = preCenter;
                }
                y += 0.1;
                ** while (true)
            }
            for (z = 0.2; z <= 0.8; z += 0.1) {
                preCenter = targetBox.lerpWith(x, y, z);
                if (((Boolean)this.rayCast.getValue()).booleanValue()) {
                    rotation = RotationUtils.toRotation((Vec3d)preCenter, (Entity)KillAura.mc.player);
                    entity = RayCastUtils.raycastEntity((double)((Double)this.rotationRange.getValue()), (float)rotation.yaw, (float)rotation.pitch, (Boolean)((Boolean)this.rayCastThroughWalls.getValue()), (Function)(Function)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Ljava/lang/Object;, lambda.calculateRotation.11(net.minecraft.entity.Entity ), (Lnet/minecraft/entity/Entity;)Ljava/lang/Boolean;)());
                    if (entity == null || entity != target && ((Boolean)this.rayCastOnlyTarget.getValue()).booleanValue()) continue;
                }
                if (!CalculateUtils.isVisible((Vec3d)preCenter).booleanValue() && !((Boolean)this.throughWallsAim.getValue()).booleanValue() || center != null && !(RotationUtils.getRotationDifference((Rotation)RotationUtils.toRotation((Vec3d)preCenter, (Entity)KillAura.mc.player), (Rotation)RotationHandler.getRotation()) < RotationUtils.getRotationDifference((Rotation)RotationUtils.toRotation((Vec3d)center, (Entity)KillAura.mc.player), (Rotation)RotationHandler.getRotation()))) continue;
                center = preCenter;
            }
            y += 0.1;
            ** while (true)
        }
        for (z = 0.2; z <= 0.8; z += 0.1) {
            preCenter = targetBox.lerpWith(x, y, z);
            if (((Boolean)this.rayCast.getValue()).booleanValue()) {
                rotation = RotationUtils.toRotation((Vec3d)preCenter, (Entity)KillAura.mc.player);
                entity = RayCastUtils.raycastEntity((double)((Double)this.rotationRange.getValue()), (float)rotation.yaw, (float)rotation.pitch, (Boolean)((Boolean)this.rayCastThroughWalls.getValue()), (Function)(Function)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Ljava/lang/Object;, lambda.calculateRotation.12(net.minecraft.entity.Entity ), (Lnet/minecraft/entity/Entity;)Ljava/lang/Boolean;)());
                if (entity == null || entity != target && ((Boolean)this.rayCastOnlyTarget.getValue()).booleanValue()) continue;
            }
            distance = KillAura.mc.player.getEyes().distanceTo(preCenter) * d;
            angle = (double)RotationUtils.getRotationDifference((Rotation)RotationUtils.toRotation((Vec3d)preCenter, (Entity)KillAura.mc.player), (Rotation)RotationHandler.getRotation()) * a;
            if (!CalculateUtils.isVisible((Vec3d)preCenter).booleanValue() && !((Boolean)this.throughWallsAim.getValue()).booleanValue() || center != null && !(distance + angle < lastD + lastA)) continue;
            center = preCenter;
            lastD = distance;
            lastA = angle;
        }
        y += 0.1;
        ** while (true)
    }

    
    public void randomizeRotation(Rotation rotation) {
        
        throw new IllegalStateException("Decompilation failed");
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (KillAura.mc.player == null) {
            return;
        }
        if (!TargetsHandler.canAdd((Entity)this.target)) {
            this.target = null;
        }
        this.target = this.getTarget();
        this.rotation(this.target);
        if (this.blockTiming.is("Tick")) {
            this.runStopBlock();
        }
        if (this.attackTimeMode.is("Tick")) {
            this.attackTarget(this.target);
        }
        if (this.blockTiming.is("Tick")) {
            this.runAutoBlock((Entity)this.target);
        }
    }

    @EventHandler
    public void onMotion(MotionEvent event) {
        if (KillAura.mc.player == null) {
            return;
        }
        if (this.blockTiming.is("Pre") && event.isPre() || this.blockTiming.is("Post") && event.isPost()) {
            this.runStopBlock();
        }
        if (this.attackTimeMode.is("Pre") && event.isPre() || this.attackTimeMode.is("Post") && event.isPost()) {
            this.attackTarget(this.target);
        }
        if (this.blockTiming.is("Pre") && event.isPre() || this.blockTiming.is("Post") && event.isPost()) {
            this.runAutoBlock((Entity)this.target);
        }
    }

    public void onEnable() {
        this.attackDelay = this.calculateDelay();
        this.attackTimer.reset();
        this.targetTimer.reset();
        this.curRotationDelay = RandomUtils.randomInt((int)((int)this.rotationDelay.getFirst()), (int)((int)this.rotationDelay.getSecond()));
    }

    public void onDisable() {
        this.target = null;
        this.targets.clear();
        if (this.blockingStatus && this.blockingTick) {
            KillAura.mc.playerController.onStoppedUsingItem((EntityPlayer)KillAura.mc.player);
        }
        this.blockingStatus = false;
        this.blockingTick = false;
        KillAura.mc.gameSettings.keyBindUseItem.setPressed(false);
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (KillAura.mc.player == null) {
            return;
        }
        if (this.attackTimer.hasTimeElapsed(this.attackDelay) && this.target != null && (((Boolean)this.fastOnFirstHit.getValue()).booleanValue() && this.canAttackTimes <= 1 || CalculateUtils.getClosetDistance((EntityLivingBase)KillAura.mc.player, (EntityLivingBase)this.target) <= (Double)this.swingRange.getValue() + 0.1)) {
            ++this.canAttackTimes;
            this.attackDelay = this.calculateDelay();
            this.attackTimer.reset();
        }
        if (this.target == null) {
            if (!((Boolean)this.fastOnFirstHit.getValue()).booleanValue()) {
                this.canAttackTimes = 0;
            }
            this.targetTimer.reset();
        }
    }

    
    private EntityLivingBase getTarget() {
        
        throw new IllegalStateException("Decompilation failed");
    }

    private void sendInteractPacket(Entity target) {
        RayTraceResult raytrace;
        if (((Boolean)this.sendInteractPacket.getValue()).booleanValue() && (raytrace = KillAura.mc.player.rayTrace(((Double)this.rotationRange.getValue()).doubleValue(), 1.0f)) != null) {
            KillAura.mc.playerController.syncCurrentPlayItem();
            Vec3d vec3d = new Vec3d(raytrace.hitVec.xCoord - target.posX, raytrace.hitVec.yCoord - target.posY, raytrace.hitVec.zCoord - target.posZ);
            PacketUtils.sendPacket((Packet)new CPacketUseEntity(target, EnumHand.MAIN_HAND, vec3d));
            PacketUtils.sendPacket((Packet)new CPacketUseEntity(target, EnumHand.MAIN_HAND, vec3d));
        }
    }

    private void blockingPacket(Entity target) {
        this.sendInteractPacket(target);
        PacketUtils.sendPacket((Packet)new CPacketPlayerTryUseItem(EnumHand.MAIN_HAND));
        PacketUtils.sendPacket((Packet)new CPacketPlayerTryUseItem(EnumHand.MAIN_HAND));
        PacketUtils.sendPacket((Packet)new CPacketPlayerTryUseItem(EnumHand.OFF_HAND));
    }

    
    public void runStopBlock() {
        
        throw new IllegalStateException("Decompilation failed");
    }

    
    private void runAutoBlock(Entity target) {
        
        throw new IllegalStateException("Decompilation failed");
    }

    @EventHandler
    public void onPacketSend(PacketSendEvent event) {
        Packet packet = event.getPacket();
        if (packet instanceof CPacketUseEntity && ((CPacketUseEntity)packet).getAction() == CPacketUseEntity.Action.ATTACK && this.autoBlockMode.is("Matrix") && this.canBlock() && this.blockingTick) {
            event.setCancelled(true);
            PacketUtils.sendPacket((Packet)new CPacketPlayerDigging(CPacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
            PacketUtils.sendPacket((Packet)packet, (boolean)false);
            this.blockingTick = false;
            this.blockingStatus = false;
            this.runAutoBlock((Entity)this.target);
        }
    }

    private void attackTarget(EntityLivingBase target) {
        if (KillAura.mc.player == null || target == null) {
            return;
        }
        if (((Boolean)this.noInventory.getValue()).booleanValue() && KillAura.mc.currentScreen instanceof GuiInventory || ((Boolean)this.noGui.getValue()).booleanValue() && KillAura.mc.currentScreen != null) {
            KillAura.mc.gameSettings.keyBindUseItem.setPressed(GameSettings.isKeyDown((KeyBinding)KillAura.mc.gameSettings.keyBindUseItem));
            return;
        }
        if (Objects.equals((Object)((Mode)this.noDoubleHit.getValue()).getName(), (Object)"Cancel") && this.canAttackTimes >= 2) {
            this.canAttackTimes = 1;
        }
        Rotation rotation = RotationHandler.clientRotation == null ? RotationHandler.getRotation() : RotationHandler.clientRotation;
        Object bestTarget = (Boolean)this.rayCast.getValue() == false ? target : RayCastUtils.raycastEntity((double)((Double)this.attackRange.getValue()), (float)rotation.yaw, (float)rotation.pitch, (Boolean)((Boolean)this.rayCastThroughWalls.getValue()), entity -> entity instanceof EntityLivingBase);
        if (bestTarget == null || bestTarget != target && ((Boolean)this.rayCastOnlyTarget.getValue()).booleanValue()) {
            return;
        }
        if (!PlayerUtils.canBeSeenEntity((Entity)KillAura.mc.player, (Entity)bestTarget) && CalculateUtils.getClosetDistance((EntityLivingBase)KillAura.mc.player, (EntityLivingBase)bestTarget) > (Double)this.throughWallAttackRange.getValue()) {
            return;
        }
        if ((double)bestTarget.hurtTime > (Double)this.hurtTime.getValue()) {
            return;
        }
        if (this.blockTiming.is("Normal")) {
            this.runAutoBlock((Entity)target);
            this.runStopBlock();
        }
        if (this.blockTiming.is("AfterAttack")) {
            this.runStopBlock();
        }
        while (this.canAttackTimes > 0) {
            --this.canAttackTimes;
            if (CalculateUtils.getClosetDistance((EntityLivingBase)KillAura.mc.player, (EntityLivingBase)bestTarget) <= (Double)this.attackRange.getValue()) {
                if (this.attackMode.is("Packet")) {
                    if (!ViaLoadingBase.getInstance().getTargetVersion().newerThan(ProtocolVersion.v1_8)) {
                        this.swing();
                    }
                    AttackEvent event = new AttackEvent((Entity)target);
                    Client.INSTANCE.getEventManager().call((Object)event);
                    if (event.isCancelled()) {
                        return;
                    }
                    PacketUtils.sendPacket((Packet)new CPacketUseEntity((Entity)bestTarget));
                    if (ViaLoadingBase.getInstance().getTargetVersion().newerThan(ProtocolVersion.v1_8)) {
                        this.swing();
                    }
                    if (!this.keepSprintMode.is("Always") && (this.keepSprintMode.is("None") || KillAura.mc.player.hurtTime > 0)) {
                        KillAura.mc.player.attackTargetEntityWithCurrentItem((Entity)target);
                    }
                } else if (this.attackMode.is("Legit")) {
                    if (this.blockTiming.is("Normal")) {
                        this.runStopBlock();
                    }
                    mc.clickMouse();
                }
            } else if (CalculateUtils.getClosetDistance((EntityLivingBase)KillAura.mc.player, (EntityLivingBase)bestTarget) <= (Double)this.swingRange.getValue()) {
                KillAura.mc.player.swingArm(EnumHand.MAIN_HAND);
            }
            if (!this.noDoubleHit.is("NextHit")) continue;
        }
        if (this.blockTiming.is("AfterAttack")) {
            this.runAutoBlock((Entity)bestTarget);
        }
    }

    
    private void swing() {
        
        throw new IllegalStateException("Decompilation failed");
    }

    
    private int calculateDelay() {
        
        throw new IllegalStateException("Decompilation failed");
    }

    private boolean canBlock() {
        if (((Boolean)this.noInventory.getValue()).booleanValue() && KillAura.mc.currentScreen instanceof GuiInventory) {
            return false;
        }
        return KillAura.mc.player.getHeldItem(EnumHand.MAIN_HAND).getItem() instanceof ItemSword && (KillAura.mc.player.getHeldItem(EnumHand.OFF_HAND).isEmptyStack() || KillAura.mc.player.getHeldItem(EnumHand.OFF_HAND).getItem() instanceof ItemShield);
    }

    @NonNull
    public String getTag() {
        return this.mode.getValueByName();
    }

    @Generated
    public boolean isBlockingTick() {
        return this.blockingTick;
    }

    @Generated
    public boolean isBlockingStatus() {
        return this.blockingStatus;
    }

    private  double lambda.getTarget.19(EntityLivingBase entityLivingBase) {
        return Math.max((double)0.0, (double)(CalculateUtils.getClosetDistance((EntityLivingBase)KillAura.mc.player, (EntityLivingBase)entityLivingBase) - (Double)this.attackRange.getValue()));
    }

    private static  int lambda.getTarget.18(EntityLivingBase entityLivingBase) {
        return RandomUtils.randomInt((int)-100, (int)100);
    }

    private  double lambda.getTarget.17(EntityLivingBase entityLivingBase) {
        return RotationUtils.getRotationDifference((Rotation)RotationUtils.toRotation((Vec3d)this.getTargetBox(entityLivingBase).getCenter(), (Entity)KillAura.mc.player), (Rotation)new Rotation(KillAura.mc.player.rotationYaw, KillAura.mc.player.rotationPitch));
    }

    private static  int lambda.getTarget.16(EntityLivingBase entityLivingBase) {
        return entityLivingBase.hurtTime;
    }

    private static  double lambda.getTarget.15(EntityLivingBase entityLivingBase) {
        return CalculateUtils.getClosetDistance((EntityLivingBase)KillAura.mc.player, (EntityLivingBase)entityLivingBase);
    }

    
    private  void lambda.randomizeRotation.14(AtomicBoolean canRandomize, String key, Boolean value) {
        
        throw new IllegalStateException("Decompilation failed");
    }

    private static  Boolean lambda.calculateRotation.13(Entity e) {
        return e instanceof EntityLivingBase;
    }

    private static  Boolean lambda.calculateRotation.12(Entity e) {
        return e instanceof EntityLivingBase;
    }

    private static  Boolean lambda.calculateRotation.11(Entity e) {
        return e instanceof EntityLivingBase;
    }

    private static  Boolean lambda.calculateRotation.10(Entity e) {
        return e instanceof EntityLivingBase;
    }

    private static  Boolean lambda.calculateRotation.9(Entity e) {
        return e instanceof EntityLivingBase;
    }
}