package dev.zenith.example;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

/** Add more modules using the same Module, settings and Orbit event APIs as Meteor. */
public final class ExampleCounter extends Module {
    private final SettingGroup general=settings.getDefaultGroup();
    private final Setting<Boolean> showCounter=general.add(new BoolSetting.Builder().name("show-counter")
        .description("Show the tick counter in module information.").defaultValue(true).build());
    private final Setting<Integer> step=general.add(new IntSetting.Builder().name("counter-step")
        .description("How much to add each tick.").defaultValue(1).range(1,10).sliderRange(1,10).build());
    private long ticks;
    public ExampleCounter(){super(ExampleAddon.CATEGORY,"example-counter","Example external addon: editable settings and a live Orbit event counter.");}
    @Override public void onActivate(){ticks=0;}
    @EventHandler private void onTick(TickEvent.Post event){ticks+=step.get();}
    @Override public String getInfoString(){return showCounter.get()?Long.toString(ticks):null;}
}
