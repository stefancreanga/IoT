import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.pskstore.AdvancedSinglePskStore;
import org.json.JSONObject;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetSocketAddress;
import java.util.Base64;

public class SecureCoapServer extends CoapServer {
    private SecretKey decryptionKey;

    public SecureCoapServer(SecretKey key) {
        this.decryptionKey = key;
        add(new SecureResource());
    }

    private class SecureResource extends CoapResource {
        public SecureResource() {
            super("secureResource");
            getAttributes().setTitle("Secure Resource with Payload Decryption");
        }

        @Override
        public void handlePOST(CoapExchange exchange) {
            try {
                String encryptedPayload = exchange.getRequestText();
                System.out.println("Received encrypted payload: " + encryptedPayload);

                // Decrypt payload
                String decryptedPayload = decrypt(encryptedPayload, decryptionKey);
                System.out.println("Decrypted payload: " + decryptedPayload);

                // Parse the decrypted JSON payload
                JSONObject jsonPayload = new JSONObject(decryptedPayload);
                String temperature = jsonPayload.getString("temperature");
                String humidity = jsonPayload.getString("humidity");

                // Print the values from the JSON
                System.out.println("Received temperature: " + temperature);
                System.out.println("Received humidity: " + humidity);

                // Respond to the client
                exchange.respond("Payload received and processed securely");
            } catch (Exception e) {
                e.printStackTrace();
                exchange.respond("Failed to process secure payload");
            }
        }
    }
    @Override
    public void start() {
        try {
            // Create PSK store and set the PSK credentials (MUST BE AdvancedSinglePskStore AND NOT AdvancedMultiPskStore)
            AdvancedSinglePskStore pskStore = new AdvancedSinglePskStore("client_identity", "secure_password".getBytes());

            // Create DTLS configuration with PSK support
            Configuration config = Configuration.createStandardWithoutFile();
            DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder(config);
            dtlsConfig.setAdvancedPskStore(pskStore);
            dtlsConfig.setAddress(new InetSocketAddress("localhost", 5684));

            // Create DTLS connector
            DTLSConnector dtlsConnector = new DTLSConnector(dtlsConfig.build());

            // Create CoAP endpoint
            CoapEndpoint secureEndpoint = new CoapEndpoint.Builder()
                    .setConnector(dtlsConnector)
                    .build();

            // Initialize CoAP client with secure endpoint
            addEndpoint(secureEndpoint);
            super.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static String decrypt(String encryptedData, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decodedData = Base64.getDecoder().decode(encryptedData);
        return new String(cipher.doFinal(decodedData));
    }
    public static SecretKey getPredefinedKey() {
        return new SecretKeySpec("stefan1234567890".getBytes(), "AES");
    }
    public static void main(String[] args) throws Exception {
        // MUST USE THE SAME KEY AS THE CLIENT
        SecretKey key = getPredefinedKey();

        // Create and start the server
        SecureCoapServer server = new SecureCoapServer(key);
        server.start();
        System.out.println("Secure DTLS CoAP Server is running on coaps://localhost:5684");
    }
}
