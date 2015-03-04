package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.LocatedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Template.TemplateView;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.environment.model.state.UnitTemplate.UnitTemplateView;
import edu.cwru.sepia.util.Direction;

import java.util.*;

/**
 * This class stores all of the information the agent
 * needs to know about the state of the game. For example this
 * might include things like footmen HP and positions.
 *
 * Add any information or methods you would like to this class,
 * but do not delete or change the signatures of the provided methods.
 */
public class GameState {

	private int xExtent;
	private int yExtent;
	private List<UnitInfo> mmUnits;
	private List<UnitInfo> archers;
	private Set<ResourceInfo> resources;
	private int turnNumber;
	
	private class ResourceInfo{
		public final int x;
		public final int y;
		
		public ResourceInfo(int x, int y){
			this.x = x;
			this.y = y;
		}
		
		@Override
		public int hashCode(){
			return Integer.valueOf(x).hashCode() + Integer.valueOf(y).hashCode()*37;
		}
		@Override
		public boolean equals(Object o){
			if(o instanceof ResourceInfo){
				ResourceInfo ri = (ResourceInfo) o;
				return ri.x == x && ri.y == y;
			}
			return false;
		}
	}
	
	private class UnitInfo{
		public final int id;
		public final int x;
		public final int y;
		public final int range;
		public final int attk;
		public final int curHealth;
		public final int baseHealth;
		
		public UnitInfo(int id, int x, int y, int range, int attk, int curHealth, int baseHealth){
			this.id = id;
			this.x = x;
			this.y = y;
			this.range = range;
			this.attk = attk;
			this.curHealth = curHealth;
			this.baseHealth = baseHealth;
		}
		
	}
	
	private List<UnitInfo> extractUnitInfo(List<UnitView> units){
		List<UnitInfo> ret = new LinkedList<UnitInfo>();
		Iterator<UnitView> it = units.iterator();
		UnitView cur;
		UnitTemplateView curTemp;
		while(it.hasNext()){
			cur = it.next();
			curTemp = cur.getTemplateView();
			ret.add(
					new UnitInfo(
							cur.getID(),
							cur.getXPosition(), 
							cur.getYPosition(), 
							curTemp.getRange(), 
							curTemp.getBasicAttack(), 
							cur.getHP(),
							curTemp.getBaseHealth())
					);
		}
		return ret;
	}
	
	private Set<ResourceInfo> extractResourceInfo(State.StateView state){
		Set<ResourceInfo> ret = new HashSet<ResourceInfo>();
		Iterator<Integer> it = state.getAllResourceIds().iterator();
		ResourceView cur;
		while(it.hasNext()){
			cur = state.getResourceNode(it.next());
			ret.add( new ResourceInfo(cur.getXPosition(), cur.getYPosition()));
		}
		return ret;
	}

    /**
     * You will implement this constructor. It will
     * extract all of the needed state information from the built in
     * SEPIA state view.
     *
     * You may find the following state methods useful:
     *
     * state.getXExtent() and state.getYExtent(): get the map dimensions
     * state.getAllResourceIDs(): returns all of the obstacles in the map
     * state.getResourceNode(Integer resourceID): Return a ResourceView for the given ID
     *
     * For a given ResourceView you can query the position using
     * resource.getXPosition() and resource.getYPosition()
     *
     * For a given unit you will need to find the attack damage, range and max HP
     * unitView.getTemplateView().getRange(): This gives you the attack range
     * unitView.getTemplateView().getBasicAttack(): The amount of damage this unit deals
     * unitView.getTemplateView().getBaseHealth(): The maximum amount of health of this unit
     *
     * @param state Current state of the episode
     */
    public GameState(State.StateView state) {
    	xExtent = state.getXExtent();
    	yExtent = state.getYExtent();
    	mmUnits = extractUnitInfo(state.getUnits(0));
    	archers = extractUnitInfo(state.getUnits(1));
    	resources = extractResourceInfo(state);
    	turnNumber = state.getTurnNumber();
    }
    
    public GameState(int xExtent, int yExtent, List<UnitInfo> mmUnits, List<UnitInfo> archers, Set<ResourceInfo> resources, int turnNumber){
    	this.xExtent = xExtent;
    	this.yExtent = yExtent;
    	this.mmUnits = mmUnits;
    	this.archers = archers;
    	this.resources = resources;
    	this.turnNumber = turnNumber;
    }

    /**
     * You will implement this function.
     *
     * You should use weighted linear combination of features.
     * The features may be primitives from the state (such as hp of a unit)
     * or they may be higher level summaries of information from the state such
     * as distance to a specific location. Come up with whatever features you think
     * are useful and weight them appropriately.
     *
     * It is recommended that you start simple until you have your algorithm working. Then watch
     * your agent play and try to add features that correct mistakes it makes. However, remember that
     * your features should be as fast as possible to compute. If the features are slow then you will be
     * able to do less plys in a turn.
     *
     * Add a good comment about what is in your utility and why you chose those features.
     *
     * @return The weighted linear combination of the features
     */
    public double getUtility() {
    	 double ret = 0;
    	 for(UnitInfo mmInfo: mmUnits){
    		 for(UnitInfo archInfo : archers){
    			 ret -= Math.abs(mmInfo.x - archInfo.x) + Math.abs(mmInfo.y - archInfo.y);
    		 }
    	 }
    	 return ret;
    }
    
    public boolean isMMTurn(){
    	return this.turnNumber % 2 == 0;
    }

    /**
     * You will implement this function.
     *
     * This will return a list of GameStateChild objects. You will generate all of the possible
     * actions in a step and then determine the resulting game state from that action. These are your GameStateChildren.
     *
     * You may find it useful to iterate over all the different directions in SEPIA.
     *
     * for(Direction direction : Directions.values())
     *
     * To get the resulting position from a move in that direction you can do the following
     * x += direction.xComponent()
     * y += direction.yComponent()
     *
     * @return All possible actions and their associated resulting game state
     */
    public List<GameStateChild> getChildren() {
    	List<GameStateChild> ret = new LinkedList<GameStateChild>();
    	Map<Integer, Action> actions = new HashMap<Integer, Action>();
    	Action unit1Actions[] = new Action[10];
    	UnitInfo unit1Positions[] = new UnitInfo[10];
    	Action unit2Actions[] = null;
    	UnitInfo unit2Positions[] = null;
    	int index = 0;
    	UnitInfo unit1 = null;

    	UnitInfo unit2 = null;
    	if(isMMTurn()){
    		unit1 = mmUnits.get(0);
    		if(mmUnits.size() > 1){
    			unit2 = mmUnits.get(1);

        		unit2Actions = new Action[10];
        		unit2Positions = new UnitInfo[10];
    		}
    	}else{
    		unit1 = archers.get(0);
    		if(archers.size() > 0){
    			unit2 = archers.get(1);

        		unit2Actions = new Action[10];
        		unit2Positions = new UnitInfo[10];
    		}
    	}
    	
    	for( Direction direction : Direction.values()){
    		unit1Actions[index] = Action.createPrimitiveMove(unit1.id, direction);
    		unit1Positions[index] = new UnitInfo(unit1.id, unit1.x + direction.xComponent(), unit1.y + direction.yComponent(), 
    				unit1.range, unit1.attk, unit1.curHealth, unit2.baseHealth);
    		if(unit2 != null){
    			unit2Actions[index] = Action.createPrimitiveMove(unit1.id, direction);
        		unit2Positions[index] = new UnitInfo(unit2.id, unit2.x + direction.xComponent(), unit2.y + direction.yComponent(), 
        				unit2.range, unit2.attk, unit2.curHealth, unit2.baseHealth);
    		}
    		index++;
    	}
    	for(int unit1Move = 0; unit1Move < 10 && unit1Actions[unit1Move] != null; unit1Move++){
    		Action curUnit1Action = unit1Actions[unit1Move];
    		UnitInfo nextUnit1 = unit1Positions[unit1Move];
    		if(unit2 != null){
	    		for( int unit2Move = 0; unit2Move < 10 && unit2Actions[unit2Move] != null; unit2Move++){
	    			Action curUnit2Action = unit2Actions[unit2Move];
	    			UnitInfo nextUnit2 = unit2Positions[unit2Move];
	    			actions.put(nextUnit1.id, curUnit1Action);
	    			actions.put(nextUnit2.id, curUnit2Action);
	    			
	    			List<UnitInfo> newMMUnits = new LinkedList<UnitInfo>();
	    			newMMUnits.add(nextUnit1);
	    			newMMUnits.add(nextUnit2);
	    			GameStateChild child = new GameStateChild(actions, new GameState(this.xExtent, this.yExtent, newMMUnits, 
	    					this.archers, this.resources, this.turnNumber + 1));
	    			ret.add(child);
	    		}
    		}else{
    			actions.put(nextUnit1.id, curUnit1Action);
    			List<UnitInfo> newMMUnits = new LinkedList<UnitInfo>();
    			newMMUnits.add(nextUnit1);
    			GameStateChild child = new GameStateChild(actions, new GameState(this.xExtent, this.yExtent, newMMUnits, 
    					this.archers, this.resources, this.turnNumber + 1));
    			ret.add(child);
    		}
    		actions.clear();
    	}
    	
    	return ret;
    }
}
