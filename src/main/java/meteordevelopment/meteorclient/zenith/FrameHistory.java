package meteordevelopment.meteorclient.zenith;

import java.util.Arrays;

/** Fixed storage; records frame durations rather than averaging displayed FPS. */
public final class FrameHistory {
    private final double[] samples;
    private int next, size;

    public FrameHistory(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity");
        samples = new double[capacity];
    }

    public void add(double milliseconds) {
        if (!Double.isFinite(milliseconds) || milliseconds <= 0) return;
        samples[next] = milliseconds;
        next = (next + 1) % samples.length;
        size = Math.min(size + 1, samples.length);
    }

    public int size() { return size; }

    public double get(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
        return samples[(next - size + index + samples.length) % samples.length];
    }

    public double averageFps() {
        if (size == 0) return 0;
        double total = 0;
        for (int i = 0; i < size; i++) total += get(i);
        return 1000.0 * size / total;
    }

    /** FPS corresponding to the mean duration of the slowest 1% of frames. */
    public double onePercentLow() {
        if (size == 0) return 0;
        double[] sorted = new double[size];
        for (int i = 0; i < size; i++) sorted[i] = get(i);
        Arrays.sort(sorted);
        int count = Math.max(1, (int) Math.ceil(size * .01));
        double total = 0;
        for (int i = size - count; i < size; i++) total += sorted[i];
        return 1000.0 * count / total;
    }

    public void clear() { next = size = 0; }
}
