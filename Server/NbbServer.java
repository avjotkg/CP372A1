client-gui
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class NbbServer {

    private final int port;
    private final Board board;
    private final Protocol.Config cfg;

    public NbbServer(int port, Board board, Protocol.Config cfg){
        //Creates object based on specs for storing and routing down
        this.port = port;
        this.board = board;
        this.cfg = cfg;
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)){
            System.out.println("server listening on port " + port); //Confirms working
            while (true) {
                //Generates new client and thread then starts program
                Socket client = serverSocket.accept();
                Thread t = new Thread(new ClientHandler(client,board,cfg));
                t.start();
                
            }
        }
    }
    
}

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class NbbServer {

    private final int port;
    private final Board board;
    private final Protocol.Config cfg;

    public NbbServer(int port, Board board, Protocol.Config cfg){
        //Creates object based on specs for storing and routing down
        this.port = port;
        this.board = board;
        this.cfg = cfg;
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)){
            System.out.println("server listening on port " + port); //Confirms working
            while (true) {
                //Generates new client and thread then starts program
                Socket client = serverSocket.accept();
                Thread t = new Thread(new ClientHandler(client,board,cfg));
                t.start();
                
            }
        }
    }
    
}
main
