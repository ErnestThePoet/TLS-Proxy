package utils;

import java.util.List;

public class ByteArrayUtil {
    public static byte[] concat(byte[]...byteArrays){
        int totalLength=0;
        for(var i:byteArrays){
            totalLength+=i.length;
        }

        byte[] concatenated=new byte[totalLength];

        int currentCopyStartIndex=0;

        for(var i:byteArrays){
            System.arraycopy(i,0,concatenated,currentCopyStartIndex,i.length);
            currentCopyStartIndex+=i.length;
        }

        return concatenated;
    }

    public static byte[] concat(List<byte[]> byteArrays){
        int totalLength=byteArrays.stream().reduce(
                0,
                (c,b)->c+b.length,
                (a,b)->b
        );

        byte[] concatenated=new byte[totalLength];

        int currentCopyStartIndex=0;

        for(var i:byteArrays){
            System.arraycopy(i,0,concatenated,currentCopyStartIndex,i.length);
            currentCopyStartIndex+=i.length;
        }

        return concatenated;
    }
}