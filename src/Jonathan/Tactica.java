package Jonathan;

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
        // -((x-7)^2)/80+1
        float p;
        if (units < 7)
            p = -((float) Math.pow(units-7, 2)/80 + 1) * 1000;
        else
            p = (7/units) * 1000;
        uc.println(p);
        if (units < 8)
            p += 1000;
        setPriority((int) p);
    }

    public void setTopPriority(int r) {
        int unitCount = 0, top = r;

        do {
            unitCount = uc.read(r+4);
            if (uc.read(r+3) > uc.read(top+3)) {
                top = r;
            }
            // r += r+unitCount+5;
            r += 100;
        } while(uc.read(r) != 0);

        ref = top;
    }

    public void setType(int t) {
        uc.write(ref, t);
    }

    public int getType() {
        return uc.read(ref);
    }

    public Location getCuadranteObjetivo() {
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

    public void setUnit(int i) {
        uc.write(ref+5+getUnitsCount(), i);
        setUnitsCount(getUnitsCount()+1);
    }

    public int getUnit(int i) {
        return uc.read(ref+5+i);
    }


}
