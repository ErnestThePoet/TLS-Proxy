package communication.utils;

import java.util.Arrays;

public class PacketUtil {
    private static byte[] getByteArrayFromInt32(int value) {
        byte[] byteArray = new byte[4];
        byteArray[0] = (byte) (value & 0xFF);
        byteArray[1] = (byte) ((value & 0xFF00) >>> 8);
        byteArray[2] = (byte) ((value & 0xFF0000) >>> 16);
        byteArray[3] = (byte) ((value & 0xFF000000) >>> 24);
        return byteArray;
    }

    private static int getInt32FromByteArray(byte[] data) {
        // make highest 24 bits are zeroed
        int value = ((int) data[0]) & 0xFF;
        value |= (((int) data[1]) & 0xFF) << 8;
        value |= (((int) data[2]) & 0xFF) << 16;
        value |= (((int) data[3]) & 0xFF) << 24;
        return value;
    }

    // send bytes:
    // [length][data]
    public static byte[] makeCommPacket(byte[] data) {
        byte[] sendBytes = new byte[data.length + 4];
        byte[] packetLengthBytes = getByteArrayFromInt32(data.length + 4);
        System.arraycopy(packetLengthBytes, 0, sendBytes, 0, 4);
        System.arraycopy(data, 0, sendBytes, 4, data.length);
        return sendBytes;
    }

    public static int getCommPacketLength(byte[] firstPacketPart) {
        return getInt32FromByteArray(firstPacketPart);
    }

    public static byte[] extractData(byte[] packet) {
        int packetLength = getCommPacketLength(packet);
        return Arrays.copyOfRange(packet, 4, packetLength);
    }
}
