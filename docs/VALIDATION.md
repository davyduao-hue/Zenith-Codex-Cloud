# Validation — 2026-10-20

Update: 2.0.1. Target: Minecraft 26.2, Fabric Loader 0.19.3, Temurin JDK 25.0.4.1,
Gradle 9.6.1 and Fabric Loom 1.17.21.

| Check | Result |
| --- | --- |
| Client build | `bash scripts/setup-cloud.sh` / `./gradlew build` passed |
| Frame history, animation, radar and docking | 12 tests, 0 failures, 0 errors, 0 skipped |
| Graphical integration | 43 successful checks; `build/smoke/result.json` |
| Startup and mixins | Minecraft launched; development mixin audit completed |
| Menu, module UI, HUD editor | Rendered screenshots captured from Minecraft |
| Custom dashboard controls | Clicked enum, boolean and slider controls; searched and toggled a module; animated close completed |
| Local world | Integrated flat world created and joined |
| Auto Totem | Supplied totem equipped in Strict mode |
| Potion HUD | Server potion state received and rendered |
| Memory/performance HUD | Real heap and frame samples recorded |
| Hit effects | Target health loss produced a hit burst |
| Crystals | Crystal corona geometry rendered |
| Pearls | Thrown pearl produced trail samples |
| Explosions/deaths | Server events produced their respective effect bursts |
| Explosion replacement | Native explosion clouds were intercepted while the golden effect rendered |
| Zenith Addons | Custom Kill Aura attacked a local zombie; Crystal Aura paused newly spawned crystals in menus and resumed to break one; both aura pairs excluded their duplicate engines |
| Radar and keys | Player face marker rendered; W/RMB press fills, purple pearl ribbon and in-tile CPS captured |
| Performance limits | Balanced storage stayed within its 384-spark budget |
| JAR packaging | Metadata, entrypoints, mixins, 13 nested libraries, license, Java target and exclusion of the smoke addon verified |

The graphical checks use Xvfb and Mesa llvmpipe (software OpenGL). They validate
startup and rendering, not a hardware FPS comparison. Windows Command Prompt,
online multiplayer combat, and optional Sodium/Lithium combinations were not
executed here. These checks do not establish every inherited Meteor module's
behavior on every server.

The recordings and screenshot guided the layout and procedural effects. An exact
match to their texture packs, shader lighting and original assets has not been
established.

![Zenith menu](screenshots/menu.png)

![Custom purple dashboard](screenshots/modules.png)

![World HUD and hit effects](screenshots/world-hud.png)

![Pearl trail and held keys](screenshots/pearl-keys.png)

![Golden explosion effect](screenshots/explosion.png)

![Custom dashboard over the world HUD](screenshots/world-gui.png)
