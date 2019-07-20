package Dio;

import aic2019.UnitController;

// DEPRECATED: NO SE USA
public class Group {
    int ref;

    public Group(UnitController uc, int ref) {
        this.ref = ref;
    }

    public int getTactic() {
        return ref+1;
    }

    public int getType() {
        return ref;
    }

    public int getUnitsCount() {
        return ref+2;
    }

    public int getUnit(int i) {
        return ref+3+i;
    }
}
