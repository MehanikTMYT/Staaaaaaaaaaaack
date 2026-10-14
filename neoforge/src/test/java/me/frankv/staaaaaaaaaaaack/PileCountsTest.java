package me.frankv.staaaaaaaaaaaack;
import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;
class PileCountsTest {
    @Test void randomExtractionConservesCount() {
        Random random = new Random(18261);
        for (int trial = 0; trial < 10000; trial++) {
            int limit = new int[] {1, 16, 64, 99}[random.nextInt(4)];
            long initial = random.nextInt(Integer.MAX_VALUE);
            long total = initial, extracted = 0;
            for (int step = 0; step < 100; step++) {
                var split = PileCounts.split(total, limit);
                int take = random.nextInt(split.visible() + 1);
                extracted += take;
                total = (long) split.visible() - take + split.reserve();
                assertEquals(initial, total + extracted);
                assertTrue(split.visible() <= limit);
                assertTrue(split.reserve() >= 0);
            }
        }
    }
    @Test void boundaryAndLegacyCounts() {
        for (int limit : new int[]{1,16,64,99}) {
            for (long total : new long[]{0,1,63,64,100,1000,5000,Integer.MAX_VALUE,(long)Integer.MAX_VALUE+limit})
                assertEquals(total, PileCounts.split(total, limit).total());
        }
        assertThrows(ArithmeticException.class, () -> PileCounts.split((long)Integer.MAX_VALUE+65,64));
        assertThrows(IllegalArgumentException.class, () -> PileCounts.split(-1,64));
    }
}
