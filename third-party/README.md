# Bundled source provenance

This distribution contains the following permitted upstream integrations. Zenith
changes are listed here; the original package names and available source copyright
headers are retained. The corresponding buildable source is shipped with the client.

| Integration | Source revision | License |
| --- | --- | --- |
| Catppuccin Addon | [X-C-0/catppuccin-addon](https://github.com/X-C-0/catppuccin-addon/tree/60720542dcea91d21f839f5135ee7b6303fe900d), `60720542dcea91d21f839f5135ee7b6303fe900d` | [GPL-3.0](catppuccin-addon/LICENSE) |
| Nora Tweaks | [noramibu/Nora-Tweaks](https://github.com/noramibu/Nora-Tweaks/tree/d1f86cd3a7be71f80bffe723fc63a0eef4e062a4), `d1f86cd3a7be71f80bffe723fc63a0eef4e062a4` | [CC0-1.0](nora-tweaks/LICENSE); inherited Meteor/meteor-rejects portions retain GPL-3.0 headers |
| Cubiomes Java bindings | [xpple/cubiomes](https://github.com/xpple/cubiomes/tree/bc70a69e7eee93a239f8d1c211548f95f5c0c8bd), `bc70a69e7eee93a239f8d1c211548f95f5c0c8bd` | [LGPL-3.0](cubiomes/LICENSE) |
| Cubiomes native library | [xpple/cubiomes](https://github.com/xpple/cubiomes/tree/b12a5325c8a195fa99711ed2052a4fc9cac077a8), `b12a5325c8a195fa99711ed2052a4fc9cac077a8` | [MIT](cubiomes/LICENSE_cubiomes.txt), [loot_library.h notice](cubiomes/LICENSE_loot_library.h.txt) |

## Changes for Zenith 2.0.5

Catppuccin source/resources and its mixins are compiled in. Theme registration is
centralized in `GuiThemes`, with the original Catppuccin theme and an independent
`ZenithGuiTheme` subclass. The subclass adds the purple palette and layout choice.
The renderer updates its current theme rather than caching a single theme forever.
The Zenith layout retains Catppuccin attribution alongside its own name. Zenith's
own module rows use sliding pill switches; a header-render extension point adds
the violet accent animation. Category pages prevent addon windows from overlapping. The settings factory inherits Meteor's default factory
to retain external addon setting registrations, and item previews use Meteor's
26.2-safe display stacks before joining a world.
Both built-in addons receive their own identity in `AddonManager`. External copies
are not required and should be removed from the mods directory.

Nora's source registers fifteen modules (with upstream duplicate checks for
Attribute Swapping and Better Locator) and five commands. Its custom category
support recognizes the built-in Catppuccin implementation. The category action is
inserted directly into both module settings screens instead of a multi-target
mixin, avoiding eager classloading and remapping failures. Optional Baritone mixins
are gated on the actual mod; the ore-simulation dependency is nested separately.
The automatic player identity/addon inventory telemetry and its configuration were
removed. No collector is compiled or started. Nora's Deepslate ESP has the unique
`nora-deepslate-esp` ID to avoid replacement by Glazed's separate module.

`LegitMaceKill` is omitted: upstream identifies its implementation as copied from
Trouser-Streak, whose source did not provide a verified redistribution license.
This omission does not remove Nora's distinct `MaceCombo` module.

**Glazed is not bundled.** The [published license](https://github.com/realnnpg/glazed/blob/6b6cab2a26f7a11985e6129ab51b4e909a2e9460/LICENSE)
expressly prohibits redistribution, modified distribution and incorporation into
software intended for distribution. No Glazed source or assets are included. Compatibility is tested with the separately
installed official `glazed-26.2-n-17.2.jar`; it is never copied into published bundles.

## Cubiomes source and replacement

The unmodified nested dependency is
`dev.xpple:cubiomes:0.0.0-2026.6.29.1782739186+b12a5325c8a195fa99711ed2052a4fc9cac077a8`.
The full Java and native source snapshots are included as
`cubiomes/cubiomes-java-source.zip` and `cubiomes/cubiomes-native-source.zip` in the
source distribution. To rebuild, extract the Java archive and the native archive;
place the native tree at the Java project's `src/main/c/cubiomes` submodule path,
and follow the included upstream README/build scripts. The Java binding generator
is the upstream `xpple/jextract` submodule.

Zenith uses the library through its public Java API. It remains a separate nested
JAR. To use a compatible modified version, replace the Cubiomes dependency in
`build.gradle.kts` with a local Maven coordinate or `jij(files("path/to/cubiomes.jar"))`,
then rebuild Zenith. Source redistribution does not limit reverse engineering for
debugging modifications to this LGPL library. The root GPL-3.0 text supplements
the LGPL-3.0 license as required.

Catppuccin's icon source credits are retained in `CatppuccinBuiltinIcons.java`
([Solar icons](https://icon-sets.iconify.design/solar/) and
[Iconsax](https://app.iconsax.io/)). The icons are used unchanged from the upstream
addon; Zenith does not claim their authorship.
