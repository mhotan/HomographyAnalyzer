package edu.uw.homographyanalyzer.tools;

/**
 * 
 * @author mhotan
 */
public class RepresentationException extends RuntimeException {

	public RepresentationException(){
		super("Representation Exception");
	}
	
	public RepresentationException(String message){
		super("Representation Exception " + message);
	}
}
