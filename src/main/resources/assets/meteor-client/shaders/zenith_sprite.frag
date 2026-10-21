#version 330 core
in vec2 v_TexCoord;
in vec4 v_Color;
out vec4 fragColor;
void main() {
    float shape=floor(v_TexCoord.x/2.0);
    vec2 p=vec2(v_TexCoord.x-shape*2.0,v_TexCoord.y)*2.0-1.0;
    float r2=dot(p,p);
    if(shape>1.5) {
        float d=abs(sqrt(r2)-0.8333333);
        float aa=max(fwidth(sqrt(r2))*0.65,0.0015);
        float core=1.0-smoothstep(0.005,0.005+aa,d);
        float glow=exp(-pow(d/0.028,2.0));
        fragColor=vec4(mix(v_Color.rgb,vec3(1.0,0.98,0.92),core*0.85),v_Color.a*(core*0.7+glow*0.35));
        return;
    }
    float halo=max(0.0,exp(-5.5*r2)-0.0041);
    float core=exp(-42.0*r2);
    float rays=0.0;
    if(shape>0.5) {
        rays=exp(-abs(p.x)*5.0-abs(p.y)*90.0)+exp(-abs(p.y)*5.0-abs(p.x)*90.0);
        rays*=smoothstep(1.0,0.45,max(abs(p.x),abs(p.y)));
    }
    float energy=halo*0.65+core*0.7+rays*0.45;
    vec3 rgb=mix(v_Color.rgb,vec3(1.0,0.98,0.94),clamp(core+rays*0.65,0.0,1.0));
    fragColor=vec4(rgb,v_Color.a*min(1.0,energy));
}
