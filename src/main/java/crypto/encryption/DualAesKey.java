package crypto.encryption;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DualAesKey {
    private AesKey clientKey;
    private AesKey serverKey;
}
