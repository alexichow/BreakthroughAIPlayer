package breakthrough;

import game.GameState;

import java.util.ArrayList;

import breakthrough.AlphaBetaBreakthroughPlayer.ScoredBreakthroughMove;

public class ABThread extends Thread {
	private AlphaBetaBreakthroughPlayer mABPlayer;
	private ArrayList<ScoredBreakthroughMove> mvStack;
	private BreakthroughState mBoard;
	protected boolean isRunning = false;
	private BreakthroughMove mMove;
	
	public ABThread(AlphaBetaBreakthroughPlayer abPlayer, 
			ArrayList<ScoredBreakthroughMove> stack, GameState brd,
			BreakthroughMove mv) {
		mABPlayer = abPlayer;
		mvStack = stack;
		mBoard = (BreakthroughState)brd;
		mMove = mv;
	}
	
	public void run() {
		isRunning = true;
		mABPlayer.alphaBeta(mBoard, 0, Double.NEGATIVE_INFINITY, 
				Double.POSITIVE_INFINITY, mvStack, mMove);
		interrupt();
		isRunning = false;
		//System.out.println("done running");
	}
}
