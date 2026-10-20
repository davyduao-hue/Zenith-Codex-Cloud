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
Right Shift > Interface style > Theme: Meteor opens the original Meteor GUI.
GUI tab > Theme: Zenith restores the custom animated dashboard.
Zenith Addons contains
the custom combat modules; Render > Zenith Effects controls the visuals.
HUD editor in the sidebar edits radar, key tiles and graphs.
Render > Zenith Effects > Skybox selects Nebula/Aurora/Cloudscape and
controls auroras, stars, shooting stars, cloud density and brightness.
Effect density and Quality control particle emission and performance.

Build: extract Zenith-Client-2.0-Source.zip, open Command Prompt inside
Zenith-Client-2.0, and run gradlew.bat build with JDK 25 on PATH.
Find the installable JAR under build\\libs. Internet is needed for the first build.
The source archive includes README.md, GPL-3.0 LICENSE and NOTICE.md.
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
