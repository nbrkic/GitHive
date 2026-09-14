package com.githive.model;

import java.util.List;

public record GraphRow(
        int myLane,
        int totalLanes,
        List<int[]> outLines,
        List<Integer> passThroughLanes,
        boolean myLaneContinues,
        boolean myLaneWasTracked
) {}
