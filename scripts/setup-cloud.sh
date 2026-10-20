#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
zenith_tools=/workspace/.tools
zenith_jdk="$zenith_tools/jdk-25.0.4.1+1"
if ! test -x "$zenith_jdk/bin/javac"; then
    mkdir -p "$zenith_tools/downloads"
    zenith_archive="$zenith_tools/downloads/jdk25.tar.gz"
    curl --fail --location --silent --show-error --retry 2 \
        'https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25.0.4.1%2B1/OpenJDK25U-jdk_x64_linux_hotspot_25.0.4.1_1.tar.gz' \
        --output "$zenith_archive"
    printf '%s  %s\n' dbb698396d478e7fa2b1e50f4103324b2a99b90569ee27c33f2261f9215cf41e "$zenith_archive" | sha256sum --check -
    tar -xzf "$zenith_archive" -C "$zenith_tools"
fi
# Trust the provided platform CA without disabling TLS or certificate checks.
if test -n "${CODEX_PROXY_CERT:-}" && test -f "$CODEX_PROXY_CERT"; then
    if ! "$zenith_jdk/bin/keytool" -list -cacerts -storepass changeit -alias zenith-cloud-proxy >/dev/null 2>&1; then
        "$zenith_jdk/bin/keytool" -importcert -noprompt -trustcacerts -alias zenith-cloud-proxy \
            -file "$CODEX_PROXY_CERT" -cacerts -storepass changeit
    fi
fi
source scripts/cloud-env.sh
./gradlew build --no-daemon --console=plain
