#!/usr/bin/env python3
"""Generate Zenith's original four-point star icon using only the standard library."""
import math
import struct
import zlib
from pathlib import Path

size = 128
rows = bytearray()
for y in range(size):
    rows.append(0)
    for x in range(size):
        u, v = abs(x + .5 - size/2)/46, abs(y + .5 - size/2)/46
        distance = u + v + 5*u*v
        coverage = min(1, max(0, (1-distance)*35 + .5))
        glow = math.exp(-max(0, distance-1)*2.5)*48
        alpha = min(255, int(coverage*255 + (1-coverage)*glow))
        core = max(0, 1-math.hypot(u,v)*2)
        mix = alpha/255
        rows.extend((int((175+core*70)*mix+18*(1-mix)),
                     int((107+core*124)*mix+11*(1-mix)),
                     int(255*mix+33*(1-mix)), 255))

def chunk(kind, data):
    return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind+data)&0xffffffff)

png = b'\x89PNG\r\n\x1a\n'
png += chunk(b'IHDR', struct.pack('>IIBBBBB', size, size, 8, 6, 0, 0, 0))
png += chunk(b'IDAT', zlib.compress(bytes(rows), 9)) + chunk(b'IEND', b'')
target = Path(__file__).resolve().parents[1]/'src/main/resources/assets/zenith/icon.png'
target.parent.mkdir(parents=True, exist_ok=True)
target.write_bytes(png)
