package Server;

import com.Game.CallEnum;
import com.Game.Cartella;
import com.Game.CartellaFactory;
import com.Game.Tomboliere;
import com.Game.controllers.Player;

import java.util.ArrayList;

public class Sessione {

    private Tomboliere t;
    private ArrayList<Player> players;

    public Sessione() {
        t = new Tomboliere();
        players = new ArrayList<>();




    }


    public void startExtractor(){
        Thread extractor = new Thread(() -> {
            while (true){
                System.out.println(t.getNumber());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });

        extractor.start();
    }

    public Player addPLayer(String username, int n){

        Player tmpPlayer = new Player(username);

        for (int i = 0; i < n; i++) {
            tmpPlayer.addCartella(CartellaFactory.createCartella());
        }

        players.add(tmpPlayer);
        return tmpPlayer;


    }


    public ArrayList<Integer> getExtractions() {
        return t.getExtractions();
    }

    public boolean checkCall(String username, int iCartella, CallEnum call) throws Exception {
        return t.checkCall(call,players.get(findPlayer(username)).getCartella(iCartella));
    }

    private int findPlayer(String username) {
        for (int i = 0; i < players.size(); i++) {
            if(players.get(i).getUsername().equals(username)) {
                return i;
            }
        }
        return -1;
    }
}
