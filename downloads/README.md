# Download Zenith Client 2.0

Current update: **2.0.5 for Minecraft 26.2** — Glazed compatibility fixes,
larger HUD, distinct animated purple controls, smooth shader glows, selectable sky
sources and camera-stable custom panoramas. Glazed remains a separate addon.

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

For Glazed, install its official **26.2-n-17.2** JAR plus **Fabric API 0.161.0+26.2**.
Download it from [Glazed's author](https://github.com/realnnpg/glazed/releases/tag/v17.2).
HUD widgets have a **Size multiplier**; **Zenith Effects → Skybox → Sky source**
selects Zenith, Vanilla, Resource Pack or Custom Panorama.
