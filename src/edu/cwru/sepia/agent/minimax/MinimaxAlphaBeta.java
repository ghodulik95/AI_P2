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
    	//Get the current state and the turn
        GameState currentGameState = node.state;
        boolean currentTurnIsMax = currentGameState.isMMTurn();
        
        //We must get the best grandchild because, if it is the archer's turn in this state,
        //we need a set of moves generated, but that would be empty if in the first ply if the
        //first ply is an archer move
        //As far as straying from minimax, I think this is okay because the game isn't actually
        //turn based, we're just pretending it is for this algorithm, thus, looking an extra move ahead
        //to get the actions from the grandchild does not break the turn order
        GameStateChild bestGrandChild = null;
        
        //This is the best child which we will return
        GameStateChild bestChild = null;
        
        //If we are at a terminal node (max number of plies reached) we return this node
        if(depth <= 0){
        	return node;
        }
        //Otherwise, get its children and order them with our heuristic
        List<GameStateChild> children = orderChildrenWithHeuristics(currentGameState.getChildren());
        GameStateChild grandChild = null;
        
        //If its a MAX turn
        if(currentTurnIsMax){
        	double newAlpha = alpha;
        	//Go through each child (for each will go in our order)
        	for(GameStateChild child : children){
        		//Get the grandChild returned by recursively calling alphabeta
        		grandChild = alphaBetaSearch(child, depth - 1, newAlpha, beta);
        		
        		//This strays from strict minimax a bit, but if null is returned,
        		//It essentially means that all the children were pruned, so
        		//we can prune this
        		if(grandChild == null){
        			break;
        		}
        		
        		
        		//Calculate the utility of the child
        		double utility = grandChild.state.getUtility();
        		//If the utility is greater than alpha
        		if(newAlpha < utility){
        			//set alpha to utiliy
        			newAlpha = utility;
        			//This child is the best child
        			bestChild = child;
        			//Remember its best grandChild
        			bestGrandChild = grandChild;
        		}
        		//Break/Prune the rest of the children if beta <= alpha
        		if( beta <= newAlpha ){
        			break;
        		}
            }
        }
        //If its a MIN turn
        else{
        	double newBeta = beta;
        	//Go through each child (for each will go in our order)
        	for(GameStateChild child : children){
        		//Get the grandChild returned by recursively calling alphabeta
        		grandChild = alphaBetaSearch(child, depth - 1, alpha, newBeta);
        		
        		//This strays from strict minimax a bit, but if null is returned,
        		//It essentially means that all the children were pruned, so
        		//we can prune this
        		if(grandChild == null){
        			break;
        		}
        		
        		//Calculate the utility of the child
        		double utility = grandChild.state.getUtility();
        		//If the utility is less than beta
        		if(newBeta > utility){
        			//set beta to utility
        			newBeta = utility;
        			//This child is the best child
        			bestChild = child;
        			//Remember its best grandChild
        			bestGrandChild = grandChild;
        		}
        		//Break/Prune the rest of the children if beta <= alpha
        		if( newBeta <= alpha ){
        			break;
        		}
        	}
        }
        
        //If the best child's size is 0, all this means is that this is a MIN (archer) turn,
        //And thus, the way the GameState class works, that actions for this move would be empty
        //This does not happen in all cases of archer's turns, only the first turn if it is an archer's
        //turn.
        if(bestChild != null && bestChild.action.size() == 0){
        	return bestGrandChild;
        }
        //Otherwise return the best child
        return bestChild;
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
     * @param children2
     * @return The list of children sorted by your heuristic.
     */
    public List<GameStateChild> orderChildrenWithHeuristics(List<GameStateChild> children)
    {
    	//After extensive testing, the only heuristic ordering that resulted in us winning was
    	//to order by utility.
    	
    	//We will store the utilities of the children in a Map<Double,GameStateChild>
    	//While this may seem counter intuitive, because many children can have identical
    	//utilities, it is actually okay that only one will get returned -- in that case, they have the
    	//same utility, so really either should "get the job done"
    	ArrayList<Double> utilities = new ArrayList<Double>();
    	Map<Double, GameStateChild> m = new HashMap<Double, GameStateChild>();
    	
    	//Get the utilities of each child
    	for(GameStateChild t : children)
    	{
    		GameState g = t.state;
    		Double d = g.getUtility();
    		m.put(d, t);
    		utilities.add(d);
    	}
    	
    	//Sort the utilities in decreasing order
    	Collections.sort(utilities, new Comparator<Double>(){
    		public int compare(Double a, Double b)
    		{
    			return  -a.compareTo(b);
    		}
    	});
    	
    	//Set the children list to be in sorted order
    	for(int i = 0; i<children.size(); i++)
    	{
    		double tem = utilities.get(i);
    		GameStateChild gsc = m.get(tem);
    		children.set(i, gsc);
    	}
        return children;
    }
}
