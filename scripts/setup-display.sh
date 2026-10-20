#!/usr/bin/env bash
# Optional local virtual display for cloud smoke tests; no system package installation.
set -euo pipefail
zenith_tools=/workspace/.tools
zenith_display_bin="$zenith_tools/xvfb/usr/bin/Xvfb"
if command -v Xvfb >/dev/null 2>&1; then
    zenith_display_bin=$(command -v Xvfb)
elif ! test -x "$zenith_display_bin"; then
    zenith_apt="$zenith_tools/apt"
    mkdir -p "$zenith_apt/lists/partial" "$zenith_apt/cache/archives/partial" "$zenith_tools/downloads"
    printf '%s\n' 'deb [signed-by=/usr/share/keyrings/debian-archive-keyring.gpg] https://deb.debian.org/debian trixie main' > "$zenith_apt/sources.list"
    zenith_apt_options=(-o "Dir::Etc::sourcelist=$zenith_apt/sources.list" -o Dir::Etc::sourceparts=- \
        -o "Dir::State::lists=$zenith_apt/lists" -o "Dir::Cache=$zenith_apt/cache")
    apt-get "${zenith_apt_options[@]}" update
    (
        cd "$zenith_tools/downloads"
        apt-get "${zenith_apt_options[@]}" download xvfb
        for zenith_deb in ./xvfb_*.deb; do dpkg-deb -x "$zenith_deb" "$zenith_tools/xvfb"; done
    )
fi
if ldd "$zenith_display_bin" | rg 'not found'; then
    printf '%s\n' 'Xvfb has a missing native dependency; inspect the ldd output.' >&2
    exit 1
fi
if ! test -S /tmp/.X11-unix/X98; then
    "$zenith_display_bin" :98 -screen 0 1280x720x24 -nolisten tcp > "$zenith_tools/xvfb.log" 2>&1 &
    printf '%s\n' "$!" > "$zenith_tools/xvfb.pid"
fi
DISPLAY=:98 python3 - <<'PY'
import ctypes, time
lib = ctypes.CDLL('libX11.so.6')
lib.XOpenDisplay.restype = ctypes.c_void_p
lib.XCloseDisplay.argtypes = [ctypes.c_void_p]
for _ in range(20):
    display = lib.XOpenDisplay(b':98')
    if display:
        lib.XCloseDisplay(display)
        print('Display :98 accepts X11 connections.')
        break
    time.sleep(.1)
else:
    raise SystemExit('Display :98 did not become ready; inspect /workspace/.tools/xvfb.log')
PY
