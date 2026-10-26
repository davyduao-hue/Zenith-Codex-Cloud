package me.noramibu.tweaks.category;

import me.noramibu.tweaks.NoraTweaks;
import me.noramibu.tweaks.events.CustomCategoriesChangedEvent;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.lang.ref.WeakReference;

public class CustomCategoryHelper {
    private static final CategoryUpdates UPDATES = new CategoryUpdates();
    private final WContainer container;
    private final List<WWindow> windows;
    private final List<Cell<WWindow>> cells = new ArrayList<>();
    private final GuiTheme theme;

    public CustomCategoryHelper(WContainer container, List<WWindow> windows) {
        this.container = container;
        this.windows = windows;
        this.theme = GuiThemes.get();
        UPDATES.add(this);
    }

    public void refresh() {
        if (GuiThemes.get() != theme) return;

        cells.forEach(c -> { container.remove(c); windows.remove(c.widget()); });
        cells.clear();

        var theme = this.theme;
        for (var category : CustomCategoryManager.getCategories()) {
            try {
                var w = theme.window(category.name);
                w.id = "custom-" + category.name;
                w.padding = w.spacing = 0;

                var cell = container.add(w);
                w.view.scrollOnlyWhenMouseOver = true;
                w.view.hasScrollBar = false;
                w.view.spacing = 0;

                var modules = CustomCategoryManager.getModules(category);
                if (modules.isEmpty()) {
                    w.add(theme.label("No modules.")).expandX();
                } else {
                    modules.sort(switch (category.sortOrder) {
                        case WEIGHT -> Comparator.comparingInt((Module m) -> CustomCategoryManager.getModuleWeight(m, category)).thenComparing(m -> m.title);
                        case Z_TO_A -> Comparator.comparing((Module m) -> m.title).reversed();
                        default -> Comparator.comparing(m -> m.title);
                    });
                    modules.forEach(m -> w.add(theme.module(m)).expandX());
                }

                windows.add(w);
                cells.add(cell);
            } catch (Exception e) {
                NoraTweaks.LOG.error("[NoraTweaks] Failed to create category '{}'", category.name, e);
            }
        }
    }

    /** Open screens remain reachable through their widgets; closed screens can be collected. */
    private static final class CategoryUpdates {
        private final List<WeakReference<CustomCategoryHelper>> helpers = new ArrayList<>();
        private boolean subscribed;

        void add(CustomCategoryHelper helper) {
            if (!subscribed) {
                MeteorClient.EVENT_BUS.subscribe(this);
                subscribed = true;
            }
            helpers.removeIf(reference -> reference.get() == null);
            helpers.add(new WeakReference<>(helper));
        }

        @EventHandler
        private void onCategoriesChanged(CustomCategoriesChangedEvent event) {
            Minecraft.getInstance().execute(() -> {
                helpers.removeIf(reference -> reference.get() == null);
                for (WeakReference<CustomCategoryHelper> reference : helpers) {
                    CustomCategoryHelper helper = reference.get();
                    if (helper != null) helper.refresh();
                }
            });
        }
    }
}
