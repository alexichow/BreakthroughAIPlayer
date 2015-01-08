package breakthrough;

import game.GameMove;
import game.GamePlayer;
import game.GameState;

import java.util.Random;

public class BaseBreakthroughPlayer extends GamePlayer {
	public static final int MAX_SCORE = (int)Math.pow(2.0, 32);//highest int value possible

	public BaseBreakthroughPlayer(String nname, boolean isDeterministic) {
		super (nname, new BreakthroughState(), isDeterministic);
	}

	public static double eval(BreakthroughState brd) {
		int home = 0;
		int away = 0;
		int takes = 0;
		int distanceScore = 0;
		int dim = BreakthroughState.N;
		// Adjust weights here
		double takesMultiplier = 1;
		double pieceMultiplier = 2;
		double distanceMultiplier = 1;
		
		for (int r=0; r<BreakthroughState.N; r++) {
			for (int c=0; c<BreakthroughState.N; c++) {
				// Distance heuristic
				if (brd.board[r][c] == brd.homeSym) { // White
					
					takes = 0;
					
					// Add take moves when evaluating from away
					if ( (r+1) < dim && (c+1) < dim && brd.board[r+1][c+1] == brd.awaySym)
							takes++;
					else if ( (r+1) < dim && (c-1) >= 0 && brd.board[r+1][c-1] == brd.awaySym)
							takes++;
					
					distanceScore = r;
					if (takes > 0)
						home += takes * takesMultiplier;
					home += distanceScore * distanceMultiplier; //(takes+1) * takeMultiplier;
					home += pieceMultiplier;

				} else if (brd.board[r][c] == brd.awaySym) { // Black
					
					takes = 0;
					
					if ( (r-1) >= 0 && (c-1) >= 0 && brd.board[r-1][c-1] == brd.homeSym)
						takes++;
					else if ( (r-1) >= 0 && (c+1) < dim && brd.board[r-1][c+1] == brd.homeSym)
						takes++;
					
					distanceScore = (BreakthroughState.N-1) - r - 1;
					if (takes > 0)
						away += takes * takesMultiplier;
					away += distanceScore * distanceMultiplier; // * (takes+1);
					away += pieceMultiplier;

				}
			}
		}
		
		return home-away;
	}


	/**
	 * The evaluation function
	 * @param brd board to be evaluated
	 * @return Black evaluation - Red evaluation
	 */
	
	public static double evalBoard(BreakthroughState brd)
	{ 
		double score = eval(brd);
		
		//System.out.println(score);
		//JOptionPane.showMessageDialog(null, "EVAL FUNCTION");
		if (Math.abs(score) > MAX_SCORE) {
			System.err.println("Problem with eval");
			System.exit(0);
		}
		return score;
	} 
	
	@Override
	public GameMove getMove(GameState state, String lastMv) {
		// TODO Auto-generated method stub
		return null;
	}
}