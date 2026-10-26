package dev.zenith.example;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;

public final class ExampleAddon extends MeteorAddon {
    public static final Category CATEGORY=new Category("Example Addon");
    @Override public String getPackage(){return "dev.zenith.example";}
    @Override public void onRegisterCategories(){Modules.registerCategory(CATEGORY);}
    @Override public void onInitialize(){Modules.get().add(new ExampleCounter());}
}
