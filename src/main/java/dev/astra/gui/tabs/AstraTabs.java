package dev.astra.gui.tabs;

/**
 * Registers the custom creative mode tabs.
 * Instantiating a CreativeTabs subclass self-registers it into the vanilla
 * CREATIVE_TAB_ARRAY, so the tabs show up in the creative inventory.
 */
public class AstraTabs {

    private static boolean initialized = false;

    private AstraTabs() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        new AstraExploitsTab();
        new AstraHeadsTab();
    }
}
