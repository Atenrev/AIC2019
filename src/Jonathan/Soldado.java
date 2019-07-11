package Jonathan;

import aic2019.*;

public class Soldado {
    UnitController uc;
    private Location meetPoint;

    private Tactica tactica;

    public Soldado(UnitController uc) {
        this.uc = uc;
    }

    public void run () {
        tactica = new Tactica(uc);
        tactica.setTopPriority(100155);
        tactica.setUnitsCount(tactica.getUnitsCount()+1);

        int x, y;

        if (uc.getTeam().getInitialLocation().x > uc.getOpponent().getInitialLocation().x)
            x = (-uc.getTeam().getInitialLocation().x + uc.getOpponent().getInitialLocation().x) / 4 + uc.getTeam().getInitialLocation().x;
        else
            x = (uc.getTeam().getInitialLocation().x - uc.getOpponent().getInitialLocation().x) / 4 + uc.getOpponent().getInitialLocation().x;

        if (uc.getTeam().getInitialLocation().y < uc.getOpponent().getInitialLocation().y)
            y = (-uc.getTeam().getInitialLocation().y + uc.getOpponent().getInitialLocation().y) / 4 + uc.getTeam().getInitialLocation().y;
        else
            y = (uc.getTeam().getInitialLocation().y - uc.getOpponent().getInitialLocation().y) / 4 + uc.getOpponent().getInitialLocation().y;

        meetPoint = new Location(x,y);

        while (true){

            int tactics_type = tactica.getType();

            if (tactics_type == 0)
                AccumulateForces();
            else if (tactics_type == 1)
                WideAttack();

            uc.yield(); //End of turn
        }
    }

    private void AccumulateForces() {
        UnitInfo[] aliados = uc.senseUnits(uc.getTeam(),false);

        if (aliados.length > 25)
            tactica.setType(1);

        UnitInfo[] visibleEnemies = uc.senseUnits(uc.getTeam(), true);
        for (int i = 0; i < visibleEnemies.length; ++i) {
            if (uc.canAttack(visibleEnemies[i].getLocation())) uc.attack(visibleEnemies[i].getLocation());
        }

        moveTo(meetPoint);
    }

    private void WideAttack() {
        Location target = tactica.getCuadranteObjetivo();

        UnitInfo[] aliados = uc.senseUnits(uc.getTeam(),false);

        if (aliados.length < 3 && distanceToBase())
            tactica.setType(0);

        moveTo(target);

        UnitInfo[] visibleEnemies = uc.senseUnits(uc.getTeam(), true);
        for (int i = 0; i < visibleEnemies.length; ++i) {
            if (uc.canAttack(visibleEnemies[i].getLocation())) uc.attack(visibleEnemies[i].getLocation());
        }
    }

    private boolean distanceToBase() {
        if (Math.abs(uc.getLocation().x - uc.getTeam().getInitialLocation().x) > 25
        || Math.abs(uc.getLocation().y - uc.getTeam().getInitialLocation().y) > 25)
            return true;
        else
            return false;
    }

    final int INF = 1000000;

    boolean rotateRight = true; //if I should rotate right or left
    Location lastObstacleFound = null; //latest obstacle I've found in my way
    int minDistToEnemy = INF; //minimum distance I've been to the enemy while going around an obstacle
    Location prevTarget = null; //previous target

    void moveTo(Location target){
        //No target? ==> bye!
        if (target == null) return;

        //different target? ==> previous data does not help!
        if (prevTarget == null || !target.isEqual(prevTarget)) resetPathfinding();

        //If I'm at a minimum distance to the target, I'm free!
        Location myLoc = uc.getLocation();
        int d = myLoc.distanceSquared(target);
        if (d <= minDistToEnemy) resetPathfinding();

        //Update data
        prevTarget = target;
        minDistToEnemy = Math.min(d, minDistToEnemy);

        //If there's an obstacle I try to go around it [until I'm free] instead of going to the target directly
        Direction dir = myLoc.directionTo(target);
        if (lastObstacleFound != null) dir = myLoc.directionTo(lastObstacleFound);

        //This should not happen for a single unit, but whatever
        if (uc.canMove(dir)) resetPathfinding();

        //I rotate clockwise or counterclockwise (depends on 'rotateRight'). If I try to go out of the map I change the orientation
        //Note that we have to try at most 16 times since we can switch orientation in the middle of the loop. (It can be done more efficiently)
        for (int i = 0; i < 16; ++i){
            if (uc.canMove(dir)){
                uc.move(dir);
                return;
            }
            Location newLoc = myLoc.add(dir);
            if (uc.isOutOfMap(newLoc)) rotateRight = !rotateRight;
                //If I could not go in that direction and it was not outside of the map, then this is the latest obstacle found
            else lastObstacleFound = myLoc.add(dir);
            if (rotateRight) dir = dir.rotateRight();
            else dir = dir.rotateLeft();
        }

        if (uc.canMove(dir)) uc.move(dir);
    }

    //clear some of the previous data
    void resetPathfinding(){
        lastObstacleFound = null;
        minDistToEnemy = INF;
    }
}
