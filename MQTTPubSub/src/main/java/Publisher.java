import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.Base64;

public class Publisher {
    public static SSLSocketFactory getSocketFactory(String keystoreFile, String keystorePassword, String truststoreFile, String truststorePassword) throws Exception {
        // Load client keystore (private key + certificate)
        KeyStore clientKeyStore = KeyStore.getInstance("JKS");
        FileInputStream fis = new FileInputStream(keystoreFile);
        clientKeyStore.load(fis, keystorePassword.toCharArray());
        fis.close();

        // Initialize KeyManagerFactory using client keystore
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(clientKeyStore, keystorePassword.toCharArray());

        // Load the truststore (CA certificate)
        KeyStore trustStore = KeyStore.getInstance("JKS");
        fis = new FileInputStream(truststoreFile);
        trustStore.load(fis, truststorePassword.toCharArray());
        fis.close();

        // Initialize truststore
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(trustStore);

        // Initialize SSL
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return sslContext.getSocketFactory();
    }
    public static String encrypt(String data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedData = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedData);
    }
    public static String generateHMAC(String message, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        mac.init(secretKey);

        byte[] hmacBytes = mac.doFinal(message.getBytes());
        return Base64.getEncoder().encodeToString(hmacBytes);
    }
    public static SecretKey loadKey(String filePath) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(filePath));
        return new SecretKeySpec(keyBytes, "AES");
    }
    public static void main(String[] args) {
        String broker = "ssl://test.mosquitto.org:8883";
        String clientId = "PublisherClient";
        String topic = "Stefan Topic MQTT";

        // HMAC Secret
        final String SHARED_SECRET = "stefanmqtt";

        try {
            // Key generated by Util class, MUST be the same as the publisher
            SecretKey key = loadKey("NeededItems/shared_key.key");

            // Encrypt the payload
            String plainTextMessage = "Stefan sends a message with MQTT!";
            String encryptedMessage = encrypt(plainTextMessage, key);

            // Generate HMAC for the encrypted message
            String hmac = generateHMAC(encryptedMessage, SHARED_SECRET);

            // Combine the HMAC and encrypted message
            String combinedMessage = hmac + "|" + encryptedMessage;

            // MQTT setup
            MqttClient client = new MqttClient(broker, clientId);
            MqttConnectOptions options = new MqttConnectOptions();
            SSLSocketFactory sslSocketFactory = getSocketFactory("NeededItems/client.jks", "client-password", "NeededItems/client.jks", "client-password");
            options.setSocketFactory(sslSocketFactory);
            client.connect(options);

            // Publish the combined message
            MqttMessage message = new MqttMessage(combinedMessage.getBytes());
            message.setQos(1);
            client.publish(topic, message);

            System.out.println("Encrypted message with HMAC published.");
            client.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
