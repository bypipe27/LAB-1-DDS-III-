import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Ventana principal de la aplicación.
 * - Dos paneles: mano del jugador y mano de la máquina.
 * - Un log de batalla desplazable.
 * - Botones: "Iniciar duelo" y "Elegir carta".
 * - Implementa BattleListener para reaccionar a los eventos del Duel.
 */
public class MainFrame extends JFrame implements BattleListener {

    private final YugiApiClient api = new YugiApiClient();
    private final Duelo duel = new Duelo();

    private final List<Card> playerHand = new ArrayList<>();
    private final List<Card> aiHand = new ArrayList<>();

    private final JButton startBtn = new JButton("Iniciar duelo");
    private final JButton playBtn  = new JButton("Elegir carta");
    private final JLabel statusLabel = new JLabel(" Presiona 'Iniciar duelo' para cargar cartas...");
    private final JPanel playerPanel = new JPanel(new FlowLayout());
    private final JPanel aiPanel     = new JPanel(new FlowLayout());
    private final JTextArea log = new JTextArea(12, 60);

    private Card selectedCard = null;

    public MainFrame() {
        super("Yu-Gi-Oh! Duelo Simple");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        // ---- Zona superior: botón de inicio + estado
        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        top.add(startBtn, BorderLayout.WEST);
        top.add(statusLabel, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // ---- Zona central: dos paneles con manos
        playerPanel.setBorder(BorderFactory.createTitledBorder("Tu mano"));
        aiPanel.setBorder(BorderFactory.createTitledBorder("Mano de la máquina"));
        JPanel center = new JPanel(new GridLayout(1, 2, 8, 8));
        center.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        center.add(playerPanel);
        center.add(aiPanel);
        add(center, BorderLayout.CENTER);

        // ---- Zona inferior: log + botón de jugar
        log.setEditable(false);
        JScrollPane scroll = new JScrollPane(log);
        scroll.setBorder(BorderFactory.createTitledBorder("Log de batalla"));

        JPanel playPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        playPanel.add(playBtn);

        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        bottom.add(scroll, BorderLayout.CENTER);
        bottom.add(playPanel, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        // ---- Listeners
        duel.addListener(this);
        startBtn.addActionListener(e -> startDuel());
        playBtn.addActionListener(e -> playSelectedCard());
        playBtn.setEnabled(false);

        setVisible(true);
    }

    /** Carga 3 cartas para cada jugador en un hilo aparte (SwingWorker). */
    private void startDuel() {
        startBtn.setEnabled(false);
        playBtn.setEnabled(false);
        log.setText("");
        appendLog("Cargando cartas desde YGOProDeck...\n");

        playerHand.clear();
        aiHand.clear();
        playerPanel.removeAll();
        aiPanel.removeAll();
        refresh(playerPanel);
        refresh(aiPanel);

        new SwingWorker<List<Card>, Void>() {
            @Override
            protected List<Card> doInBackground() throws Exception {
                List<Card> all = new ArrayList<>();
                for (int i = 0; i < 6; i++) {
                    all.add(api.getRandomMonster());
                }
                return all;
            }

            @Override
            protected void done() {
                try {
                    List<Card> all = get();
                    playerHand.addAll(all.subList(0, 3));
                    aiHand.addAll(all.subList(3, 6));

                    duel.setDecks(playerHand, aiHand);
                    duel.start();

                    renderHands();
                    statusLabel.setText(" Duelo en curso. Selecciona una carta y presiona 'Elegir carta'.");
                    startBtn.setEnabled(true);
                } catch (Exception ex) {
                    appendLog("No se pudo cargar la carta: " + ex.getMessage() + "\n");
                    appendLog("Verifica tu conexión a internet e inténtalo de nuevo.\n");
                    statusLabel.setText(" Error al cargar cartas.");
                    startBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    /** Pinta las manos actuales del jugador y de la máquina. */
    private void renderHands() {
        playerPanel.removeAll();
        aiPanel.removeAll();

        for (Card c : playerHand) {
            playerPanel.add(makeCardButton(c, true));
        }
        for (Card c : aiHand) {
            aiPanel.add(makeCardButton(c, false));
        }
        refresh(playerPanel);
        refresh(aiPanel);
    }

    /** Crea el botón que representa una carta. */
    private JComponent makeCardButton(Card c, boolean visible) {
        JButton btn = new JButton();
        btn.setPreferredSize(new Dimension(170, 260));
        btn.setLayout(new BorderLayout());

        if (c.getImage() != null) {
            Image scaled = c.getImage().getScaledInstance(150, 210, Image.SCALE_SMOOTH);
            btn.setIcon(new ImageIcon(scaled));
        }

        String text = visible
                ? "<html><center>" + c.getName() + "<br>ATK: " + c.getAtk() + " / DEF: " + c.getDef() + "</center></html>"
                : "<html><center>Carta oculta</center></html>";
        btn.setText(text);
        btn.setVerticalTextPosition(SwingConstants.BOTTOM);
        btn.setHorizontalTextPosition(SwingConstants.CENTER);

        if (visible) {
            btn.addActionListener(e -> {
                selectedCard = c;
                appendLog("Seleccionaste: " + c + "\n");
                playBtn.setEnabled(!duel.isEnded());
            });
        } else {
            btn.setEnabled(false);
        }
        return btn;
    }

    /** Envía la carta seleccionada al duelo. */
    private void playSelectedCard() {
        if (selectedCard == null) {
            appendLog("Debes seleccionar una carta primero.\n");
            return;
        }
        duel.playTurn(selectedCard);
        selectedCard = null;
        playBtn.setEnabled(false);
        renderHands(); // quita cartas usadas
    }

    private void appendLog(String s) {
        log.append(s);
        log.setCaretPosition(log.getDocument().getLength());
    }

    private void refresh(JComponent c) {
        c.revalidate();
        c.repaint();
    }

    // ============ BattleListener ============

    @Override
    public void onTurn(String playerCard, String aiCard, String winner) {
        if (aiCard == null || aiCard.isEmpty()) {
            appendLog(playerCard + "\n");
        } else {
            appendLog("Tú: " + playerCard + "  |  IA: " + aiCard + "  ->  " + winner + "\n");
        }
    }

    @Override
    public void onScoreChanged(int playerScore, int aiScore) {
        appendLog("Marcador -> Tú: " + playerScore + "  |  IA: " + aiScore + "\n");
    }

    @Override
    public void onDuelEnded(String winner) {
        appendLog("\n=== GANADOR: " + winner + " ===\n");
        playBtn.setEnabled(false);
        statusLabel.setText(" Duelo terminado. Ganador: " + winner);
        JOptionPane.showMessageDialog(this, "Ganador: " + winner, "Fin del duelo",
                JOptionPane.INFORMATION_MESSAGE);
    }
}
