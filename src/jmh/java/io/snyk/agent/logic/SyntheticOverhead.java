package io.snyk.agent.logic;

import io.snyk.asm.ClassReader;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class SyntheticOverhead {
    private static final AtomicBoolean bool = new AtomicBoolean();

    @Benchmark
    public Object allocateDumb() {
        return new Object();
    }

    @Benchmark
    public void lazySet() {
        bool.lazySet(true);
    }
}
