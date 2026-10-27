# Zenith Donut tools — 26.2 / update 2.2.0

Open **Right Shift → Zenith Donut**. Enable **Zenith Donut Panel** to open the
purple quick-settings sidebar; bind it to a key in its module settings.
Each row toggles a tool; `...` opens its complete native settings editor.
Fullbright, Auto Totem and Stash Finder reuse the existing Meteor implementations.

- **Chunk Finder:** highlights loaded chunks containing at least three storage
  block entities (adjustable). An optional incremental underground scan detects
  concentrations of cobbled/crafted deepslate or obsidian. Sampling defaults to
  4,096 blocks per tick, with four loaded storage chunks inspected every two ticks.
  It never loads chunks, and keeps only nearby results. A full scan takes multiple
  ticks; natural structures can produce false positives. These are clues, not
  proof of player activity, hidden bases or chunk age.
- **Chunk Region:** outlines your current chunk and 512-block region. A configurable
  HUD grid shows neighboring region coordinates and your position/heading.
  Negative coordinates use floor division. These are Minecraft region coordinates;
  DonutSMP-specific rank/media region labels await the user's map reference.
- **RTPer:** sends the configurable command (default `rtp`) while enabled, no more
  frequently than the selected interval (default 45 seconds, minimum 15). It stops
  at the request limit (default ten). Operate any server selection menus manually;
  by default command requests pause while menus are open.
- **RTP Base Finder:** the same bounded retry loop, stopping when your horizontal
  distance from 0,0 is within the configured radius (default 1,000 blocks). Only
  one Zenith RTP module can run at a time. Server randomness and cooldowns remain
  authoritative; this cannot force a destination or guarantee proximity to 0,0.
- **Fake Ban Screen:** customizable local preview with a Return button. It leaves
  the multiplayer connection intact and identifies itself as a local preview.

All tools start disabled. Findings clear on disable/world changes. These features
are original Zenith code, with no Glazed/Prestige source or assets incorporated.
The supplied Glazed ZIP still prohibits redistribution and incorporation. Glazed
remains separately installed, using its matching 26.2 JAR and Fabric API.

Screenshots for the requested DonutSMP layout were not supplied in this update.
This implements the named workflows, not every Glazed feature or an exact copy of
its detection algorithms/map. The older 1.21.11 release remains available, but
these additions target only Minecraft 26.2.

Validation: 33 unit tests and 234 Glazed-enabled graphical checks passed. Live
DonutSMP command behavior and real-base detection accuracy have not been tested.
