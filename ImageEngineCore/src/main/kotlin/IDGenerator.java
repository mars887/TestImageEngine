import java.util.HashSet;
import java.util.Random;

public class IDGenerator {

    private static final HashSet<Long> ids = new HashSet<>();
    private static final Random random = new Random();

    public synchronized static boolean putUsedId(long id) {
        return ids.add(id);
    }
    public synchronized static boolean removeIDFromUsed(long id) {
        return ids.remove(id);
    }

    public synchronized static long generateId() {
        long id = 0;
        do {
            id = random.nextLong();
        } while (ids.contains(id));

        ids.add(id);
        return id;
    }

}
