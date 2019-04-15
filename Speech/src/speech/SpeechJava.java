/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package speech;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Port;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.result.WordResult;
import static speech.SpeechForm.jTextArea1;
/**
 *
 * @author PLPNghi
 */
public class SpeechJava {
    // Necessary
	private LiveSpeechRecognizer recognizer;
	
	// Logger
	private Logger logger = Logger.getLogger(getClass().getName());
	
	/**
	 * This String contains the Result that is coming back from SpeechRecognizer
	 */
	private String speechRecognitionResult;
	
	//-----------------Lock Variables-----------------------------
	
	/**
	 * This variable is used to ignore the results of speech recognition cause actually it can't be stopped...
	 * 
	 * <br>
	 * Check this link for more information: <a href=
	 * "https://sourceforge.net/p/cmusphinx/discussion/sphinx4/thread/3875fc39/">https://sourceforge.net/p/cmusphinx/discussion/sphinx4/thread/3875fc39/</a>
	 */
	private boolean ignoreSpeechRecognitionResults = false;
	
	/**
	 * Checks if the speech recognise is already running
	 */
	private boolean speechRecognizerThreadRunning = false;
	
	/**
	 * Checks if the resources Thread is already running
	 */
	private boolean resourcesThreadRunning;
	
	//---
	
	/**
	 * This executor service is used in order the playerState events to be executed in an order
	 */
	private ExecutorService eventsExecutorService = Executors.newFixedThreadPool(2);
	
	//------------------------------------------------------------------------------------
	
	/**
	 * Constructor
	 */
	public SpeechJava() {
		// Loading Message
		logger.log(Level.INFO, "Loading Speech Recognizer...\n");
		
		// Configuration
		Configuration configuration = new Configuration();
		
		// Load model from the jar
//		configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
//		configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
		configuration.setAcousticModelPath("src/data/speech");
		configuration.setDictionaryPath("src/data/speech/speech.dict");
		
		//====================================================================================
		//=====================READ THIS!!!===============================================
		//Uncomment this line of code if you want the recognizer to recognize every word of the language 
		//you are using , here it is English for example	
		//====================================================================================
		configuration.setLanguageModelPath("src/data/speech/speech.lm.bin");
		
		//====================================================================================
		//=====================READ THIS!!!===============================================
		//If you don't want to use a grammar file comment below 3 lines and uncomment the above line for language model	
		//====================================================================================
		
		// Grammar
		configuration.setGrammarPath("src/grammars");
		configuration.setGrammarName("grammar");
		configuration.setUseGrammar(true);
		
		try {
			recognizer = new LiveSpeechRecognizer(configuration);
		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
		
		// Start recognition process pruning previously cached data.
		// recognizer.startRecognition(true);
//		SpeechForm.jTextArea1.append("Xin Chao");
		//Check if needed resources are available
		startResourcesThread();
		//Start speech recognition thread
		startSpeechRecognition();
	}
	
	//-----------------------------------------------------------------------------------------------
	
	/**
	 * Starts the Speech Recognition Thread
	 */
	public synchronized void startSpeechRecognition() {
		
		//Check lock
		if (speechRecognizerThreadRunning)
			logger.log(Level.INFO, "Speech Recognition Thread already running...\n");
		else
			//Submit to ExecutorService
			eventsExecutorService.submit(() -> {
				
				//locks
				speechRecognizerThreadRunning = true;
				ignoreSpeechRecognitionResults = false;
				
				//Start Recognition
				recognizer.startRecognition(true);
				
				//Information			
				logger.log(Level.INFO, "You can start to speak...\n");
				
				try {
					while (speechRecognizerThreadRunning) {
						/*
						 * This method will return when the end of speech is reached. Note that the end pointer will determine the end of speech.
						 */
						SpeechResult speechResult = recognizer.getResult();
						
						//Check if we ignore the speech recognition results
						if (!ignoreSpeechRecognitionResults) {
							
							//Check the result
							if (speechResult == null)
								logger.log(Level.INFO, "I can't understand what you said.\n");
							else {
								
								//Get the hypothesis
								speechRecognitionResult = speechResult.getHypothesis();
								
								//You said?
								System.out.println("You said: [" + speechRecognitionResult + "]\n");
                                                                speechRecognitionResult = mapToVietnamese(speechRecognitionResult);
                                                                SpeechForm.jTextArea1.append(speechRecognitionResult + " ");
								//Call the appropriate method 
								makeDecision(speechRecognitionResult, speechResult.getWords());
                                                                
								
							}
						} else
							logger.log(Level.INFO, "Ingoring Speech Recognition Results...");
						
					}
				} catch (Exception ex) {
					logger.log(Level.WARNING, null, ex);
					speechRecognizerThreadRunning = false;
				}
				
				logger.log(Level.INFO, "SpeechThread has exited...");
				
			});
	}
	
	/**
	 * Stops ignoring the results of SpeechRecognition
	 */
	public synchronized void stopIgnoreSpeechRecognitionResults() {
		
		//Stop ignoring speech recognition results
		ignoreSpeechRecognitionResults = false;
	}
	
	/**
	 * Ignores the results of SpeechRecognition
	 */
	public synchronized void ignoreSpeechRecognitionResults() {
		
		//Instead of stopping the speech recognition we are ignoring it's results
		ignoreSpeechRecognitionResults = true;
		
	}
	
	//-----------------------------------------------------------------------------------------------
	
	/**
	 * Starting a Thread that checks if the resources needed to the SpeechRecognition library are available
	 */
	public void startResourcesThread() {
		
		//Check lock
		if (resourcesThreadRunning)
			logger.log(Level.INFO, "Resources Thread already running...\n");
		else
			//Submit to ExecutorService
			eventsExecutorService.submit(() -> {
				try {
					
					//Lock
					resourcesThreadRunning = true;
					
					// Detect if the microphone is available
					while (true) {
						
						//Is the Microphone Available
						if (!AudioSystem.isLineSupported(Port.Info.MICROPHONE))
							logger.log(Level.INFO, "Microphone is not available.\n");
						
						// Sleep some period
						Thread.sleep(350);
					}
					
				} catch (InterruptedException ex) {
					logger.log(Level.WARNING, null, ex);
					resourcesThreadRunning = false;
				}
			});
	}
	
	/**
	 * Takes a decision based on the given result
	 * 
	 * @param speechWords
	 */
	public void makeDecision(String speech , List<WordResult> speechWords) {
		
		System.out.println(speech);
		
	}
	
	public boolean getIgnoreSpeechRecognitionResults() {
		return ignoreSpeechRecognitionResults;
	}
	
	public boolean getSpeechRecognizerThreadRunning() {
		return speechRecognizerThreadRunning;
	}
	
        public String mapToVietnamese(String input){
            String[] lstOutput = input.split(" ");
            String output = "";
            if (lstOutput.length == 0) {
                output = input;
                if (output.equals("KHOONG")){
                    output = "KHÔNG ";
                } else if (output.equals("MOOTJ")){
                    output = "MỘT ";
                } else if (output.equals("HAI")){
                    output = "HAI ";
                } else if (output.equals("BA")){
                    output = "BA ";
                } else if (output.equals("BOONS")){
                    output = "BỐN ";
                } else if (output.equals("NAMW")){
                    output = "NĂM ";
                } else if (output.equals("SAUS")){
                    output = "SÁU ";
                } else if (output.equals("BAYR")){
                    output = "BẢY ";
                } else if (output.equals("TAMS")){
                    output = "TÁM ";
                } else if (output.equals("CHINS")){
                    output = "CHÍN ";
                }
            } else if (lstOutput.length > 0){
                for (int i = 0; i < lstOutput.length; i++){
                    String temp = lstOutput[i];
                    if (temp.equals("KHOONG")){
                        output += "KHÔNG ";
                    } else if (temp.equals("MOOTJ")){
                        output += "MỘT ";
                    } else if (temp.equals("HAI")){
                        output += "HAI ";
                    } else if (temp.equals("BA")){
                        output += "BA ";
                    } else if (temp.equals("BOONS")){
                        output += "BỐN ";
                    } else if (temp.equals("NAMW")){
                        output += "NĂM ";
                    } else if (temp.equals("SAUS")){
                        output += "SÁU ";
                    } else if (temp.equals("BAYR")){
                        output += "BẢY ";
                    } else if (temp.equals("TAMS")){
                        output += "TÁM ";
                    } else if (temp.equals("CHINS")){
                        output += "CHÍN ";
                    }
                }
            }
            
//            String[] lstOutput = input.split(" ");
//            String output = "";
//            System.out.println("Length: " + lstOutput.length);
//            if (lstOutput.length == 1) {
//                output = input;
//                if (output.equals("MOT")){
//                    output = "Một";
//                } else if (output.equals("BON")){
//                    output = "Bốn";
//                } else if (output.equals("NAM")){
//                    output = "Năm";
//                } else if (output.equals("SAU")){
//                    output = "Sáu";
//                } else if (output.equals("BAY")){
//                    output = "Bảy";
//                } else if (output.equals("TAM")){
//                    output = "Tám";
//                } else if (output.equals("CHIN")){
//                    output = "Chín";
//                }
//            } else if (lstOutput.length > 1){
//                for (int i = 0; i < lstOutput.length; i++){
//                    String temp = lstOutput[i];
//                    if (temp.equals("KHONG")){
//                        output += "KHÔNG ";
//                    } else if (temp.equals("MOT")){
//                        output += "MỘT ";
//                    } else if (temp.equals("HAI")){
//                        output += "HAI ";
//                    } else if (temp.equals("BA")){
//                        output += "BA ";
//                    } else if (temp.equals("BON")){
//                        output += "BỐN ";
//                    } else if (temp.equals("NAM")){
//                        output += "NĂM ";
//                    } else if (temp.equals("SAU")){
//                        output += "SÁU ";
//                    } else if (temp.equals("BAY")){
//                        output += "BẢY ";
//                    } else if (temp.equals("TAM")){
//                        output += "TÁM ";
//                    } else if (temp.equals("CHIN")){
//                        output += "CHÍN ";
//                    }
//                }
//                System.out.println("Output: " + output);
//            }
            return output;
        }
        
	/**
	 * Main Method
	 * 
	 * @param args
	 */
//	public static void main(String[] args) {
//		new SpeechJava();
//	}
}
