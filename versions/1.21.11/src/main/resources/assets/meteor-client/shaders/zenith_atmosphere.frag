#version 330 core

// Original procedural atmosphere, drawn in the native sky pass before terrain.
// The recordings use a red horizon/dark cloud ceiling and a separate silver storm.
// This has no downloaded texture, no longitude seam, and no screen-space camera swim.
layout (std140) uniform Atmosphere {
    vec4 u_Atmosphere; // time, style (0 dusk / 1 storm / >=2 other), brightness, density
    vec4 u_Detail;     // noise octaves, reserved
};
in vec3 v_Direction;
in vec4 v_Color;
out vec4 color;

float hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}
float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1, 0)), u.x),
               mix(hash(i + vec2(0, 1)), hash(i + vec2(1)), u.x), u.y);
}
float cloud(vec2 p) {
    float sum = 0.0, amplitude = 0.55, weight = 0.0;
    // Constant upper bound; Low/Balanced/High select 3/4/5 octaves.
    for (int i = 0; i < 5; ++i) {
        if (float(i) >= u_Detail.x) break;
        sum += noise(p) * amplitude;
        weight += amplitude;
        p = mat2(1.62, 1.18, -1.18, 1.62) * p + vec2(17.1, -9.3);
        amplitude *= 0.48;
    }
    return sum / weight;
}
void main() {
    vec3 d = normalize(v_Direction);
    float height = max(d.y, 0.0);
    float time = u_Atmosphere.x;
    float style = u_Atmosphere.y;
    float density = u_Atmosphere.w;
    // Intersect two distant cloud planes: a wide low bank and a finer upper layer.
    vec2 p = d.xz / (height + 0.16);
    vec2 drift = vec2(time * 0.008, -time * 0.004);
    float lower = cloud(p * 1.2 + drift);
    float upper = cloud(p * 3.0 - drift * 0.7 + vec2(15.0, 3.0));
    float body = smoothstep(0.34, 0.73, lower + (density - 1.0) * 0.14);
    float wisps = smoothstep(0.43, 0.72, upper);
    float horizon = exp(-height * 8.5);
    vec3 rgb;
    if (style < 0.5) {
        // Saturated molten-orange horizon, crimson bank, dark wine ceiling.
        rgb = mix(vec3(0.105, 0.029, 0.043), vec3(1.0, 0.17, 0.012), horizon);
        float litEdge = smoothstep(0.34, 0.51, lower) * (1.0 - smoothstep(0.51, 0.66, lower));
        vec3 cloudColor = mix(vec3(0.115, 0.035, 0.047), vec3(0.32, 0.042, 0.021), horizon);
        cloudColor += vec3(0.32, 0.025, 0.006) * litEdge * horizon;
        rgb = mix(rgb, cloudColor, body * smoothstep(0.0, 0.14, height) * 0.88);
        rgb += vec3(0.08, 0.023, 0.023) * wisps * (1.0 - horizon) * density;
        // Distant glow remains below the cloud bank, rather than a giant planet.
        rgb += vec3(0.24, 0.043, 0.0) * exp(-height * 34.0);
    } else if (style < 1.5) {
        rgb = mix(vec3(0.19, 0.205, 0.24), vec3(0.65, 0.68, 0.72), horizon);
        vec3 cloudColor = mix(vec3(0.285, 0.30, 0.33), vec3(0.50, 0.52, 0.56), horizon);
        rgb = mix(rgb, cloudColor, body * 0.85);
        rgb += vec3(0.075, 0.077, 0.08) * wisps * density;
        // Soft directional opening in the cloud deck.
        float opening = pow(max(dot(d, normalize(vec3(-0.4, 0.65, 0.7))), 0.0), 8.0);
        rgb += vec3(0.13, 0.14, 0.15) * opening * (1.0 - body * 0.6);
    } else {
        // Existing mood colors supply the nebula/aurora base; fragment clouds add detail.
        rgb = v_Color.rgb * mix(0.77, 1.14, lower);
        rgb += vec3(0.11, 0.065, 0.15) * wisps * density * smoothstep(0.04, 0.3, height);
    }
    // Fade below the horizon into the native sky/fog, not onto terrain.
    float alpha = smoothstep(-0.055, 0.018, d.y);
    color = vec4(clamp(rgb * u_Atmosphere.z, 0.0, 1.0), alpha);
}
