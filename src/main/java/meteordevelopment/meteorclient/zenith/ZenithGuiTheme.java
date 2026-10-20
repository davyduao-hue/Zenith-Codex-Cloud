package meteordevelopment.meteorclient.zenith;

import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import net.minecraft.client.gui.screens.Screen;

/** The custom dashboard is scoped to Zenith, leaving other themes' native screens intact. */
public final class ZenithGuiTheme extends MeteorGuiTheme {
    public ZenithGuiTheme(){super("Zenith");}
    @Override public TabScreen modulesScreen(){return new ZenithGuiScreen(this);}
    @Override public boolean isModulesScreen(Screen screen){return screen instanceof ZenithGuiScreen||super.isModulesScreen(screen);}
}
