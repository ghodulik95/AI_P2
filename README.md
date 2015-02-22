# P1
# Zachary Jaffee zij
# George Hodulik gmh73

Project 1 for intro to AI

In addition to writing the AstarSearch function and shouldReplanPath function as instructed,
we added several private methods to simplify our code, and also made some changes other parts
of the given code.

I feel the need to explain our shouldReplanPath a bit, as it initially looks a bit daunting.
(It is actually quite simple - I promise!)
Originally, we only checked to see if the enemy footman was on our current proposed path within
a certain number of steps.  If the footman was in the way, we returned true to replan the path.
However, this was not optimal for two primary reasons:
    1) Sometimes the footman was too far away for it to possibly be on our current path 
       (within a number of steps), so we were checking pointlessly.
    2) Just because the enemy footman was in our way does not mean it won't move out
       of the way in a step or two -- the optmimal path upon replanning may become drastically
       suboptimal in just a couple of steps if the footman moves out of the way.
       This was the case in the given map with our fist attempt of the shouldReplanPath() function.
    
    To address the first problem, we checked the Chebyshev distance between our footman and the enemy's,
    and only decided to look ahead in our path if the enemy is close enough for it to be worth worrying about.
    
    To address the second problem, we introduced the variable dangerLevel, which represents the 'danger' that
    our path may be suboptimal. Initially, danger is zero, meaning we are safe and our path should be optimal.
    When the enemy is in our way, triggering a path replan, we set dangerLevel equal to 1, and then increment it
    at each step. When dangerLevel reaches a maximum value, we replan the path again, just in
    case a better path opened up. We then set dangerLevel to zero, at this point convinced our path is optimal.
    Note that if the footman gets in the way again, it retriggers this danger cycle.

Modified given methods include: 
  Changes to MapLocation class:
    Adding a field for MapLocation to determine the parent (cameFrom)
    Adding f and g values to the MapLocation class
    Adding constructors that include the above variables
    Implementing compareTo() in order to be able to use a PriorityQueue open set in our A* Search
        compareTo() only uses the f score of a location, as that is all that determines if a MapLocation is "better"
        than another
    Overriding hashCode() with a consistent hash so that we could use a HashSet closed set in our A* Search
        hashCode() only uses the x and y coordinates of the MapLocation, as that is all that determines if 2 MapLocations
        are equal.
        Note that 2 MapLocations should be considered equals when they have the same coordinates -- even if the other
        values are different.
    Overriding equals() with a consistent equals function so that PriorityQueues and HashSets would be functional
        equals() only uses the x and y coordinates of the MapLocation, as that is all that determines if 2 MapLocations
        are equal.
    Overriding toString() to make printlines easier to write.
  
  Change in middleStep:
    We check to see if there is an enemyFootman (enemyFootmanID != -1) before we check if we should replan the path (by calling shouldReplanPath).
    We did this because we should never have to replan the path in the mazes where there is no enemy footman.
