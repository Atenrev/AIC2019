package Dio;

import aic2019.Location;
import aic2019.UnitController;

public class Tactica {
    UnitController uc;
    int ref;

    public Tactica(UnitController uc, int ref) {
        this.ref = ref;
        this.uc = uc;
    }

    public Tactica(UnitController uc) {
        this.uc = uc;
    }

    public void updatePriority() {
        int units = getUnitsCount();
        // -((x-15)^2)/80+3
        float p;
        if (units < 7)
            p = ((float) -Math.pow(units-15, 2)/80 + 3) * 1000;
        else
            p = (15/units) * 1000;
        setPriority((int) p);
    }

    public void setTopPriority(int r) {
        int unitCount = 0, top = r;

        do {
            if (uc.read(r+3) > uc.read(top+3)) {
                top = r;
            }
            uc.println(uc.read(r+3));
            // r += r+unitCount+5;
            r += 100;
        } while(uc.read(r) != 0);

        ref = top;
    }

    public void setMode(int t) {
        uc.write(ref, t);
    }

    public int getMode() {
        return uc.read(ref);
    }

    public Location getObjetivo() {
        return new Location(uc.read(ref+1), uc.read(ref+2));
    }

    public void setObjective(int x, int y) {
        uc.write(ref+1, x);
        uc.write(ref+2, y);
    }

    public void setPriority(int p) {
        uc.write(ref+3, p);
    }

    public int getPriority() {
        return uc.read(ref+3);
    }

    public void setUnitsCount(int c) {
        uc.write(ref+4, c);
    }

    public int getUnitsCount() {
        return uc.read(ref+4);
    }

    public void setUnit() {
        // uc.write(ref+5+getUnitsCount(), i);
        setUnitsCount(getUnitsCount()+1);
    }

    public void setLastEnemyCount(int i) {
        uc.write(ref+5, i);
    }

    public int getLastEnemyCount() {
        return uc.read(ref+5);
    }

    public void setType(int i) {
        uc.write(ref+6, i);
    }

    public int getType() {
        // uc.write(ref+5+getUnitsCount(), i);
        return uc.read(ref+6);
    }

    public void deleteUnit() {
        setUnitsCount(getUnitsCount()-1);
    }

    /*public int getUnit(int i) {
        return uc.read(ref+5+i);
    }*/


}
