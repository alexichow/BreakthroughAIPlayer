package breakthrough;
import game.GameMove;
import game.GamePlayer;
import game.GameState;

import java.util.ArrayList;


// AlphaBetaConnect4Player is identical to MiniMaxConnect4Player
// except for the search process, which uses alpha beta pruning.

public class AlphaBetaBreakthroughPlayer extends BaseBreakthroughPlayer {
	public final int MAX_DEPTH = 30;
	public final int MAX_MOVES = 75;
	public final int MAX_TIME = 420 * 1000 / MAX_MOVES;//Time per move in ms
	public final int MAX_DEPTH_LIMIT = 9;
	public int depthLimit;
	public ScoredBreakthroughMove move;
	protected long elapsedTime, startTime;
	private ArrayList<ScoredBreakthroughMove> threadMoves;
	private ArrayList<ABThread> threadPool; 
	private ArrayList<ArrayList<ScoredBreakthroughMove>> mvStackList;
	private int lastChildrenSize = 0;

	protected class ScoredBreakthroughMove extends BreakthroughMove {

		public ScoredBreakthroughMove(int oldRow, int oldCol, int newRow, int newCol, double s) {
			super(oldRow, oldCol, newRow, newCol);
			score = s;
		}

		public void acceptScore(int oldRow, int oldCol, int newRow, int newCol, double s) {
			startRow = oldRow;
			startCol = oldCol;
			endingRow = newRow;
			endingCol = newCol;
			score = s;
		}

		public void acceptScore(ScoredBreakthroughMove mv, double s) {
			acceptScore(mv.startRow,mv.startCol,mv.endingRow,mv.endingCol,s);
		}

		public void acceptScore(BreakthroughMove bm, double s) {
			acceptScore(bm.startRow,bm.startCol,bm.endingRow,bm.endingCol,s);
		}

		public String getMove() {return startRow + "," + startCol + " - " + endingRow + "," + endingCol;}

		public double score;
	}


	public AlphaBetaBreakthroughPlayer(String nname, int d) {
		super(nname, false);
		depthLimit = d;
		move = new ScoredBreakthroughMove(0,0,0,0,0);
		elapsedTime = 0;
		startTime = 0;
		threadMoves = new ArrayList<ScoredBreakthroughMove>();
		threadPool = new ArrayList<ABThread>();
		mvStackList = new ArrayList<ArrayList<ScoredBreakthroughMove>>();
	}

	public void init(ArrayList<ScoredBreakthroughMove> list) {
		for (int i=0; i<MAX_DEPTH; i++) {
			list.add(new ScoredBreakthroughMove(0,0,0,0,0));
		}
	}

	/**
	 * Performs alpha beta pruning.
	 * @param brd
	 * @param currDepth
	 * @param alpha
	 * @param beta
	 */
	protected void alphaBeta(BreakthroughState brd, int currDepth,
			double alpha, double beta, ArrayList<ScoredBreakthroughMove> mvStack,
			BreakthroughMove bm)
	{
		boolean toMaximize = (brd.getWho() == GameState.Who.HOME);
		boolean toMinimize = !toMaximize;

		boolean isTerminal = terminalValue(brd, mvStack.get(currDepth));

		if (isTerminal) {
			;
		} else if (currDepth == depthLimit) {
			mvStack.get(currDepth).acceptScore(move, evalBoard(brd));
		} else {

			double bestScore = (toMaximize ? 
					Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
			ScoredBreakthroughMove bestMove = mvStack.get(currDepth);
			ScoredBreakthroughMove nextMove = mvStack.get(currDepth+1);

			bestMove.acceptScore(move, bestScore);

			//get current player's symbol
			char sym = brd.getWho() == GameState.Who.HOME ?
					BreakthroughState.homeSym : BreakthroughState.awaySym;
			//determine if we should move up or down
			int diff = brd.getWho() == GameState.Who.HOME ? 1 : -1;

			if (currDepth == 0) {
				BreakthroughState tmp = (BreakthroughState) brd.clone();
				brd.makeMove(bm);
				alphaBeta(brd,currDepth+1,alpha,beta,mvStack,bm);
				brd = tmp;
				// Check out the results, relative to what we've seen before
				if (toMaximize && nextMove.score > bestMove.score) {
					bestMove.acceptScore(bm, nextMove.score);
				} else if (!toMaximize && nextMove.score < bestMove.score) {
					bestMove.acceptScore(bm, nextMove.score);
				}


				// Update alpha and beta. Perform pruning, if possible.
				if (toMinimize) {
					beta = Math.min(bestMove.score, beta);
					if (bestMove.score <= alpha || bestMove.score == -MAX_SCORE) {
						return;
					}
				} else {
					alpha = Math.max(bestMove.score, alpha);
					if (bestMove.score >= beta || bestMove.score == MAX_SCORE) {
						return;
					}
				}
			} else {
				ArrayList<BreakthroughMove> children = new ArrayList<BreakthroughMove>();
				generateChildren(children,brd,sym,diff);
				java.util.Collections.shuffle(children);

				for (int i=0; i<children.size(); i++) {

					BreakthroughState tmp = (BreakthroughState) brd.clone();				
					brd.makeMove(children.get(i));	
					alphaBeta(brd, currDepth+1, alpha,beta, mvStack, children.get(i));
					brd = tmp;

					// Check out the results, relative to what we've seen before
					if (toMaximize && nextMove.score > bestMove.score) {
						bestMove.acceptScore(children.get(i), nextMove.score);
					} else if (!toMaximize && nextMove.score < bestMove.score) {
						bestMove.acceptScore(children.get(i), nextMove.score);
					}


					// Update alpha and beta. Perform pruning, if possible.
					if (toMinimize) {
						beta = Math.min(bestMove.score, beta);
						if (bestMove.score <= alpha || bestMove.score == -MAX_SCORE) {
							return;
						}
					} else {
						alpha = Math.max(bestMove.score, alpha);
						if (bestMove.score >= beta || bestMove.score == MAX_SCORE) {
							return;
						}
					}

				}
			}
		}
	}

	private void generateChildren(ArrayList<BreakthroughMove> children,
			BreakthroughState brd, char sym, int diff) {		

		for (int j = 0; j < BreakthroughState.NUM_ROWS; j++) {
			for (int k = 0; k < BreakthroughState.NUM_COLS; k++) {
				if (brd.board[j][k] == sym) {
					//add each children if move is ok
					BreakthroughMove tmp = new BreakthroughMove(j,k,j+diff,k-1);
					if (brd.moveOK(tmp))
						children.add(tmp);
					tmp = new BreakthroughMove(j,k,j+diff,k);
					if (brd.moveOK(tmp))
						children.add(tmp);
					tmp = new BreakthroughMove(j,k,j+diff,k+1);
					if (brd.moveOK(tmp))
						children.add(tmp);
				}
			}
		}
	}

	protected boolean terminalValue(GameState brd, ScoredBreakthroughMove mv) {
		GameState.Status status = brd.getStatus();
		boolean isTerminal = true;
		if (status == GameState.Status.HOME_WIN) {
			mv.acceptScore(move , MAX_SCORE);
		} else if (status == GameState.Status.AWAY_WIN) {
			mv.acceptScore(move, - MAX_SCORE);
		} else if (status == GameState.Status.DRAW) {
			mv.acceptScore(move, 0);
		} else {
			isTerminal = false;
		}
		return isTerminal;
	}

	public int startThreads(BreakthroughState brd) {
		int diff = brd.getWho() == GameState.Who.HOME ? 1 : -1;
		char sym = brd.getWho() == GameState.Who.HOME ?
				BreakthroughState.homeSym : BreakthroughState.awaySym;
		ArrayList<BreakthroughMove> children =
				new ArrayList<BreakthroughMove>();
		generateChildren(children,brd,sym,diff);
		threadPool = new ArrayList<ABThread>();
		mvStackList = new ArrayList<ArrayList<ScoredBreakthroughMove>>();
		for (int i = 0; i < children.size(); i++) {
			BreakthroughState b = (BreakthroughState)brd.clone();
			mvStackList.add(new ArrayList<ScoredBreakthroughMove>());
			init(mvStackList.get(i));
			threadPool.add(new ABThread(this,mvStackList.get(i),
					b,children.get(i)));
			threadPool.get(i).start();
		}
		return children.size();
	}

	public void waitForThreads() {
		for (int j = 0; j < lastChildrenSize; j++) {
			try {
				threadPool.get(j).join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			threadMoves.add(mvStackList.get(j).get(0));
		}

	}

	public GameMove minimum(GameState brd, int size,
			ArrayList<ScoredBreakthroughMove> moves) {
		int index = 0;
		double bestMaxScore = Double.NEGATIVE_INFINITY;
		double bestMinScore = Double.POSITIVE_INFINITY;
		boolean toMaximize = brd.getWho() == GameState.Who.HOME;
		for (int i = 0; i < size; i++) {
			if (toMaximize) {
				if (moves.get(i).score > bestMaxScore) {
					index = i;
					bestMaxScore = moves.get(i).score;
				}
			} else {
				if (moves.get(i).score < bestMinScore) {
					index = i;
					bestMinScore = moves.get(i).score;
				}
			}
		}
		return new BreakthroughMove(moves.get(index).startRow,moves.get(index).startCol,
				moves.get(index).endingRow,moves.get(index).endingCol);
	}

	public boolean checkFresh(GameState brd) {
		int pieceCount = 0;
		char sym = brd.getWho() == GameState.Who.HOME ?
				BreakthroughState.homeSym : BreakthroughState.awaySym;
		BreakthroughState b = (BreakthroughState)brd;
		for (int i = 0; i < 7; i++) {
			for (int j = 0; j < 7; j++) {
				if (b.board[i][j] == sym)
					pieceCount += i;
			}
		}
		if (b.getWho() == GameState.Who.HOME){
			return (pieceCount / 7 == 1) && (pieceCount % 7 == 0); 
		} else return (pieceCount / 77 == 1) && (pieceCount % 77 == 0);
	}

	public GameMove getMove(GameState brd, String lastMove) {
		boolean isBeginning = checkFresh(brd);
		//starttimer for isBeginning
		BreakthroughMove mv;
		
		char sym = brd.getWho() == GameState.Who.HOME ? BreakthroughState.homeSym : BreakthroughState.awaySym;
		BreakthroughState b = (BreakthroughState)brd;
		int lastRow;
		if(sym == BreakthroughState.homeSym) {
			lastRow = 5;		//1 row less than the end
			for(int i = 0; i < 7; i++) {
				if(b.board[lastRow][i] == sym) {
					if(i < 6) {
						return new BreakthroughMove(lastRow, i, lastRow+1, i+1); //move down and right
					}
					else {
						return new BreakthroughMove(lastRow, i, lastRow+1, i-1); //move down and left
					}
				}
			}//for 
		}//home player

		else {
			lastRow = 1;
			for (int i = 0; i < 7; i++) {
				if(b.board[lastRow][i] == sym) {
					if(i < 6) {
						return new BreakthroughMove(lastRow, i, lastRow-1, i+1); //move up and right
					}
					else {
						return new BreakthroughMove(lastRow, i, lastRow-1, i-1); //move up and left
					}
				}
			}//for
		} //away player
		
		lastChildrenSize = startThreads((BreakthroughState)brd);
		waitForThreads();
		ArrayList<ScoredBreakthroughMove> moves = 
				new ArrayList<ScoredBreakthroughMove>();
		for (int i = 0; i < lastChildrenSize; i++)
			moves.add(mvStackList.get(i).get(0));
		int size = lastChildrenSize;
		return minimum(brd,size,moves);
	}

	/*startTime = System.currentTimeMillis();
		ArrayList<ScoredBreakthroughMove> mvStack = new ArrayList<ScoredBreakthroughMove>();
		init(mvStack);
		depthLimit = 7;
		BreakthroughState b = (BreakthroughState)brd.clone();
		while ((System.currentTimeMillis() - startTime) < (MAX_TIME / 10.0) && depthLimit < MAX_DEPTH_LIMIT) {
			BreakthroughState board = (BreakthroughState)b.clone();//it needs to be a fresh version each restart
			depthLimit++;
			alphaBeta(board, 0, Double.NEGATIVE_INFINITY, 
					Double.POSITIVE_INFINITY, mvStack);
			System.out.println(depthLimit);
			System.out.println(System.currentTimeMillis() - startTime);
		}
		//alphaBeta((BreakthroughState)brd, 0, Double.NEGATIVE_INFINITY, 
		//					Double.POSITIVE_INFINITY, mvStack);
		long time = System.currentTimeMillis() - startTime;
		elapsedTime += time;
		System.out.println("elapsedTime: " + elapsedTime);
		//return sMoveList.get(sMoveList.size()-1).get(0);
	 */
	//return mvStack.get(0);
	//}

	public static char [] toChars(String x) {
		char [] res = new char [x.length()];
		for (int i=0; i<x.length(); i++)
			res[i] = x.charAt(i);
		return res;
	}

	public static void main(String [] args) {
		int depth = 7;
		GamePlayer p = new AlphaBetaBreakthroughPlayer("Beast Mode " + depth, depth);
		p.compete(args);
	}
}