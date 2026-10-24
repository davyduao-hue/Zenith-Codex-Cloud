# Glazed alongside Zenith

Use these files in the **same Minecraft 26.2 Fabric profile**:

1. Zenith Client **2.0.9**.
2. The author's official **`glazed-26.2-n-17.2.jar`** from
   [Glazed v17.2](https://github.com/realnnpg/glazed/releases/tag/v17.2).
3. **Fabric API 0.161.0+26.2** from the official Fabric API distribution.

Use Java 25 and Fabric Loader 0.19.3 or newer. Remove older Zenith versions and
any separate Meteor client. Zenith already provides the `meteor-client` mod ID
and addon entrypoint. Remove separate Nora Tweaks and Catppuccin addon JARs,
because their permitted source is built into Zenith. Glazed's 1.21.x and 26.1.2
JARs target different Minecraft APIs and are not interchangeable with its 26.2 JAR.

Glazed declares a dependency on the full Fabric API mod; Zenith's two embedded
Fabric API modules do not satisfy that dependency by themselves. Missing the full
API prevents Fabric from starting before Zenith can initialize.

## Compatibility changes

- Zenith and Catppuccin now inherit Meteor's settings factory, so Glazed can
  register its random-range and text-display setting editors in all three themes.
- The themed item/block previews use 26.2's menu-safe display stacks. This fixes
  `NullPointerException: Components not bound yet` when opening relevant Glazed
  module settings before joining a world.
- Nora's module is now **Nora Deepslate ESP** (`nora-deepslate-esp`). Glazed's
  **Deepslate ESP** keeps its own identity, and no longer removes Nora's instance. Zenith Aim Assist similarly keeps its own
  `zenith-aim-assist` ID alongside Glazed Aim Assist.
- Zenith retains its own TitleScreen-compatible main menu and theme selection.
  Glazed's background switcher does not replace Zenith's custom menu background.

The graphical harness opens all 80 modules' settings under Meteor, Catppuccin and
Zenith, checks the external setting factories, and exercises an integrated world
with Glazed installed. This checks the integration surface; it does not establish
that every Glazed automation works on DonutSMP or other online servers.

Glazed stays a separately installed, unmodified upstream JAR. Its code, assets
and binary are excluded from Zenith's downloadable JAR/source/bundle.
