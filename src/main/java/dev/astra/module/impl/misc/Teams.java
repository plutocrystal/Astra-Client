package dev.astra.module.impl.misc;

import dev.astra.Main;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.BooleanValue;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Team;
import org.lwjgl.input.Keyboard;

public class Teams extends Module {
    public final BooleanValue scoreboardTeam = new BooleanValue("ScoreboardTeam", true);

    public final BooleanValue nameColor = new BooleanValue("NameColor", true);

    public final BooleanValue armorColor = new BooleanValue("ArmorColor", true);

    public final BooleanValue gommeSW = new BooleanValue("GommeSW", false);

    public Teams() {
        super("Teams", Keyboard.KEY_NONE, Category.MISC);
        addValues(scoreboardTeam, nameColor, armorColor, gommeSW);
    }

    public static Teams getInstance() {
        return Main.moduleManager == null ? null : Main.moduleManager.getModuleByClass(Teams.class);
    }

    /**
     * Check if [entity] is in your own team using scoreboard, name/armor color or team prefix.
     * Ported from LiquidBounce legacy (net.ccbluex.liquidbounce.features.module.modules.misc.Teams).
     */
    public static boolean isInYourTeam(EntityLivingBase entity) {
        Teams teams = getInstance();
        if (teams == null || !teams.isToggle()) return false;
        if (mc.thePlayer == null || entity == null) return false;

        if (teams.scoreboardTeam.getValue()) {
            Team ownTeam = mc.thePlayer.getTeam();
            Team targetTeam = entity.getTeam();
            if (ownTeam != null && targetTeam != null && ownTeam.isSameTeam(targetTeam)) {
                return true;
            }
        }

        String clientName = mc.thePlayer.getDisplayName().getFormattedText().replace("\u00a7r", "");
        String targetName = entity.getDisplayName().getFormattedText().replace("\u00a7r", "");

        if (teams.gommeSW.getValue()
                && targetName.startsWith("T") && clientName.startsWith("T")
                && targetName.length() >= 2 && clientName.length() >= 2
                && Character.isDigit(targetName.charAt(1)) && Character.isDigit(clientName.charAt(1))) {
            return targetName.charAt(1) == clientName.charAt(1);
        }

        if (teams.nameColor.getValue() && clientName.length() >= 2 && targetName.length() >= 2) {
            return targetName.startsWith("\u00a7" + clientName.charAt(1));
        }

        if (teams.armorColor.getValue()) {
            for (int i = 0; i < 4; i++) {
                ItemStack playerArmor = mc.thePlayer.getCurrentArmor(i);
                ItemStack entityArmor = entity.getCurrentArmor(i);
                if (playerArmor == null || entityArmor == null) continue;
                if (!(playerArmor.getItem() instanceof ItemArmor) || !(entityArmor.getItem() instanceof ItemArmor)) continue;
                ItemArmor playerItem = (ItemArmor) playerArmor.getItem();
                ItemArmor entityItem = (ItemArmor) entityArmor.getItem();
                if (playerItem.getArmorMaterial() == ItemArmor.ArmorMaterial.LEATHER
                        && playerItem.getColor(playerArmor) == entityItem.getColor(entityArmor)) {
                    return true;
                }
            }
        }

        return false;
    }
}
