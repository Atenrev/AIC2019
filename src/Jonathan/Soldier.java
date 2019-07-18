package Jonathan;

import aic2019.*;

import java.util.Arrays;

public class Soldier {
    UnitController uc;
    private Location meetPoint;

    private Tactica tactica;
    Location target;

    // Variables soldier
    int tactics_mode = 0;
    int soldierState = 0;
    float hp_limit = 0;
    boolean enemyInSight = false;
    boolean isDeleted = false;
    UnitInfo enemy;

    public Soldier (UnitController uc) {
        this.uc = uc;
    }

    public void run () {
        hp_limit = uc.getInfo().getHealth()/2;
        tactica = new Tactica(uc);
        tactica.setTopPriority(100155);
        tactica.setUnit();
        tactica.updatePriority();

        int x, y;

        if (uc.getTeam().getInitialLocation().x > tactica.getObjetivo().x)
            x = (-uc.getTeam().getInitialLocation().x + tactica.getObjetivo().x) / 3 + uc.getTeam().getInitialLocation().x;
        else
            x = (uc.getTeam().getInitialLocation().x - tactica.getObjetivo().x) / 3 + tactica.getObjetivo().x;

        if (uc.getTeam().getInitialLocation().y < tactica.getObjetivo().y)
            y = (-uc.getTeam().getInitialLocation().y + tactica.getObjetivo().y) / 3 + uc.getTeam().getInitialLocation().y;
        else
            y = (uc.getTeam().getInitialLocation().y - tactica.getObjetivo().y) / 3 + tactica.getObjetivo().y;

        meetPoint = new Location(x,y);

        while (true){
            tactics_mode = tactica.getMode();
            target = tactica.getObjetivo();

            enemyInSight();

            //uc.println("Estado: " + soldierState);
            //uc.println("Tactica que tengo: " + tactics_mode);

            switch(tactics_mode){
                case 1:
                    accumulateForces();
                    break;
                case 2: // sera la 2
                    switch (soldierState) {
                        case 0:
                            moveGroupToTarget(target);
                            break;
                        case 1:
                            moveToEnemy();
                            break;
                        case 2:
                            rearguardAttack(target);
                            break;
                        case 3:
                            rearguardToTarget(target);
                            break;
                    }
                    break;
            }

            uc.yield(); //End of turn
        }
    }

        // TACTIC 2 - C0 FUNCTIONS
        /*
         *  - If there is no enemy in sight
         *
         * */
    private void moveGroupToTarget(Location target) {
        attackTown();
        if (tactica.getType() != -1 || getDistanceToPoint(target) > 25) {
            moveTo(target);
        }
    }

    private double getDistanceToPoint(Location loc) {
        return Math.pow(loc.x-uc.getLocation().x, 2) + Math.pow(loc.y-uc.getLocation().y, 2);
    }

        // TACTIC 2 - C1 FUNCTIONS
        /*
        *  - If there is enemy in sight
        *  - If the enemy detected remains in the range of vision
        * */
    private void moveToEnemy() {
        if (enemy != null) {
            Location enemy_location = enemy.getLocation();
            moveTo(enemy_location);

            if (uc.canAttack(enemy_location)) {
                uc.attack(enemy_location);
            }

            if (!enemyInSight || getDistanceToPoint(enemy_location) > 50) {
                soldierState = 0;
            }
        }
        else {
            attackTown();
            soldierState = 0;
        }
    }

    private boolean isRearguard (Location enemy) {
        Direction dir = uc.getLocation().directionTo(enemy);
        UnitInfo[] friends = uc.senseUnits(uc.getTeam(), false);
        for (UnitInfo friend : friends){
            Direction dirFriend = uc.getLocation().directionTo(friend.getLocation());
            double distanceFriend = getDistanceToPoint(friend.getLocation());

            if (distanceFriend < getDistanceToPoint(enemy) && dir == dirFriend) {
                return true;
            }
        }

        return false;
    }

    // TACTIC 2 - C2 FUNCTIONS
    private void rearguardAttack (Location target) {
        boolean rearguard = isRearguard(target);

        if(rearguard) {
            moveTo(target);
            if (uc.canAttack(target)) {
                uc.attack(target);
            }

            if (!enemyInSight) {
                soldierState = 3;
            }
        } else {
            //Direction newDir = directionToMove(uc.getLocation().directionTo(enemy.getLocation()));
            moveTo(meetPoint);
        }
    }

    // TACTIC 2 - C3 FUNCTIONS
    private void rearguardToTarget(Location target) {
        moveTo(target);
    }

    // FUNCTIONAL FUNCTIONS
    private void enemyInSight() {
        UnitInfo[] enemies = uc.senseUnits(uc.getTeam(), true);
        if (enemies.length > 0) {
            enemyInSight = true;
                // I keep this in case you want set at the beginning until he dies
            enemy = (enemy != null && Arrays.asList(enemies).contains(enemy)) ? enemy : obtainEnemy(enemies);

            if ((float) uc.getInfo().getHealth() <= hp_limit) {
                soldierState = 2;
                if (!isDeleted) {
                    tactica.deleteUnit();
                    isDeleted = true;
                }
            } else {
                soldierState = 1;
            }
        } else {
            enemyInSight = false;
            enemy = null;
            //enemy = null;

            if ((float) uc.getInfo().getHealth() <= hp_limit) {
                soldierState = 3;
            }/* else {
                soldierState = 0;
            }*/
        }
    }

    Direction directionToMove (Direction senseEnemy) {
        Direction dir = senseEnemy;
        //uc.println("[" + uc.getInfo().getID() + "]: Hay un enemigo en " + dir);

        Direction newDir = senseEnemy.opposite();

        while (newDir != dir){
            newDir = newDir.rotateLeft();
            //uc.println("[" + uc.getInfo().getID() + "]: LEFT - Yendo hacia " + newDir);
            if (uc.canMove(newDir)) return newDir;
        }

        // if we don't achieve moving to the left, try right
        newDir = senseEnemy.opposite();
        while (newDir != dir) {
            newDir = newDir.rotateRight();
            //uc.println("[" + uc.getInfo().getID() + "]: RIGHT - Yendo hacia " + newDir);
            if (uc.canMove(newDir)) return newDir;
        }

        return senseEnemy.opposite();
    }

    // Priority to the nearest enemy and with lower hp
    private UnitInfo obtainEnemy (UnitInfo[]array) {
        //UnitInfo objective = array[0];
        int minHp = 0;
        int minDistance = 0;

        for (int i = 0; i < array.length; i++) {
            minHp = array[i].getHealth() < array[minHp].getHealth() ? i : minHp;
            minDistance = array[i].getLocation().distanceSquared(uc.getLocation()) < array[minDistance].getLocation().distanceSquared(uc.getLocation()) ? i : minDistance;
        }

        return minHp == minDistance ? array[minHp] : array[minDistance];
    }

    private void attackTown() {
        if (tactica.getType() != 1) {
            TownInfo[] town = uc.senseTowns();
            if (town.length > 0)
                if (town[0].getOwner() != uc.getTeam())
                    uc.attack(town[0].getLocation());
        }
    }

    private void accumulateForces() {
        if (tactica.getUnitsCount() > 6)
            tactica.setMode(2);

        UnitInfo[] visibleEnemies = uc.senseUnits(uc.getTeam(), true);
        for (int i = 0; i < visibleEnemies.length; ++i) {
            if (uc.canAttack(visibleEnemies[i].getLocation())) uc.attack(visibleEnemies[i].getLocation());
        }

        moveTo(meetPoint);
    }

    private void wideAttack() {
        Location target = tactica.getObjetivo();

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
