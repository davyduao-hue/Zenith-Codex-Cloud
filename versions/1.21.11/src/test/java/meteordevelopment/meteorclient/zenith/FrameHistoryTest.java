package meteordevelopment.meteorclient.zenith;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FrameHistoryTest {
    @Test void rolloverKeepsSamplesInTimeOrder() {
        FrameHistory history=new FrameHistory(3);
        for(int n=1;n<=10;n++)history.add(n);
        assertEquals(3,history.size());
        assertEquals(8,history.get(0));assertEquals(9,history.get(1));assertEquals(10,history.get(2));
    }
    @Test void averageUsesElapsedTimeRatherThanMeanOfFpsReadings() {
        FrameHistory history=new FrameHistory(3);history.add(10);history.add(30);
        assertEquals(50,history.averageFps(),.001);
    }
    @Test void onePercentLowUsesSlowFramesAfterRollover() {
        FrameHistory history=new FrameHistory(200);
        history.add(5000); // This obsolete stall must leave the sampling window.
        for(int n=0;n<198;n++)history.add(5);
        history.add(40);history.add(60);
        assertEquals(20,history.onePercentLow(),.001);
    }
    @Test void invalidInputsCannotPoisonGraphsOrSummary() {
        FrameHistory history=new FrameHistory(4);
        history.add(Double.NaN);history.add(Double.POSITIVE_INFINITY);history.add(0);history.add(-2);history.add(16);
        assertEquals(1,history.size());assertEquals(62.5,history.averageFps(),.001);
    }
    @Test void disconnectResetDropsTheOldSession() {
        FrameHistory history=new FrameHistory(2);history.add(16);history.clear();
        assertEquals(0,history.size());assertEquals(0,history.averageFps());assertEquals(0,history.onePercentLow());
        history.add(10);assertEquals(100,history.averageFps());
        assertThrows(IndexOutOfBoundsException.class,()->history.get(1));
    }
    @Test void capacityMustBePositive() { assertThrows(IllegalArgumentException.class,()->new FrameHistory(0)); }
}
