# Minecraft 1.21.11 validation — 2026-10-26

Zenith 2.1.0 was built with JDK 25, Gradle 9.2.0, Loom 1.14.10 and Fabric Loader 0.18.2.
All 31 unit tests passed. The remapped client JAR was launched through the standard
Fabric Knot launcher using the original Minecraft client and its runtime libraries.

- 222 checks passed with separately installed Glazed 1.21.11-n-17.2, Fabric API
  0.141.6+1.21.11 and the built example addon.
- 204 checks passed without Glazed/full Fabric API; the separate example remained.
- All three themes, all 80 Glazed settings screens, native Cubiomes loading,
  main-menu restoration, HUD/input feedback, sky sources, world effects and all
  ten optional visual modules were exercised.
- Culling reduced the controlled scene from 16 to 8 crystal effect groups and
  131 to 83 glow sprites; stable sky/panorama settings caused zero mesh rebuilds.
- The installable client excludes Glazed and all smoke/example test addon code.

Runs used Linux, Xvfb and Mesa software OpenGL. These results do not establish
hardware FPS gains, a 4% utilization cap, online-server behavior or identical
pixels to the reference recordings. Windows Command Prompt and optional
Sodium/Lithium/Baritone combinations were not executed.

Full report and screenshots: https://github.com/davyduao-hue/Zenith-Codex-Cloud/blob/main/docs/VALIDATION.md
