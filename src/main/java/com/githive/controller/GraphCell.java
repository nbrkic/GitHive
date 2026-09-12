package com.githive.controller;

import com.githive.model.GraphRow;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TableCell;
import javafx.scene.paint.Color;

public class GraphCell extends TableCell<com.githive.model.CommitInfo, GraphRow> {

    private static final int LANE_W = 16;
    private static final int H = 24;
    private static final Color[] COLORS = {
            Color.web("#58a6ff"), Color.web("#3fb950"), Color.web("#f78166"),
            Color.web("#d2a8ff"), Color.web("#ffa657"), Color.web("#79c0ff")
    };

    private final Canvas canvas = new Canvas();

    public GraphCell() {
        setGraphic(canvas);
        canvas.setHeight(H);
    }

    @Override
    protected void updateItem(GraphRow row, boolean empty) {
        super.updateItem(row, empty);
        if (empty || row == null) {
            canvas.setWidth(0);
            return;
        }

        int width = (row.totalLanes() + 1) * LANE_W;
        canvas.setWidth(width);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, H);

        for (int[] line : row.outLines()) {
            int from = line[0];
            int to = line[1];
            gc.setStroke(COLORS[from % COLORS.length]);
            gc.setLineWidth(2);
            double x1 = from * LANE_W + LANE_W / 2.0;
            double x2 = to * LANE_W + LANE_W / 2.0;
            gc.strokeLine(x1, H / 2.0, x2, H);
        }

        for (int i = 0; i < row.totalLanes(); i++) {
            if (i == row.myLane()) continue;
            gc.setStroke(COLORS[i % COLORS.length]);
            gc.setLineWidth(2);
            double x = i * LANE_W + LANE_W / 2.0;
            gc.strokeLine(x, 0, x, H);
        }

        int ml = row.myLane();
        double cx = ml * LANE_W + LANE_W / 2.0;
        gc.setStroke(COLORS[ml % COLORS.length]);
        gc.setLineWidth(2);
        gc.strokeLine(cx, 0, cx, H / 2.0);
        gc.setFill(COLORS[ml % COLORS.length]);
        gc.fillOval(cx - 5, H / 2.0 - 5, 10, 10);
    }
}