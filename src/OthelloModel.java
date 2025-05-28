    //The game will be human vs computer.
    // The computer's move will be based on a Greedy Algorithm, seeking the immediate short term benefit from the current move
    // (without any other logic like prioritizing corners, sides or using MiniMax)

    //making board private and giving a public accessor is dangerous, instead pass a deep copy? for getBoard()
    //dont violate dry, instead of a black count and white count, make one method that takes color as arg and returns its count getPieceCount

    import java.awt.Point;
    import java.util.*;

    /**
     * Model for Othello/Reversi game logic, including board state, move generation, and undo support.
     */
    public class OthelloModel {
        public static final int SIZE = 8;

        /**
         * Disk states on the board.
         */
        public enum Disk {
            EMPTY, BLACK, WHITE;
            /**
             * Returns the opponent disk color (EMPTY returns EMPTY).
             */
            public Disk opponent() {
                switch (this) {
                    case BLACK: return WHITE;
                    case WHITE: return BLACK;
                    default:    return EMPTY;
                }
            }
        }

        /** 8×8 board array holding disk states. */
        private final Disk[][] board = new Disk[SIZE][SIZE];
        /** The player whose turn it is currently. */
        private Disk currentPlayer;
        /** Stack of past moves for undo functionality. */
        private final Deque<Move> history = new ArrayDeque<>();

        /**
         * Constructs a new model, sets up initial board and starting player.
         */
        public OthelloModel() {
            resetBoard();
            currentPlayer = Disk.BLACK;
        }

        /**
         * Resets the board to the standard opening configuration and clears history.
         */
        private void resetBoard() {
            for (int r = 0; r < SIZE; r++)
                for (int c = 0; c < SIZE; c++)
                    board[r][c] = Disk.EMPTY;
            board[3][3] = board[4][4] = Disk.WHITE;
            board[3][4] = board[4][3] = Disk.BLACK;
            history.clear();
        }

        /** Public API to restart the game. */
        public void resetGame() {
            resetBoard();
            currentPlayer = Disk.BLACK;
        }

        /**
         * Returns the disk at the specified row and column.
         */
        public Disk getDisk(int r, int c) {
            return board[r][c];
        }

        /**
         * Returns which player has the current turn.
         */
        public Disk getCurrentPlayer() {
            return currentPlayer;
        }

        /**
         * Computes all legal move positions for the given player.
         * @param player the disk color to generate moves for
         * @return set of Points (row,col) where a move is legal
         */
        public Set<Point> getLegalMoves(Disk player) {
            Set<Point> moves = new HashSet<>();
            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c < SIZE; c++) {
                    if (board[r][c] == Disk.EMPTY && !allFlips(r, c, player).isEmpty()) {
                        moves.add(new Point(r, c));
                    }
                }
            }
            return moves;
        }

        /**
         * Attempts to place a disk at (r,c) for the current player.
         * Records the move, flips opponent disks, and switches turn if legal.
         * @return true if move was applied, false if illegal
         */
        public boolean makeMove(int r, int c) {
            List<Point> flips = allFlips(r, c, currentPlayer);
            if (flips.isEmpty()) return false;
            history.push(new Move(r, c, currentPlayer, new ArrayList<>(flips)));
            board[r][c] = currentPlayer;
            for (Point p : flips) {
                board[p.x][p.y] = currentPlayer;
            }
            switchPlayer();
            return true;
        }

        /**
         * Checks if placing a disk at (r,c) is legal for the current player.
         */
        public boolean isLegalMove(int r, int c) {
            return board[r][c] == Disk.EMPTY && !allFlips(r, c, currentPlayer).isEmpty();
        }

        /**
         * Switches the turn to the opponent.
         */
        public void switchPlayer() {
            currentPlayer = currentPlayer.opponent();
        }

        /**
         * Passes the turn to the opponent without placing a disk.
         * (same as switchPlayer—semantic alias for auto‑pass logic)
         */
        public void passTurn() {
            switchPlayer();
        }

        /**
         * Returns true if the specified player has any legal moves.
         */
        public boolean hasLegalMoves(Disk player) {
            return !getLegalMoves(player).isEmpty();
        }

        /**
         * Returns true when neither player can make a legal move.
         */
        public boolean isGameOver() {
            return !hasLegalMoves(Disk.BLACK) && !hasLegalMoves(Disk.WHITE);
        }

        /**
         * Determines the winner disk if game is over; empty if tie or not finished.
         */
        public Optional<Disk> getWinner() {
            if (!isGameOver()) return Optional.empty();
            int b = getScore(Disk.BLACK), w = getScore(Disk.WHITE);
            if (b > w) return Optional.of(Disk.BLACK);
            if (w > b) return Optional.of(Disk.WHITE);
            return Optional.empty();
        }

        /**
         * Counts the number of disks on the board for the given player.
         */
        public int getScore(Disk player) {
            int cnt = 0;
            for (var row : board) for (var d : row) {
                if (d == player) cnt++;
            }
            return cnt;
        }

        /**
         * Counts how many empty squares remain on the board.
         */
        public int countEmpty() {
            int cnt = 0;
            for (var row : board) for (var d : row) {
                if (d == Disk.EMPTY) cnt++;
            }
            return cnt;
        }

        /**
         * Returns a deep copy of the current board array.
         */
        public Disk[][] getBoardCopy() {
            Disk[][] copy = new Disk[SIZE][SIZE];
            for (int r = 0; r < SIZE; r++) {
                System.arraycopy(board[r], 0, copy[r], 0, SIZE);
            }
            return copy;
        }

        /**
         * Creates a deep clone of this model (board, currentPlayer, history).
         */
        @Override
        public OthelloModel clone() {
            OthelloModel copy = new OthelloModel();
            copy.currentPlayer = this.currentPlayer;
            for (int r = 0; r < SIZE; r++) {
                System.arraycopy(this.board[r], 0, copy.board[r], 0, SIZE);
            }
            copy.history.clear();
            copy.history.addAll(this.history);
            return copy;
        }

        /**
         * Undoes the last move, restoring board state and turn.
         */
        public void undoMove() {
            if (history.isEmpty()) return;
            Move last = history.pop();
            board[last.row][last.col] = Disk.EMPTY;
            for (Point p : last.flips) {
                board[p.x][p.y] = last.player.opponent();
            }
            currentPlayer = last.player;
        }

        /**
         * Heuristic: difference between player’s and opponent’s disk counts.
         */
        public int evaluate(Disk player) {
            return getScore(player) - getScore(player.opponent());
        }

        /**
         * Checks if the given position is one of the four corners.
         */
        public boolean isCorner(int r, int c) {
            return (r == 0 || r == SIZE - 1) && (c == 0 || c == SIZE - 1);
        }

        /**
         * Returns an ASCII diagram of the board for debugging.
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c < SIZE; c++) {
                    switch (board[r][c]) {
                        case BLACK: sb.append('B'); break;
                        case WHITE: sb.append('W'); break;
                        default:    sb.append('.');
                    }
                    sb.append(' ');
                }
                sb.append('\n');
            }
            return sb.toString();
        }

        //—— Flipping logic ————————————————————————————————————
        /**
         * Collects all opponent disks to flip for a move at (r,c) in every direction.
         */
        private List<Point> allFlips(int r, int c, Disk player) {
            List<Point> result = new ArrayList<>();
            int[] dirs = {-1, 0, 1};
            for (int dr : dirs) for (int dc : dirs) {
                if (dr == 0 && dc == 0) continue;
                List<Point> part = directionFlips(r, c, dr, dc, player);
                if (!part.isEmpty()) result.addAll(part);
            }
            return result;
        }

        /**
         * Finds disks to flip along a single (dr,dc) direction from (r,c).
         */
        private List<Point> directionFlips(int r, int c, int dr, int dc, Disk player) {
            List<Point> path = new ArrayList<>();
            int x = r + dr, y = c + dc;
            while (x >= 0 && x < SIZE && y >= 0 && y < SIZE) {
                if (board[x][y] == player.opponent()) {
                    path.add(new Point(x, y));
                } else if (board[x][y] == player) {
                    return path;
                } else {
                    break;
                }
                x += dr; y += dc;
            }
            return Collections.emptyList();
        }

        /**
         * Internal record of a move for undo: position, player, flipped disks.
         */
        private static class Move {
            final int row, col;
            final Disk player;
            final List<Point> flips;
            Move(int r, int c, Disk p, List<Point> f) {
                row = r; col = c; player = p; flips = f;
            }
        }


        /**
         * Computes and executes the best greedy move for the current player.
         * Selects the move that immediately flips the maximum disks.
         * @return true if move was successfully made; false if no moves available
         */
        public boolean makeGreedyMove() {
            Set<Point> legalMoves = getLegalMoves(currentPlayer);
            if (legalMoves.isEmpty()) {
                return false;  // No moves available
            }

            Point bestMove = null;
            int maxFlips = -1;

            for (Point move : legalMoves) {
                int flipCount = allFlips(move.x, move.y, currentPlayer).size();
                if (flipCount > maxFlips) {
                    maxFlips = flipCount;
                    bestMove = move;
                }
            }

            if (bestMove != null) {
                makeMove(bestMove.x, bestMove.y);
                return true;
            }

            return false;
        }

        // Only for unit tests!
        void forceBoardFill(Disk d) {
            for (int r = 0; r < SIZE; r++)
                for (int c = 0; c < SIZE; c++)
                    board[r][c] = d;
        }
    }
