#!/usr/bin/env python3
"""Check the deliverable rather than treating a Gradle exit code as proof of packaging."""
import io
import json
import struct
import zipfile
from pathlib import Path

root = Path(__file__).resolve().parents[1]
properties = dict(line.split('=', 1) for line in (root / 'gradle.properties').read_text().splitlines() if '=' in line and not line.startswith('#'))
artifact = root / f'build/libs/zenith-client-{properties["zenith_version"]}+mc26.2.jar'
with zipfile.ZipFile(artifact) as jar:
    names = set(jar.namelist())
    metadata = json.loads(jar.read('fabric.mod.json'))
    assert metadata['name'] == 'Zenith Client 2.0'
    assert metadata['depends']['java'] == '>=25'
    assert metadata['depends']['minecraft'] == '~26.2'
    assert '${' not in jar.read('fabric.mod.json').decode()
    assert any(n.startswith('LICENSE_') for n in names)
    assert 'NOTICE.md' in names
    assert not any('/zenith/smoke/' in n for n in names), 'Test addon leaked into client'
    for name in ['ZenithGuiScreen', 'ZenithGuiTheme', 'ZenithDraw', 'UiMotion', 'VisualBudget', 'SkyPattern', 'ZenithSkybox', 'addons/ZenithAddons', 'addons/ZenithCrystalAura', 'addons/ZenithKillAura']:
        assert f'meteordevelopment/meteorclient/zenith/{name}.class' in names, name
    assert not any(n.startswith('dev/zenith/example/') for n in names), 'Example addon was bundled into client'
    assert 'assets/meteor-client/shaders/zenith_sky.vert' in names
    assert 'uniform Projection' in jar.read('assets/meteor-client/shaders/zenith_sky.vert').decode()
    for entry in metadata['entrypoints']['client']:
        assert entry.replace('.', '/') + '.class' in names
    for config_name in metadata['mixins']:
        config = json.loads(jar.read(config_name))
        for name in config.get('client', []) + config.get('mixins', []):
            assert (config['package'] + '.' + name).replace('.', '/') + '.class' in names
    for name in names:
        if name.endswith('.class'):
            major = struct.unpack('>H', jar.read(name)[6:8])[0]
            assert major <= 69, (name, major)
    nested = metadata.get('jars', [])
    assert nested, 'Runtime libraries are missing'
    for item in nested:
        with zipfile.ZipFile(io.BytesIO(jar.read(item['file']))) as dep:
            assert dep.testzip() is None, item['file']
    assert jar.testzip() is None
print(f'Verified {artifact.name}: metadata, entrypoints, mixins, Java target, license, {len(nested)} nested JARs, no test addon.')
