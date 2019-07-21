package Dio;

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

    int round_beginning_waiting = 0;

    float hp_limit = 0;

    boolean enemyInSight = false;
    boolean isDeleted = false;
    boolean isWaiting = false;

    UnitInfo enemy;
    UnitInfo[] enemies;

    // CONSTANTS
    final int ACCOMPLISHED_TYPE = -1;
    final int LOCATION_TYPE = 1;
    final int NEUTRAL_TOWN_TYPE = 2;
    final int ENEMY_TYPE = 3;

    final int TURNS_TO_WAIT = 3;

    // POINTERS
    final int rpointer = 0;
    int last_rpointer = 0;
    final int apointer = 3000;
    int last_apointer = 0;
    final int epointer = 6000;
    int last_epointer = 0;
    final int num_resources_pointer = 199999;
    final int enemy_count_pointer = 200001;

    public Soldier (UnitController uc) {
        this.uc = uc;
    }

    public void run () {
        hp_limit = uc.getInfo().getHealth()/2;
        tactica = new Tactica(uc);
        tactica.setTopPriority(100155);
        tactica.setUnit();
        tactica.updatePriority();

        getMeetPoint();

        while (true){
            tactics_mode = tactica.getMode();
            target = tactica.getObjetivo();

            enemyInSight();

            // to do: verificar catapultas

            uc.println("State: " + soldierState);
            uc.println("Tactics mode: " + tactics_mode);


            switch(tactics_mode){
                case 1:
                    switch (soldierState) {
                        case 0:
                            accumulateForces();
                            break;
                        case 1:
                            moveToEnemyTactic1();
                            break;
                    }
                    break;
                case 2:
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

            observar();

            uc.yield(); //End of turn
        }
    }

    // TACTIC 1 - C0 FUNCTIONS
    private void accumulateForces() {
        uc.println("GetLastEnemyCount: " + tactica.getLastEnemyCount() + " , getUnitsCount: " + tactica.getUnitsCount());

        if (tactica.getUnitsCount() > tactica.getLastEnemyCount()) {

            if (!isWaiting){
                isWaiting = true;
                round_beginning_waiting = uc.getRound();
            }

            boolean next = waiting();
            uc.println("next: " + next);

            if (!next) {
                tactica.setMode(2);
            }
        }

        for (int i = 0; i < enemies.length; ++i) {
            if (uc.canAttack(enemies[i].getLocation())) {
                uc.attack(enemies[i].getLocation());
                checkEnemyExistence(enemies[i].getLocation());
            }
        }

        moveTo(meetPoint);
        attackTown();
    }

    // TACTIC 1 - C1 FUNCTIONS
    private void moveToEnemyTactic1() {
        if (enemy != null) {
            Location enemy_location = enemy.getLocation();
            moveTo(enemy_location);

            if (uc.canAttack(enemy_location)) {
                uc.attack(enemy_location);
                checkEnemyExistence(enemy_location);
            }

            if (!enemyInSight || getDistanceToPoint(meetPoint) >= 4) {
                soldierState = 0;
            }
        }
        else {
            attackTown();
            soldierState = 0;
        }

        if ((float) uc.getInfo().getHealth() <= hp_limit) {
            deleteUnit();
        }
    }

    // TACTIC 2 - C0 FUNCTIONS
        /* - If there is no enemy in sight */
    private void moveGroupToTarget(Location target) {
        if (getDistanceToPoint(target) > 25 || tactica.getType() != ACCOMPLISHED_TYPE) {
            moveTo(target);
        }
        if (!enemyInSight) {
            if ((float) uc.getInfo().getHealth() <= hp_limit) {
                soldierState = 3;
                deleteUnit();
            }
        } else {
            if ((float) uc.getInfo().getHealth() > hp_limit) {
                soldierState = 1;
            } else {
                soldierState = 2;
                deleteUnit();
            }
        }
        attackTown();
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
                checkEnemyExistence(enemy_location);
            }

            if (!enemyInSight || getDistanceToPoint(target) > 50) {
                soldierState = 0;
            }
        }
        else {
            attackTown();
            soldierState = 0;
        }

        if ((float) uc.getInfo().getHealth() <= hp_limit) {
            soldierState = 2;
            deleteUnit();
        }
    }

    // TACTIC 2 - C2 FUNCTIONS
    private void rearguardAttack (Location target) {
        boolean rearguard = isRearguard(target);
        boolean canAttack = uc.canAttack(target);

        if(rearguard || enemy.getType() == UnitType.ARCHER || enemy.getType() == UnitType.TOWER || (uc.getInfo().getHealth() >= enemy.getHealth()/2 && canAttack)) {
            moveTo(target);

            if (canAttack) {
                uc.attack(target);
                checkEnemyExistence(target);
            }

            if (!enemyInSight) {
                soldierState = 3;
            }
        } else {
            //Direction newDir = directionToMove(uc.getLocation().directionTo(enemy.getLocation()));
            moveTo(meetPoint);
        }
        if ((float) uc.getInfo().getHealth() <= hp_limit) {
            soldierState = 3;
            deleteUnit();
        }

        attackTown();
    }

    // TACTIC 2 - C3 FUNCTIONS
    private void rearguardToTarget(Location target) {
        if (isRearguard(target)) {
            moveTo(target);
        }

        if (enemyInSight) {
            soldierState = 2;
        }

        if ((float) uc.getInfo().getHealth() <= hp_limit) {
            deleteUnit();
        }

        attackTown();
    }

    // FUNCTIONAL FUNCTIONS
        // SENSE FUNCTIONS
    private void enemyInSight() {
        enemies = uc.senseUnits(uc.getTeam(), true);

        if (enemies.length > 0) {
            tactica.setLastEnemyCount(enemies.length);

            uc.println("GetLastEnemyCount: " + tactica.getLastEnemyCount() + " , getUnitsCount: " + tactica.getUnitsCount());

            if (tactica.getLastEnemyCount() >= tactica.getUnitsCount() && tactica.getMode() != 1 && tactica.getType() != ENEMY_TYPE
                || (tactica.getType() == ENEMY_TYPE && uc.read(enemy_count_pointer) >= tactica.getUnitsCount()) ) {

                if (!isWaiting){
                    isWaiting = true;
                    round_beginning_waiting = uc.getRound();
                }

                boolean next = waiting();
                uc.println("next: " + next);

                if (!next) {
                    tactica.setMode(1);
                }

                getMeetPoint();
            }

            enemyInSight = true;
                // I keep this in case you want set at the beginning until he dies
            enemy = (enemy != null && Arrays.asList(enemies).contains(enemy)) ? enemy : obtainEnemy(enemies);
        } else {
            enemyInSight = false;
            enemy = null;
        }
    }

    private void observar() {
        ResourceInfo[] recursos = uc.senseResources();

        for (ResourceInfo r : recursos) {
            if (r.getResource() == Resource.WOOD) {
                addResource(1, r.getLocation());
            }
            else if (r.getResource() == Resource.IRON) {
                addResource(2, r.getLocation());
            }
            else if (r.getResource() == Resource.CRYSTAL) {
                addResource(3, r.getLocation());
            }
        }

        for (UnitInfo e : enemies) {
            if (e.getTeam().equals(uc.getOpponent()))
                addEnemy(e.getID(), e.getLocation(), e.getType());
        }
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

    private UnitInfo checkEnemyDied(Location enemy_location) {
        return uc.senseUnit(enemy_location);
    }

    private void deleteUnit () {
        if (!isDeleted) {
            tactica.deleteUnit();
            isDeleted = true;
        }
    }

    private void checkEnemyExistence (Location loc) {
        if (checkEnemyDied(loc) == null && enemy.getTeam().equals(uc.getOpponent())) {
            uc.write(enemy_count_pointer, uc.read(enemy_count_pointer) - 1);
        }
    }

        // DIRECTION/LOCATION/DISTANCE FUNCTIONS
    private void moveTo(Location target){
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

    private Direction directionToMove (Direction senseEnemy) {
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

    private void getMeetPoint() {
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
    }

    private double getDistanceToPoint(Location loc) {
        return Math.pow(loc.x-uc.getLocation().x, 2) + Math.pow(loc.y-uc.getLocation().y, 2);
    }

    private int distanceToBase() {
        return (int) Math.pow(uc.getLocation().x - uc.getTeam().getInitialLocation().x, 2)
                + (int) Math.pow(uc.getLocation().y - uc.getTeam().getInitialLocation().y, 2);
    }

        // ATTACK/DEFENSE FUNCTIONS
    private void attackEnemy() {
        UnitInfo[] visibleEnemies = uc.senseUnits(uc.getTeam(), true);
        for (int i = 0; i < visibleEnemies.length; ++i) {
            if (uc.canAttack(visibleEnemies[i].getLocation())) uc.attack(visibleEnemies[i].getLocation());
        }
    }

    private void attackTown() {
        if (tactica.getType() != LOCATION_TYPE) {
            TownInfo[] town = uc.senseTowns();
            if (town.length > 0)
                for (TownInfo t : town) {
                    //if (t.getOwner() != uc.getTeam()) {
                    if (!t.getOwner().equals(uc.getTeam())) {
                        if (uc.canAttack(t.getLocation()))
                            uc.attack(t.getLocation());
                    }
                    else if (t.getLocation().x == tactica.getObjetivo().x && t.getLocation().y == tactica.getObjetivo().y)
                        tactica.setType(ACCOMPLISHED_TYPE);
                }
        }
    }

    private void wideAttack() {
        Location target = tactica.getObjetivo();

        UnitInfo[] aliados = uc.senseUnits(uc.getTeam(),false);

        if (aliados.length < 3 && distanceToBase() < 25)
            tactica.setMode(2);

        moveTo(target);

        UnitInfo[] visibleEnemies = uc.senseUnits(uc.getTeam(), true);
        for (int i = 0; i < visibleEnemies.length; ++i) {
            if (uc.canAttack(visibleEnemies[i].getLocation())) uc.attack(visibleEnemies[i].getLocation());
        }
    }

    private boolean isRearguard (Location enemy) {
        Direction dir = uc.getLocation().directionTo(enemy);
        UnitInfo[] friends = uc.senseUnits(uc.getTeam(), false);
        int backFriend = 0;

        for (UnitInfo friend : friends){
            Direction dirFriend = uc.getLocation().directionTo(friend.getLocation());
            double distanceFriend = getDistanceToPoint(friend.getLocation());
            if (dirFriend.opposite() == dir) {
                backFriend ++;
            }

            if (distanceFriend < getDistanceToPoint(enemy) && dir == dirFriend) {
                return true;
            }
        }

        if (backFriend == 0) {
            return true;
        }

        return false;
    }

    private boolean waiting () {
        int sum = round_beginning_waiting + TURNS_TO_WAIT;
        uc.println("Round beginning waiting: " + round_beginning_waiting + " , turn when i need continue: " + sum + " , round actual: " + uc.getRound());

        if (sum != uc.getRound()) {
            isWaiting = true;
            return true;
        }

        isWaiting = false;
        return false;
    }

        // MEMORY FUNCTIONS
    private void addResource(int type, Location loc) {
        int mem = rpointer;
        boolean exists = false;

        while (uc.read(mem) != 0) {
            if (uc.read(mem+1) == loc.x && uc.read(mem+2) == loc.y) {
                exists = true;
                break;
            }
            mem+=4;
            last_rpointer+=4;
        }

        if (!exists) {
            uc.write(mem, type);
            uc.write(mem+1, loc.x);
            uc.write(mem+2, loc.y);
            uc.write(mem+3, -1);
            uc.write(num_resources_pointer, uc.read(num_resources_pointer)+1);
        }
    }

    private void addEnemy(int id, Location loc, UnitType type) {
        int mem = epointer;
        boolean exists = false;

        while (uc.read(mem) != 0) {
            if (uc.read(mem) == id) {
                exists = true;
                break;
            }
            mem+=4;
            last_epointer+=4;
        }

        if (!exists) {
            uc.write(200001, uc.read(200001) + 1);
        }
        else {
            int t;

            if (type == UnitType.SOLDIER)
                t = 0;
            else if (type == UnitType.WORKER)
                t = 1;
            else if (type == UnitType.ARCHER)
                t = 2;
            else if (type == UnitType.EXPLORER)
                t = 3;
            else if (type == UnitType.CATAPULT)
                t = 4;
            else if (type == UnitType.KNIGHT)
                t = 5;
            else if (type == UnitType.BARRACKS)
                t = 6;
            else if (type == UnitType.TOWER)
                t = 7;
            else if (type == UnitType.MAGE)
                t = 8;
            else
                t = 9;

            uc.write(mem, id);
            uc.write(mem+3, t);
        }

        uc.write(mem+1, loc.x);
        uc.write(mem+2, loc.y);

    }

        // PATHFINDING FUNCTIONS
    final int INF = 1000000;

    boolean rotateRight = true; //if I should rotate right or left
    Location lastObstacleFound = null; //latest obstacle I've found in my way
    int minDistToEnemy = INF; //minimum distance I've been to the enemy while going around an obstacle
    Location prevTarget = null; //previous target

    //clear some of the previous data
    private void resetPathfinding() {
        lastObstacleFound = null;
        minDistToEnemy = INF;
    }
}
