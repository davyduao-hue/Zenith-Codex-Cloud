package meteordevelopment.meteorclient.zenith;

import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import me.pindour.catppuccin.gui.themes.catppuccin.CatppuccinGuiTheme;
import me.pindour.catppuccin.gui.themes.catppuccin.colors.CatppuccinColor;
import me.pindour.catppuccin.gui.themes.catppuccin.colors.ColorLinkRegistry;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.screens.Screen;

import java.util.Map;

import static java.util.Map.entry;

/** A purple Catppuccin-based Meteor theme with the complete native addon/settings API. */
public final class ZenithGuiTheme extends CatppuccinGuiTheme {
    public enum Layout { Categories, Dashboard }

    private static final Map<CatppuccinColor, Color> PALETTE = Map.ofEntries(
        entry(CatppuccinColor.Mauve, new Color(183,117,255)),
        entry(CatppuccinColor.Lavender, new Color(208,175,255)),
        entry(CatppuccinColor.Text, new Color(241,232,255)),
        entry(CatppuccinColor.Subtext1, new Color(202,183,222)),
        entry(CatppuccinColor.Subtext0, new Color(165,143,190)),
        entry(CatppuccinColor.Overlay2, new Color(151,114,181)),
        entry(CatppuccinColor.Overlay1, new Color(121,86,151)),
        entry(CatppuccinColor.Overlay0, new Color(92,61,119)),
        entry(CatppuccinColor.Surface2, new Color(72,45,96)),
        entry(CatppuccinColor.Surface1, new Color(49,31,69)),
        entry(CatppuccinColor.Surface0, new Color(35,23,49)),
        entry(CatppuccinColor.Base, new Color(24,15,35)),
        entry(CatppuccinColor.Mantle, new Color(18,11,28)),
        entry(CatppuccinColor.Crust, new Color(12,7,20))
    );

    private final SettingGroup zenith = settings.createGroup("Zenith");
    public final Setting<Layout> layout = zenith.add(new EnumSetting.Builder<Layout>()
        .name("layout").description("Animated category windows or the full Zenith dashboard. Reopen Modules to apply.")
        .defaultValue(Layout.Categories).build());
    private final Setting<Boolean> zenithPalette = zenith.add(new BoolSetting.Builder()
        .name("zenith-palette").description("Use Zenith's dark violet surfaces and purple highlights.")
        .defaultValue(true).onChanged(ignored -> ColorLinkRegistry.applyAll()).build());
    public final Setting<SettingColor> zenithAccent = zenith.add(new ColorSetting.Builder()
        .name("zenith-accent").description("Highlight color for the Zenith palette.")
        .defaultValue(new SettingColor(183,117,255))
        .onChanged(ignored -> ColorLinkRegistry.applyAll()).visible(zenithPalette::get).build());

    public ZenithGuiTheme(){super("Zenith 2.0");}

    @Override public Color getColor(CatppuccinColor color) {
        if (zenithPalette != null && !zenithPalette.get()) return super.getColor(color);
        if (color == CatppuccinColor.Accent) {
            return zenithAccent == null ? PALETTE.get(CatppuccinColor.Mauve) : zenithAccent.get();
        }
        Color result = PALETTE.get(color);
        return result == null ? super.getColor(color) : result;
    }

    @Override public meteordevelopment.meteorclient.gui.widgets.containers.WWindow window(meteordevelopment.meteorclient.gui.widgets.WWidget icon,String title) {
        return w(new WZenithWindow(icon,title));
    }
    @Override public meteordevelopment.meteorclient.gui.widgets.WWidget module(meteordevelopment.meteorclient.systems.modules.Module module,String title) {
        return w(new WZenithModule(module,title));
    }
    @Override public TabScreen modulesScreen(){
        return layout.get() == Layout.Dashboard ? new ZenithGuiScreen(this) : new ZenithModulesScreen(this);
    }
    @Override public boolean isModulesScreen(Screen screen){return screen instanceof ZenithGuiScreen||super.isModulesScreen(screen);}
}
