package com.githive.service;

import com.githive.model.CommitInfo;
import com.githive.model.GraphRow;

import java.util.ArrayList;
import java.util.List;

public class GraphLayoutService {

    public List<GraphRow> compute(List<CommitInfo> commits) {
        List<GraphRow> rows = new ArrayList<>();
        List<String> lanes = new ArrayList<>();

        for (CommitInfo commit : commits) {
            // Find or assign lane for this commit
            int myLane = lanes.indexOf(commit.fullHash());
            boolean myLaneWasTracked = myLane != -1;
            if (myLane == -1) {
                myLane = findFirstEmpty(lanes);
                set(lanes, myLane, commit.fullHash());
            }

            // Snapshot pass-through lanes: active before this commit, excluding myLane
            List<Integer> passThroughLanes = new ArrayList<>();
            for (int i = 0; i < lanes.size(); i++) {
                if (i != myLane && lanes.get(i) != null) {
                    passThroughLanes.add(i);
                }
            }

            // Clear myLane
            set(lanes, myLane, null);

            // Process parents
            List<int[]> outLines = new ArrayList<>();
            List<String> parents = commit.parentHashes();
            for (int i = 0; i < parents.size(); i++) {
                String parent = parents.get(i);
                int existing = lanes.indexOf(parent);
                if (existing != -1) {
                    outLines.add(new int[]{myLane, existing});
                } else if (i == 0) {
                    set(lanes, myLane, parent);
                } else {
                    int newLane = findFirstEmpty(lanes);
                    set(lanes, newLane, parent);
                    outLines.add(new int[]{myLane, newLane});
                }
            }

            boolean myLaneContinues = myLane < lanes.size() && lanes.get(myLane) != null;

            // Compute canvas width
            int total = myLane + 1;
            for (int i = 0; i < lanes.size(); i++) {
                if (lanes.get(i) != null) total = Math.max(total, i + 1);
            }
            for (int l : passThroughLanes) total = Math.max(total, l + 1);

            rows.add(new GraphRow(myLane, total, outLines, passThroughLanes, myLaneContinues, myLaneWasTracked));
        }
        return rows;
    }

    private int findFirstEmpty(List<String> lanes) {
        for (int i = 0; i < lanes.size(); i++) {
            if (lanes.get(i) == null) return i;
        }
        return lanes.size();
    }

    private void set(List<String> lanes, int index, String value) {
        while (lanes.size() <= index) lanes.add(null);
        lanes.set(index, value);
    }
}
