#!/usr/bin/env bash
# Source this file; it only activates workspace-local tooling.
export JAVA_HOME=/workspace/.tools/jdk-25.0.4.1+1
export PATH="$JAVA_HOME/bin:$PATH"
export GRADLE_USER_HOME=/workspace/.gradle
export MESA_SHADER_CACHE_DIR=/workspace/.tools/mesa-cache
zenith_proxy_args=$(python3 - <<'PY'
import os, urllib.parse
proxy = urllib.parse.urlsplit(os.environ.get('HTTPS_PROXY', ''))
if proxy.hostname:
    print('-Dhttps.proxyHost=' + proxy.hostname + ' -Dhttps.proxyPort=' + str(proxy.port or 80)
          + ' -Dhttp.proxyHost=' + proxy.hostname + ' -Dhttp.proxyPort=' + str(proxy.port or 80)
          + ' -Dhttp.nonProxyHosts=localhost|127.*')
PY
)
export GRADLE_OPTS="${GRADLE_OPTS:-} $zenith_proxy_args -Djavax.net.ssl.trustStore=$JAVA_HOME/lib/security/cacerts"
unset zenith_proxy_args
