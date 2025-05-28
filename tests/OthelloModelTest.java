import org.junit.jupiter.api.*;
import java.awt.Point;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class OthelloModelTest {

    OthelloModel model;

    @BeforeEach
    void setUp() {
        model = new OthelloModel();
    }

    @Test
    void testInitialBoardSetup() {
        assertEquals(OthelloModel.Disk.BLACK, model.getDisk(3, 4));
        assertEquals(OthelloModel.Disk.BLACK, model.getDisk(4, 3));
        assertEquals(OthelloModel.Disk.WHITE, model.getDisk(3, 3));
        assertEquals(OthelloModel.Disk.WHITE, model.getDisk(4, 4));
    }

    @Test
    void testMakeLegalMove() {
        assertTrue(model.makeMove(2, 3)); // Valid BLACK move
        assertEquals(OthelloModel.Disk.BLACK, model.getDisk(2, 3));
    }

    @Test
    void testMakeIllegalMove() {
        assertFalse(model.makeMove(0, 0)); // Illegal at start
        assertEquals(OthelloModel.Disk.EMPTY, model.getDisk(0, 0));
    }

    @Test
    void testSwitchPlayer() {
        OthelloModel.Disk first = model.getCurrentPlayer();
        model.switchPlayer();
        assertEquals(first.opponent(), model.getCurrentPlayer());
    }

    @Test
    void testGetLegalMoves() {
        Set<Point> moves = model.getLegalMoves(OthelloModel.Disk.BLACK);
        assertTrue(moves.contains(new Point(2, 3)));
        assertTrue(moves.contains(new Point(3, 2)));
    }

    @Test
    void testGameOverDetection() {
        model.forceBoardFill(OthelloModel.Disk.BLACK);
        assertTrue(model.isGameOver());
    }

    @Test
    void testGetScore() {
        assertEquals(2, model.getScore(OthelloModel.Disk.BLACK));
        assertEquals(2, model.getScore(OthelloModel.Disk.WHITE));
    }

    @Test
    void testUndoMove() {
        model.makeMove(2, 3);
        model.undoMove();
        assertEquals(OthelloModel.Disk.EMPTY, model.getDisk(2, 3));
        assertEquals(OthelloModel.Disk.BLACK, model.getCurrentPlayer());
    }
}