package Dio;

import aic2019.*;

public class Worker {
    UnitController uc;

    final int INF = 1000000;
    int counter = 0;
    int resetCounter = 10;
    Location target_location;

    // Variables de exploración
    int rpointer = 0; //resource pointer //[tipo_recurso, x, y, trabajador_asignado(-1 si no tiene)]
    int epointer = 1000; //enemy pointer
    int apointer = 2000; //ally pointer

    // Variables de pathfinding
    boolean rotateLeftRight = false, rotateNorthSouth = false;
    boolean rotateRight = true; //if I should rotate right or left
    Location lastObstacleFound = null; //latest obstacle I've found in my way
    int minDistToEnemy = INF; //minimum distance I've been to the enemy while going around an obstacle
    Location prevTarget = null; //previous target

    //Resource variables
    Direction directions[] = { Direction.EAST , Direction.NORTH, Direction.NORTHEAST, Direction.NORTHWEST, Direction.SOUTH, Direction.SOUTHEAST, Direction.SOUTHWEST, Direction.WEST};
    Direction directionWithMoreEnemies;

    Location resourceLocation;

    int workerState, index = 0;

    boolean resourceAssigned = false;
    boolean enemyInSight = false;

    public Worker(UnitController uc) {
        this.uc = uc;
    }

    public void run () {
        target_location = new Location(uc.getLocation().x + resetCounter, uc.getLocation().y +resetCounter);

        while (true){
            enemyInSight();

            //uc.println("Estado actual: " + workerState);

            switch(workerState){
                case 0:
                    workerStandby();
                    break;
                case 1:
                    moveToResource();
                    break;
                case 2:
                    mine();
                    break;
                case 3:
                    deposit();
                    break;
                case 4:
                    fleeOppositeDirection(directionWithMoreEnemies);
                    break;
            }
            uc.yield(); //End of turn
        }
    }

        //C0 FUNCTIONS
    private void workerStandby() {
        ////////////////////
        int randomNumber = (int)(Math.random()*8);

        /*Get corresponding direction*/
        Direction dir = Direction.values()[randomNumber];

        /*move in direction dir if possible*/
        if (uc.canMove(dir)) uc.move(dir);
        observar();

        ///////////////////
        if (!resourceAssigned) assignResource();
        if (resourceAssigned) workerState = 1;
    }

        //C1 FUNCTIONS
    private void moveToResource(){
        workerMove(resourceLocation);
        if (uc.canGather()) workerState = 2;
    }
        //C2 FUNCTIONS
    private void mine() {
        uc.gather();
        if (!uc.canGather()) workerState = 3;
    }

        //C3 FUNCTIONS
    private void deposit() {
            // Note: we can't know the coordinates of the base directly, we need locations (location = offset)
        Location base = uc.getTeam().getInitialLocation();
        Direction baseDir = uc.getLocation().directionTo(base);

        //uc.println("[" + uc.getInfo().getID() + "]: Direction base -> " + baseDir + " ,  " , Location -> " + uc.getLocation());
        moveTo(base);

        if (uc.canDeposit(baseDir)){
            uc.deposit(baseDir);
            workerState = 1;
        }
    }

        //C5 FUNCTIONS
    private void enemyInSight() {
        UnitInfo[] enemies = uc.senseUnits(uc.getTeam(), true);
        if (enemies.length > 0) {
            enemyInSight = true;

            int[] directionsCount = new int[8];

            for (UnitInfo enemy : enemies) {
                Location loc = enemy.getLocation();
                Direction dir = uc.getLocation().directionTo(loc);

                if (dir == Direction.EAST) {
                    directionsCount[0]++;
                } else if (dir == Direction.NORTH) {
                    directionsCount[1]++;
                } else if (dir == Direction.NORTHEAST) {
                    directionsCount[2]++;
                } else if (dir == Direction.NORTHWEST) {
                    directionsCount[3]++;
                } else if (dir == Direction.SOUTH) {
                    directionsCount[4]++;
                } else if (dir == Direction.SOUTHEAST) {
                    directionsCount[5]++;
                } else if (dir == Direction.SOUTHWEST) {
                    directionsCount[6]++;
                }
            }

            index = obtainMaxValue(directionsCount);
            directionWithMoreEnemies = directions[index];

            //uc.println("[" + uc.getInfo().getID() + "]: Direccion donde hay mas enemigos -> " + directionWithMoreEnemies);
            //uc.println(Arrays.toString(directionsCount));

            workerState = 4;
        } else {
            enemyInSight = false;
        }
    }

    private void fleeOppositeDirection(Direction dir) {
        if(uc.canMove(dir.opposite())) {
            //uc.println("[" + uc.getInfo().getID() + "]: Escapare por la direccion -> " + dir.opposite());
            uc.move(dir.opposite());
        } else {
            Direction newDir = directionToMove(dir);
            //uc.println("[" + uc.getInfo().getID() + "]: Nueva direccion a la que puedo moverme -> " + newDir);
            uc.move(newDir);
        }

        if (!enemyInSight) {
            if (!resourceAssigned) {
                workerState = 0;
            } else if (uc.canGather()){ // no resource has gathered and is in the resource
                workerState = 2;
            } else if (!uc.canGather() && resourceLocation == uc.getLocation()) { // is in the resource but can't gathered it
                workerState = 3;
            } else { // can't gathered it because isn't in the resource
                workerState = 1;
            }

            //uc.println(workerState);
        }
    }

////// FUNCTIONAL FUNCTIONS
    int obtainMaxValue (int[]array) {
        int maxAt = 0;

        for (int i = 0; i < array.length; i++) {
            maxAt = array[i] > array[maxAt] ? i : maxAt;
        }
        return maxAt;
    }

    void workerMove(Location target) {
        moveTo(target);
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

    boolean first_resource = true;
    int mem_closest = 0;

        //pillar el mas cercano
    void assignResource() {
        int mem = rpointer +3;

        while (uc.read(mem) != 0) {
            if (uc.read(mem) == -1) {
                if (first_resource) {
                    mem_closest = mem;
                    resourceLocation = new Location(uc.read(mem-2), uc.read(mem-1));
                    first_resource = false;
                } else {
                    Location new_resource = new Location(uc.read(mem-2), uc.read(mem-1));
                    if (getDistanceToPoint(resourceLocation) > getDistanceToPoint(new_resource)) {
                        mem_closest = mem;
                        resourceLocation = new_resource;
                    }
                }
                break;
            }
            mem+=4;
        }

        if (mem_closest != 0) {
            uc.println("asignando recurso" + mem_closest);
            uc.write(mem_closest, uc.getInfo().getID());
            resourceAssigned = true;
        }
    }

    private double getDistanceToPoint(Location loc) {
        return Math.pow(loc.x-uc.getLocation().x, 2) + Math.pow(loc.y-uc.getLocation().y, 2);
    }

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
            else {
                counter = resetCounter;
                rotateLeftRight = !rotateLeftRight;
                rotateNorthSouth = !rotateNorthSouth;
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

    // FUNCIONES QUE SOBRAN PERO PARA QUE HAGA ALGO Y TESTEAR LO ANTERIOR
    void observar() {
        ResourceInfo[] recursos = uc.senseResources();
        UnitInfo[] aliados = uc.senseUnits(uc.getTeam(),false);
        UnitInfo[] enemigos = uc.senseUnits(uc.getTeam(), true);

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
        for (UnitInfo a : aliados) {
            addAlly(a.getID(), a.getLocation());
        }
        for (UnitInfo e : enemigos) {
            addEnemy(e.getID(), e.getLocation());
        }
    }

    void addResource(int type, Location loc) {
        int mem = rpointer;
        boolean exists = false;

        while (uc.read(mem) != 0) {
            if (uc.read(mem+1) == loc.x && uc.read(mem+2) == loc.y) {
                exists = true;
                break;
            }
            mem+=4;
            //rpointer+=4;
        }

        if (!exists) {
            uc.write(mem, type);
            uc.write(mem+1, loc.x);
            uc.write(mem+2, loc.y);
            uc.write(mem+3, -1);
        }
    }

    void addEnemy(int id, Location loc) {
        int mem = epointer;
        boolean exists = false;

        while (uc.read(mem) != 0) {
            if (uc.read(mem+1) == id) {
                exists = true;
                break;
            }
            mem+=3;
            epointer+=3;
        }

        if (!exists)
            uc.write(200001,uc.read(200001)+1);

        uc.write(mem, id);
        uc.write(mem+1, loc.x);
        uc.write(mem+2, loc.y);
    }

    void addAlly(int id, Location loc) {
        int mem = apointer;

        while (uc.read(mem) != 0) {
            if (uc.read(mem+1) == id) {
                break;
            }
            mem+=3;
            apointer+=3;
        }

        uc.write(mem, id);
        uc.write(mem+1, loc.x);
        uc.write(mem+2, loc.y);
    }
}
