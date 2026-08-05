package dev.astra.module.impl.render;

import dev.astra.module.Category;
import dev.astra.module.Module;
import org.lwjgl.input.Keyboard;

public class CameraClip extends Module {

    public CameraClip() {
        super("CameraClip", Keyboard.KEY_NONE, Category.RENDER);
    }
}