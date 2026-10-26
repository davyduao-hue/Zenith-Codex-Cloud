#version 330 core
in vec3 v_Direction;
in vec4 v_Color;
uniform sampler2D u_Texture;
out vec4 fragColor;
void main() {
    vec3 d=normalize(v_Direction);
    vec2 uv=vec2(atan(d.z,d.x)/6.28318530718+0.5,0.5-asin(clamp(d.y,-1.0,1.0))/3.14159265359);
    // Explicit gradients prevent a blurred stripe at the wrapped panorama seam.
    vec2 dx=dFdx(uv),dy=dFdy(uv);
    dx.x-=round(dx.x);dy.x-=round(dy.x);
    fragColor=vec4(textureGrad(u_Texture,uv,dx,dy).rgb,1.0);
}
