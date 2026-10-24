package meteordevelopment.meteorclient.zenith;

import com.mojang.blaze3d.buffers.GpuBuffer;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.MeshRenderer;

/** Owned GPU geometry; camera transforms and shader time can change without re-uploading it. */
final class CachedSkyMesh implements AutoCloseable {
    private GpuBuffer vertices,indices;
    private int builds;
    boolean ready(){return vertices!=null&&indices!=null;}
    int builds(){return builds;}

    void upload(MeshBuilder mesh,boolean topologyChanged) {
        GpuBuffer nextVertices=mesh.getVertexBuffer();
        GpuBuffer nextIndices;
        try {nextIndices=topologyChanged||indices==null?mesh.getIndexBuffer():indices;}
        catch(RuntimeException error){nextVertices.close();throw error;}
        if(vertices!=null)vertices.close();
        if(indices!=null&&nextIndices!=indices)indices.close();
        vertices=nextVertices;indices=nextIndices;builds++;
    }

    MeshRenderer bind(MeshRenderer renderer){return renderer.mesh(vertices.slice(),indices.slice());}

    @Override public void close() {
        if(vertices!=null){vertices.close();vertices=null;}
        if(indices!=null){indices.close();indices=null;}
        builds=0;
    }
}
