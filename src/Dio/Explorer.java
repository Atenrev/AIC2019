package Dio;

import aic2019.*;

public class Explorer {
    UnitController uc;
    private int estado = 0;

    final int INF = 1000000;
    int counter = 0;
    int resetCounter = 10;
    boolean rotateLeftRight = false, rotateNorthSouth = false;
    boolean rotateRight = true; //if I should rotate right or left
    Location lastObstacleFound = null; //latest obstacle I've found in my way
    int minDistToEnemy = INF; //minimum distance I've been to the enemy while going around an obstacle
    Location prevTarget = null; //previous target

    // PUNTEROS
    final int base_location_x_pointer = 100000;
    final int base_location_y_pointer = 100001;
    final int enemy_base_location_x_pointer = 100002;
    final int enemy_base_location_y_pointer = 100003;
    final int rpointer = 0;
    int last_rpointer = 0;
    final int apointer = 3000;
    int last_apointer = 0;
    final int epointer = 6000;
    int last_epointer = 0;
    final int num_resources_pointer = 199999;

    Direction exploring_dir;
    Location target_location;

    // en función de dónde aparezca, explorar una dirección (según esquina) evitando enemigos
    public Explorer(UnitController uc) {
        this.uc = uc;
    }

    public void run () {
        if (uc.read(base_location_x_pointer) > uc.read(enemy_base_location_x_pointer)) {
            // NORTHEAST
            if (uc.read(base_location_y_pointer) > uc.read(enemy_base_location_y_pointer)) {
                exploring_dir = Direction.SOUTH;
            }
            // SOUTHEAST
            else {
                exploring_dir = Direction.NORTH;
            }
        }
        else {
            // NORTHWEAST
            if (uc.read(base_location_y_pointer) > uc.read(enemy_base_location_y_pointer)) {
                exploring_dir = Direction.SOUTH;
            }
            // SOUTHWEAST
            else {
                exploring_dir = Direction.NORTH;
            }
        }

        while (true){
            switch(estado) {
                case 0:
                    explore();
                    break;
            }
            uc.yield(); //End of turn
        }
    }

    private void explore() {

        moveTo(target_location);
        observar();
    }

    private void observar() {
        ResourceInfo[] recursos = uc.senseResources();
//        UnitInfo[] aliados = uc.senseUnits(uc.getTeam(),false);
        UnitInfo[] enemigos = uc.senseUnits(uc.getOpponent(), false);

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
//        for (UnitInfo a : aliados) {
//            addAlly(a.getID(), a.getLocation());
//        }
        for (UnitInfo e : enemigos) {
            addEnemy(e.getID(), e.getLocation(), e.getType());
        }
    }

    private void generarQuadrants() {

    }

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

    private void addAlly(int id, Location loc) {
        int mem = apointer;

        while (uc.read(mem) != 0) {
            if (uc.read(mem+1) == id) {
                break;
            }
            mem+=3;
            last_apointer+=3;
        }

        uc.write(mem, id);
        uc.write(mem+1, loc.x);
        uc.write(mem+2, loc.y);
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
}
