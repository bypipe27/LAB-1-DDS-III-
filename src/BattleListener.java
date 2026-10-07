

public interface BattleListener {

    /** Se llama cada vez que termina un turno. */
    void onTurn(String playerCard, String aiCard, String winner);

    /** Se llama cada vez que cambia el marcador. */
    void onScoreChanged(int playerScore, int aiScore);

    /** Se llama cuando alguien llega a 2 rondas ganadas. */
    void onDuelEnded(String winner);
}