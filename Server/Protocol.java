client-gui
import java.util.*;

public class Protocol {
    private Protocol() {}

    public record Config(int board_width, int board_height, int note_width, int note_height, List<String> colors){
        //Java data object constructor. Colorset can output hashed organzied version.
        public Set<String> colorSet() {
            return new HashSet<>(colors);
        }

    }

    public record Response(String text, boolean closeAfterWrite){
        //Java data object for responses.
        public static Response ok(String line) { return new Response(line.endsWith("\n") ? line : (line + "\n"), false);}
        public static Response okAndClose(String line) { return new Response(line.endsWith("\n") ? line : (line + "\n"), true);}
    }

    public enum Err {
        //Str enum for errors
        INVALID_FORMAT("01", "INVALID_FORMAT", "This format does not align with expected standard of %s."), 
        OUT_OF_BOUNDS("02", "OUT_OF_BOUNDS", "Coordinates are outside the valid board upper or lower bounds, or the object extends beyond"), 
        INVALID_COORDINATES("03", "INVALID_COORDINATES", "Coordinates must be non-negative integers in the expected format."), 
        UNSUPPORTED_COLOR("04", "UNSUPPORTED_COLOR", "This color is not supported by the server."), 
        COMPLETE_OVERLAP("05", "COMPLETE_OVERLAP", "A note already exists at this exact position (complete overlap is not allowed)."), 
        NO_NOTE_AT_COORDINATE("06", "NO_NOTE_AT_COORDINATE", "No note exists at the given coordinate or there is existing conflict"), 
        PIN_NOT_FOUND("07", "PIN_NOT_FOUND", "No pin exists at the given coordinate."); 
        final String nn, code, msg; 
        Err(String nn, String code, String msg) { this.nn = nn; this.code = code; this.msg = msg; }
    
    }

    public static String handshake (Config cfg){
        //Returns delimited and formatted cfg data class for handshaking.
        String colorpart = String.join(" ", cfg.colors());
        return String.format(
            "HELLO NBB/1.0 %d %d %d %d %s\n",
            cfg.board_width(),
            cfg.board_height(),
            cfg.note_width(),
            cfg.note_height(),
            colorpart
        );
    }

    public static Response error(Err e, String commandName){
        //Returns, expected error format for client communication
        String msg = e.msg.contains("%s") ? String.format(e.msg, commandName): e.msg;
        return Response.ok(String.format("ERROR [%s] [%s] [%s]\n", e.nn, e.code, msg));
    }

    
    public static Response handleLine(String rawLine, Board board, Config cfg){
        //Delimit and clean command to determine where to route
        String line = rawLine.trim().toLowerCase();;
        if (line.isEmpty()) return error (Err.INVALID_FORMAT, "<COMMAND>");

        String[] parts = line.split("\\s+");
        String cmd = parts[0];

        //Take header and use switch statement to route to approperite function
        return switch (cmd) {

            case "post" -> handlePost(rawLine, parts, board, cfg);
            case "get" -> handleGet(parts, board, cfg);
            case "pin" -> handlePin(parts, board, cfg);
            case "unpin" -> handleUnpin(parts, board, cfg);
            case "shake" -> handleShake(parts, board);
            case "clear" -> handleClear(parts, board);
            case "disconnect" -> handleDisconnect(parts);
            default -> error (Err.INVALID_FORMAT, "<COMMAND>");
        };
    }

    private static Response handlePost(String rawLine, String[] parts, Board board, Config cfg){
        if (parts.length < 5) return error (Err.INVALID_FORMAT, "POST");

        Integer x = parseNonNegInt(parts[1]); //Grab sticky position X
        Integer y = parseNonNegInt(parts[2]); //Grab sticky position Y

        if (x == null || y == null) return error (Err.INVALID_COORDINATES, "POST");

        String color = parts[3]; //Grab sticky color and verify it is within set.
        if (!cfg.colorSet().contains(color)) return error (Err.UNSUPPORTED_COLOR, "POST");

        int idx = scrapeMessage(rawLine, 4);

        if (idx < 0 || idx >= rawLine.length()) return error(Err.INVALID_FORMAT, "POST");
        String message = rawLine.substring(idx).trim();

        Board.Result r = board.post(x,y,color,message);

        return switch (r) {
            case OK -> Response.ok("OK POSTED\n");
            case OUT_OF_BOUNDS -> error(Err.OUT_OF_BOUNDS, "POST");
            case COMPLETE_OVERLAP -> error(Err.COMPLETE_OVERLAP, "POST");
            default -> error(Err.INVALID_FORMAT, "POST");
        };

    }

    private static Response handleGet(String[] parts, Board board, Config cfg){

        //Determine what type of get is being called, parse and route to correct funtion in board.
        if (parts.length == 1){
            //Get with returns all
            return Response.ok(board.getAllNotes());
        }

        String arg = parts[1];

        if (arg.equals("pins") && parts.length == 2){
            return Response.ok(board.getAllPins());
        }

        if (arg.startsWith("color=") && parts.length == 2){
            String color = arg.substring("color=".length());
            if (!cfg.colorSet().contains(color)) return error (Err.UNSUPPORTED_COLOR, "GET");
            return Response.ok(board.getNotesByColor(color));
        }

        if (arg.startsWith("contains="))
            {
                String aftereq = arg.substring("contains=".length());
            
                String xs;
                String ys;
            
                // supports: contains=1,1
                if (aftereq.contains(","))
                {
                    String[] p = aftereq.split(",");
                    if (p.length != 2) return error(Err.INVALID_FORMAT, "GET");
                    xs = p[0];
                    ys = p[1];
                }
                // supports: contains=1 1  (space separated, y is next token)
                else
                {
                    xs = aftereq;
                    if (parts.length < 3) return error(Err.INVALID_FORMAT, "GET");
                    ys = parts[2];
                }
            
                Integer x = parseNonNegInt(xs);
                Integer y = parseNonNegInt(ys);
            
                if (x == null || y == null) return error(Err.INVALID_COORDINATES, "GET");
                if (!board.isOnBoardPoint(x, y)) return error(Err.OUT_OF_BOUNDS, "GET");
            
                return Response.ok(board.getNotesAt(x, y));
            }
            

        return error(Err.INVALID_FORMAT, "GET");

    }

    private static Response handlePin(String[] parts, Board board, Config cfg){
        //Parse Pin Command
        if (parts.length != 3) return error (Err.INVALID_FORMAT, "PIN");
        Integer x = parseNonNegInt(parts[1]);
        Integer y = parseNonNegInt(parts[2]);
        
        //Check for errors
        if (x == null || y == null) return error (Err.INVALID_COORDINATES, "PIN");
        if (!board.isOnBoardPoint(x,y)) return error(Err.OUT_OF_BOUNDS, "PIN");

        //Call board Pin function and give client response.
        Board.Result r = board.pin(x,y);
        return switch (r) {
            case OK -> Response.ok("OK PINNED\n");
            case NO_NOTE_AT_COORDINATE -> error(Err.NO_NOTE_AT_COORDINATE, "PIN");
            default -> error (Err.INVALID_FORMAT, "PIN");
        };
    }

    private static Response handleUnpin(String [] parts, Board board, Config cfg){
        //PArse
        if (parts.length != 3) return error (Err.INVALID_FORMAT, "UNPIN");
        Integer x = parseNonNegInt(parts[1]);
        Integer y = parseNonNegInt(parts[2]);
        
        //Error Catch
        if (x == null || y == null) return error (Err.INVALID_COORDINATES, "UNPIN");
        if (!board.isOnBoardPoint(x,y)) return error(Err.OUT_OF_BOUNDS, "UNPIN");

        //Call board Unpin function, relay to client result.
        Board.Result r = board.unpin(x,y);
        return switch (r) {
            case OK -> Response.ok("OK UNPINNED\n");
            case PIN_NOT_FOUND -> error(Err.PIN_NOT_FOUND, "UNPIN");
            default -> error (Err.INVALID_FORMAT, "UNPIN");
        };
    }


    //Shake, clear, disconnect dont require much parsing or error checking so their functions are all relativly atomic.
    private static Response handleShake(String [] parts, Board board){
        if (parts.length != 1) return error (Err.INVALID_FORMAT, "SHAKE");
        int removed = board.shake();
        return Response.ok("OK SHAKEN " + removed + "\n");

    }

    private static Response handleClear (String [] parts, Board board){
        if (parts.length != 1) return error (Err.INVALID_FORMAT, "CLEAR");
        board.clear();
        return Response.ok("OK CLEARED\n");
    }

    private static Response handleDisconnect(String[] parts){
        if (parts.length != 1) return error (Err.INVALID_FORMAT, "DISCONNECT");

        return Response.okAndClose("OK DISCONNECTING\n");
    }

    private static Integer parseNonNegInt(String s){
        //Helper function to parse an string to int
        try {
            int v = Integer.parseInt(s.trim());
            return v < 0 ? null : v; 
            
        } catch (Exception e){
            return null;
        }
    }

    private static int scrapeMessage(String rawLine, int tokenCount){
        //Helper function to scrape posted messages
        int seenTokens = 0;
        boolean inToken = false;

        for (int i = 0; i < rawLine.length(); i++){
            char c = rawLine.charAt(i);
            boolean isSpace = Character.isWhitespace(c);
            if (!isSpace && !inToken) {
                inToken = true;
                seenTokens ++;
                if (seenTokens == tokenCount + 1) return i;
            } else if (isSpace && inToken){
                inToken = false;
            }
        }
        return -1;
    }



}

import java.util.*;
import java.util.stream.Collectors;

public class Protocol {
    private Protocol() {}

    public record Config(int board_width, int board_height, int note_width, int note_height, List<String> colors){
        //Java data object constructor. Colorset can output hashed organzied version.
        public Set<String> colorSet() {
            return new HashSet<>(colors);
        }

    }

    public record Response(String text, boolean closeAfterWrite){
        //Java data object for responses.
        public static Response ok(String line) { return new Response(line.endsWith("\n") ? line : (line + "\n"), false);}
        public static Response okAndClose(String line) { return new Response(line.endsWith("\n") ? line : (line + "\n"), true);}
    }

    public enum Err {
        //Str enum for errors
        INVALID_FORMAT("01", "INVALID_FORMAT", "This format does not align with expected standard of %s."), 
        OUT_OF_BOUNDS("02", "OUT_OF_BOUNDS", "Coordinates are outside the valid board upper or lower bounds, or the object extends beyond"), 
        INVALID_COORDINATES("03", "INVALID_COORDINATES", "Coordinates must be non-negative integers in the expected format."), 
        UNSUPPORTED_COLOR("04", "UNSUPPORTED_COLOR", "This color is not supported by the server."), 
        COMPLETE_OVERLAP("05", "COMPLETE_OVERLAP", "A note already exists at this exact position (complete overlap is not allowed)."), 
        NO_NOTE_AT_COORDINATE("06", "NO_NOTE_AT_COORDINATE", "No note exists at the given coordinate or there is existing conflict"), 
        PIN_NOT_FOUND("07", "PIN_NOT_FOUND", "No pin exists at the given coordinate."); 
        final String nn, code, msg; 
        Err(String nn, String code, String msg) { this.nn = nn; this.code = code; this.msg = msg; }
    
    }

    public static String handshake (Config cfg){
        //Returns delimited and formatted cfg data class for handshaking.
        String colorPart = String.join(" ", cfg.colors());
        return String.format("HELLO %d %d %d %d %s\n",cfg.board_width, cfg.board_height, cfg.note_width, cfg.note_height, colorPart);

    }

    public static Response error(Err e, String commandName){
        //Returns, expected error format for client communication
        String msg = e.msg.contains("%s") ? String.format(e.msg, commandName): e.msg;
        return Response.ok(String.format("ERROR [%s] [%s] [%s]\n", e.nn, e.code, msg));
    }

    
    public static Response handleLine(String rawLine, Board board, Config cfg){
        //Delimit and clean command to determine where to route
        String line = rawLine.trim().toLowerCase();;
        if (line.isEmpty()) return error (Err.INVALID_FORMAT, "<COMMAND>");

        String[] parts = line.split("\\s+");
        String cmd = parts[0];

        //Take header and use switch statement to route to approperite function
        return switch (cmd) {

            case "post" -> handlePost(rawLine, parts, board, cfg);
            case "get" -> handleGet(parts, board, cfg);
            case "pin" -> handlePin(parts, board, cfg);
            case "unpin" -> handleUnpin(parts, board, cfg);
            case "shake" -> handleShake(parts, board);
            case "clear" -> handleClear(parts, board);
            case "disconnect" -> handleDisconnect(parts);
            default -> error (Err.INVALID_FORMAT, "<COMMAND>");
        };
    }

    private static Response handlePost(String rawLine, String[] parts, Board board, Config cfg){
        if (parts.length < 5) return error (Err.INVALID_FORMAT, "POST");

        Integer x = parseNonNegInt(parts[1]); //Grab sticky position X
        Integer y = parseNonNegInt(parts[2]); //Grab sticky position Y

        if (x == null || y == null) return error (Err.INVALID_COORDINATES, "POST");

        String color = parts[3]; //Grab sticky color and verify it is within set.
        if (!cfg.colorSet().contains(color)) return error (Err.UNSUPPORTED_COLOR, "POST");

        int idx = scrapeMessage(rawLine, 4);

        if (idx < 0 || idx >= rawLine.length()) return error(Err.INVALID_FORMAT, "POST");
        String message = rawLine.substring(idx).trim();

        Board.Result r = board.post(x,y,color,message);

        return switch (r) {
            case OK -> Response.ok("OK POSTED\n");
            case OUT_OF_BOUNDS -> error(Err.OUT_OF_BOUNDS, "POST");
            case COMPLETE_OVERLAP -> error(Err.COMPLETE_OVERLAP, "POST");
            default -> error(Err.INVALID_FORMAT, "POST");
        };

    }

    private static Response handleGet(String[] parts, Board board, Config cfg){

        //Determine what type of get is being called, parse and route to correct funtion in board.
        if (parts.length == 1){
            //Get with returns all
            return Response.ok(board.getAllNotes());
        }

        String arg = parts[1];

        if (arg.equals("pins") && parts.length == 2){
            return Response.ok(board.getAllPins());
        }

        if (arg.startsWith("color=") && parts.length == 2){
            String color = arg.substring("color=".length());
            if (!cfg.colorSet().contains(color)) return error (Err.UNSUPPORTED_COLOR, "GET");
            return Response.ok(board.getNotesByColor(color));
        }

        if (arg.startsWith("contains=") && parts.length == 2){
            String xy = arg.substring("contains=".length());
            String[] p = xy.split(",");
            if (p.length != 2) {
                p = xy.split(" ");
            }
            if (p.length != 2) return error (Err.INVALID_FORMAT, "GET");
            Integer x = parseNonNegInt(p[0]);
            Integer y = parseNonNegInt(p[1]);
            if (x == null || y == null) return error (Err.INVALID_COORDINATES, "GET");
            if (!board.isOnBoardPoint(x,y)) return error(Err.OUT_OF_BOUNDS, "GET");

            return Response.ok(board.getNotesAt(x,y));
        };

        return error(Err.INVALID_FORMAT, "GET");

    }

    private static Response handlePin(String[] parts, Board board, Config cfg){
        //Parse Pin Command
        if (parts.length != 3) return error (Err.INVALID_FORMAT, "PIN");
        Integer x = parseNonNegInt(parts[1]);
        Integer y = parseNonNegInt(parts[2]);
        
        //Check for errors
        if (x == null || y == null) return error (Err.INVALID_COORDINATES, "PIN");
        if (!board.isOnBoardPoint(x,y)) return error(Err.OUT_OF_BOUNDS, "PIN");

        //Call board Pin function and give client response.
        Board.Result r = board.pin(x,y);
        return switch (r) {
            case OK -> Response.ok("OK PINNED\n");
            case NO_NOTE_AT_COORDINATE -> error(Err.NO_NOTE_AT_COORDINATE, "PIN");
            default -> error (Err.INVALID_FORMAT, "PIN");
        };
    }

    private static Response handleUnpin(String [] parts, Board board, Config cfg){
        //PArse
        if (parts.length != 3) return error (Err.INVALID_FORMAT, "UNPIN");
        Integer x = parseNonNegInt(parts[1]);
        Integer y = parseNonNegInt(parts[2]);
        
        //Error Catch
        if (x == null || y == null) return error (Err.INVALID_COORDINATES, "UNPIN");
        if (!board.isOnBoardPoint(x,y)) return error(Err.OUT_OF_BOUNDS, "UNPIN");

        //Call board Unpin function, relay to client result.
        Board.Result r = board.unpin(x,y);
        return switch (r) {
            case OK -> Response.ok("OK UNPINNED\n");
            case PIN_NOT_FOUND -> error(Err.PIN_NOT_FOUND, "UNPIN");
            default -> error (Err.INVALID_FORMAT, "UNPIN");
        };
    }


    //Shake, clear, disconnect dont require much parsing or error checking so their functions are all relativly atomic.
    private static Response handleShake(String [] parts, Board board){
        if (parts.length != 1) return error (Err.INVALID_FORMAT, "SHAKE");
        int removed = board.shake();
        return Response.ok("OK SHAKEN" + removed + "\n");

    }

    private static Response handleClear (String [] parts, Board board){
        if (parts.length != 1) return error (Err.INVALID_FORMAT, "CLEAR");
        board.clear();
        return Response.ok("OK CLEARED\n");
    }

    private static Response handleDisconnect(String[] parts){
        if (parts.length != 1) return error (Err.INVALID_FORMAT, "DISCONNECT");

        return Response.okAndClose("OK DISCONNECTING\n");
    }

    private static Integer parseNonNegInt(String s){
        //Helper function to parse an string to int
        try {
            int v = Integer.parseInt(s.trim());
            return v < 0 ? null : v; 
            
        } catch (Exception e){
            return null;
        }
    }

    private static int scrapeMessage(String rawLine, int tokenCount){
        //Helper function to scrape posted messages
        int seenTokens = 0;
        boolean inToken = false;

        for (int i = 0; i < rawLine.length(); i++){
            char c = rawLine.charAt(i);
            boolean isSpace = Character.isWhitespace(c);
            if (!isSpace && !inToken) {
                inToken = true;
                seenTokens ++;
                if (seenTokens == tokenCount + 1) return i;
            } else if (isSpace && inToken){
                inToken = false;
            }
        }
        return -1;
    }



}
main
