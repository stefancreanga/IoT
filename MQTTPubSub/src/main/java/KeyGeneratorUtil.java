import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.FileOutputStream;

public class KeyGeneratorUtil {
    public static void main(String[] args) throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SecretKey key = keyGen.generateKey();
        try (FileOutputStream fos = new FileOutputStream("NeededItems/shared_key.key")) {
            fos.write(key.getEncoded());
        }
        System.out.println("Key generated and saved to shared_key.key");
    }
}
