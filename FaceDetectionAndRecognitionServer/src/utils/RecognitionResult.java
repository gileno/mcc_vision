package utils;

public class RecognitionResult {

	// RETURN A MAP WITH:
	// faceIndex (THE INDEX INT OF THE FACE IN THE DATABASE),
	// lsd (LEAST SQUARED DISTANCE)
	// conf (CONFIDENCE)

	private int faceIndex;
	private double least_squared_distance;
	private float confidence;

	public int getFaceIndex() {
		return faceIndex;
	}

	public void setFaceIndex(int faceIndex) {
		this.faceIndex = faceIndex;
	}

	public double getLeast_squared_distance() {
		return least_squared_distance;
	}

	public void setLeast_squared_distance(double least_squared_distance) {
		this.least_squared_distance = least_squared_distance;
	}

	public float getConfidence() {
		return confidence;
	}

	public void setConfidence(float confidence) {
		this.confidence = confidence;
	}

}
