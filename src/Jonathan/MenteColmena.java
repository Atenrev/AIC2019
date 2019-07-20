package Jonathan;

import aic2019.*;

public class MenteColmena {
    UnitController uc;

    // CONSTANTS
    final int LOCATION_TYPE = 1;
    final int NEUTRAL_TOWN_TYPE = 2;
    final int ENEMY_TYPE = 3;

    // POINTERS
    final int tactics_pointer = 100155;
    int tactics_cursor = 0;
    int new_tactics_pointer = 0;
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

    int spawn_dir;

    private Tactica[] tacticas;
    private TownInfo[] towns;

    public MenteColmena(UnitController uc) {
        this.uc = uc;
        tacticas = new Tactica[50];
    }

    public void run () {
        uc.write(100000, uc.getLocation().x);
        uc.write(100001, uc.getLocation().y);
        uc.write(100002, uc.getOpponent().getInitialLocation().x);
        uc.write(100003, uc.getOpponent().getInitialLocation().y);

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

        towns = uc.getTowns();
        Location tl;
        for (int i = 0; i < towns.length; i++) {
            if (towns[i].getOwner() != uc.getOpponent()) {
                tl = towns[i].getLocation();
                uc.write(neutral_towns_pointer+i*2, tl.x);
                uc.write(neutral_towns_pointer+i*2, tl.y);
                createTacticGroup(tl.x, tl.y, NEUTRAL_TOWN_TYPE);
            }
        }

        createTacticGroup(uc.getOpponent().getInitialLocation().x, uc.getOpponent().getInitialLocation().y, ENEMY_TYPE);

        while (true){
            economia();
            combate();
            creacion();

            uc.yield(); //End of turn
        }
    }

    private void economia() {
        if (uc.canTrade()) {
            if (uc.getCrystal() > 0) {
                if (uc.getWood() * 0.43 < uc.getIron()) {
                    uc.trade(Resource.CRYSTAL, Resource.WOOD, uc.getCrystal());
                } else {
                    uc.trade(Resource.CRYSTAL, Resource.IRON, uc.getCrystal());
                }
            }
            if (uc.getWood() * 0.43 > uc.getIron()) {
                uc.trade(Resource.WOOD, Resource.IRON, uc.getWood()*0.43f);
            }

            if (uc.getWood() * 0.43 < uc.getIron()) {
                uc.trade(Resource.IRON, Resource.WOOD, uc.getIron()*1.43f);
            }
        }

        economyPriority = (int)
                ((float)((24/((uc.read(num_ally_workers)+1))) * 1000));
    }

    private void combate() {
        combatPriority = (int) (
                (float)( (3*(uc.read(num_enemy_soldiers)+1)/(uc.read(num_ally_soldiers)+1)) * 1000 )
        );

        // itera las tácticas en busca de las cumplidas y les asigna una nueva misión
        int c = 0;
        while (tacticas[c] != null) {
            if (tacticas[c].getType() == -1) {
                tacticas[c].setMode(1);
                tacticas[c].setType(3);
                tacticas[c].setObjective(uc.read(100002), uc.read(100003));
            }
            c++;
        }
    }

    private void creacion() {
        int workers = uc.read(num_ally_workers);
        int soldiers = uc.read(num_ally_soldiers);
        int explorers = uc.read(num_explorers_pointer);

        if (explorers < 2) {
            if (spawnUnit(UnitType.EXPLORER))
                uc.write(num_explorers_pointer, explorers+1);
        }

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
            uc.println("spawning");
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
}
