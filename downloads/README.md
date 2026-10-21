# Download Zenith Client 2.0

Current update: **2.0.4 for Minecraft 26.2** — built-in Catppuccin and Nora Tweaks,
selectable Zenith 2.0 / Catppuccin / Meteor themes, restored title-screen integration,
camera-direction dot, white key-press rings, red-dusk/silver-storm skies and revised
reference combat effects. Glazed is excluded because its license forbids distribution.

- [Client JAR](https://github.com/davyduao-hue/Zenith-Codex-Cloud/raw/refs/heads/main/downloads/Zenith-Client-2.0-MC26.2.jar)
- [Buildable source ZIP](https://github.com/davyduao-hue/Zenith-Codex-Cloud/raw/refs/heads/main/downloads/Zenith-Client-2.0-Source.zip)
- [Client and source bundle](https://github.com/davyduao-hue/Zenith-Codex-Cloud/raw/refs/heads/main/downloads/Zenith-Client-2.0-Download.zip)

Requires Minecraft 26.2, Fabric Loader 0.19.3+ and Java 25. Put the client JAR in your Fabric profile's mods folder, replacing any separate Meteor Client JAR. Right Shift opens settings. The source ZIP includes GPL-3.0 licensing and full build instructions.

Checksums are in [SHA256SUMS](SHA256SUMS). To reproduce these downloads from
source, run the build, `python scripts/verify-artifact.py`, then
`python scripts/package-downloads.py`.

Select **Right Shift → GUI → Theme** for Zenith 2.0, Catppuccin or Meteor.
Remove separate Catppuccin and Nora Tweaks JARs. Under **Zenith Effects**, choose
**Visual preset → Reference Crystal / Reference Sword** to apply the new look.
A buildable addon example is included under `examples/meteor-addon`.
