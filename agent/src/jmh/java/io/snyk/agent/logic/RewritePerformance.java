package io.snyk.agent.logic;

import com.google.common.io.ByteStreams;
import io.snyk.agent.jvm.LandingZone;
import io.snyk.agent.util.AsmUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class RewritePerformance {

    // this is "protected" by the @Scope annotation on the class,
    // don't fiddle with it.
    private byte[] classBlackHole = getTestBytes();
    private ClassReader classReaderBlackHole = new ClassReader(classBlackHole);

    @Benchmark
    public String justLoadName() {
        return new ClassReader(classBlackHole).getClassName();
    }

    @Benchmark
    public ClassNode justParse() {
        return AsmUtil.parse(new ClassReader(classBlackHole));
    }

    @Benchmark
    public byte[] parseAndSave() {
        return AsmUtil.byteArray(AsmUtil.parse(new ClassReader(classBlackHole)));
    }

    @Benchmark
    public byte[] justRewrite() {
        return new Rewriter(LandingZone.class, LandingZone.SEEN_SET::add)
                .rewrite(classReaderBlackHole);
    }

    @Benchmark
    public byte[] loadBoth() {
        return new Rewriter(LandingZone.class, LandingZone.SEEN_SET::add)
                .rewrite(new ClassReader(classBlackHole));
    }

    private byte[] getTestBytes() {
        try {
            final Class<?> victim = TestVictim.class;
            final String fileName = victim.getName()
                    .replace('.', '/') + ".class";
            return ByteStreams.toByteArray(victim.getClassLoader()
                    .getResourceAsStream(fileName));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
