import java.util.ArrayList;
import java.util.List;
import java.util.Random;



public class Duelo {

    // Referencias a las listas reales (no copias) para que la UI se sincronice sola.
    private List<Card> playerDeck;
    private List<Card> aiDeck;

    private final List<BattleListener> listeners = new ArrayList<>();
    private final Random random = new Random();

    private int playerScore;
    private int aiScore;
    private boolean ended;

    public Duelo() {
        this.playerDeck = new ArrayList<>();
        this.aiDeck = new ArrayList<>();
    }

    /** Conecta las listas de cartas del jugador y de la IA (por referencia). */
    public void setDecks(List<Card> player, List<Card> ai) {
        this.playerDeck = player;
        this.aiDeck = ai;
    }

    public void addListener(BattleListener l) {
        listeners.add(l);
    }

    /** Reinicia el marcador y sortea quién empieza (solo informativo). */
    public void start() {
        playerScore = 0;
        aiScore = 0;
        ended = false;
        boolean playerStarts = random.nextBoolean();
        notifyScore();
        // Aviso de turno inicial: lo mandamos como un turno "especial".
        notifyTurn(
                playerStarts ? "Tú empiezas" : "La máquina empieza",
                "",
                "Comienza el duelo"
        );
    }

    public boolean isEnded() { return ended; }
    public int getPlayerScore() { return playerScore; }
    public int getAiScore() { return aiScore; }

    /** Ejecuta un turno completo: jugador juega playerCard, la IA responde. */
    public void playTurn(Card playerCard) {
        if (ended || playerCard == null) return;
        if (aiDeck.isEmpty() || playerDeck.isEmpty()) return;

        // La IA elige carta al azar de las que le quedan
        Card aiCard = aiDeck.remove(random.nextInt(aiDeck.size()));
        // Y también al azar si ataca o defiende
        boolean aiAttacks = random.nextBoolean();

        // El jugador siempre ataca
        int playerValue = playerCard.getAtk();
        int aiValue = aiAttacks ? aiCard.getAtk() : aiCard.getDef();

        String winner;
        if (playerValue > aiValue) {
            playerScore++;
            playerDeck.remove(playerCard);
            winner = "Ganaste la ronda";
        } else if (aiValue > playerValue) {
            aiScore++;
            playerDeck.remove(playerCard);
            winner = "La máquina ganó la ronda";
        } else {
            // Empate: nadie gana, las cartas vuelven a la mano
            aiDeck.add(aiCard);
            winner = "Empate (se repite)";
        }

        // Notificar al listener (UI)
        notifyTurn(playerCard.toString(), aiCard.toString() + (aiAttacks ? " [ATK]" : " [DEF]"), winner);
        notifyScore();

        // ¿Fin del duelo?
        if (playerScore >= 2 || aiScore >= 2) {
            ended = true;
            notifyDuelEnded(playerScore >= 2 ? "¡Jugador!" : "Máquina");
        }
    }

    // ---- Notificaciones ----
    private void notifyTurn(String p, String a, String w) {
        for (BattleListener l : listeners) l.onTurn(p, a, w);
    }
    private void notifyScore() {
        for (BattleListener l : listeners) l.onScoreChanged(playerScore, aiScore);
    }
    private void notifyDuelEnded(String w) {
        for (BattleListener l : listeners) l.onDuelEnded(w);
    }
}
