package benchmarks.helpers;

import org.openjdk.jmh.annotations.AuxCounters;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Thread)
@AuxCounters(AuxCounters.Type.EVENTS)
public class NativeTime {
    public long nativeNs;
    public long frames;

    public void add(long ns) {
        nativeNs += ns;
    }

    public void frame() {
        frames++;
    }
}
