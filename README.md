# Zenith Client 2.0

A purple Minecraft **26.2 / Fabric / Java 25** client built from Meteor's GPL-3.0
source. Version **2.0.3** includes a custom animated rounded purple dashboard,
floating-star main menu, editable HUD, an animated procedural skybox, and layered combat visuals inspired by the supplied recordings.

## Build in Windows Command Prompt

Install a **64-bit JDK 25** (for example Eclipse Temurin 25), then open a new
Command Prompt in this folder:

```bat
java -version
gradlew.bat build
```

The wrapper downloads and verifies Gradle 9.6.1. Do not install Gradle separately.
The first build needs Internet access to resolve Minecraft, Fabric and libraries.

Install `build\libs\zenith-client-2.0.3+mc26.2.jar` in the `mods` folder of a
Minecraft **26.2** Fabric profile, using Fabric Loader **0.19.3 or newer** and
Java 25. Fabric's required resource modules and client libraries are included.
The `-sources.jar` is the corresponding source, not an installable client.
The same command also creates a complete buildable source ZIP under
`build\distributions\zenith-client-2.0.3+mc26.2-source-project.zip`.

This fork retains the internal `meteor-client` mod ID for compatibility with
Meteor's mixins and module code. Install it as a replacement for Meteor in that
profile; loading both creates a duplicate mod ID. Zenith saves its own settings
under `.minecraft/zenith-client`.

## Controls and settings

- **Right Shift:** open the custom dashboard. Left click a module to toggle it;
  right click to select it without toggling. The right pane edits toggles, sliders,
  enum values and key bindings. **Advanced editor** opens Meteor's full settings
  editor for color pickers, entity lists and other complex settings. **Ctrl+F**
  focuses search. Scroll each pane independently. Escape animates the dashboard closed.
- **Interface style** in the sidebar → **Theme: Meteor** selects Meteor's original
  category-window GUI, stock widget factory, scale and colors. **GUI tab → Theme:
  Zenith** switches back to the animated dashboard. The selection and each theme's
  colors, scale and window layout are saved independently. Addon-provided themes
  use their native screens. This setting controls the click GUI; HUD and effects
  have their own settings.
- **HUD editor** in the sidebar: drag any widget, set anchors, adjust scale, tint, opacity,
  text shadow and details, or add/remove widgets. Reset restores the Zenith layout.
- **Render → Zenith Effects:** toggle icy hit rings, golden crystal coronas and
  explosions, violet pearl ribbons, death visuals, sword animation, sky tint,
  starfield and local rain. **Effect palette** switches between video-inspired
  colors and Zenith purple. **Sky mood** selects Ember Dusk, Rainy Silver, Purple or Aurora.
  **Replace explosion smoke** removes vanilla explosion clouds so the custom glow
  stays visible; switch it off to keep both.
  Rain is optional and off by default. Disabling the module restores weather.
- **Zenith Effects → Skybox:** select **Nebula**, **Aurora** or **Cloudscape**.
  Adjust cloud density, brightness and animation speed; toggle aurora curtains,
  dense twinkling stars, shooting stars and a celestial halo. The sky uses native
  sky-pass rendering and reversed far depth, so terrain, glass, water and particles
  render over it. It follows camera rotation without world-position parallax and
  applies in dimensions with skylight. **Replace vanilla clouds** hides block clouds
  while the custom sky is active. Turn off **Animated skybox** for the simpler tint.
- **Zenith Effects → Effects:** independently toggle crossed sword slash arcs,
  crystal orbitals and spiraling cores, branching blast filaments, particle motion
  trails, ascending death spirals, ambient motes, rain streaks and surface ripples.
  Pearls leave a bright core, soft outer ribbon and orbiting sparks, then an arrival
  pulse when removed. **Effect density** defaults to 1.5; **Effect size** scales the
  visuals. Quality caps still apply at maximum density.
- **Zenith Addons:** custom Crystal Aura and Kill Aura with menu-pause and optional
  measured-FPS guards. Crystal profiles select a one-tick placement gap (Balanced),
  zero gap (Fast), or preserve manual settings. Kill Aura has a hold-to-attack option.
  Enabling either disables its matching base module, and vice versa. These modules
  reuse Meteor's combat engines; this update does not establish faster combat.
  The addon is bundled inside Zenith, with its own category and identity; no second
  JAR is required. Cosmetic effects stay inbuilt under Render.
- **Render → Small Totem:** independently scales totems in either hand.
- **Combat → Aimbot / Aim Assist:** range, field of view, turn speed, line of sight,
  and attack-only settings. Aimbot snaps the view; Aim Assist turns gradually.
  Both respect friends and stay disabled until enabled. Aimbot takes precedence
  when both are enabled. Bow Aimbot is also available from the Meteor base.

The eight default HUD widgets are a star/FPS/clock status panel, memory graph,
frame-time graph, circular player radar, movement/mouse/CPS keys, potion rings,
target-health card, and compass. Radar and keys dock to the actual hotbar, including
its GUI scale. At narrower resolutions they move above the hotbar to avoid the
corner graphs. Dragging either widget turns docking off; **Dock to hotbar** restores
it. **Reference colors** enables teal graphs/radar, white animated key fills and
pale green health. Radar supports real player head markers and a camera-facing
direction cone. Keys use smooth press/release fades, glow and expanding pulses,
with CPS inside the mouse tiles. They use actual client state. Target statistics
observe health changes after a recent attack; they are not server-authoritative
attribution of every hit. Additional Meteor HUD widgets remain available.

## Modules

| Requested feature | Implementation |
| --- | --- |
| Crystal Aura, Kill Aura | Zenith Addons controls on Meteor's complete 26.2 engines; base modules also available |
| Aimbot, Aim Assist | Zenith view controllers; do not automatically attack |
| ESP, Block ESP | Meteor entity/block renderers and configurable filters |
| Stash Finder | Meteor loaded-chunk storage scanner and results UI |
| Auto Totem | Meteor inventory/offhand logic |
| Small Totem | Zenith per-hand totem-only scaling |
| HUD settings | Eight Zenith elements in Meteor's persistent HUD editor |

The screenshots and videos are visual references, not supplied UI source or
shader assets. The effects are a new procedural implementation. A pixel-for-pixel
match, the recordings' lighting/texture pack, and identical FPS on your hardware
are not established by a successful build. No external shader pack is bundled.

## Making addons

The client retains the `meteor-client` mod ID, `meteor` Fabric addon entrypoint,
`MeteorAddon`, `Module`, `Category`, setting builders and Orbit event APIs.
Compatible addons can register modules/categories and open the full native Meteor
settings editor in either GUI mode. Changing the theme changes presentation;
addon compatibility depends on the Minecraft version and the APIs it uses.
Addons for other Minecraft versions, strict dependency ranges, or changed mixin
internals need adaptation. Keep only one Zenith/Meteor client JAR installed.

A working separate addon project is included in
[examples/meteor-addon](examples/meteor-addon/README.md). After building Zenith:

```bat
cd examples\meteor-addon
gradlew.bat build
```

Put its `build\libs\zenith-example-addon-1.0.0.jar` alongside Zenith in `mods`.
The **Example Addon** category contains **Example Counter**, with a boolean,
integer slider and an Orbit tick handler. Rename the ID/package/category and add
modules in `ExampleAddon.onInitialize()` to make your own addon. The example
compiles against the local built client JAR, so it targets this Minecraft 26.2 API
rather than a different Meteor release. The example is not bundled into Zenith.

## Performance

Balanced is the default. Effects use bounded storage and batched additive geometry
with world depth tests. There is no full-screen bloom or blur pass. The sky samples
seeded continuous volume noise on a cached dome at most five times per second;
auroras, twinkles and shooting stars animate each frame without per-pixel cloud noise.

| Quality | Ring segments | Bursts | Sparks | Sky vertices | Stars |
| --- | ---: | ---: | ---: | ---: | ---: |
| Low | 24 | 16 | 256 | 833 | 128 |
| Balanced | 40 | 32 | 768 | 2,813 | 384 |
| High | 64 | 48 | 1,536 | 4,773 | 768 |

Lowering quality trims active storage on the next tick and resizes the sky mesh.
Effects have distance limits and short lifetimes; tracked projectiles/crystals,
trail length, rain samples and frame history remain bounded. Surface ripples use
cached terrain heights. Disabling cinematic rain restores the previous local rain
and thunder levels. FPS summaries update twice a second.

The graph plots frame duration in milliseconds. Average FPS is based on total
elapsed sample time; 1% low is calculated from the slowest 1% of recent frames.
The memory graph measures used JVM heap against maximum heap. Neither graph
contains invented readings. Compatible Sodium 0.9.1+ for 26.2 and Lithium 0.25.2
can be installed separately; their runtime combination needs separate validation.

## Development and validation

```bat
gradlew.bat build
gradlew.bat runClient
gradlew.bat runSmokeClient -PzenithSmoke
```

Linux equivalents use `./gradlew`. `build` compiles the client and runs eighteen
frame-history, animation, radar/docking, continuous sky-noise and visual-budget tests. The optional graphical smoke run needs a display and uses
its own ignored `run-smoke` directory. It checks module registration, the menu,
GUI and HUD editor, then creates a uniquely named local flat world to exercise
HUD sampling, totems, potions, pearls, explosions and death rendering, plus custom
GUI interactions, addon attacks, duplicate-engine exclusion and menu pausing. It also
checks switching between native Meteor and Zenith, persistent theme selection,
upstream Meteor defaults, and (when loaded) a separate Fabric addon's category,
module, settings editor and live event handlers. It checks sky geometry at all three
quality levels, live quality changes, shooting
stars, rain restoration, pearl arrival pulses and cleanup when disabled. Results
and screenshots go to `build/smoke`; inspect `result.json` as well as the process
exit code. The test addon is excluded from the distributable JAR.

To include the external addon integration checks, build the example first and
copy its JAR into `run-smoke/mods/` before running the smoke command. The current
73-check run includes that separately loaded addon.

Actual Minecraft screenshots and the scope of the checks are in
[docs/VALIDATION.md](docs/VALIDATION.md).

For this cloud workspace, `bash scripts/setup-cloud.sh` prepares the verified
JDK and builds the project. Source `scripts/cloud-env.sh` before future Gradle
commands. Cloud tasks already have isolated checkouts; use the existing checkout
and do not create a Git worktree unless explicitly requested.

See [NOTICE.md](NOTICE.md) for upstream provenance and [LICENSE](LICENSE) for GPL-3.0.
