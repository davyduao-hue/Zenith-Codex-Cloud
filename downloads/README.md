# Download Zenith Client 2.0

Current update: **2.2.1 for 26.2**, adding [Donut tools](../docs/DONUT.md).
The **1.21.11** download remains at **2.1.0**. Both builds include
ten optional visual modules, configurable GUI animations, purple themes, HUD,
procedural effects and selectable skyboxes. Glazed remains a separate addon.

| Minecraft | Installable JAR | Buildable source | Client + source bundle |
| --- | --- | --- | --- |
| 1.21.11 | [Download JAR](https://github.com/davyduao-hue/Zenith-Codex-Cloud/raw/refs/tags/v2.1.0/downloads/Zenith-Client-2.0-MC1.21.11.jar) | [Source ZIP](https://github.com/davyduao-hue/Zenith-Codex-Cloud/raw/refs/tags/v2.1.0/downloads/Zenith-Client-2.0-Source-MC1.21.11.zip) | [Bundle](https://github.com/davyduao-hue/Zenith-Codex-Cloud/raw/refs/tags/v2.1.0/downloads/Zenith-Client-2.0-Download-MC1.21.11.zip) |
| 26.2 | [Download JAR](https://github.com/davyduao-hue/Zenith-Codex-Cloud/raw/refs/tags/v2.2.1-mc26.2/downloads/Zenith-Client-2.2.1-MC26.2.jar) | [Source ZIP](https://github.com/davyduao-hue/Zenith-Codex-Cloud/raw/refs/tags/v2.2.1-mc26.2/downloads/Zenith-Client-2.2.1-MC26.2-Source.zip) | [Bundle](https://github.com/davyduao-hue/Zenith-Codex-Cloud/raw/refs/tags/v2.2.1-mc26.2/downloads/Zenith-Client-2.2.1-MC26.2-Bundle.zip) |

Use **Java 25** for both versions. Use Fabric Loader **0.18.2+ for 1.21.11** or
**0.19.3+ for 26.2**. Put the matching client JAR in your Fabric profile's `mods`
folder, replacing previous Zenith/Meteor. Remove separate Catppuccin and Nora
Tweaks JARs. Keep the two Minecraft versions in separate launcher profiles.

Right Shift opens settings. **GUI → Theme** chooses Zenith 2.0, Catppuccin or
Meteor. **Zenith Addons** contains the [ten new optional modules](../docs/NEW-MODULES.md).
**Zenith Effects → Skybox → Sky source** selects Zenith, Vanilla, Resource Pack
or Custom Panorama. HUD widgets have their own size controls.

Glazed requires its version-matched JAR and full Fabric API; see the
[compatibility instructions](../docs/GLAZED.md). It is excluded from these downloads.

Extract the source ZIP, open Command Prompt in its project folder, and run
`gradlew.bat build` with JDK 25 on PATH. Each source includes licenses and a separate
addon example. Checksums are in [SHA256SUMS](SHA256SUMS).

To reproduce repository downloads, build the chosen project, then run
`python scripts/verify-artifact.py --minecraft 1.21.11` and
`python scripts/package-downloads.py --minecraft 1.21.11` from the repository root.
Use `--minecraft 26.2` for the other version.

New releases use versioned filenames. Remove the old Zenith JAR when updating;
renaming does not permit two Zenith versions in the same mods folder.
Right Shift → UI controls HUD/menu/sidebar sizing.
