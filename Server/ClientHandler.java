client-gui
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable{
    private final Socket socket;
    private final Board board;
    private final Protocol.Config cfg;

    public ClientHandler(Socket socket, Board board, Protocol.Config cfg){
        this.socket = socket;
        this.board = board;
        this.cfg = cfg;
    }

    @Override
    public void run(){
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        ){
            out.write(Protocol.handshake(cfg));
            out.flush();

            String line;
            while ((line = in.readLine()) != null) {
                Protocol.Response resp = Protocol.handleLine(line, board, cfg);
                out.write(resp.text());
                out.flush();
                if (resp.closeAfterWrite()) break;
            }
        } catch (Exception e){
            // Some Error Message
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {}
            
        }
    }


}

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable{
    private final Socket socket;
    private final Board board;
    private final Protocol.Config cfg;

    public ClientHandler(Socket socket, Board board, Protocol.Config cfg){
        this.socket = socket;
        this.board = board;
        this.cfg = cfg;
    }

    @Override
    public void run(){
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        ){
            out.write(Protocol.handshake(cfg));
            out.flush();

            String line;
            while ((line = in.readLine()) != null) {
                Protocol.Response resp = Protocol.handleLine(line, board, cfg);
                out.write(resp.text());
                out.flush();
                if (resp.closeAfterWrite()) break;
            }
        } catch (Exception e){
            // Some Error Message
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {}
            
        }
    }


}
main
