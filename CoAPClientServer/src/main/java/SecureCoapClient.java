import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.pskstore.AdvancedSinglePskStore;
import org.json.JSONObject;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class SecureCoapClient {
    private CoapClient client;
    private SecretKey encryptionKey;

    public SecureCoapClient(String uri, SecretKey key) {
        this.encryptionKey = key;
        try {
            // Create PSK store and set the PSK credentials (MUST BE AdvancedSinglePskStore AND NOT AdvancedMultiPskStore)
            AdvancedSinglePskStore pskStore = new AdvancedSinglePskStore("client_identity", "secure_password".getBytes()); // Using identity and PSK in byte form

            // Create DTLS configuration with PSK support
            Configuration config = Configuration.createStandardWithoutFile();
            DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder(config);
            dtlsConfig.setAdvancedPskStore(pskStore);

            // Build DTLS connector
            DTLSConnector dtlsConnector = new DTLSConnector(dtlsConfig.build());

            // Create CoAP endpoint
            CoapEndpoint secureEndpoint = new CoapEndpoint.Builder()
                    .setConnector(dtlsConnector)
                    .build();

            // Initialize CoAP client with secure endpoint
            this.client = new CoapClient(uri);
            this.client.setEndpoint(secureEndpoint);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void jsonPayload(String temperature, String humidity) {
        try {
            // Create a JSON object
            JSONObject jsonPayload = new JSONObject();
            jsonPayload.put("temperature", temperature);
            jsonPayload.put("humidity", humidity);

            // Encrypt the JSON payload
            String encryptedPayload = encrypt(jsonPayload.toString(), encryptionKey);
            System.out.println("Sending encrypted JSON payload: " + encryptedPayload);

            // Send the encrypted payload as plain text
            CoapResponse response = client.post(encryptedPayload, MediaTypeRegistry.APPLICATION_JSON);

            if (response != null) {
                System.out.println("Server response: " + response.getResponseText());
            } else {
                System.out.println("No response received from server.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static String encrypt(String data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedData = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedData);
    }
    public static SecretKey getPredefinedKey() {
        return new SecretKeySpec("stefan1234567890".getBytes(), "AES");
    }
    public static void main(String[] args) throws Exception {
        // MUST USE THE SAME KEY AS THE SERVER
        SecretKey key = getPredefinedKey();

        // Create the client (5684 for SSL, 5683 for no SSL)
        SecureCoapClient client = new SecureCoapClient("coaps://localhost:5684/secureResource", key);

        // Send message
        client.jsonPayload("40 C", "Not so humid.");
    }
}
