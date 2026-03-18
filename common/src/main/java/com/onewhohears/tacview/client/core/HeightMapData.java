package com.onewhohears.tacview.client.core;

public record HeightMapData(int[][] heights, int[][] colors) {
    public HeightMapData(int xLength, int zLength) {
        this(new int[xLength][zLength], new int[xLength][zLength]);
    }
}
