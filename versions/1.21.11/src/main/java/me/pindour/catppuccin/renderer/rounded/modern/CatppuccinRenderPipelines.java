package me.pindour.catppuccin.renderer.rounded.modern;

//? if >=1.21.5 {
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import me.pindour.catppuccin.CatppuccinAddon;
import meteordevelopment.meteorclient.renderer.ExtendedRenderPipelineBuilder;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.renderer.MeteorVertexFormats;
import com.mojang.blaze3d.shaders.UniformType;

import java.lang.reflect.Method;

//? if >=26.1 {
import java.util.Optional;
//? } else {
/*import com.mojang.blaze3d.platform.DepthTestFunction;
*///? }

//? if >=26.2 {
//? } else {
/*import com.mojang.blaze3d.vertex.VertexFormat;
*///?}

public class CatppuccinRenderPipelines {

    private static final RenderPipeline.Snippet MESH_UNIFORMS = RenderPipeline.builder()
        //? if >=26.2 {
        
                .withUniform("MeshData", UniformType.UNIFORM_BUFFER)
                
        //? } else {
        /*.withUniform("MeshData", UniformType.UNIFORM_BUFFER)
        *///? }
        .buildSnippet();

    public static final RenderPipeline ROUNDED_UI = register(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
        .withLocation(CatppuccinAddon.identifier("pipeline/rounded_ui"))
        .withVertexShader(CatppuccinAddon.identifier("shaders/rounded_ui.vert"))
        .withFragmentShader(CatppuccinAddon.identifier("shaders/rounded_ui.frag"))

        //? if >=26.2 {
        .withVertexFormat(MeteorVertexFormats.POS2_COLOR, com.mojang.blaze3d.vertex.VertexFormat.Mode.TRIANGLES)
        
                .withUniform("RoundedRectData", UniformType.UNIFORM_BUFFER)
                
        //? } else {
        /*.withVertexFormat(MeteorVertexFormats.POS2_COLOR, VertexFormat.Mode.TRIANGLES)
        .withUniform("RoundedRectData", UniformType.UNIFORM_BUFFER)
        *///? }

        //? if >=26.1 {
        .withDepthTestFunction(com.mojang.blaze3d.platform.DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false)
        .withBlend(BlendFunction.TRANSLUCENT)

        //? } else {
        /*.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false)
        .withBlend(BlendFunction.TRANSLUCENT)
        *///? }

        .withCull(false)
        .build()
    );

    private static RenderPipeline register(RenderPipeline pipeline) {
        try {
            Method method = MeteorRenderPipelines.class.getDeclaredMethod("add", RenderPipeline.class);
            method.setAccessible(true);
            return (RenderPipeline) method.invoke(null, pipeline);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to register pipeline", e);
        }
    }
}
//?}