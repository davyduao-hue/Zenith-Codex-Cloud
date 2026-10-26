# Glazed alongside Zenith

Use **Zenith Client 2.1.0** and the matching files in one Fabric profile:

| Minecraft | Zenith download | Separate Glazed JAR | Fabric API | Minimum Loader |
| --- | --- | --- | --- | --- |
| 26.2 | `Zenith-Client-2.0-MC26.2.jar` | `glazed-26.2-n-17.2.jar` | `0.161.0+26.2` | `0.19.3` |
| 1.21.11 | `Zenith-Client-2.0-MC1.21.11.jar` | `glazed-1.21.11-n-17.2.jar` | `0.141.6+1.21.11` | `0.18.2` |

Download Glazed from the author's [v17.2 release](https://github.com/realnnpg/glazed/releases/tag/v17.2)
and Fabric API from its official distribution. Use **Java 25** for either Zenith build. Remove older Zenith versions and
any separate Meteor client. Zenith already provides the `meteor-client` mod ID
and addon entrypoint. Remove separate Nora Tweaks and Catppuccin addon JARs,
because their permitted source is built into Zenith. Keep every JAR matched to
your Minecraft version; the 1.21.11 and 26.2 builds are not interchangeable.

Glazed declares a dependency on the full Fabric API mod; Zenith's two embedded
Fabric API modules do not satisfy that dependency by themselves. Missing the full
API prevents Fabric from starting before Zenith can initialize.

## Compatibility changes

- Zenith and Catppuccin now inherit Meteor's settings factory, so Glazed can
  register its random-range and text-display setting editors in all three themes.
- The themed item/block previews use version-appropriate display stacks. This fixes
  `NullPointerException: Components not bound yet` when opening relevant Glazed
  module settings before joining a world.
- Nora's module is now **Nora Deepslate ESP** (`nora-deepslate-esp`). Glazed's
  **Deepslate ESP** keeps its own identity, and no longer removes Nora's instance. Zenith Aim Assist similarly keeps its own
  `zenith-aim-assist` ID alongside Glazed Aim Assist.
- Zenith retains its own TitleScreen-compatible main menu and theme selection.
  Glazed's background switcher does not replace Zenith's custom menu background.

The graphical harness opens Glazed's registered module settings under Meteor, Catppuccin and
Zenith, checks the external setting factories, and exercises an integrated world
with Glazed installed. This checks the integration surface; it does not establish
that every Glazed automation works on DonutSMP or other online servers.

Glazed stays a separately installed, unmodified upstream JAR. Its code, assets
and binary are excluded from Zenith's downloadable JAR/source/bundle.
