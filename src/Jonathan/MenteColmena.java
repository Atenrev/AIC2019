package Jonathan;

import aic2019.Direction;
import aic2019.UnitController;
import aic2019.UnitInfo;
import aic2019.UnitType;

public class MenteColmena {
    UnitController uc;

    private int combatPriority;
    private int economyPriority;

    private Tactica[] tacticas;

    public MenteColmena(UnitController uc) {
        this.uc = uc;
        tacticas = new Tactica[50];
    }

    public void run () {
        uc.write(100000,uc.getLocation().x);
        uc.write(100001,uc.getLocation().y);
        uc.write(100002,uc.getOpponent().getInitialLocation().x);
        uc.write(100003,uc.getOpponent().getInitialLocation().y);

        while (true){
            economia();
            combate();
            creacion();

            uc.yield(); //End of turn
        }
    }

    private void economia() {
        economyPriority = 0;
    }

    private void combate() {
        combatPriority = (int) ((float)(((uc.read(200001)+1)/(uc.read(200000)+1)) * 1000));
        Tactica t = new Tactica(uc, 100155);
        if (t.getUnitsCount() == 0) {
            tacticas[0] = t;
            t.setType(0);
            t.setPriority(combatPriority);
            t.setObjective(uc.read(100002), uc.read(100003));
        }
    }

    private void creacion() {
        Direction dir = Direction.values()[(int)(Math.random()*8)];

        if (uc.read(100004) < 1) {
            if (uc.canSpawn(dir, UnitType.EXPLORER)) {
                uc.spawn(dir, UnitType.EXPLORER);
                uc.write(100004, uc.read(100004)+1);
            }
        }

        if (economyPriority < combatPriority) {
            if (uc.canSpawn(dir, UnitType.SOLDIER))
                uc.spawn(dir, UnitType.SOLDIER);
        }
    }
}
