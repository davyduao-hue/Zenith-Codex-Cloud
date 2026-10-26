package meteordevelopment.meteorclient.zenith;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import meteordevelopment.meteorclient.renderer.FixedUniformStorage;
import net.minecraft.client.renderer.DynamicUniformStorage;
import org.jspecify.annotations.NonNull;

import java.nio.ByteBuffer;

/** One small, bounded ring-buffer upload per sky pass. Allocated on the render thread. */
final class SkyUniforms implements AutoCloseable {
    private final FixedUniformStorage<Data> storage = new FixedUniformStorage<>("Zenith atmosphere", 32, 1);

    GpuBufferSlice write(double time, int style, double brightness, double density, int octaves) {
        storage.clear();
        return storage.write(new Data((float) time, style, (float) brightness, (float) density, octaves));
    }

    @Override public void close() { storage.close(); }

    private record Data(float time, float style, float brightness, float density, float octaves)
        implements DynamicUniformStorage.DynamicUniform {
        @Override public void write(@NonNull ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer).putVec4(time, style, brightness, density).putVec4(octaves, 0, 0, 0);
        }
    }
}
