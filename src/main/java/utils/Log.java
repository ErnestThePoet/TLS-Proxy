package utils;

public class Log {
    public static void info(String s){
        System.out.printf("[INFO] %s%n",s);
    }

    public static void warn(String s){
        System.out.printf("[WARN] %s%n",s);
    }

    public static void error(String s){
        System.out.printf("[ERROR] %s%n",s);
    }
}
