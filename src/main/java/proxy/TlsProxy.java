package proxy;

import java.util.List;

public interface TlsProxy {
    void start(int port, List<Integer> targetPorts);
}
