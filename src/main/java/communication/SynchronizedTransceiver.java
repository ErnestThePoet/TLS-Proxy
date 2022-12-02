package communication;

import communication.objs.PacketAndData;
import communication.utils.PacketUtil;
import utils.ByteArrayUtil;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// TCP Socket guarantees the order of packets,
// so this communicator only ensures the communication
// is synchronized----only when previous ACK is received
// will we continue sending next packet.
public class SynchronizedTransceiver {
    private final Socket remoteSocket;

    private final byte NORMAL_ACK = -1;

    public SynchronizedTransceiver(Socket remoteSocket) {
        this.remoteSocket = remoteSocket;
    }

    public byte[] sendData(byte[] data) throws IOException {
        byte[] packet = PacketUtil.makeCommPacket(data);
        this.remoteSocket.getOutputStream().write(packet);
        this.remoteSocket.getOutputStream().flush();

        byte[] ackPacket = new byte[2];
        int ackLength = this.remoteSocket.getInputStream().read(ackPacket);

        if (ackLength != 1) {
            throw new IOException("ACK数据包长度不为1");
        }

        if (ackPacket[0] != this.NORMAL_ACK) {
            throw new IOException("ACK数据包非法");
        }

        return packet;
    }

    public PacketAndData receiveData() throws IOException {
        List<byte[]> packetParts = new ArrayList<>();

        // 128KB
        byte[] currentPacketPart = new byte[128 * 1024];

        int currentReadLength = this.remoteSocket.getInputStream().read(currentPacketPart);
        packetParts.add(Arrays.copyOf(currentPacketPart, currentReadLength));

        int totalReadLength = currentReadLength;

        int packetLength = PacketUtil.getCommPacketLength(packetParts.get(0));

        while (totalReadLength < packetLength) {
            currentReadLength = this.remoteSocket.getInputStream().read(currentPacketPart);
            packetParts.add(Arrays.copyOf(currentPacketPart, currentReadLength));
            totalReadLength += currentReadLength;
        }

        this.remoteSocket.getOutputStream().write(new byte[]{this.NORMAL_ACK});
        this.remoteSocket.getOutputStream().flush();

        byte[] packet = ByteArrayUtil.concat(packetParts);

        return new PacketAndData(packet, PacketUtil.extractData(packet));
    }
}
