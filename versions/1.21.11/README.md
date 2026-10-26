# Zenith Client 2.1 — Minecraft 1.21.11

This separate Fabric build includes the Zenith purple theme/menu, Catppuccin,
Nora Tweaks, eight editable HUD elements, procedural skies and effects, cached
sky geometry, offscreen culling, and ten new optional Zenith visual modules.

## Install and build

Use **Minecraft 1.21.11, Java 25 and Fabric Loader 0.18.2 or newer**. This build
uses Java 25 for the bundled integrations even though vanilla 1.21.11 supports
Java 21. Put only the matching Zenith client JAR in your profile's mods folder.
Replace the old Zenith/Meteor JAR. Remove separate Nora and Catppuccin JARs.

Open Command Prompt in this folder with JDK 25 on PATH, then run:

```bat
gradlew.bat build
```

The client is `build\libs\zenith-client-2.1.0+mc1.21.11.jar`.
The complete corresponding source archive is in `build\distributions`.
From the repository root, use `gradlew.bat -p versions\1.21.11 build`.
The first build needs Internet access; the Gradle wrapper installs Gradle.

Glazed stays separate: use **glazed-1.21.11-n-17.2.jar** and
**Fabric API 0.141.6+1.21.11**, both for 1.21.11.
Official Glazed: https://github.com/realnnpg/glazed/releases/tag/v17.2

Right Shift opens the GUI. GUI → Theme selects Zenith 2.0, Catppuccin or Meteor.
Zenith Addons contains the ten optional visual/utility modules. GUI → Zenith
controls motion speed, click pulses, header motion and reduced motion.
Render → Zenith Effects contains visual presets, sky-source selection,
custom panoramas, quality and Economy mode. HUD → Edit adjusts widget positions.
Settings are stored separately in `.minecraft/zenith-client-1.21.11`.

This is a version-specific port, not a universal JAR. Addons must target the
matching 1.21.11 Meteor APIs. Build the included external addon example after
building Zenith: open Command Prompt in `examples\meteor-addon`, then run
`gradlew.bat build`.

Source provenance and licensing: NOTICE.md, LICENSE and third-party/README.md.
