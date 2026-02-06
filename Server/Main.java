client-gui
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception{

        /*
        Input Format: <port> <board_width> <board_height> <note_width> <note_height> <color 1> ... <color n>
        Must be at least one color.
        */

        //Confirms minimum ammount of arguments
        if (args.length < 6){
            System.err.println("Not Enough Arguments");
            System.exit(1);
        }

        
        int port = Integer.parseInt(args[0]);
        int board_width = Integer.parseInt(args[1]);
        int board_height = Integer.parseInt(args[2]);
        int note_width = Integer.parseInt(args[3]);
        int note_height = Integer.parseInt(args[4]);
        
        List<String> note_colors = new ArrayList<>();

        //Note: Since the colors will be anything following the first 5 manditory arguments, we just append all following arguments to the list
        for (int i = 5; i < args.length; i++)
            {
                note_colors.add(args[i].trim().toLowerCase());
            }
            
        //Create a new Protocol object which contains all the 'settings' to persist
        Protocol.Config cfg = new Protocol.Config(board_width,board_height,note_width,note_height,note_colors);
        //With the newly created Protocol object create a Board Object
        Board board = new Board(cfg);
        //With all previous objects, start a new server object
        new NbbServer(port, board, cfg).start();
    }
}

import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception{

        /*
        Input Format: <port> <board_width> <board_height> <note_width> <note_height> <color 1> ... <color n>
        Must be at least one color.
        */

        //Confirms minimum ammount of arguments
        if (args.length < 6){
            System.err.println("Not Enough Arguments");
            System.exit(1);
        }

        
        int port = Integer.parseInt(args[0]);
        int board_width = Integer.parseInt(args[1]);
        int board_height = Integer.parseInt(args[2]);
        int note_width = Integer.parseInt(args[3]);
        int note_height = Integer.parseInt(args[4]);
        
        List<String> note_colors = new ArrayList<>();

        //Note: Since the colors will be anything following the first 5 manditory arguments, we just append all following arguments to the list
        for (int i = 5; i < args.length; i++) note_colors.add(args[i]);
        //Create a new Protocol object which contains all the 'settings' to persist
        Protocol.Config cfg = new Protocol.Config(board_width,board_height,note_width,note_height,note_colors);
        //With the newly created Protocol object create a Board Object
        Board board = new Board(cfg);
        //With all previous objects, start a new server object
        new NbbServer(port, board, cfg).start();
    }
}
main
