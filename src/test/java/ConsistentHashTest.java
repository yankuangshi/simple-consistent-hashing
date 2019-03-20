import org.junit.Assert;
import org.junit.Test;

import java.util.SortedMap;
import java.util.TreeMap;

public class ConsistentHashTest {

    @Test
    public void testTreeMap() {
        TreeMap<Long, String> tm = new TreeMap<>();
        tm.put(100L, "cache0.server.com:12345");
        tm.put(10L, "cache1.server.com:12345");
        tm.put(1000L, "cache2.server.com:12345");
        tm.put(500L, "cache3.server.com:12345");

        long key1 = tm.ceilingKey(120L);
        Assert.assertEquals(500L, key1);
        long key2 = tm.ceilingKey(1200L) != null ? tm.ceilingKey(1200L) : tm.firstKey();
        Assert.assertEquals(10L, key2);
    }

    @Test
    public void testSortedMap() {
        SortedMap<Long, String> sm = new TreeMap<>();
        sm.put(100L, "cache0.server.com:12345");
        sm.put(10L, "cache1.server.com:12345");
        sm.put(1000L, "cache2.server.com:12345");
        sm.put(500L, "cache3.server.com:12345");

        SortedMap<Long, String> sub = ((TreeMap<Long, String>) sm).tailMap(120L);
        long key1 = sub != null && !sub.isEmpty() ? sub.firstKey() : sm.firstKey();
        Assert.assertEquals(500L, key1);

        sub = ((TreeMap<Long, String>) sm).tailMap(1200L);
        long key2 = sub != null && !sub.isEmpty() ? sub.firstKey() : sm.firstKey();
        Assert.assertEquals(10L, key2);
    }
}
