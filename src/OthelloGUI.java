import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Point;
import java.util.Optional;
import java.util.Set;

/**
 * Isometric Swing GUI / View-Controller for OthelloModel.
 * Displays turn status, handles accurate tile selection, and game-over dialog with Play Again.
 */
public class OthelloGUI extends JFrame {
    private final OthelloModel model;
    private final BoardPanel boardPanel;
    private final JLabel statusLabel;

    public OthelloGUI(OthelloModel model) {
        super("Isometric Othello");
        this.model = model;
        this.boardPanel = new BoardPanel();
        this.statusLabel = new JLabel("", SwingConstants.CENTER);
        updateStatus();

        setLayout(new BorderLayout());
        add(statusLabel, BorderLayout.NORTH);
        add(boardPanel, BorderLayout.CENTER);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Refreshes the status label and handles auto-pass.
     */
    private void updateStatus() {
        // Auto-pass if no legal moves
        if (!model.isGameOver() && !model.hasLegalMoves(model.getCurrentPlayer())) {
            OthelloModel.Disk skipped = model.getCurrentPlayer();
            model.passTurn();
            JOptionPane.showMessageDialog(
                    this,
                    skipped + " has no legal moves. Passing to " + model.getCurrentPlayer(),
                    "Turn Passed",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
        // Display turn or game-over
        if (model.isGameOver()) {
            Optional<OthelloModel.Disk> win = model.getWinner();
            String text = win.map(d -> "Game Over! Winner: " +
                            (d == OthelloModel.Disk.BLACK ? "Black" : "White"))
                    .orElse("Game Over! Tie");
            statusLabel.setText(text);
        } else {
            OthelloModel.Disk p = model.getCurrentPlayer();
            statusLabel.setText("Turn: " + (p == OthelloModel.Disk.BLACK ? "Black" : "White"));
        }
    }


    private class BoardPanel extends JPanel {
        BoardPanel() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (model.isGameOver()) return;
                    Point cell = viewToModel(e.getPoint());
                    if (cell != null && model.getDisk(cell.x, cell.y) == OthelloModel.Disk.EMPTY) {
                        if (model.makeMove(cell.x, cell.y)) {
                            updateStatus();
                            repaint();

                            // If it's now White's turn (the AI), let it play immediately
                            if (!model.isGameOver() && model.getCurrentPlayer() == OthelloModel.Disk.WHITE) {
                                // Delay to make the AI move noticeable
                                Timer timer = new Timer(300, e1 -> {
                                    model.makeGreedyMove();
                                    updateStatus();
                                    repaint();
                                    if (model.isGameOver()) handleGameOver();
                                });
                                timer.setRepeats(false);
                                timer.start();
                            } else if (model.isGameOver()) {
                                handleGameOver();
                            }
                        } else {
                            Toolkit.getDefaultToolkit().beep();
                        }
                    }
                }
            });
        }

        /**
         * Shows game-over dialog with Play Again / Exit options.
         */
        private void handleGameOver() {
            Optional<OthelloModel.Disk> win = model.getWinner();
            String msg = win.map(d ->
                    (d == OthelloModel.Disk.BLACK ? "Black" : "White") + " wins!"
            ).orElse("It's a tie!");
            int choice = JOptionPane.showOptionDialog(
                    OthelloGUI.this,
                    msg + " Play again?",
                    "Game Over",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    new String[]{"Play Again", "Exit"},
                    "Play Again"
            );
            if (choice == JOptionPane.YES_OPTION) {
                model.resetGame();
                updateStatus();
                repaint();
            } else {
                System.exit(0);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int size = OthelloModel.SIZE;
            double panelW = getWidth(), panelH = getHeight();
            double tw = Math.min(panelW / size, panelH * 2 / size);
            double th = tw / 2;
            double originX = panelW / 2 - tw / 2;
            double originY = panelH / 2 - size * th / 2;

            Set<Point> legal = model.getLegalMoves(model.getCurrentPlayer());
            for (int r = 0; r < size; r++) {
                for (int c = 0; c < size; c++) {
                    double x = originX + (c - r) * tw / 2;
                    double y = originY + (c + r) * th / 2;
                    Polygon tile = new Polygon(
                            new int[]{(int)x, (int)(x + tw/2), (int)(x + tw), (int)(x + tw/2)},
                            new int[]{(int)(y + th/2), (int)y, (int)(y + th/2), (int)(y + th)},
                            4
                    );
                    // tile base
                    g2.setPaint(new GradientPaint(
                            0, 0, new Color(34,139,34),
                            0, (float)th, new Color(0,100,0)
                    ));
                    g2.fill(tile);
                    g2.setPaint(null);
                    g2.setColor(new Color(0,80,0));
                    g2.draw(tile);
                    // highlight legal
                    if (legal.contains(new Point(r, c))) {
                        g2.setColor(new Color(255,255,0,80));
                        g2.fill(tile);
                    }
                    // disc
                    OthelloModel.Disk disk = model.getDisk(r, c);
                    if (disk != OthelloModel.Disk.EMPTY) {
                        double dw = tw * 0.8, dh = th * 0.8;
                        double dx = x + (tw - dw)/2, dy = y + (th - dh)/2 - th/4;
                        // shadow
                        g2.setColor(disk==OthelloModel.Disk.BLACK?Color.DARK_GRAY:Color.GRAY);
                        g2.fillOval((int)dx, (int)(dy+dh/6), (int)dw, (int)dh);
                        // main
                        g2.setColor(disk==OthelloModel.Disk.BLACK?Color.BLACK:Color.WHITE);
                        g2.fillOval((int)dx, (int)dy, (int)dw, (int)dh);
                        // highlight
                        g2.setColor(new Color(255,255,255,80));
                        g2.fillOval((int)(dx+dw/4), (int)(dy+dh/8), (int)(dw/3), (int)(dh/3));
                    }
                }
            }
        }

        /**
         * Maps a click point to board cell by testing each tile's polygon.
         */
        private Point viewToModel(Point p) {
            int size = OthelloModel.SIZE;
            double panelW = getWidth(), panelH = getHeight();
            double tw = Math.min(panelW / size, panelH * 2 / size);
            double th = tw / 2;
            double originX = panelW / 2 - tw / 2;
            double originY = panelH / 2 - size * th / 2;
            for (int r = 0; r < size; r++) {
                for (int c = 0; c < size; c++) {
                    double x = originX + (c - r) * tw / 2;
                    double y = originY + (c + r) * th / 2;
                    Polygon tile = new Polygon(
                            new int[]{(int)x,(int)(x+tw/2),(int)(x+tw),(int)(x+tw/2)},
                            new int[]{(int)(y+th/2),(int)y,(int)(y+th/2),(int)(y+th)},4);
                    if (tile.contains(p)) return new Point(r,c);
                }
            }
            return null;
        }
    }
}