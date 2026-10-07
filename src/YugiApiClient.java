import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Cliente para consumir la API pública de YGOProDeck usando java.net.http.HttpClient.
 * - Pide una carta al azar.
 * - Si NO es Monster, vuelve a pedir.
 * - Descarga también la imagen de la carta.
 *
 * Los métodos son bloqueantes; quien los use debe llamarlos desde un
 * SwingWorker para no congelar la UI.
 */
public class YugiApiClient {

    private static final String RANDOM_URL =
            "https://db.ygoprodeck.com/api/v7/randomcard.php";

    // Cliente HTTP reutilizable (nuevo en Java 11)
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /** Pide cartas al azar hasta obtener una Monster. */
    public Card getRandomMonster() throws Exception {
        while (true) {
            String json = httpGet(RANDOM_URL);
            Card card = parseCard(json);
            if (card != null) {
                try {
                    card.setImage(downloadImage(card.getImageUrl()));
                } catch (Exception e) {
                    System.out.println("No se pudo cargar la imagen: " + e.getMessage());
                }
                return card;
            }
        }
    }

    /** GET con java.net.http.HttpClient (Java 11+). */
    private String httpGet(String urlStr) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlStr))
                .timeout(java.time.Duration.ofSeconds(8))
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("Error de red: HTTP " + response.statusCode());
        }
        return response.body();
    }

    /** Parsea el JSON y devuelve null si la carta no es Monster. */
    private Card parseCard(String json) {
        JSONObject root = new JSONObject(json);
        JSONArray data = root.getJSONArray("data");
        JSONObject c = data.getJSONObject(0);

        String type = c.optString("type", "");
        if (!type.contains("Monster")) {
            return null;
        }

        String name = c.getString("name");
        int atk = c.optInt("atk", 0);
        int def = c.optInt("def", 0);

        JSONArray images = c.getJSONArray("card_images");
        String imageUrl = images.getJSONObject(0).getString("image_url");

        return new Card(name, atk, def, imageUrl);
    }

    /** Descarga la imagen (ImageIO sigue siendo la forma estándar). */
    private BufferedImage downloadImage(String urlStr) throws Exception {
        try (InputStream in = new java.net.URL(urlStr).openStream()) {
            return ImageIO.read(in);
        }
    }
}