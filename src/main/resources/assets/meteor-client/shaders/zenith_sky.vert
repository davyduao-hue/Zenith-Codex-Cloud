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
    // Minecraft 26.2 uses reversed depth. Zero is the far plane; sky never covers terrain.
    gl_Position.z = 0.0;
    v_Color = Color;
}
