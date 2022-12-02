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
            throw new IOException("ACK packet length is not 1");
        }

        if (ackPacket[0] != -1) {
            throw new IOException("ACK packet invalid");
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

        this.remoteSocket.getOutputStream().write(new byte[]{-1});
        this.remoteSocket.getOutputStream().flush();

        byte[] packet = ByteArrayUtil.concat(packetParts);

        return new PacketAndData(packet, PacketUtil.extractData(packet));
    }
}
