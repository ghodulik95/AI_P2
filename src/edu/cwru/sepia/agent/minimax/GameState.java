package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
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

	private static final int PLENTY_OBSTACLES = 6;
	//We will keep track of the boundaries of the map
	private int xExtent;
	private int yExtent;
	//We will keep track of the minimax units (mmUnits == footmen) and the archers
	private List<UnitInfo> mmUnits;
	private List<UnitInfo> archers;
	//We will keep track of where the resources are
	private Set<MapLocation> resources;
	//We will keep track of the turn number
	private int turnNumber;
	
	/**
	 * In the UnitInfo class, we store all relevant information on units
	 */
	private class UnitInfo{
		//The unit's id
		public final int id;
		//The unit's coordinates
		public final int x;
		public final int y;
		//The unit's stats
		public final int range;
		public final int attk;
		public final int curHealth;
		public final int baseHealth;
		//Who's side the unit is on (is it a footman or archer, us or them)
		public final boolean isMMUnit;
		
		//Basic constructor
		public UnitInfo(int id, int x, int y, int range, int attk, int curHealth, int baseHealth, boolean isMMUnit){
			this.id = id;
			this.x = x;
			this.y = y;
			this.range = range;
			this.attk = attk;
			this.curHealth = curHealth;
			this.baseHealth = baseHealth;
			this.isMMUnit = isMMUnit;
		}
		
		//Returns true if this unit is at the coordinate (x,y)
		public boolean isAt(int x, int y){
			return this.x == x && this.y == y;
		}
		
		//Two units should be considered the same unit if they have the
		//same id (even if they are in dirrent spots, have a different HP, etc)
		@Override
		public int hashCode(){
			return Integer.valueOf(id).hashCode();
		}
		@Override
		public boolean equals(Object o){
			if(o instanceof UnitInfo){
				UnitInfo u = (UnitInfo) o;
				return u.id == this.id;
			}
			return false;
		}
		
	}
	

	/**
	 * Given a list of units, return the one at (x,y), or null if there is none
	 * @param units the list on units
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @return
	 */
	public UnitInfo getUnitAt(List<UnitInfo> units, int x, int y) {
		Iterator<UnitInfo> it = units.iterator();
		UnitInfo cur;
		while(it.hasNext()){
			cur = it.next();
			if(cur.isAt(x, y)){
				return cur;
			}
		}
		return null;
	}
	
	/**
	 * Takes a list of UnitView's and returns the same in UnitInfo form
	 * @param units	a list of units
	 * @param isMMUnits	whether or not the units are minimax units (footmen)
	 * @return
	 */
	private List<UnitInfo> extractUnitInfo(List<UnitView> units, boolean isMMUnits){
		//Iterate through the list of UnitViews, and build up a list of UnitInfos
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
							isMMUnits
							)
					);
		}
		return ret;
	}
	
	//Extract the MapLocations of the ResourceViews in the state
	private Set<MapLocation> extractResourceInfo(State.StateView state){
		Set<MapLocation> ret = new HashSet<MapLocation>();
		Iterator<Integer> it = state.getAllResourceIds().iterator();
		ResourceView cur;
		while(it.hasNext()){
			cur = state.getResourceNode(it.next());
			ret.add( new MapLocation(cur.getXPosition(), cur.getYPosition()));
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
    	//Get all the information we want
    	xExtent = state.getXExtent();
    	yExtent = state.getYExtent();
    	mmUnits = extractUnitInfo(state.getUnits(0), true);
    	archers = extractUnitInfo(state.getUnits(1), false);
    	resources = extractResourceInfo(state);
    	turnNumber = state.getTurnNumber();
    }
    
    //This is the constructor we primarily use, as the above StateView one is only called once
    public GameState(int xExtent, int yExtent, List<UnitInfo> mmUnits, List<UnitInfo> archers, Set<MapLocation> resources, int turnNumber){
    	this.xExtent = xExtent;
    	this.yExtent = yExtent;
    	this.mmUnits = mmUnits;
    	this.archers = archers;
    	this.resources = resources;
    	this.turnNumber = turnNumber;
    }

    private double getAstarMinPathSum(){
    	//Get the locations of the footmen
    	MapLocation footLocation1 = new MapLocation(mmUnits.get(0).x,mmUnits.get(0).y);
    	MapLocation footLocation2 = null;
    	if(mmUnits.size()>1)
    	{
    		 footLocation2 = new MapLocation(mmUnits.get(1).x,mmUnits.get(1).y);
    	}
    	
    	//These will be the stacks where the results of AstarSearch is saved
    	Stack<MapLocation> astarFoot1 = new Stack<MapLocation>();
    	Stack<MapLocation> astarFoor2 = new Stack<MapLocation>();
    	
    	//The current archer
    	MapLocation archLocation = null;
    	
    	//The initial values of s1 and s2 will be MAX, or s2 is 0 if footman 2 is dead
    	int s1 = Integer.MAX_VALUE;
    	int s2 = mmUnits.size() > 1 ? Integer.MAX_VALUE : 0;
    	//Get the minimum astar path length to any archer
    	for(UnitInfo archInfo : archers)
    	{
    		//Get this archer's location and call Astar
    		archLocation = new MapLocation(archInfo.x,archInfo.y);
    		astarFoot1 = AstarSearch(footLocation1, archLocation, this.xExtent, this.yExtent, null, this.resources);
    		
    		//If the result is less than s1, set s1 t it
    		if(astarFoot1.size()<s1)
    		{
    			s1 = astarFoot1.size();
    		}
        	//If the second footman is alive, do the same with s2
    		if(footLocation2 != null)
        	{
        		astarFoor2 = AstarSearch(footLocation2, archLocation, this.xExtent, this.yExtent, null, this.resources);
        		if(astarFoor2.size()<s2)
        		{
        			s2 = astarFoor2.size();
        		}
        	}	
    	}
    	return -(s1 + s2);
    }
    
    private double minDistanceNegativeSum(){
    	//Get the minimum distance to an archer for each mmUnit, and return the negative sum
		double dist = 0;
		for(UnitInfo mm : mmUnits){
			double minDist = Double.POSITIVE_INFINITY;
			for(UnitInfo arch : archers){
				double curDist = Math.abs(mm.x - arch.x) + Math.abs(mm.y - arch.y);
				if(curDist < minDist){
					minDist = curDist;
				}
			}
			dist -= minDist;
		}
		return dist;
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
    	//We split utility function into two cases:
    	//	1) There are plenty of obstacles
    	//			In this case, the utility is -(s1 + s2) where s1 and s2 are the lengths of the shortest paths
    	//			Of footman 1 and footman 2 (or just one of them if one is dead) to the nearest (path-wise, not distance-wise)
    	//			archer.
    	//	2) There are few obstacles
    	//		In this case, we think it is sufficient to use -(d1 + d2), where d1 and d2 are the distances to the nearest archers
    	//		for footman 2 and footman 2.
    	
    	//Case 1: if there are more plenty of obstacles (we use the number 6)
    	if(resources.size() >= PLENTY_OBSTACLES){
    		//Return the sum of the minimal astar paths from each mmUnit to any archer
	    	return getAstarMinPathSum();    		
    	}
    	//Case 2: if there are few obstacles
    	else{
    		//return the negative sum of all the closest distances to any archer from each footman
    		return minDistanceNegativeSum();
    	}
	}
    
    //We will consider even turn numbers to be MAX turns (footmen turns)
    public boolean isMMTurn(){
    	return this.turnNumber % 2 == 0;
    }
  
    /**
     * Gets the list of	 all possible moves for a unit in a given state
     * @param unit	the unit
     * @param preState	the state from before the function call
     * @return
     */
    private List<GameStateChild> getUnitMoves(UnitInfo unit, GameStateChild preState){
    	//Initialize the return list
    	List<GameStateChild> children = new ArrayList<GameStateChild>();
    	//Get this units allies and enemies
    	List<UnitInfo> enemies = unit.isMMUnit ? preState.state.archers : preState.state.mmUnits;
    	List<UnitInfo> myUnits = unit.isMMUnit ? preState.state.mmUnits : preState.state.archers;
    	
    	//Get the closest enemy that is in range (or otherwise null)
    	UnitInfo closeEnemy = getClosestEnemyWithinRange(unit, enemies);
    	//If there is such an enemy, make an attack move for it
    	if(closeEnemy != null){
    		//Make a list of all units on the map, updating the value of the enemy to have lower hp
			List<UnitInfo> allUnits = new ArrayList<UnitInfo>();
	    	allUnits.addAll(mmUnits);
	    	allUnits.addAll(archers);
			allUnits.remove(closeEnemy);
			allUnits.add(new UnitInfo(closeEnemy.id, closeEnemy.x, closeEnemy.y, closeEnemy.range, closeEnemy.attk, closeEnemy.curHealth - unit.attk, 
					closeEnemy.baseHealth, closeEnemy.isMMUnit));
			
			//Separate the units into mmUnits and archers
			List<UnitInfo> newMMUnits = new LinkedList<UnitInfo>();
			List<UnitInfo> newArchers = new LinkedList<UnitInfo>();
			separateMMUnits(allUnits, newMMUnits, newArchers);
			
			//Make a new game state and action map for this move
			GameState newGameState = new GameState(this.xExtent, this.yExtent, newMMUnits, newArchers, this.resources, this.turnNumber + 1);
			Map<Integer, Action> newActions = new HashMap<Integer, Action>();
			//Inherit parent's actions
			newActions.putAll(preState.action);
			//If this is a footman, add this action to the action map as well
			if(unit.isMMUnit){
				newActions.put(unit.id, Action.createPrimitiveAttack(unit.id, closeEnemy.id));
			}
			//Add this child to the return list
			children.add(new GameStateChild(newActions, newGameState));
    	}
    	
    	//For each direction, add a move to the list for any direction we can move to
    	for(Direction direction: Direction.values()){
    		//Get the new direction
    		int x = unit.x + direction.xComponent();
    		int y = unit.y + direction.yComponent();
    		//If it is valid to move to, make a move for it
    		if( canMoveTo(x,y,direction,myUnits,enemies) ){
    			//Make a list of all units, updating this unit to have a new coordinate
    			List<UnitInfo> allUnits = new ArrayList<UnitInfo>();
    	    	allUnits.addAll(mmUnits);
    	    	allUnits.addAll(archers);
    			allUnits.remove(unit);
    			allUnits.add(new UnitInfo(unit.id, x, y, unit.range, unit.attk, unit.curHealth, unit.baseHealth, unit.isMMUnit));
    			
    			//Separate archers from mmUnits
    			List<UnitInfo> newMMUnits = new LinkedList<UnitInfo>();
    			List<UnitInfo> newArchers = new LinkedList<UnitInfo>();
    			separateMMUnits(allUnits, newMMUnits, newArchers);
    			
    			//Make a new game state and action map for this action
    			GameState newGameState = new GameState(this.xExtent, this.yExtent, newMMUnits, newArchers, this.resources, this.turnNumber + 1);
    			Map<Integer, Action> newActions = new HashMap<Integer, Action>();
    			//Inherit parent's actions
    			newActions.putAll(preState.action);
    			if(unit.isMMUnit)	newActions.put(unit.id, Action.createPrimitiveMove(unit.id, direction));
    			children.add(new GameStateChild(newActions, newGameState));
    		}	
    	}
    	//Return the list of children
    	return children;
    }
    
    /**
     * Returns true if (x,y) is a valid location, direction is a valid direction (the assignment specifies we can only move up down left or right
     * (no diagonals), and also checks to see if there is a unit at (x,y)
     * @param x the c coord
     * @param y	the y coord
     * @param direction	the direction of the move
     * @param myUnits	the allies of the unit we are checking in regard to
     * @param enemies	the enemies of the unit we are checking in regard to
     * @return
     */
    private boolean canMoveTo(int x, int y, Direction direction,
			List<UnitInfo> myUnits, List<UnitInfo> enemies) {
    	//If the coordinate is valid and contains no resources or units, return true
    	return coordinateValid(direction,x,y) && !resourceAt(x,y) && getUnitAt(myUnits, x, y) == null &&  getUnitAt(enemies,x,y) == null;
	}

    /**
     * Returns a unit in the list of enemies if it is the closest enemy and it is within range
     * @param unit	the unit we are checking in regard to
     * @param enemies	the list of unit's enemies
     * @return
     */
	private UnitInfo getClosestEnemyWithinRange(UnitInfo unit,
			List<UnitInfo> enemies) {
		//Set the initial closest distance to MAX
    	int closestDist = Integer.MAX_VALUE;
    	UnitInfo closestUnit = null;
    	int curDist;
    	//Check each enemy
		for(UnitInfo enemy : enemies){
			//Get the distance to that enemy
			curDist = Math.max( Math.abs(unit.x - enemy.x), Math.abs(unit.y - enemy.y));
			//If it is within range and the closest enemy on record
			if(curDist <= unit.range && curDist < closestDist){
				//set closestDist and closestUnit accoridngly
				closestDist = curDist;
				closestUnit = enemy;
			}
		}
		return closestUnit;
	}

	/**
	 * Returns true if this location is a valid location on the map, and if the direction is not diagonal
	 * @param direction	a direction
	 * @param x	the x coord
	 * @param y	the y coord
	 * @return
	 */
	private boolean coordinateValid(Direction direction, int x, int y) {
		//The sum of the absolute values of the components of direction must be 1 in order to not be diagonal
		//And x and y must be between 0 and x or y extent, respectively
		return Math.abs(direction.xComponent()) + Math.abs(direction.yComponent()) == 1 && x >= 0 && y >= 0 && x < xExtent && y < yExtent;
	}

	/**
	 * Separates a list of mmUnits and archers into the mm list for mmUnits and arch list for archaers
	 * @param all	all units
	 * @param mm	Where we want to put the mmUnits
	 * @param arch	Where we want to put the archers
	 */
	private void separateMMUnits(List<UnitInfo> all, List<UnitInfo> mm, List<UnitInfo> arch){
		//Just loop through and add to the lists accordingly
    	for(UnitInfo u : all){
			if(u.isMMUnit){
				mm.add(u);
			}else{
				arch.add(u);
			}
		}
    }
    
	/**
	 * Return true if there is a resource at (x,y)
	 * @param x
	 * @param y
	 * @return
	 */
    private boolean resourceAt(int x, int y){
    	return resources.contains(new MapLocation(x, y));
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
    	List<GameStateChild> childrenToReturn = new LinkedList<GameStateChild>();
    	//Get units
    	UnitInfo unit1 = null;
    	UnitInfo unit2 = null;
    	//If it is minimax agent's turn (footmen)
    	if(isMMTurn()){
    		//unit1 is the first footman, and unit 2 is the second if it exists
    		unit1 = mmUnits.get(0);
    		if(mmUnits.size() > 1){
    			unit2 = mmUnits.get(1);
    		}
    	}
    	//Otherwise unit 1 is archer 1 and unit 2 is archer 2, if it exists
    	else{
    		unit1 = archers.get(0);
    		if(archers.size() > 1){
    			unit2 = archers.get(1);
    		}
    	}
    	
    	//Get the moves for unit 1
    	//Note here that it is necessary to get the moves for unit 1 and 2 discretely, or else they might try to do something
    	//like move to the same space at the same time
    	List<GameStateChild> unit1Moves = getUnitMoves(unit1, new GameStateChild(new HashMap<Integer, Action>(), this));
    	//Get the moves for unit2, if there is a unit2, and add them to the return list
    	if(unit2 != null){
    		for(GameStateChild child : unit1Moves){
    			childrenToReturn.addAll(getUnitMoves(unit2, new GameStateChild(child.action, child.state)));
    		}
    	}
    	//If there is no unit 2, just add all the unit1 moves
    	else{
    		childrenToReturn.addAll(unit1Moves);
    	}
    	//return the children
    	return childrenToReturn; 	
    }

    
    //All below code is copied and pasted from the last Astar assignment
    
	/**
	 * An abstraction for the map locations
	 * We implement comparable and override hashCode and equals so that we
	 * can properly use a PriorityQueue and HashMap with MapLocation
	 */
    class MapLocation implements Comparable<MapLocation>
    {
    	//The x and y coordinates of the location
        public int x, y;
        //The location we came from (null at head)
        public MapLocation cameFrom;
        //The f score of this location (f = g + h)
        public int f_score;
        //The g score of this location (g = cameFrom.g + 1);
        public int g_score;
        
        /**
         * Constructor when we only know or only need the x and y coordinates
         * @param x		the x coordinate
         * @param y		the y coordinate
         */
        public MapLocation(int x, int y){
        	this.x = x;
        	this.y = y;
        }
        
        /**
         * This is the default constructor the assignment came with
         * None of the code we added really uses it, but it was necessary to keep it
         * for the code the assignment came with
         * @param x the x coordinate
         * @param y the y coordinate
         * @param cameFrom	the MapLocation this once came from
         * @param cost	the cost to this location
         */
        public MapLocation(int x, int y, MapLocation cameFrom, float cost)
        {
            this(x,y);
            this.cameFrom = cameFrom;
        }
        
        /**
         * A constructor with set values for all the private variables
         * @param x the x coordinate
         * @param y the y coordinate
         * @param cameFrom	the MapLocation this once came from
         * @param cost	the cost to this location
         * @param f the f value (f = g + h)
         * @param g the g values
         */
        public MapLocation(int x, int y, MapLocation cameFrom, float cost, int f, int g)
        {
            this(x, y, cameFrom, cost);
            f_score = f;
            g_score = g;   
        }
        
        //Prints the x and y coordinate of this MapLocation in (x,y) format
        @Override
        public String toString()
        {
        	return "("+x+","+y+")";
        }
        
        //compareTo function to enable PriorityQueue
        //Simply subtract the other f score for this f score
		@Override
		public int compareTo(MapLocation o) {
			return (this.f_score - o.f_score);
		}
		
		//We needed to add a consistent hashCode for HashMaps to work
		//This is only based on the x and y coordinates
		//We use the default Integer hashCode, with a constant multiplier on the y
		@Override
		public int hashCode(){
			return Integer.valueOf(x).hashCode() + Integer.valueOf(x).hashCode()*23;
		}
		
		//We needed to add a consistent equals function for PriorityQueue and HashMaps to work
		//This is only based on the x and y coordinates because 2 MapLocations are equal
		//if they have the same coordinates, even if the other information is inconsistent.
		@Override
		public boolean equals(Object o){
			if(o instanceof MapLocation){
				MapLocation o_ml = (MapLocation) o;
				return this.x == o_ml.x && this.y == o_ml.y;
			}else{
				return false;
			}
		}
    }

    Stack<MapLocation> path;
    int footmanID, townhallID, enemyFootmanID;
    MapLocation nextLoc;

    private long totalPlanTime = 0; // nsecs
    private long totalExecutionTime = 0; //nsecs
    
    //For deciding whether or not we want to replan our path, one thing we
    //take into account is the dangerLevel.
    //For our purposes, dangerLevel is proportional to our concern that our
    //path is not optimal.
    //When dangerLevel >= MAX_DANGER_LEVEL, we will replan the path
    private int dangerLevel = 0;
    private final int MAX_DANGER_LEVEL = 4;
    //For deciding whether or not we want to replan our path, we also 
    //check to see if the enemy fotman is on our current path in within
    //MAX_LOOKAHEAD steps
    private final int MAX_LOOKAHEAD = 3;


    /**
     * This method should return true when the path needs to be replanned
     * and false otherwise. This will be necessary on the dynamic map where the
     * footman will move to block your unit.
     * 
     * There are two cases when we should replan:
     * 1) The dangerLevel is >= MAX_DANGER_LEVEL.  dangerLevel is related to
     * 		the 'danger' that we may be following a non-optimal path.
     * 		dangerLevel is incremented every time we think we are not on an
     * 		optimal pathm and then we replan when the dangerLevel hits the max.
     * 		This ensures that, when we replan the path because the enemy footman is in
     * 		our way, we check again soon to see if the footman moved out of the way.
     * 2) The enemy footman is within MAX_LOOKAHEAD steps ahead in out current chosen path.
     * 		If the enemy is in our way, we can't travel this path so we must replan.
     * 		However, we set dangerLevel to 1, which increments, ensuring that we will
     * 		try replanning again in MAX_DANGER_LEVEL - 1 steps.
     *
     * @param state		The state
     * @param history	The history
     * @param currentPath	The currentPath stack
     * @return	true if we should replan the path
     */
    private boolean shouldReplanPath(State.StateView state, History.HistoryView history, Stack<MapLocation> currentPath)
    {
        //Case 1: Check if danger level is too high
    	
    	//If we are in danger (danger Level == 0 when we are not in danger)
        if(dangerLevel != 0)
        {
        	//check if we are at max danger
	    	if(dangerLevel >= MAX_DANGER_LEVEL)
	        {
	        	//Set dangerLevel to 0 (we are no longer in danger, until we sense the
	        	//enemy footman again)
	        	dangerLevel = 0;
	        	//We should replan the path
	        	return true;
	        }
	    	//If danger is not max but also not zero, we must increment the danger level 
	    	//to ensure we'll try replanning the path soon
	    	dangerLevel++;
        }
        
        
        //Case 2: Check if the enemy footman is in the way of our current path
        //			(up to MAX_LOOKAHEAD future steps)
    	
        //Get the positions of our footman and the enemy footman
    	Unit.UnitView footmanUnit = state.getUnit(footmanID);
    	Unit.UnitView enemyFootmanUnit = state.getUnit(enemyFootmanID);
    	
    	//We just use the x and y coordinates since there isn't a built in
    	//getMapLocationFunction()
        int footmanX = footmanUnit.getXPosition();
        int footmanY = footmanUnit.getYPosition();

        int enemyFootmanX = enemyFootmanUnit.getXPosition();
        int enemyFootmanY = enemyFootmanUnit.getYPosition();
        
        //Calculate the Chebyshev distance from our footman to the enemy footman
        int chebyshevDist = (int)Math.max( Math.abs((double)(footmanX - enemyFootmanX)),
										Math.abs((double)(footmanY - enemyFootmanY))) ;
        
        //We know Case 2 is false if the Chebyshev distance from our footman to the enemy
        //is > MAX_LOOKAHEAD, so no use in looking ahead in that case.
        if(chebyshevDist <= MAX_LOOKAHEAD){
        	//Now we check the next MAX_LOOKAHEAD steps in our path to see if the enemy
        	//footman is there
        
	        int stepsLookingAhead = 0;
	        //Loop through the first checkDist locations, or until the path is over
	        for(int i = currentPath.size()-1; 0 <= i && stepsLookingAhead < MAX_LOOKAHEAD; i--, stepsLookingAhead++)
	        {
	        	//Get the ith location
	        	MapLocation m = currentPath.get(i);
	        	
	        	//If the footman is spotted at this location, we want to replan the path
	        	if(m.x == enemyFootmanX && m.y == enemyFootmanY)
	        	{
	        		//Set dangerLevel to 1, ensuring that we will replan again in
	        		//at most MAX_DANGER_LEVEL - 1 steps
	        		dangerLevel = 1;
	        		//We want to replan the path because the footman is in our way
	        		return true;
	        	}
	        }
        }
        
        //If the danger level is okay, and the footman is not currently in our way
        //we do not replan
        return false;
    }

	/**
     * This method is implemented for you. You should look at it to see examples of
     * how to find units and resources in Sepia.
     *
     * @param state
     * @return
     */
    private Stack<MapLocation> findPath(State.StateView state)
    {
        Unit.UnitView townhallUnit = state.getUnit(townhallID);
        Unit.UnitView footmanUnit = state.getUnit(footmanID);

        MapLocation startLoc = new MapLocation(footmanUnit.getXPosition(), footmanUnit.getYPosition(), null, 0);

        MapLocation goalLoc = new MapLocation(townhallUnit.getXPosition(), townhallUnit.getYPosition(), null, 0);

        MapLocation footmanLoc = null;
        if(enemyFootmanID != -1) {
            Unit.UnitView enemyFootmanUnit = state.getUnit(enemyFootmanID);
            footmanLoc = new MapLocation(enemyFootmanUnit.getXPosition(), enemyFootmanUnit.getYPosition(), null, 0);
        }

        // get resource locations
        List<Integer> resourceIDs = state.getAllResourceIds();
        Set<MapLocation> resourceLocations = new HashSet<MapLocation>();
        for(Integer resourceID : resourceIDs)
        {
            ResourceNode.ResourceView resource = state.getResourceNode(resourceID);

            resourceLocations.add(new MapLocation(resource.getXPosition(), resource.getYPosition(), null, 0));
        }

        return AstarSearch(startLoc, goalLoc, state.getXExtent(), state.getYExtent(), footmanLoc, resourceLocations);
    }
    /**
     * This is the method you will implement for the assignment. Your implementation
     * will use the A* algorithm to compute the optimum path from the start position to
     * a position adjacent to the goal position.
     *
     * You will return a Stack of positions with the top of the stack being the first space to move to
     * and the bottom of the stack being the last space to move to. If there is no path to the townhall
     * then return null from the method and the agent will print a message and do nothing.
     * The code to execute the plan is provided for you in the middleStep method.
     *
     * As an example consider the following simple map
     *
     * F - - - -
     * x x x - x
     * H - - - -
     *
     * F is the footman
     * H is the townhall
     * x's are occupied spaces
     *
     * xExtent would be 5 for this map with valid X coordinates in the range of [0, 4]
     * x=0 is the left most column and x=4 is the right most column
     *
     * yExtent would be 3 for this map with valid Y coordinates in the range of [0, 2]
     * y=0 is the top most row and y=2 is the bottom most row
     *
     * resourceLocations would be {(0,1), (1,1), (2,1), (4,1)}
     *
     * The path would be
     *
     * (1,0)
     * (2,0)
     * (3,1)
     * (2,2)
     * (1,2)
     *
     * Notice how the initial footman position and the townhall position are not included in the path stack
     *
     * @param start Starting position of the footman
     * @param goal MapLocation of the townhall
     * @param xExtent Width of the map
     * @param yExtent Height of the map
     * @param resourceLocations Set of positions occupied by resources
     * @return Stack of positions with top of stack being first move in plan
     */
    private Stack<MapLocation> AstarSearch(MapLocation start, MapLocation goal, int xExtent, int yExtent, MapLocation enemyFootmanLoc, Set<MapLocation> resourceLocations)
    {
    	//Initialize the open and closed sets
    	//Closed set is a HashMap in order to have constant .contains() check
    	Set<MapLocation> closedSet = new HashSet<MapLocation>();
    	
    	//Open set is a priority queue in order to have logn getMinF value
    		//A possible optimization would be to have an additional HashSet version of openSet
    		//because it would have a O(1) .contains() method rather than O(logn)
    		//The expense of this optimization would be that it requires double the space
    		// -- an expense which was not optimal for our smaller sample sizes
    	PriorityQueue<MapLocation> openSet = new PriorityQueue<MapLocation>();
    	
    	//The given MapLocation is not the right format for a HashSet and Priority Queue, 
    	//so we convert it and add it to the open set
    	start = new MapLocation(start.x, start.y, start.cameFrom, 0, hfun(start, goal), 0);
    	openSet.add(start);
    	
    	MapLocation current;
    	//While the open set is not empty
    	while(!openSet.isEmpty())
    	{
    		//Remove the best (lowest F value) location from the open set
    		//remove() for a priority queue will remove the best location
    		//based on the .compareTo() function we have defined
    		current = openSet.remove();
    		
    		//If this is the goal, we've completed the search
    		if(current.equals(goal))
    		{
    			return reconstructPath(start,current);
    		}
    		//Otherwise, add this location to the closed set
    		closedSet.add(current);
    		//Get the successors of this node
    		Set<MapLocation> sucessors = getSucessors(current, enemyFootmanLoc, resourceLocations, xExtent, yExtent);
    		
    		//Check each successor
    		for(MapLocation neighbor : sucessors)
    		{

    			//If it is not in the open set
				if(!openSet.contains(neighbor)){
					//Add g and f scores to neighbor
					int g_score_estimate = current.g_score + 1;
					MapLocation n = new MapLocation(neighbor.x, neighbor.y, current, 0, 
							g_score_estimate + hfun(neighbor, goal), g_score_estimate);
					
					//Add it to the open set if its not in the closed set
					if(!closedSet.contains(n))
						openSet.add(n);
				}
    			
    		}
    	}
        // there is no path
    	System.out.println("No Available Path");
        return new Stack<MapLocation>();
    }
 
    /**
     * Rebuilds the path by taking the parent location and backtracking through its cameFrom
     * @param start The start of the path	(where the footman starts)
     * @param end	The end of the path (the fort)
     * @return	a stack form of the path
     */
    private Stack<MapLocation> reconstructPath(MapLocation start, MapLocation end)
    {
    	//Initialize our returned stack
    	Stack<MapLocation> toReturn = new Stack<MapLocation>();
    	
    	//We start from the end
    	//We do not include the end and start in the stack because the end is the
    	//fort (we cannot travel there) and the start is our footman.
    	MapLocation current = end;
    	//Until we reach the start, keep pulling from the cameFrom value
    	while(!current.equals(start))
    	{
    		current = current.cameFrom;
    		//Push the cameFrom to our return stack
    		toReturn.push(current);
    	}
    	
    	//Return the completed path
    	return toReturn;
    	
    }
    
    /**
     * Finds all the possible children locations from the current location
     * @param current The current location
     * @param enemyFootmanLoc	The Location of the enemy footman
     * @param resourceLocations	The set of resource locations
     * @param xExtent	The extent of the x coordinate of the grid
     * @param yExtent	The extent of the y coordinate of the grid
     * @return	Returns a set of the successors
     */
    private Set<MapLocation> getSucessors(MapLocation current, MapLocation enemyFootmanLoc, Set<MapLocation> resourceLocations, int xExtent, int yExtent)
    {
    	//Initialize the returned set
    	Set<MapLocation> toReturn = new HashSet<MapLocation>();
    	//We must check all the grid locations around current
    	//-- up, down, left, right, and diagonoals
    	for(int i = -1; i<=1; i++)
    	{
    		for(int j = -1; j<=1; j++)
    		{
    			if(Math.abs(i)+Math.abs(j) > 1) continue;
    			//Make the map location of this potential succesor
    			MapLocation loc = new MapLocation(current.x+i,current.y+j);
    			//Check if there's anything already in this potential successor
    			if(isValidLocation(current, loc, enemyFootmanLoc, resourceLocations, xExtent,  yExtent))
    			{
    				//If there's nothing at loc, it is a successor, so add it to the return set
    				toReturn.add(loc);
    			}
    		}
    	}
    	return toReturn;
    	
    }

    /**
     * Checks if a location is one that is possible to move to, ie it is empty.
     * @param current The current location
     * @param loc	The location we are checking
     * @param enemyFootmanLoc	The enemy footman location
     * @param resourceLocations	The set of resource locations
     * @param xExtent	The extent of the x coordinate of the grid
     * @param yExtent	The extent of the y coordinate of the grid
     * @return	True if the loc is empty, false otherwise
     */
    private boolean isValidLocation(MapLocation current, MapLocation loc, MapLocation enemyFootmanLoc, Set<MapLocation> resourceLocations, int xExtent, int yExtent)
    {
    	//If there is a resource at loc, return false
    	if (resourceLocations.contains(loc))
    		return false;
    	//If the location is the current (our fighter is currently there) return false
    	else if(loc.equals(current))
    		return false;
      	//If the location is off the board, return false
    	else if(loc.x<0 || loc.y<0 || loc.x>=xExtent || loc.y>=yExtent)
    		return false;
    	//If there is an enemy footman, and it is at loc, return false
    	else if(enemyFootmanLoc!= null && loc.equals(enemyFootmanLoc))
    		return false;
    	//Otherwise, loc is valid
    	else
    		return true;
    }
    
    /**
     * The heuristic described by the assignment (Chebyshev distance).
     * @param start The start location (location we are testing from)
     * @param goal	The goal location
     * @return	the chebyshev distance (MAX( |start.x = goal.x|, |start.y - goal.y| )
     */
    private int hfun(MapLocation start, MapLocation goal)
    {
    	return (int)(Math.abs(start.x - goal.x) + Math.abs(start.y - goal.y));
    }
    
    /**
     * Primitive actions take a direction (e.g. NORTH, NORTHEAST, etc)
     * This converts the difference between the current position and the
     * desired position to a direction.
     *
     * @param xDiff Integer equal to 1, 0 or -1
     * @param yDiff Integer equal to 1, 0 or -1
     * @return A Direction instance (e.g. SOUTHWEST) or null in the case of error
     */
    private Direction getNextDirection(int xDiff, int yDiff) {

        // figure out the direction the footman needs to move in
        if(xDiff == 1 && yDiff == 1)
        {
            return Direction.SOUTHEAST;
        }
        else if(xDiff == 1 && yDiff == 0)
        {
            return Direction.EAST;
        }
        else if(xDiff == 1 && yDiff == -1)
        {
            return Direction.NORTHEAST;
        }
        else if(xDiff == 0 && yDiff == 1)
        {
            return Direction.SOUTH;
        }
        else if(xDiff == 0 && yDiff == -1)
        {
            return Direction.NORTH;
        }
        else if(xDiff == -1 && yDiff == 1)
        {
            return Direction.SOUTHWEST;
        }
        else if(xDiff == -1 && yDiff == 0)
        {
            return Direction.WEST;
        }
        else if(xDiff == -1 && yDiff == -1)
        {
            return Direction.NORTHWEST;
        }

        System.err.println("Invalid path. Could not determine direction");
        return null;
    }
}
