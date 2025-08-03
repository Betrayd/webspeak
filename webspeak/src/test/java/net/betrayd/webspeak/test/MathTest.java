package net.betrayd.webspeak.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import net.betrayd.webspeak.v1.util.WebSpeakMath;

public class MathTest {
    
    record ThreeIntValues(int a, int b, int c) {
    }

    @ParameterizedTest
    @MethodSource("threeIntRange")
    void testInvertExponentialGain(ThreeIntValues vals) {
        // Distance being less than reference distance is irrelevent.
        if (vals.a() < vals.b())
            return;
            
        double targetGain = WebSpeakMath.computeExponentialGain(vals.a, vals.b, vals.c);
        assertEquals(vals.a, WebSpeakMath.invertExponentialGain(targetGain, vals.b, vals.c), 0.001);
    }

    static Stream<ThreeIntValues> threeIntRange() {
        Iterable<ThreeIntValues> iterable = () -> new IntIterator(1, 11, 1, 11, 1, 11);
        return StreamSupport.stream(iterable.spliterator(), false);
    }
    
    static class IntIterator implements Iterator<ThreeIntValues> {
        final int minA;
        final int maxA;
        final int minB;
        final int maxB;
        final int minC;
        final int maxC;

        int a;
        int b;
        int c;

        IntIterator(int minA, int maxA, int minB, int maxB, int minC, int maxC) {
            this.minA = minA;
            this.maxA = maxA;
            this.minB = minB;
            this.maxB = maxB;
            this.minC = minC;
            this.maxC = maxC;

            a = minA;
            b = minB;
            c = minC;
        }
        

        @Override
        public boolean hasNext() {
            return (a < maxA || b < maxB || c < maxC);
        }

        @Override
        public ThreeIntValues next() {
            ThreeIntValues val = new ThreeIntValues(a, b, c);
            if (a < maxA) {
                a++;
            } else if (b < maxB) {
                a = minA;
                b++;
            } else if (c < maxC) {
                a = minA;
                b = minB;
                c++;
            } else {
                throw new IndexOutOfBoundsException("Iterator is finished");
            }
            return val;
        }
    }
}
