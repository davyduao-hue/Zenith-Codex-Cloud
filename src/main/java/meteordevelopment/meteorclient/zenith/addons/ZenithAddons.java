package meteordevelopment.meteorclient.zenith.addons;

import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;

/** Bundled addon identity; cosmetic effects remain in the main client. */
public final class ZenithAddons extends MeteorAddon {
    public static final Category CATEGORY=new Category("Zenith Addons");
    public static void install() {
        ZenithAddons addon=new ZenithAddons();addon.name="Zenith Addons";addon.authors=new String[]{"Zenith contributors"};
        addon.color.set(183,117,255,255);AddonManager.ADDONS.addFirst(addon);
    }
    @Override public String getPackage(){return "meteordevelopment.meteorclient.zenith.addons";}
    @Override public String getWebsite(){return "https://github.com/davyduao-hue/Zenith-Codex-Cloud";}
    @Override public void onRegisterCategories(){Modules.registerCategory(CATEGORY);}
    @Override public void onInitialize(){Modules.get().add(new ZenithCrystalAura());Modules.get().add(new ZenithKillAura());}
}
