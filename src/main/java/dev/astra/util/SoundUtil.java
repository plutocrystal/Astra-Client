package dev.astra.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.util.ResourceLocation;

public class SoundUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static void playSound(String soundName, float volume) {
        if (mc == null || mc.getSoundHandler() == null || soundName == null) return;
        mc.getSoundHandler().playSound(new SimpleSound(new ResourceLocation(soundName), volume));
    }

    private static class SimpleSound implements ISound {
        private final ResourceLocation location;
        private final float volume;
        public SimpleSound(ResourceLocation location, float volume) {
            this.location = location;
            this.volume = volume;
        }
        @Override public ResourceLocation getSoundLocation() { return location; }
        @Override public boolean canRepeat() { return false; }
        @Override public int getRepeatDelay() { return 0; }
        @Override public float getVolume() { return volume; }
        @Override public float getPitch() { return 1.0F; }
        @Override public float getXPosF() { return 0.0F; }
        @Override public float getYPosF() { return 0.0F; }
        @Override public float getZPosF() { return 0.0F; }
        @Override public AttenuationType getAttenuationType() { return AttenuationType.NONE; }
    }
}
