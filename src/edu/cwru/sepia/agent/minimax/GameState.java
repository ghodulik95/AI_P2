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
		public final int HP;
		public final int baseHealth;
		public final boolean isMMUnit;
		
		public UnitInfo(int id, int x, int y, int range, int attk, int HP, int baseHealth, boolean isMM){
			this.id = id;
			this.x = x;
			this.y = y;
			this.range = range;
			this.attk = attk;
			this.HP = HP;
			this.baseHealth = baseHealth;
			this.isMMUnit = isMM;
		}

		public UnitInfo(UnitInfo unit, int newX, int newY) {
			this(unit.id, newX, newY, unit.range, unit.attk, unit.HP, unit.baseHealth, unit.isMMUnit);
		}

		public boolean isAt(int x, int y) {
			return this.x == x && this.y == y;
		}
		
		@Override
		public boolean equals(Object o){
			if(o instanceof UnitInfo){
				UnitInfo u = (UnitInfo) o;
				return u.id == this.id;
			}
			return false;
		}
		
		@Override
		public int hashCode(){
			return Integer.valueOf(id).hashCode();
		}
		
	}
	
	private List<UnitInfo> extractUnitInfo(List<UnitView> units, boolean isMM){
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
							curTemp.getBaseHealth(),
							isMM)
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
    	mmUnits = extractUnitInfo(state.getUnits(0), true);
    	archers = extractUnitInfo(state.getUnits(1), false);
    	resources = extractResourceInfo(state);
    	turnNumber = 0;
    }

    public GameState(GameState gameState, List<UnitInfo> otherUnits) {
		xExtent = gameState.xExtent;
		yExtent = gameState.yExtent;
		resources = gameState.resources;
		
		mmUnits = new ArrayList<UnitInfo>();
		archers = new ArrayList<UnitInfo>();
		
		List<UnitInfo> allUnits = new ArrayList<UnitInfo>();
		allUnits.addAll(gameState.mmUnits);
		allUnits.addAll(gameState.archers);
		
		setUnits(otherUnits, allUnits);
	}

	private void setUnits(List<UnitInfo> otherUnits, List<UnitInfo> allUnits) {
		for(UnitInfo unit : otherUnits){
			if(unit.isMMUnit){
				mmUnits.add(unit);
			}else{
				archers.add(unit);
			}

			allUnits.remove(unit);
		}
		for(UnitInfo unit : allUnits){
			if(unit.isMMUnit){
				mmUnits.add(unit);
			}else{
				archers.add(unit);
			}
		}
	}

	public void incrementTurn(){
		turnNumber++;
	}
	
	public boolean isMMTurn(){
		return turnNumber % 2 == 0;
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
    	
    	if(isMMTurn()){
    		UnitInfo unit1 = mmUnits.get(0);
    		UnitInfo unit2 = mmUnits.size() > 1 ? mmUnits.get(1) : null;
    		List<GameStateChild> unit1Moves = getAllMoves(unit1, new ArrayList<UnitInfo>(), new HashMap<Integer, Action>(), 0, false);
    		for(GameStateChild move : unit1Moves){
    			List<GameStateChild> unit2Moves = getAllMoves(unit2, move.state.getAllUnits(), move.action, 1, true);
    			if(unit2 != null){
    			for(GameStateChild secondMove : unit2Moves){
    				if(!secondMove.state.unitsOverlap(true)){
    					ret.add(secondMove);
    				}
    			}
    			}else
    				ret.add(move);
    		}
    	}else{
    		this.incrementTurn();
    		ret.add(new GameStateChild(new HashMap<Integer, Action>(), this));
    	}
    	
    	return ret;
    }
    
    private boolean unitsOverlap(boolean isMMTurn) {
		UnitInfo unit1;
		UnitInfo unit2;
		if(isMMTurn){
			unit1 = mmUnits.get(0);
			unit2 = mmUnits.get(1);
		}else{
			if(archers.size() == 1){
				return false;
			}
			unit1 = archers.get(0);
			unit2 = archers.get(1);
		}
		return unit1.isAt(unit2.x, unit2.y);
	}

	private List<UnitInfo> getAllUnits() {
		ArrayList<UnitInfo> allUnits = new ArrayList<UnitInfo>();
		allUnits.addAll(mmUnits);
		allUnits.addAll(archers);
		return allUnits;
	}

	private List<GameStateChild> getAllMoves(UnitInfo unit, List<UnitInfo> otherUnits, Map<Integer, Action> otherActions, int nextActionNum, boolean incTurn){
    	List<GameStateChild> toReturn = new ArrayList<GameStateChild>();
    	
    	for(Direction direction : Direction.values()){
    		int x = unit.x + direction.xComponent();
    		int y = unit.y + direction.yComponent();
    		if(!validLocation(x,y)){
    			continue;
    		}
    		UnitInfo inTheWay = getUnitAt(x,y,otherUnits, unit.id);
    		if(inTheWay == null){
    			Action a = Action.createPrimitiveMove(unit.id, direction);
    			otherActions.put(nextActionNum, a);
    			otherUnits.add(new UnitInfo(unit, x,y));
    			GameState gs = new GameState(this, otherUnits);
    			if(incTurn){
    				gs.incrementTurn();
    			}
    			toReturn.add(new GameStateChild(otherActions, gs));
    		}
    	}
    	
    	return toReturn;
    }

	private boolean validLocation(int x, int y) {
		return x >= 0 && y >= 0 && x < xExtent && y < yExtent && !resources.contains(new ResourceInfo(x,y));
	}

	private UnitInfo getUnitAt(int x, int y, List<UnitInfo> others, int myID) {
		for(UnitInfo other : others){
			if(other.isAt(x,y)){
				return other;
			}
		}
		List<UnitInfo> allUnits = new ArrayList<UnitInfo>();
		allUnits.addAll(mmUnits);
		allUnits.addAll(archers);
		for(UnitInfo other : allUnits){
			if(other.isAt(x, y) && other.id != myID){
				return other;
			}
		}
		return null;
	}
}
