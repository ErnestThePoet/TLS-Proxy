package crypto.encryption;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AesKey {
    private byte[] key;
    private byte[] iv;
}
