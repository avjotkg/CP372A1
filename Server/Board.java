import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Board {
    public enum Result {
        //Enum class which relays information back up to the top level protocol for error messaging
        OK, 
        OUT_OF_BOUNDS,
        COMPLETE_OVERLAP,
        NO_NOTE_AT_COORDINATE,
        PIN_NOT_FOUND
    }

    private final Protocol.Config cfg;
    private final ReentrantReadWriteLock rw = new ReentrantReadWriteLock(); //To prevent race conditions have a simple rw lock.
    
    private final List <Note> notes = new ArrayList<>();
    private long seq = 0;

    public Board (Protocol.Config cfg){
        this.cfg = cfg;
    }

    //Functions below are small relativly atomic helper functions.
    public boolean isOnBoardPoint(int px, int py){
        return 0 <= px && px < cfg.board_width() && 0 <= py && py < cfg.board_height();
    }

    private boolean noteFits(int x, int y){
        return 0 <= x && 0 <= y && x + cfg.note_width() <= cfg.board_width() && y + cfg.note_height() <= cfg.board_height();
    }

    private boolean hasCompleteOverlap (int x, int y){
        for (Note n: notes) if (n.x() == x && n.y() == y) return true;
        return false;
    }
    
    private boolean isPinInNote(Note n, int px, int py){
        return n.x() <= px && px < n.x() + cfg.note_width() && n.y() <= py && py < n.y() + cfg.note_height();
    }

    private String noteLine (Note n){
        return "NOTE " + n.x() + " " + n.y() + " " + n.color() + " " + escapeMessage(n.message());

    }

    private String escapeMessage(String s){
        return s;
    }

    public Result post(int x, int y, String color, String message){
        rw.writeLock().lock(); //Aquires lock
        try{
            //Determines if note is in valid position
            if (!noteFits(x, y)) return Result.OUT_OF_BOUNDS;
            if (hasCompleteOverlap(x, y)) return Result.COMPLETE_OVERLAP;
            //If so creates new note object and adds it to the board object
            Note n = new Note(x, y, color, message, ++seq);
            notes.add(n);
            return Result.OK;
        } finally {
            rw.writeLock().unlock(); //Releases lock
        }
    }



    public Result pin(int x, int y){
        rw.writeLock().lock(); //Aquires lock
        try {
            boolean pinnedAny = false;
            Pin p = new Pin(x,y);
            //Checks each note to determine overlap, and updates variables if true.
            for (Note n: notes){
                if (!n.pins().contains(p) && isPinInNote(n, x, y)){
                    n.pins().add(p);
                    pinnedAny = true;
                }
            }
            return pinnedAny? Result.OK : Result.NO_NOTE_AT_COORDINATE;
        } finally {
            rw.writeLock().unlock(); //Releases lock
        }
    }

    public Result unpin(int x, int y){
        rw.writeLock().lock(); //Aquires lock
        try {
            boolean anything = false;
            //Checks through all notes and jeeps track of found
            for (Note n: notes){
                for (Pin p: n.pins()){
                    if (p.x() == x && p.y() == y) {
                        n.pins().remove(p);
                        anything = true;
                        break;
                    }
                }
            }
            //If found anything remove most recent
            if (!anything) return Result.PIN_NOT_FOUND;
            return Result.OK;
        } finally {
            rw.writeLock().unlock(); //Releases lock
        }
    }

    public int shake () {
        rw.writeLock().lock();  //Aquires lock
        try{
            //Remove all notes not attached to board through force. Keep track of count
            int before = notes.size();
            notes.removeIf(n -> n.pins().isEmpty());
            return before - notes.size();
        } finally {
            rw.writeLock().unlock(); //Releases lock
        }
    }

    public void clear () {
        rw.writeLock().lock();  //Aquires lock
        try {
            //Wipe all notes with helper function.
            notes.clear();
        } finally {
            rw.writeLock().unlock(); //Releases lock
        }
    }

    public String getAllNotes(){
        rw.readLock().lock();  //Aquires lock
        try {
            //Use string builder to track all notes and display neatly.
            StringBuilder sb = new StringBuilder();
            sb.append("OK ").append(notes.size()).append(" RESULTS\n");
            for (Note n: notes) sb.append(noteLine(n)).append("\n");
            sb.append("END\n");
            return sb.toString();

        } finally {
            rw.readLock().unlock(); //Releases lock
        }
    }

    public String getNotesByColor(String color){
        rw.readLock().lock();  //Aquires lock
        try {
            //Interate through notes keeping those which align on color
            List<Note> filtered = new ArrayList<>();
            for (Note n: notes) if (n.color().equals(color)) filtered.add(n);

            StringBuilder sb = new StringBuilder();
            sb.append("OK ").append(filtered.size()).append(" RESULTS\n");
            for (Note n: filtered) sb.append(noteLine(n)).append("\n");
            sb.append("END\n");
            return sb.toString();

        } finally {
            rw.readLock().unlock(); //Releases lock
        }
    }

    public String getNotesAt(int x, int y){
        rw.readLock().lock();  //Aquires lock
        try {
            //Uses helper function to determine all notes which are overlaying the pin locaiton
            List<Note> filtered = new ArrayList<>();
            for (Note n: notes) if (isPinInNote(n, x, y)) filtered.add(n);

            StringBuilder sb = new StringBuilder();
            sb.append("OK ").append(filtered.size()).append(" RESULTS\n");
            for (Note n: filtered) sb.append(noteLine(n)).append("\n");
            sb.append("END\n");
            return sb.toString();

        } finally {
            rw.readLock().unlock(); //Releases lock
        }
    }

    public String getAllPins(){
        rw.readLock().lock();  //Aquires lock
        try { 
            //Returns list of all found pins
            Set<Pin> pins = new LinkedHashSet<>();
            for (Note n: notes) pins.addAll(n.pins());

            StringBuilder sb = new StringBuilder();
            sb.append("OK ").append(pins.size()).append(" RESULTS\n");
            for (Pin p: pins) sb.append("PIN ").append(p.x()).append(" ").append(p.y()).append("\n");

            sb.append("END\n");
            return sb.toString();

        } finally {
            rw.readLock().unlock(); //Releases lock
        }
    }


}