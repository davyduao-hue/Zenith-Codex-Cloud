#!/usr/bin/env python3
"""Publishable client/source bundle using the already verified Gradle outputs."""
import hashlib
import shutil
import zipfile
from pathlib import Path

root = Path(__file__).resolve().parents[1]
properties = dict(line.split('=', 1) for line in (root / 'gradle.properties').read_text().splitlines() if '=' in line and not line.startswith('#'))
version = properties['zenith_version']
output = root / 'downloads'
output.mkdir(exist_ok=True)
jar = output / 'Zenith-Client-2.0-MC26.2.jar'
source = output / 'Zenith-Client-2.0-Source.zip'
shutil.copyfile(root / f'build/libs/zenith-client-{version}+mc26.2.jar', jar)
shutil.copyfile(root / f'build/distributions/zenith-client-{version}+mc26.2-source-project.zip', source)
instructions = f'''Zenith Client 2.0 - update {version}

Install: Minecraft 26.2, Fabric Loader 0.19.3 or newer, Java 25.
Put Zenith-Client-2.0-MC26.2.jar in your Fabric profile's mods folder.
Replace your previous Zenith or Meteor JAR; keep only one installed.
Remove separate Catppuccin and Nora Tweaks addon JARs: their permitted source
is now built in. Glazed stays separate: install its official 26.2-n-17.2 JAR and
Fabric API 0.161.0+26.2 alongside Zenith. Do not add a separate Meteor JAR.
Glazed download: https://github.com/realnnpg/glazed/releases/tag/v17.2
Right Shift > GUI > Theme chooses Zenith 2.0, Catppuccin, or Meteor.
Zenith 2.0 uses rounded purple category windows; Layout > Dashboard restores
the previous full dashboard. HUD > Edit opens the widget editor.
Render > Zenith Effects > Visual preset selects Reference Crystal (red dusk),
Reference Sword (silver storm), or Zenith Purple. Select a preset after upgrading
to apply the new defaults to an existing settings profile.
The former radar is now Look Direction, a mint camera-motion dot with L/R counters.
Keys show press/release fills and expanding white rings, including short clicks.
HUD widgets default to double size; each widget's Size multiplier adjusts it.
Skybox > Sky source selects Zenith, Vanilla, Resource Pack or Custom Panorama.
See docs/SKYBOXES.md in the source for custom panorama resource packs.
Extra Effects adds impact crescents, blast comets, crystal energy funnels,
death stars and pearl spirals, enabled by default with individual toggles.
Extra effects strength changes their brightness; zero disables the extra layers.
Soft glow size/strength adjust the smooth shader lights.
Effect density and Quality control particle emission and performance.
Zenith Addons contains the custom combat modules; Nora Tweaks has its own category.

Build: extract Zenith-Client-2.0-Source.zip, open Command Prompt inside
Zenith-Client-2.0, and run gradlew.bat build with JDK 25 on PATH.
Find the installable JAR under build\\libs. Internet is needed for the first build.
The source archive includes README.md, GPL-3.0 LICENSE, NOTICE.md and third-party
licenses/provenance/dependency sources.
To build your own addon: open examples\\meteor-addon inside the source, then
run gradlew.bat build. Its README explains the entrypoint, settings and modules.
The example builds a separate JAR to put alongside Zenith; addons must target
Minecraft 26.2 and compatible Meteor APIs.

Source: https://github.com/davyduao-hue/Zenith-Codex-Cloud
'''
bundle = output / 'Zenith-Client-2.0-Download.zip'
with zipfile.ZipFile(bundle, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
    archive.write(jar, jar.name)
    archive.write(source, source.name)
    archive.writestr('INSTALL.txt', instructions)
files = [jar, source, bundle]
(output / 'SHA256SUMS').write_text(''.join(f'{hashlib.sha256(path.read_bytes()).hexdigest()}  {path.name}\n' for path in files))
for path in files:
    print(f'{path.name}: {path.stat().st_size:,} bytes')
