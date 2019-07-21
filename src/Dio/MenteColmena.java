package Dio;

import aic2019.*;

public class MenteColmena {
    UnitController uc;

    // CONSTANTS
    final int ACCOMPLISHED_TYPE = -1;
    final int LOCATION_TYPE = 1;
    final int NEUTRAL_TOWN_TYPE = 2;
    final int ENEMY_TYPE = 3;
    final int DEFFENSE_TYPE = 3;

    // POINTERS
    final int tactics_pointer = 100155;
    int tactics_cursor = 0;
    int new_tactics_pointer = 0;
    final int BASE_X = 100000;
    final int BASE_Y = 100001;
    final int ENEMY_BASE_X = 100002;
    final int ENEMY_BASE_Y = 100003;
    final int num_ally_soldiers = 200000;
    final int num_enemy_soldiers = 200001;
    final int num_ally_workers = 200002;
    final int num_explorers_pointer = 200003;
    final int num_resources_pointer = 199999;
    final int rpointer = 0;
    int last_rpointer = 0;
    final int neutral_towns_pointer = 100004;

    private int combatPriority;
    private int economyPriority;
    private int distance_to_enemy_base;
    private int distance_to_enemy_base_priority;

    int spawn_dir;
    boolean enemyBaseNear, neutralTownsFar;

    private Tactica[] tacticas;
    private TownInfo[] towns;

    public MenteColmena(UnitController uc) {
        this.uc = uc;
        tacticas = new Tactica[50];
    }

    public void run () {
        uc.write(BASE_X, uc.getLocation().x);
        uc.write(BASE_Y, uc.getLocation().y);
        uc.write(ENEMY_BASE_X, uc.getOpponent().getInitialLocation().x);
        uc.write(ENEMY_BASE_Y, uc.getOpponent().getInitialLocation().y);
        distance_to_enemy_base = distanceToPoint(uc.read(BASE_X), uc.read(BASE_Y),
                uc.read(ENEMY_BASE_X), uc.read(ENEMY_BASE_Y));
        distance_to_enemy_base_priority = 1 / distance_to_enemy_base;

        if (distance_to_enemy_base < 225 && !uc.isObstructed(uc.getTeam().getInitialLocation(), uc.getOpponent().getInitialLocation()))
            enemyBaseNear = true;
        else
            enemyBaseNear = false;

        /*int best_distance = 999999;
        int[] loc = new int[] {0, 0};
        Location[] visible_locations = uc.getVisibleLocations();
        int dist;
        for (int i = 0; i < visible_locations.length; i++) {
            if (uc.isOutOfMap(visible_locations[i])) {
                dist = (int) Math.pow(
                        visible_locations[i].x - uc.getLocation().x, 2)
                        + (int) Math.pow(visible_locations[i].y - uc.getLocation().y, 2
                );
                if (dist < best_distance) {
                    best_distance = dist;
                    loc[0] = visible_locations[i].x;
                    loc[1] = visible_locations[i].y;
                }
            }
        }

        if (!uc.isOutOfMap(new Location(loc[0]+1, loc[1]))) {
            uc.write(100004, loc[0]+1);
            uc.write(100005, loc[1]);
        }
        else if (!uc.isOutOfMap(new Location(loc[0]-1, loc[1]))){
            uc.write(100004, loc[0]-1);
            uc.write(100005, loc[1]);
        }
        else if (!uc.isOutOfMap(new Location(loc[0], loc[1]+1))) {
            uc.write(100004, loc[0]);
            uc.write(100005, loc[1]+1);
        }
        else {
            uc.write(100004, loc[0]);
            uc.write(100005, loc[1]-1);
        }*/

        int distance;
        int min_distance = 99999999;
        towns = uc.getTowns();
        Location tl;
        for (int i = 0; i < towns.length; i++) {
            tl = towns[i].getLocation();
            distance = distanceToPoint(tl.x, tl.y, uc.read(BASE_X), uc.read(BASE_Y));
            min_distance = distance < min_distance ? distance : min_distance;

            if (distance < distance_to_enemy_base && !enemyBaseNear) {
                uc.write(neutral_towns_pointer + i * 3, tl.x);
                uc.write(neutral_towns_pointer + i * 3 + 1, tl.y);
                uc.write(neutral_towns_pointer + i * 3 + 2, 0);
                createTacticGroup(tl.x, tl.y, NEUTRAL_TOWN_TYPE);
            }
        }

        if (min_distance > distance_to_enemy_base)
            neutralTownsFar = true;
        else
            neutralTownsFar = false;

        createTacticGroup(uc.getOpponent().getInitialLocation().x, uc.getOpponent().getInitialLocation().y, ENEMY_TYPE);

        while (true){
            towns = uc.getTowns();
            economia();
            combate();
            creacion();

            uc.yield(); //End of turn
        }
    }

    private void economia() {
        if (uc.canTrade()) {
            /*if (uc.getCrystal() > 0) {
                if (uc.getWood() * 0.43 < uc.getIron()) {
                    uc.trade(Resource.CRYSTAL, Resource.WOOD, uc.getCrystal());
                } else {
                    uc.trade(Resource.CRYSTAL, Resource.IRON, uc.getCrystal());
                }
            }*/
            if (uc.getWood() * 0.43 > uc.getIron()) {
                uc.trade(Resource.WOOD, Resource.IRON, (uc.getWood()*7) / 3);
            }
            if (uc.getWood() * 0.43 < uc.getIron()) {
                uc.trade(Resource.IRON, Resource.WOOD, (uc.getIron()*3) / 7);
            }
        }

        economyPriority = (int)
                ((float)((8/((uc.read(num_ally_workers)+1))) * 1000));
    }

    private void combate() {
        UnitInfo[] enemies_inbound = uc.senseUnits(uc.getTeam(), true);
        combatPriority = (int) (
                (float)( ((uc.read(num_enemy_soldiers)+1)/(uc.read(num_ally_soldiers)+1)+distance_to_enemy_base_priority) * 1000 )
        );

        for (int i = 0; i < enemies_inbound.length; ++i) {
            if (uc.canAttack(enemies_inbound[i].getLocation())) {
                uc.attack(enemies_inbound[i].getLocation());
                checkEnemyExistence(enemies_inbound[i]);
            }
        }

        // itera las tácticas en busca de las cumplidas y les asigna una nueva misión
        int min_distance = 99999999;
        int c = 0;
        int tc = 0;
        boolean targetAssigned;
        if (enemies_inbound.length < 2) {
            if (!enemyBaseNear) {
                while (tacticas[c] != null) {
                    int type = tacticas[c].getType();
                    if (type == ACCOMPLISHED_TYPE
                            || (type == DEFFENSE_TYPE)) {
                        tacticas[c].setMode(1);
                        targetAssigned = false;
                        tc = 0;
                        for (int i = 0; i < towns.length; i++) {
                            int tower = 0;
                            int ox = towns[i].getLocation().x;
                            int oy = towns[i].getLocation().y;
                            int mem = neutral_towns_pointer;
                            while (uc.read(mem) != 0) {
                                if (uc.read(mem) == ox && uc.read(mem + 1) == oy) {
                                    tower = uc.read(mem+2);
                                    break;
                                }
                                mem += 3;
                            }

                            uc.println(towns[i].getLocation().toString() + " " + towns[i].getOwner().toString());
                            if (!towns[i].getOwner().equals(uc.getTeam()) && tower != -1) {
                                uc.println("Reasignando");
                                tacticas[c].setObjective(towns[i].getLocation().x, towns[i].getLocation().y);
                                tacticas[c].setType(NEUTRAL_TOWN_TYPE);
                                targetAssigned = true;
                                break;
                            }
                            tc++;
                        }
                        if (!targetAssigned || uc.read(num_enemy_soldiers) * 1.25f < uc.read(num_ally_soldiers)) {
                            tacticas[c].setObjective(uc.read(ENEMY_BASE_X), uc.read(ENEMY_BASE_Y));
                            tacticas[c].setType(ENEMY_TYPE);
                        }
                    }
                    c++;
                }
            }
        }
        else {
            while (tacticas[c] != null) {
                uc.println("Base en peligro");
                tacticas[c].setMode(2);
                tacticas[c].setObjective(uc.read(BASE_X), uc.read(BASE_Y));
                tacticas[c].setType(DEFFENSE_TYPE);
                c++;
            }
        }
    }

    private void creacion() {
        int workers = uc.read(num_ally_workers);
        int soldiers = uc.read(num_ally_soldiers);
        // int explorers = uc.read(num_explorers_pointer);

       /* if (explorers < 2) {
            if (spawnUnit(UnitType.EXPLORER))
                uc.write(num_explorers_pointer, explorers+1);
        }
*/
        if (economyPriority > combatPriority && uc.read(num_resources_pointer) > workers) {
            while (spawnUnit(UnitType.WORKER))
                uc.write(num_ally_workers, workers + 1);
        }
        else {
            while (spawnUnit(UnitType.SOLDIER))
                uc.write(num_ally_soldiers, soldiers + 1);
        }
    }

    private boolean spawnUnit(UnitType ut) {
        if (uc.canSpawn(Direction.EAST, ut)) {
            uc.spawn(Direction.EAST, ut);
            return true;
        }
        else if (uc.canSpawn(Direction.WEST, ut)) {
            uc.spawn(Direction.WEST, ut);
            return true;
        }
        else if (uc.canSpawn(Direction.NORTH, ut)) {
            uc.spawn(Direction.NORTH, ut);
            return true;
        }
        else if (uc.canSpawn(Direction.SOUTH, ut)) {
            uc.spawn(Direction.SOUTH, ut);
            return true;
        }
        else if (uc.canSpawn(Direction.SOUTHEAST, ut)) {
            uc.spawn(Direction.SOUTHEAST, ut);
            return true;
        }
        else if (uc.canSpawn(Direction.SOUTHWEST, ut)) {
            uc.spawn(Direction.SOUTHWEST, ut);
            return true;
        }
        else if (uc.canSpawn(Direction.NORTHEAST, ut)) {
            uc.spawn(Direction.NORTHEAST, ut);
            return true;
        }
        else if (uc.canSpawn(Direction.NORTHWEST, ut)) {
            uc.spawn(Direction.NORTHWEST, ut);
            return true;
        }
        return false;
    }

    private void createTacticGroup(int x, int y, int type) {
        Tactica t = new Tactica(uc, tactics_pointer+new_tactics_pointer);
        tacticas[tactics_cursor] = t;
        tactics_cursor++;
        t.setType(type);
        t.setMode(1);
        t.updatePriority();
        t.setObjective(x, y);
        new_tactics_pointer += 100;
    }

    private int distanceToPoint(int x1, int y1, int x2, int y2) {
        return (int) Math.pow(x2 - x1, 2)
                + (int) Math.pow(y2 - y1, 2);
    }

    private void checkEnemyExistence (UnitInfo enemy) {
        if (checkEnemyDied(enemy.getLocation()) == null && enemy.getTeam().equals(uc.getOpponent())) {
            uc.write(num_enemy_soldiers, uc.read(num_enemy_soldiers) - 1);
        }
    }

    private UnitInfo checkEnemyDied(Location enemy_location) {
        return uc.senseUnit(enemy_location);
    }
}
