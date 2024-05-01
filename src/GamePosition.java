import java.io.Serializable;

public class GamePosition implements Serializable {
    // you are player 1!
    float player1Score;
    float player2Score;
    int dealer;

    public GamePosition(float player1Score, float player2Score, int dealer) {
        this.player1Score = player1Score;
        this.player2Score = player2Score;
        this.dealer = dealer;
    }

    public float getPlayer1Score() {
        return player1Score;
    }
    public void setPlayer1Score(float player1Score) {
        this.player1Score = player1Score;
    }

    public float getPlayer2Score() {
        return player2Score;
    }
    public void setPlayer2Score(float player2Score) {
        this.player2Score = player2Score;
    }

    @Override
    public String toString() {
        return "[" + player1Score + "," + player2Score + "," + (dealer == 0) + "]";

    }
}
