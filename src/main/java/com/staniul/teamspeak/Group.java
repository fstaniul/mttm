package com.staniul.teamspeak;

public class Group implements Comparable<Group> {
    private int id;
    private int rank;

    public Group() {
    }

    public Group(int id, int rank) {
        this.id = id;
        this.rank = rank;
    }

    public int getId() {
        return id;
    }

    public int getRank() {
        return rank;
    }

    @Override
    public int compareTo(Group o) {
        return Integer.compare(id, o.id);
    }

    @Override
    public String toString() {
        return String.format("Group: %d (%d)", id, rank);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Group &&
                ((Group) obj).id == id;
    }
}
