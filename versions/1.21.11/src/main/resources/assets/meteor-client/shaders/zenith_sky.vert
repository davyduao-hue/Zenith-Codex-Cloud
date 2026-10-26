#version 330 core

layout (location = 0) in vec4 Position;
layout (location = 1) in vec4 Color;
layout (std140) uniform MeshData {
    mat4 u_Proj;
    mat4 u_ModelView;
};
layout (std140) uniform Projection {
    mat4 ProjMat;
};
out vec4 v_Color;

void main() {
    gl_Position = ProjMat * u_ModelView * Position;
    // Minecraft 1.21.11 uses normal depth. Place sky on the far plane.
    gl_Position.z = gl_Position.w;
    v_Color = Color;
}
