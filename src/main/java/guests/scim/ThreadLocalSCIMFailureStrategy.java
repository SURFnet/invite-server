package guests.scim;

public class ThreadLocalSCIMFailureStrategy {

    private static final ThreadLocal<Boolean> contextHolder = new ThreadLocal<>();

    public static Boolean ignoreFailures() {
        return contextHolder.get() != null;
    }

    public static void startIgnoringFailures() {
        contextHolder.set(Boolean.TRUE);
    }

    public static void stopIgnoringFailures() {
        contextHolder.remove();
    }

}
