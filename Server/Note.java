import java.util.HashSet;
import java.util.Set;

public class Note {
    //Base level class for storing data related to note objects
    private final int x, y;
    private final String color, message;
    private final long seq;
    private final Set<Pin> pins = new HashSet<>();

    public Note(int x, int y, String color, String message, long seq){
        this.x = x;
        this.y = y;
        this.color = color;
        this.message = message;
        this.seq = seq;
    }

    public int x() {return x;}
    public int y() {return y;}
    public String color() {return color;}
    public String message() {return message;}
    public long seq() {return seq;}
    public Set<Pin> pins() {return pins;}

    boolean addPin(Pin p) { return pins.add(p); }
    boolean removePin(Pin p) { return pins.remove(p); }
    boolean hasPin(Pin p) { return pins.contains(p); }
    boolean hasAnyPins() { return !pins.isEmpty(); }
}

