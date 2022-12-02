package utils;

public class Log {
    public static void success(String s){
        System.out.printf("[SUCCESS] %s%n",s);
    }
    public static void info(String s){
        System.out.printf("[INFO] %s%n",s);
    }

    public static void warn(String s){
        System.out.printf("[WARN] %s%n",s);
    }

    public static void error(String s){
        System.out.printf("[ERROR] %s%n",s);
    }

    public static void success(String s,String url){
        success(String.format("%s [%s]",s,url));
    }
    public static void info(String s,String url){
        info(String.format("%s [%s]",s,url));
    }

    public static void warn(String s,String url){
        warn(String.format("%s [%s]",s,url));
    }

    public static void error(String s,String url){
        error(String.format("%s [%s]",s,url));
    }
}
