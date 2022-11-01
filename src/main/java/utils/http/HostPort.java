package utils.http;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HostPort {
    private String host;
    private Integer port;
}
