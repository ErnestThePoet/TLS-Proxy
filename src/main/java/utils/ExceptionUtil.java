package utils;

public class ExceptionUtil {
    public static String getExceptionBrief(Exception e) {
        return e.getClass().getName() + " " + e.getMessage();
    }
}
