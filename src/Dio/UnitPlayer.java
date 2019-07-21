package Dio;

import aic2019.*;

public class UnitPlayer {

    public void run(UnitController uc) {
        if (uc.getType() == UnitType.BASE) {
            MenteColmena mc = new MenteColmena(uc);
            mc.run();
        }
        else if (uc.getType() == UnitType.WORKER) {
            Worker wo = new Worker(uc);
            wo.run();
        }
        else if (uc.getType() == UnitType.EXPLORER) {
            Explorer ex = new Explorer(uc);
            ex.run();
        }
        else if (uc.getType() == UnitType.SOLDIER) {
            Soldier so = new Soldier(uc);
            so.run();
        }
        else if (uc.getType() == UnitType.CATAPULT) {
            Catapult ca = new Catapult(uc);
            ca.run();
        }
    }
}
