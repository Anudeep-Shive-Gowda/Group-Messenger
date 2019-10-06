package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.Comparator;

public class PropoasalOrder implements Comparator<String> {
    public int compare(String s1, String s2) {
        String[] s1P = s1.split("-");
        String[] s2P = s2.split("-");
        if (Double.valueOf(s2P[4]) < Double.valueOf(s1P[4]))
            return 1;
        else if (Double.valueOf(s2P[4]) > Double.valueOf(s1P[4]))
            return -1;
        return 0;
    }
}