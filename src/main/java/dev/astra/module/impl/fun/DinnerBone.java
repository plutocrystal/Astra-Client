package dev.astra.module.impl.fun;

import dev.astra.Main;
import dev.astra.module.Category;
import dev.astra.module.Module;

public class DinnerBone extends Module {
    public DinnerBone() {
        super("DinnerBone", 0, Category.FUN);
    }

    public static boolean isEnabled() {
        DinnerBone m = Main.moduleManager.getModuleByClass(DinnerBone.class);
        return m != null && m.isToggle();
    }
}
