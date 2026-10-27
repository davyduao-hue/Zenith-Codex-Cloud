package meteordevelopment.meteorclient.gui.tabs.builtin;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.*;
import meteordevelopment.meteorclient.systems.config.Config;
import net.minecraft.client.gui.screens.Screen;

/** Persistent global UI sizing, separate from per-widget layout and module settings. */
public final class ZenithUiTab extends Tab {
    public ZenithUiTab(){super("UI");}
    @Override public TabScreen createScreen(GuiTheme theme){return new UiScreen(theme,this);}
    @Override public boolean isScreen(Screen screen){return screen instanceof UiScreen;}
    private static final class UiScreen extends WindowTabScreen {
        UiScreen(GuiTheme theme,Tab tab){super(theme,tab);Config.get().zenithUi.onActivated();}
        @Override public void initWidgets(){
            var config=Config.get();
            add(theme.label("HUD changes apply live. Apply menu size after editing."));
            add(theme.settings(config.zenithUi)).expandX();
            var apply=add(theme.button("Apply menu size")).expandX().widget();
            apply.action=()->{config.save();tab.openScreen(theme);};
            var compact=add(theme.button("Compact defaults")).expandX().widget();
            compact.action=()->{config.zenithUi.reset();config.save();tab.openScreen(theme);};
            var editor=add(theme.button("Edit HUD positions / individual sizes")).expandX().widget();
            editor.action=()->Tabs.get(HudTab.class).openScreen(theme);
        }
        @Override public void onClose(){Config.get().save();super.onClose();}
    }
}
