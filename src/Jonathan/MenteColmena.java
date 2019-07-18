package Jonathan;

import aic2019.*;

public class MenteColmena {
    UnitController uc;

    // POINTERS
    final int tactics_pointer = 100155;
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
                createTacticGroup(tl.x, tl.y);
            }
        }

        createTacticGroup(uc.getOpponent().getInitialLocation().x, uc.getOpponent().getInitialLocation().y);

        while (true){
            economia();
            combate();
            creacion();

            uc.yield(); //End of turn
        }
    }

    private void economia() {
        if (uc.getCrystal() > 0) {
            if (uc.getWood()*0.43 < uc.getIron()) {
                uc.trade(Resource.CRYSTAL, Resource.WOOD, uc.getCrystal());
            }
            else {
                uc.trade(Resource.CRYSTAL, Resource.IRON, uc.getCrystal());
            }
        }

        economyPriority = (int)
                ((float)((3/(2*(uc.read(num_ally_workers)+1))) * 1000));
    }

    private void combate() {
        combatPriority = (int) (
                (float)( (3*((uc.read(num_enemy_soldiers)+1))/(uc.read(num_ally_soldiers)+1)) * 1000 )
        );


    }

    private void creacion() {
        int workers = uc.read(num_ally_workers);
        Direction dir = Direction.values()[(int)(Math.random()*8)];
        int explorers = uc.read(num_explorers_pointer);

        if (explorers < 2) {
            if (uc.canSpawn(dir, UnitType.EXPLORER)) {
                uc.spawn(dir, UnitType.EXPLORER);
                uc.write(num_explorers_pointer, explorers+1);
            }
        }

        if (economyPriority > combatPriority || uc.read(num_resources_pointer) > workers) {
            if (uc.canSpawn(dir, UnitType.WORKER)) {
                uc.spawn(dir, UnitType.WORKER);
                uc.write(num_ally_workers, workers + 1);;
            }
        }
        else {
            if (uc.canSpawn(dir, UnitType.SOLDIER)) {
                uc.spawn(dir, UnitType.SOLDIER);
                uc.write(num_ally_soldiers, workers + 1);;
            }
        }
    }

    private void createTacticGroup(int x, int y) {
        Tactica t = new Tactica(uc, tactics_pointer+new_tactics_pointer);
        tacticas[0] = t;
        t.setType(1);
        t.updatePriority();
        t.setObjective(x, y);
        new_tactics_pointer += 100;
    }
}
