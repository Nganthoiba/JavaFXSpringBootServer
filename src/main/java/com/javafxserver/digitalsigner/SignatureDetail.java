package com.javafxserver.digitalsigner;

public class SignatureDetail {
	//The (x,y) co ordinate on the page where the rectangle will be drawn
	public Coordinate coordinate = new Coordinate(340.0f, 750.0f);
	
	//The area on the pdf page where signature(timestamp) will appear
	public Rectangle rectangle = new Rectangle(210, 60);
	
	public String location; //It can be the Office Name or City
	
	public String reason; //It can be the reason for signing the document
	
	//The page number at which the signature will appear
	public int pageNumber = 1; //by default
}
