package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MinimaxAlphaBeta extends Agent {

    private final int numPlys;

    public MinimaxAlphaBeta(int playernum, String[] args)
    {
        super(playernum);

        if(args.length < 1)
        {
            System.err.println("You must specify the number of plys");
            System.exit(1);
        }

        numPlys = Integer.parseInt(args[0]);
    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
        return middleStep(newstate, statehistory);
    }

    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {
        GameStateChild bestChild = alphaBetaSearch(new GameStateChild(newstate),
                numPlys,
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY);

        return bestChild.action;
    }

    @Override
    public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {

    }

    @Override
    public void savePlayerData(OutputStream os) {

    }

    @Override
    public void loadPlayerData(InputStream is) {

    }

    /**
     * You will implement this.
     *
     * This is the main entry point to the alpha beta search. Refer to the slides, assignment description
     * and book for more information.
     *
     * Try to keep the logic in this function as abstract as possible (i.e. move as much SEPIA specific
     * code into other functions and methods)
     *
     * @param node The action and state to search from
     * @param depth The remaining number of plys under this node
     * @param alpha The current best value for the maximizing node from this node to the root
     * @param beta The current best value for the minimizing node from this node to the root
     * @return The best child of this node with updated values
     */
    public GameStateChild alphaBetaSearch(GameStateChild node, int depth, double alpha, double beta)
    {
    	
        GameState orig = node.state;
        
        GameStateChild bestChild = null;
        GameStateChild toReturn = null;
        
        if(node.action != null && node.action.size() > 0 &&  depth <= 0){
        	return node;
        }
        List<GameStateChild> children = orderChildrenWithHeuristics(orig.getChildren());
        GameStateChild nextChild = null;
        
        if(orig.isMMTurn()){
        	double newAlpha = alpha;
        	for(GameStateChild child : children){
        		nextChild = alphaBetaSearch(child, depth - 1, newAlpha, beta);
        		if(nextChild == null) break;
        		double utility = nextChild.state.getUtility();
        		if(newAlpha < utility){
        			newAlpha = utility;
        			bestChild = nextChild;
        			toReturn = child;
        		}
        		if( beta <= newAlpha )
        			break;
            }
        }else{
        	double newBeta = beta;
        	for(GameStateChild child : children){
        		nextChild = alphaBetaSearch(child, depth - 1, alpha, newBeta);
        		if(nextChild == null) break;
        		double utility = nextChild.state.getUtility();
        		if(newBeta > utility){
        			newBeta = utility;
        			bestChild = nextChild;
        			toReturn = child;
        		}
        		if( newBeta <= alpha )
        			break;
        	}
         }
        	if(toReturn != null && toReturn.action.size() == 0){
        		return bestChild;
        	}
        	return toReturn;
        }
    

    /**
     * You will implement this.
     *
     * Given a list of children you will order them according to heuristics you make up.
     * See the assignment description for suggestions on heuristics to use when sorting.
     *
     * Use this function inside of your alphaBetaSearch method.
     *
     * Include a good comment about what your heuristics are and why you chose them.
     *
     * @param children
     * @return The list of children sorted by your heuristic.
     */
    public List<GameStateChild> orderChildrenWithHeuristics(List<GameStateChild> children)
    {
    	List<GameStateChild> temp = children;
    	ArrayList<Double> list = new ArrayList<Double>();
    	Map<Double, GameStateChild> m = new HashMap<Double, GameStateChild>();
    	for(GameStateChild t : children)
    	{
    		GameState g = t.state;
    		Double d = g.getUtility();
    		m.put(d, t);
    		list.add(d);
    	}
    	Collections.sort(list, new Comparator<Double>(){
    	        public int compare(Double a, Double b)
    	        {
    	            return  -a.compareTo(b);
    	        }

    	});
    	for(int i = 0; i<children.size(); i++)
    	{
    		double tem = list.get(i);
    		GameStateChild gsc = m.get(tem);
    		temp.set(i, gsc);
    	}
    		
    	
        return temp;
    }
}
