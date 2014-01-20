package recognition;

import java.util.Observable;
import java.util.Observer;

public class RecognitionObserver implements Observer {

	private String result = "";
	private int totalThreads = 0;
	private int totalReceived = 0;
	
	public RecognitionObserver(int totalThreads) {
		this.totalThreads = totalThreads;
	}
	
	private synchronized void updateResult(String addToResult){
		if(addToResult == null){
			addToResult = "";
		}
		this.result += addToResult;
		this.totalReceived += 1;
		System.out.println("Updated observer, total received: " + this.totalReceived + " of " + this.totalThreads);
	}
	
	public boolean isFinish() {
		return this.totalReceived >= this.totalThreads;
	}
	
	public String getResult(){
		return this.result;
	}
	
	@Override
	public void update(Observable o, Object arg) {
		String result = o.toString();
		this.updateResult(result);
	}

}
