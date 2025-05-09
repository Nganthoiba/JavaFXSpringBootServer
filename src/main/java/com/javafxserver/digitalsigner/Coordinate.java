package com.javafxserver.digitalsigner;

public class Coordinate {
	public Float X = 340.0f;
	public Float Y = 750.0f;
	public Coordinate(Float x, Float y) {
		this.X = (x == null)? 340 : x;
		this.Y = (y == null)? 750 : y;
	}
}
