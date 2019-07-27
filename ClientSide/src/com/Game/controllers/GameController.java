package com.Game.controllers;

import com.Game.CallEnum;
import com.Game.Cartella;
import com.Game.Tomboliere;
import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import com.sun.org.apache.regexp.internal.RE;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
// import com.util.TextToSpeech;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Create and set the methods to manage the game in client and server side
 */
public class GameController {

    //Player of a game
	private Player p;

    //Numbers that has been extracted
	private ArrayList<Integer> extractions;

    //Winnings of a game
	private HashMap<String,String> winnings;

    //Extractor of the numbers
	private Thread estrattore;

    //Number of cards of a player
	private int n;

    //The ip address to that the client has to connect to join a game session
	private String ipaddress = "localhost";

	//Last number that has been extracted
	private int lastNum;

	//The phrase that will be displayed when a player will make any type winning
	private String lastWinningPhrase;

	//Texttospeech
	//private TextToSpeech tts;



    /**
     * Constructor of the class GameController
     *
     * @param playerName username of a player
     * @param n numbers of cards of a player
     */
	public GameController(String playerName, int n) {

        //Set the json of the player
		String playerJson = connectHttpTo("http://"+ipaddress+":8282/addplayer?U=" + playerName + "&N=" + n);

		//tts = new TextToSpeech();
		//tts.setVoice("istc-lucia-hsmm");


		extractions = new ArrayList<>();
		winnings = new HashMap<>();
		p = deserializePlayer(playerJson,n);


		this.n = n;

		//tts.speak("Ciao " + p.getUsername(),1.0f,false,false);


	}

    /**
     * Traslate the player information from Json to object
     *
     * @param playerJson json of the player
     * @param n numbers of cards
     * @return p the istance of Player
     */
	private Player deserializePlayer(String playerJson, int n) {
		//Parsing json
		Any anyClass = JsonIterator.deserialize(playerJson);

		String uName = String.valueOf(anyClass.get("username"));

		Any cards = anyClass.get("cartelle");

        //Create a player
		Player p = new Player(uName);

        //Set the card of the player
		for (int i = 0; i < n; i++) {
			Any card = cards.get(i);
			String numbers = String.valueOf(card.get("numeri"));
			ArrayList<Integer> numArr = string2Array(numbers);

			p.addCartella(new Cartella(numArr));

		}
		return p;
	}


    /**
     * Translate the string of numbers to an Arraylist of numbers
     *
     * @param numbers String of numbers
     * @return Arraylist of numbers
     */
	private ArrayList<Integer> string2Array(String numbers) {

        //Parsing the string
		String num = numbers.substring(1,numbers.length()-1);
		String[] nums = num.split(",");

        //Create and add the numbers to the Arraylist
		ArrayList<Integer> numsArr = new ArrayList<>();

		for (int i = 0; i < nums.length; i++) {
			numsArr.add(Integer.valueOf(nums[i]));
		}
		return numsArr;
	}

    /**
     * Set the connection to the Http server
     *
     * @param url address of the server
     * @return the response, if the connection is successful
     * 		   null, in the other case
     */
	private String  connectHttpTo(String url) {

		try {

            //Set and initialize the connection
			URL connectionUrl = new URL(url);
			HttpURLConnection connection = (HttpURLConnection) connectionUrl.openConnection();
			connection.setRequestMethod("GET");

			int rcode = connection.getResponseCode();

			//System.out.println("\nSending 'GET' request to URL : " + url);
			//System.out.println("Response Code : " + rcode);

			BufferedReader in = new BufferedReader(
					new InputStreamReader(connection.getInputStream()));
			String inputLine;
			StringBuilder response = new StringBuilder();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			//return result
			return response.toString();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

    /**
     * Start the extraction of the numbers of a game
     * TODO: commentare singolarmente il codice
     *
     * @param updateFunction
     */
	public void startExtraction(Runnable updateFunction) {

        //Set and initialize the extractor
		estrattore = new Thread(() -> {

			while (true){

				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				String nums = connectHttpTo("http://"+ipaddress+":8282/extractions");

				Any anyNums = JsonIterator.deserialize(nums);
				extractions = string2Array(String.valueOf(anyNums.get("numbers")));

				String wins = connectHttpTo("http://"+ipaddress+":8282/winnings");
				Any anyWins = JsonIterator.deserialize(wins);
				String winners = anyWins.get("winners").toString();
				System.out.println(winners);
				winners=winners.substring(1,winners.length()-1);
				takeWinningUser(winners,winnings);

				if(!extractions.isEmpty() && extractions.get(extractions.size()-1) != lastNum) {
					//Reproduce audio and update last num
					lastNum = extractions.get(extractions.size()-1);

					String bip = "src/resources/BipSound.mp3";
					Media hit = new Media(new File(bip).toURI().toString());
					MediaPlayer mediaPlayer = new MediaPlayer(hit);
					mediaPlayer.play();


					//tts.speak(String.valueOf(lastNum),1.0f,false,false);
				}


				updateFunction.run();
			}
		});

		estrattore.start();
	}


    /**
     *Stop the extraction of the numbers
     */
	public void stopExtractions(){
		estrattore.interrupt();
		estrattore.stop();
	}


	public Cartella getCartella(int index) {
		return p.getCartella(index);
	}

    /**
     * Convert a card in an Array of numbers
     *
     * @param index of the card
     * @return Array of the numbers of a card
     */
	public Integer[] getCartellaAsArray(int index) {return p.getCartella(index).getNumeri();}

    /**
     * Send information of a player to the server if he presses a winning button
     *
     * @param callEnum the type of winning call
     * @param iCartella index of the card
     * @return true, if the response is correct
     *         false, if the response is null
     * @throws NullPointerException
     */
	public boolean buttonControl(CallEnum callEnum, int iCartella) throws NullPointerException {

	    //Set and initialize the response
		String resp;
		resp=connectHttpTo("http://"+ipaddress+":8282/checkcard?U=" +p.getUsername()+ "&C="+iCartella +"&CT=" +callEnum.name()+ "&LN="+extractions.get(extractions.size()-1));

		if(resp!=null && resp.equals("{\"check\":\"true\"}")){
			return true;
		}else{
			return false;
		}

	}

    /**
     * Obtain the numbers that has been extracted
     *
     * @return extractions numbers extracted
     */
	public ArrayList<Integer> getExtractions() {
		return extractions;
	}

    /**
     * Obtain the number of cards of a player
     *
     * @return n the number of cards
     */
	public int getCartellaCount() {
		return n;
	}

    /**
     * Control and set the variable lastWinningPhrase
     *
     * @param jsonwin contains the user and his winning
     * @param winnings the set of winnings
     */
	private void takeWinningUser (String jsonwin,HashMap winnings){
	    //Initialize the win string
		String[] win= jsonwin.split(",");

            //Set the lastWinningPhrase
			for (int i = 0; i < win.length; i++) {
				String[] userWin = win[i].split(":");
				if (userWin.length == 2) {
					winnings.put(userWin[1], userWin[0]); //TODO modificare in modo da ottenere tramite la chiave il nome utente
					lastWinningPhrase = userWin[0] + " ha fatto " + userWin[1];
				}
			}

	}

	public HashMap<String, String> getWinnings() {
		return winnings;
	}

    /**
     * Getter of an username
     *
     * @return the username of a player
     */
	public String getPlayerName() {
		return p.getUsername();
	}

    /**
     * Getter of lastWinningPhrase
     *
     * @return lastWinningPhrase
     */
	public String getLastWinningPhrase() {
		return lastWinningPhrase;
	}
}
