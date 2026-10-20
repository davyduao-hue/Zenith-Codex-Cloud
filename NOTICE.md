# Source provenance and license

Zenith Client 2.0 is an independent, modified distribution of Meteor Client,
licensed under GNU GPL version 3. The complete license is in LICENSE. Preserve
the license, copyright notices, and corresponding source when distributing builds.
Zenith is not an official Meteor Development release.

Upstream: https://github.com/MeteorDevelopment/meteor-client

Imported revision: `2385f7b4bb9de1a698b4503254bd32f30fd7a87d` (Minecraft 26.2,
the parent of upstream's 26.3 update). Upstream copyright headers are retained.
No upstream Git history or credentials were copied into this repository.

Changes by Zenith contributors, 2026-10-20:

- Purple GUI theme, compact module rows, animated main menu and world-loading star.
- Eight configurable, draggable HUD widgets with measured frame and heap history.
- Bounded local hit, explosion, crystal, pearl, death, and sky effects.
- Aimbot, Aim Assist, and Small Totem modules integrated with the existing module system.
- Separate Zenith configuration directory and removal of upstream online-player pings.
- Pinned build tooling, Windows wrapper, tests, cloud setup and integration harness.

The existing combat, ESP, inventory, and world modules retain their upstream
implementations; no comparative performance improvement is claimed for them.

The dependency JARs retain their own notices and licenses. This repository does
not contain Minecraft game assets, the reference videos, external texture packs,
or third-party shader packs. Gradle downloads Minecraft dependencies from the
official distribution services for building and development.
