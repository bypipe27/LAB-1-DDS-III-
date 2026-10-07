
import java.awt.image.BufferedImage;

/**
 * Modelo simple de una carta Monster de Yu-Gi-Oh!.
 * Solo guarda los datos que nos interesan para el duelo: nombre, ATK, DEF
 * y la imagen (URL + imagen ya descargada en memoria).
 */
public class Card {

    private final String name;
    private final int atk;
    private final int def;
    private final String imageUrl;

    // Se llena después de descargar la imagen. Puede quedar null si falla.
    private BufferedImage image;

    public Card(String name, int atk, int def, String imageUrl) {
        this.name = name;
        this.atk = atk;
        this.def = def;
        this.imageUrl = imageUrl;
    }

    public String getName()      { return name; }
    public int getAtk()          { return atk; }
    public int getDef()          { return def; }
    public String getImageUrl()  { return imageUrl; }
    public BufferedImage getImage() { return image; }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    @Override
    public String toString() {
        return name + " [ATK " + atk + " / DEF " + def + "]";
    }
}