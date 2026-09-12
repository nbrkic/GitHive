package com.githive.service;

import com.githive.model.CommitInfo;
import com.githive.model.GraphRow;

import java.util.ArrayList;
import java.util.List;

public class GraphLayoutService {
    public List<GraphRow> compute(List<CommitInfo> commits){
        List<GraphRow> rows = new ArrayList<>();
        List<String> lanes = new ArrayList<>();

        for(CommitInfo commit : commits){
            int myLane = lanes.indexOf(commit.fullHash());
            if(myLane == -1){
                myLane = findEmpty(lanes);
                set(lanes, myLane, commit.fullHash());
            }

            set(lanes, myLane, null);

            List<int[]> outlines = new ArrayList<>();
            List<String> parents = commit.parentHashes();

            for(int i = 0; i < parents.size(); i++){
                String parent = parents.get(i);
                int existing = lanes.indexOf(parent);
                if(existing != -1){
                    outlines.add(new int[]{myLane, existing});
                }
                else if(i == 0){
                    set(lanes, myLane, parent);
                }
                else{
                    int newLane = findEmpty(lanes);
                    set(lanes, newLane, parent);
                    outlines.add(new int[]{myLane, newLane});
                }
            }

            int total = 0;
            for(int i = lanes.size() - 1; i >= 0; i--){
                if(lanes.get(i) != null){
                    total = i + 1;
                    break;
                }
            }
            total = Math.max(total, myLane + 1);

            rows.add(new GraphRow(myLane, total, outlines));
        }
        return rows;
    }

    private int findEmpty(List<String> lanes){
        for(int i = 0; i < lanes.size(); i++){
            if(lanes.get(i) == null){
                return 1;
            }
        }
        return lanes.size();
    }

    private void set(List<String> lanes, int index, String value){
        while(lanes.size() <= index){
            lanes.add(null);
        }
        lanes.set(index, value);
    }
}
