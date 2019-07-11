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

    public void setTopPriority(int r) {
        int unitCount = 0, top = r;

        do {
            unitCount = uc.read(r+3);
            if (uc.read(r+2) > uc.read(top+2)) {
                top = r;
            }
            r += r+unitCount+4;
        } while(unitCount != 0);

        ref = top;
    }

    public void setType(int t) {
        uc.write(ref, t);
    }

    public void setObjective(int x, int y) {
        uc.write(ref+1, x);
        uc.write(ref+2, y);
    }

    public void setPriority(int p) {
        uc.write(ref+3, p);
    }

    public void setUnitsCount(int c) {
        uc.write(ref+4, c);
    }

    public int getType() {
        return uc.read(ref);
    }

    public int getUnitsCount() {
        return uc.read(+4);
    }

    public int getUnit(int i) {
        return uc.read(ref+5+i);
    }

    public Location getCuadranteObjetivo() {
        return new Location(uc.read(ref+1), uc.read(ref+2));
    }

    public int getPriority() {
        return uc.read(ref+2);
    }


}
