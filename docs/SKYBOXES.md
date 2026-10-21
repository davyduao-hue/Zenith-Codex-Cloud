# Choosing a sky

Open **Right Shift → Render → Zenith Effects → Skybox → Sky source**.
The effects module must be enabled. Sky replacements apply in dimensions with skylight.

| Source | Behavior |
| --- | --- |
| Zenith | Procedural dusk, storm, nebula, aurora or cloudscape; adjustable brightness, density and drift |
| Vanilla | Leaves Minecraft's sky, fog and clouds alone; enabled packs can still replace vanilla assets |
| Resource Pack | Also leaves the sky to Minecraft and compatible external sky renderers, without Zenith's tint |
| Custom Panorama | Maps a 2:1 equirectangular PNG from an enabled resource pack around the camera |

The custom sky uses camera rotation only: walking, jumping, view bob and damage
animation do not translate it. Looking around still rotates your view normally.
**Sky speed = 0** freezes Zenith's procedural drift; custom panoramas are static.
**Sky rotation** adjusts a panorama's heading. **Replace vanilla clouds** removes
Minecraft's block clouds when a custom sky is available.

Vanilla and Resource Pack are intentionally passthrough choices: neither disables
your selected packs. Zenith cannot interpret every third-party sky format. A pack
using OptiFine-style `sky.properties`, cubemap atlases or shader-specific sky code
needs a compatible renderer for Minecraft 26.2, or conversion to the panorama
format below. Resource Pack mode leaves that renderer in control.

## Custom panorama pack

Use your own **2:1 equirectangular** image, for example 2048×1024 or 4096×2048.
A 3×2 cubemap atlas is a different projection and must be converted first.

Create this directory layout, then zip its contents with `pack.mcmeta` at the root:

```text
MySky/
  pack.mcmeta
  assets/
    zenith/
      textures/
        sky/
          panorama.png
```

For Minecraft 26.2, `pack.mcmeta` can contain:

```json
{
  "pack": {
    "description": "My Zenith panorama",
    "min_format": 88,
    "max_format": 88
  }
}
```

Place the ZIP or folder in `.minecraft/resourcepacks`, enable it under Minecraft's
Resource Packs screen, and select **Custom Panorama** in Zenith. Set
**Panorama texture** to `zenith:textures/sky/panorama.png` (the default).
Other namespaces and paths work too, such as `mysky:textures/night.png`.
The usual pack priority determines which texture is selected. Resource reloads
use the newly loaded image. Missing/invalid resource locations fall back to the
native sky; the file is never fetched from the web by this setting.

Only the image comes from the pack. Effects, HUD and GUI remain separately configurable.
