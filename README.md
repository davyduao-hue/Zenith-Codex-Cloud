# Zenith Client 2.0

A purple Minecraft **26.2 / Fabric / Java 25** client built from Meteor's GPL-3.0
source. Version **2.1.0** includes built-in Catppuccin and Nora Tweaks source,
a distinct purple Zenith theme with animated pill switches, a floating-star main menu, editable HUD,
and red-dusk/silver-storm atmosphere and combat visuals based on the recordings.

[Download the matching JAR or complete source ZIP](downloads/README.md).

A separate **Minecraft 1.21.11** build lives in [versions/1.21.11](versions/1.21.11).
Use its matching JAR and Java 25; from the repository root build it with
`gradlew.bat -p versions\1.21.11 build`. Each version has its own source ZIP.

Update 2.1 adds [ten optional modules and GUI motion controls](docs/NEW-MODULES.md),
including jump rings, damage numbers, viewmodel motion, armor warnings and waypoints.

## Build in Windows Command Prompt

Install a **64-bit JDK 25** (for example Eclipse Temurin 25), then open a new
Command Prompt in this folder:

```bat
java -version
gradlew.bat build
```

The wrapper downloads and verifies Gradle 9.6.1. Do not install Gradle separately.
The first build needs Internet access to resolve Minecraft, Fabric and libraries.

Install `build\libs\zenith-client-2.1.0+mc26.2.jar` in the `mods` folder of a
Minecraft **26.2** Fabric profile, using Fabric Loader **0.19.3 or newer** and
Java 25. Fabric's required resource modules and client libraries are included.
The `-sources.jar` is the corresponding source, not an installable client.
The same command also creates a complete buildable source ZIP under
`build\distributions\zenith-client-2.1.0+mc26.2-source-project.zip`.

This fork retains the internal `meteor-client` mod ID for compatibility with
Meteor's mixins and module code. Install it as a replacement for Meteor in that
profile; loading both creates a duplicate mod ID. Zenith saves its own settings
under `.minecraft/zenith-client`.

## Controls and settings

- **Right Shift:** open the selected theme. **GUI → Theme** offers **Zenith 2.0**,
  **Catppuccin**, and **Meteor**. Zenith uses rounded purple category windows and
  animated controls and category pages; Catppuccin has its original theme controls, and Meteor keeps
  its original widgets. Each theme saves its own colors, scale and window layout.
  Existing saved `Zenith` selections migrate to `Zenith 2.0`.
- **Zenith 2.0 → Layout:** use **Categories** for the native module windows or
  **Dashboard** for the previous sidebar/card layout. The dashboard's **Interface
  style** button opens the same GUI settings. Complex module settings keep their
  full native editor.
- **HUD tab:** edit, add, move or remove widgets. Every Zenith element now has
  **Size multiplier = 2** by default, including older profiles. Lower that setting
  for the previous size. Rounded corners, graph lines and circles have soft edges;
  keys and Look Direction stack above the graphs when the hotbar leaves too little room.
- **Render → Zenith Effects → Visual preset:** **Reference Crystal** selects the
  red dusk sky, orange embers and golden blast effects; **Reference Sword** selects
  silver storm/rain and short pale hit flashes; **Zenith Purple** selects the purple
  atmosphere. Presets apply when selected; individual controls remain editable.
  Existing settings are retained on upgrade, so select a preset to apply the new look.
- **Zenith Effects → Skybox → Sky source:** **Zenith**, **Vanilla**, **Resource Pack**
  or **Custom Panorama**. Vanilla and Resource Pack release sky, cloud and fog
  overrides while keeping the combat effects. Custom Panorama loads a 2:1 PNG
  from an enabled resource pack; see [custom sky instructions](docs/SKYBOXES.md).
  Missing images fall back to the native sky. Zenith sky orientation ignores
  player position, jump height, hurt animation and view bob. Set **Sky speed = 0**
  to freeze procedural cloud drift too.
- **Zenith Effects → Skybox:** **Reference Dusk** and **Reference Storm** use
  continuously shaded cloud detail. **Nebula**, **Aurora** and **Cloudscape** remain
  available. Brightness, cloud density and drift speed are adjustable. Vanilla
  block clouds stay visible in the reference presets. Auroras, shooting stars and
  the celestial halo are optional. The sky draws before terrain at far depth in
  dimensions with skylight; it follows camera rotation without position parallax.
- **Zenith Effects → Effects:** camera-facing hit rings and star rays, golden blast
  domes and floor bloom, crystal flares, bright pearl trails and arrival pulses,
  death visuals, orange ambient embers and silver rain. Extra orbitals and blast
  lightning remain optional. **Replace explosion smoke** removes native clouds
  that would cover the custom effect. Density and size obey the selected quality
  limits. Disabling cinematic rain restores the previous local weather.
- **Zenith Effects → Extra Effects:** crossed impact crescents, curved blast comets
  and delayed shockwaves, rising crystal energy funnels, a death star shower and
  crown, and twin pearl spirals. These layers default on and have independent
  toggles. **Extra effects strength** adjusts their brightness; zero disables them.
  **Energy blooms** adds rotating crystal floor patterns, blast ripples and pearl
  portals. Blast shards now have shaded facets and soft edges, and hit crescents
  cross on independently rotated axes. **Crystal comet trails** adds two inclined
  comet paths; **Blast spark shower** adds falling embers and fading ripples;
  **Pearl arrival burst** adds curved petal trails with faceted tips. Each defaults
  on and has its own toggle. **Soften overlapping blasts** reduces glare during
  simultaneous explosions and can be disabled for full intensity.
  The added ribbons and sprites share a per-frame ceiling of 128/384/704 for
  Low/Balanced/High, alongside the existing particle limits and terrain occlusion.
- **Zenith Addons:** custom Crystal Aura and Kill Aura with menu-pause and optional
  measured-FPS guards. Crystal profiles select a one-tick placement gap (Balanced),
  zero gap (Fast), or preserve manual settings. Kill Aura has a hold-to-attack option.
  Enabling either disables its matching base module, and vice versa. These modules
  reuse Meteor's combat engines; this update does not establish faster combat.
  The addon is bundled inside Zenith, with its own category and identity; no second
  JAR is required. Cosmetic effects stay inbuilt under Render.
- **Render → Small Totem:** independently scales totems in either hand.
- **Combat → Aimbot / Zenith Aim Assist:** range, field of view, turn speed, line of sight,
  and attack-only settings. Aimbot snaps the view; Aim Assist turns gradually.
  Both respect friends and stay disabled until enabled. Aimbot takes precedence
  when both are enabled. Bow Aimbot is also available from the Meteor base.

The eight default HUD widgets are a star/FPS/clock panel, memory graph,
frame-time graph, **Look Direction**, movement/mouse/CPS keys, potion rings,
target-health card, and compass. Look Direction is a mint dot driven by camera yaw
and pitch motion, with faint concentric circles and L/R click counters; it settles
back to the center when the camera stops. It retains the saved `zenith-radar` ID
so existing HUD layouts migrate. It does not track other players.

Keys use white press fills and expanding circular rings outside each tile,
including the jump bar. Short taps are latched so they remain visible between
rendered frames. The direction indicator and keys dock to the actual hotbar and
adapt to GUI scale. Dragging them disables docking; **Dock to hotbar** restores it.
The default size multiplier doubles their dimensions and text. Memory and performance
graphs use real JVM heap and frame samples. Target-health
statistics observe health loss following recent attacks.

## Built-in addon sources

**Catppuccin Addon** and **Nora Tweaks** are compiled into the client with their
original package names, source notices and permitted licenses. Remove their
separate addon JARs when installing this version. Nora's modules, commands and
category editor retain their own addon identity. Optional Baritone functions need
a compatible Baritone installation.

**Glazed stays a separate addon.** Use the official **Glazed 26.2-n-17.2** JAR
and **Fabric API 0.161.0+26.2** alongside Zenith. Get it from
[Glazed's release page](https://github.com/realnnpg/glazed/releases/tag/v17.2).
Do not install another Meteor JAR. Its license does not allow bundling it here.
Zenith's theme factory now accepts Glazed's custom controls; item/block settings
also open before joining a world. Nora's **Deepslate ESP** is now named
**Nora Deepslate ESP**, preventing Glazed from removing it during registration. Zenith Aim Assist also
keeps a separate `zenith-aim-assist` identity.
See [compatibility details](docs/GLAZED.md). Nora's upstream `LegitMaceKill`
module is also excluded because its copied Trouser-Streak source lacks a verified
redistribution license. Nora's automatic identity/addon telemetry is omitted.
Exact source revisions, modifications, licenses and dependency sources are recorded
in [third-party](third-party) and [NOTICE.md](NOTICE.md).

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
Compatible addons can register modules/categories and open the full settings
editor under all three themes. Changing the theme changes presentation;
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

**Offscreen culling** is enabled by default in Zenith Effects. It skips complete
crystal/burst groups and particle/trail work outside the camera view. Expanded
bounds preserve visible glow and trail edges, including with camera rotation.
The toggle is available if a custom camera or shader integration needs it.

Sky and custom-panorama meshes now stay in GPU buffers: camera movement changes
the transform, and animated reference skies update shader uniforms without
rebuilding the mesh. Other sky styles refresh vertex tint at five Hz. Quality
changes rebuild the required buffers; disabling the module releases them.

In the integrated test scene (eight crystals ahead and eight behind), culling
reduced rendered crystal effect groups from **16 to 8** and glow sprites from
**131 to 83**. The animated reference sky and rotating custom panorama required
**zero geometry rebuilds** while their settings stayed unchanged. These are
render-work measurements, not CPU/GPU utilization or hardware FPS measurements.

Balanced is the default. Effects use bounded storage and batched additive geometry
with world depth tests. Soft particle halos use one quad each with per-fragment Gaussian falloff and star
rays, batched into one extra depth-tested draw. **Soft glow size/strength** control
this glow; it does not require a fullscreen bloom pass. The sky uses a
separate fragment shader with at most 3/4/5 cloud-noise octaves for Low/Balanced/High,
plus a bounded dome mesh and optional stars. Catppuccin GUI blur is separately
configurable. Hardware FPS gains are not claimed.

**Zenith Effects → Economy mode** is optional. It limits sparks to 96, active bursts
to 8, tracked crystals/pearls to 8, trail length to 16, extra primitives to 64 per
frame, and effect range to 24 blocks. It preserves your quality selection and
restores it when switched off. Full visuals remain the default. Reused color
buffers, cached circle geometry and skipped unused weather/entity work also apply
with Economy off. This controls Zenith's work; it cannot enforce a universal 4%
CPU, GPU or RAM limit for Minecraft.

| Quality | Curve segments | Bursts | Sparks | Sky vertices | Stars |
| --- | ---: | ---: | ---: | ---: | ---: |
| Economy | 16 | 8 | 96 | 833 | 64 |
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

Linux equivalents use `./gradlew`. `build` compiles the client and runs
**31** camera-frustum, frame-history, input-animation, look-motion/docking, sky-noise and visual-budget tests. The optional graphical smoke run needs a display and uses
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
exit code. The test addon is excluded from the distributable JAR. The 2.1.0 run with Glazed passed **222**
checks, including the built-in addons, three themes, native library loading, actual
short key taps, look-dot motion/recentering, silver rain, both reference sky presets,
custom panoramas, sky-source switching and Glazed module settings in every theme.
Standalone runs passed **204** checks on each version. The 1.21.11 checks also
launch the packaged JAR through standard Fabric, both with and without Glazed. Checks exercise the additional
effect layers, strength/toggle controls, frame limits and cleanup. Performance
checks compare culling off/on, turn the camera, verify cached sky/panorama reuse,
change quality and disable/re-enable the effects. Render-work measurements are
written to `build/smoke/performance.json`.

To include the external addon integration checks, build the example first and
copy its JAR into `run-smoke/mods/` before running the smoke command. See the current
validation report for results with that separately loaded addon.

Actual Minecraft screenshots and the scope of the checks are in
[docs/VALIDATION.md](docs/VALIDATION.md).

For this cloud workspace, `bash scripts/setup-cloud.sh` prepares the verified
JDK and builds the project. Source `scripts/cloud-env.sh` before future Gradle
commands. Cloud tasks already have isolated checkouts; use the existing checkout
and do not create a Git worktree unless explicitly requested.

See [NOTICE.md](NOTICE.md) for upstream provenance and [LICENSE](LICENSE) for GPL-3.0.
