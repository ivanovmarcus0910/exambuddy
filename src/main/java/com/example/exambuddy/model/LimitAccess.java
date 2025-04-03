package com.example.exambuddy.model;

public class LimitAccess {
    int limit;
    long timeDo;

    public LimitAccess() {
    }

    public LimitAccess(int limit, long timeDo) {
        this.limit = limit;
        this.timeDo = timeDo;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public long getTimeDo() {
        return timeDo;
    }

    public void setTimeDo(long timeDo) {
        this.timeDo = timeDo;
    }
}
