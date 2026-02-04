
// client protocol notes based on updated rfc
// outgoing requests (these must be one line each):
    // POST <x> <y> <color> <message>
    // GET [color=<color>] [contains=<x> <y>] [refersTo=<substring>]  // any subset, if none just simply GET
    // PIN <x> <y>
    // UNPIN <x> <y>
    // SHAKE
    // CLEAR
    // DISCONNECT

// incoming replies:
    // print every received line exactly as received (no parsing)
    // multi-line replies (like get) end with END (print it too)

import java.awt.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;

public class ClientGUI extends JFrame
{
    // connection controls
    private JTextField hostfield;
    private JTextField portfield;
    private JButton connectbutton;
    private JButton disconnectbutton;
    private JLabel statuslabel;
    private JLabel serverinfolabel;

    // post controls
    private JTextField postxfield;
    private JTextField postyfield;
    private JComboBox<String> postcolorbox;
    private JTextField postmessagefield;
    private JButton postbutton;

    // get controls
    private JComboBox<String> getcolorbox;
    private JTextField containsxfield;
    private JTextField containsyfield;
    private JTextField referstofield;
    private JButton getbutton;
    private JButton getpinsbutton;

    // pin/unpin controls
    private JTextField pinxfield;
    private JTextField pinyfield;
    private JButton pinbutton;
    private JButton unpinbutton;

    // admin controls
    private JButton shakebutton;
    private JButton clearbutton;

    // output controls
    private JTextArea outputarea;
    private JButton clearoutputbutton;

    // connection backend
    private ClientConnection clientconnection;

    // handshake information
    private boolean helloreceived;
    private int boardwidth;
    private int boardheight;
    private int notewidth;
    private int noteheight;
    private String[] validcolors;

    public ClientGUI()
    {
        // basic window setup
        setTitle("cp372 a1 client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 700);
        setLocationRelativeTo(null);

        // main layout
        JPanel mainpanel = new JPanel(new BorderLayout(10, 10));
        mainpanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setContentPane(mainpanel);

        // top connection panel
        JPanel connectionpanel = buildconnectionpanel();
        mainpanel.add(connectionpanel, BorderLayout.NORTH);

        // middle commands panel
        JPanel commandspanel = buildcommandspanel();
        mainpanel.add(commandspanel, BorderLayout.CENTER);

        // bottom output panel
        JPanel outputpanel = buildoutputpanel();
        mainpanel.add(outputpanel, BorderLayout.SOUTH);

        // initial state: not connected
        setconnectedstate(false);

        // handshake defaults
        helloreceived = false;
        boardwidth = -1;
        boardheight = -1;
        notewidth = -1;
        noteheight = -1;
        validcolors = null;

        // local wiring
        clearoutputbutton.addActionListener(e ->
        {
            outputarea.setText("");
        });

        connectbutton.addActionListener(e ->
        {
            doconnect();
        });

        disconnectbutton.addActionListener(e ->
        {
            dodisconnect();
        });

        // command button wiring
        postbutton.addActionListener(e ->
        {
            dopost();
        });

        getbutton.addActionListener(e ->
        {
            doget();
        });

        getpinsbutton.addActionListener(e ->
        {
            dogetpins();
        });

        pinbutton.addActionListener(e ->
        {
            dopin(true);
        });

        unpinbutton.addActionListener(e ->
        {
            dopin(false);
        });

        shakebutton.addActionListener(e ->
        {
            doshake();
        });

        clearbutton.addActionListener(e ->
        {
            doclear();
        });
    }

    private JPanel buildconnectionpanel()
    {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "connection",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel hostlabel = new JLabel("host:");
        hostfield = new JTextField("localhost", 14);

        JLabel portlabel = new JLabel("port:");
        portfield = new JTextField("5000", 6);

        connectbutton = new JButton("connect");
        disconnectbutton = new JButton("disconnect");

        statuslabel = new JLabel("status: disconnected");
        serverinfolabel = new JLabel("server: (not connected)");

        // row 0
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(hostlabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        panel.add(hostfield, gbc);

        gbc.gridx = 2;
        gbc.gridy = 0;
        panel.add(portlabel, gbc);

        gbc.gridx = 3;
        gbc.gridy = 0;
        panel.add(portfield, gbc);

        gbc.gridx = 4;
        gbc.gridy = 0;
        panel.add(connectbutton, gbc);

        gbc.gridx = 5;
        gbc.gridy = 0;
        panel.add(disconnectbutton, gbc);

        // row 1
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        panel.add(statuslabel, gbc);

        gbc.gridx = 3;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        panel.add(serverinfolabel, gbc);

        gbc.gridwidth = 1;

        return panel;
    }

    private JPanel buildcommandspanel()
    {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));

        panel.add(buildpostpanel());
        panel.add(buildgetpanel());
        panel.add(buildpinpanel());
        panel.add(buildadminpanel());

        return panel;
    }

    private JPanel buildpostpanel()
    {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "post",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel xlabel = new JLabel("x:");
        postxfield = new JTextField(6);

        JLabel ylabel = new JLabel("y:");
        postyfield = new JTextField(6);

        JLabel colorlabel = new JLabel("color:");
        postcolorbox = new JComboBox<>(new String[] { "(no colors yet)" });

        JLabel messagelabel = new JLabel("message:");
        postmessagefield = new JTextField(18);

        postbutton = new JButton("send post");

        // row 0
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(xlabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        panel.add(postxfield, gbc);

        gbc.gridx = 2;
        gbc.gridy = 0;
        panel.add(ylabel, gbc);

        gbc.gridx = 3;
        gbc.gridy = 0;
        panel.add(postyfield, gbc);

        // row 1
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(colorlabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        panel.add(postcolorbox, gbc);
        gbc.gridwidth = 1;

        // row 2
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(messagelabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        panel.add(postmessagefield, gbc);
        gbc.gridwidth = 1;

        // row 3
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 4;
        panel.add(postbutton, gbc);
        gbc.gridwidth = 1;

        return panel;
    }

    private JPanel buildgetpanel()
    {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "get",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel colorlabel = new JLabel("color:");
        getcolorbox = new JComboBox<>(new String[] { "(any)", "(no colors yet)" });

        JLabel containslabel = new JLabel("contains x y:");
        containsxfield = new JTextField(5);
        containsyfield = new JTextField(5);

        JLabel referstolabel = new JLabel("refersto:");
        referstofield = new JTextField(16);

        getbutton = new JButton("send get");
        getpinsbutton = new JButton("get pins");

        // row 0
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(colorlabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        panel.add(getcolorbox, gbc);
        gbc.gridwidth = 1;

        // row 1
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(containslabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        panel.add(containsxfield, gbc);

        gbc.gridx = 2;
        gbc.gridy = 1;
        panel.add(containsyfield, gbc);

        // row 2
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(referstolabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        panel.add(referstofield, gbc);
        gbc.gridwidth = 1;

        // row 3
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(getbutton, gbc);

        gbc.gridx = 2;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(getpinsbutton, gbc);

        gbc.gridwidth = 1;

        return panel;
    }

    private JPanel buildpinpanel()
    {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "pin / unpin",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel xlabel = new JLabel("x:");
        pinxfield = new JTextField(6);

        JLabel ylabel = new JLabel("y:");
        pinyfield = new JTextField(6);

        pinbutton = new JButton("pin");
        unpinbutton = new JButton("unpin");

        // row 0
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(xlabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        panel.add(pinxfield, gbc);

        gbc.gridx = 2;
        gbc.gridy = 0;
        panel.add(ylabel, gbc);

        gbc.gridx = 3;
        gbc.gridy = 0;
        panel.add(pinyfield, gbc);

        // row 1
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        panel.add(pinbutton, gbc);

        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        panel.add(unpinbutton, gbc);

        gbc.gridwidth = 1;

        return panel;
    }

    private JPanel buildadminpanel()
    {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "shake / clear",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        shakebutton = new JButton("shake");
        clearbutton = new JButton("clear");

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(shakebutton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        panel.add(clearbutton, gbc);

        gbc.gridwidth = 1;

        return panel;
    }

    private JPanel buildoutputpanel()
    {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "output",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));

        outputarea = new JTextArea(10, 60);
        outputarea.setEditable(false);
        outputarea.setLineWrap(true);
        outputarea.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(outputarea);
        panel.add(scroll, BorderLayout.CENTER);

        clearoutputbutton = new JButton("clear output");
        panel.add(clearoutputbutton, BorderLayout.SOUTH);

        return panel;
    }

    private void doconnect()
    {
        String hosttext = hostfield.getText().trim();
        String porttext = portfield.getText().trim();

        if (hosttext.length() == 0)
        {
            JOptionPane.showMessageDialog(this, "host is required");
            return;
        }

        int port;
        try
        {
            port = Integer.parseInt(porttext);
        }
        catch (Exception ex)
        {
            JOptionPane.showMessageDialog(this, "port must be a number");
            return;
        }

        if (port < 1 || port > 65535)
        {
            JOptionPane.showMessageDialog(this, "port must be 1 to 65535");
            return;
        }

        // reset handshake info for new connection
        helloreceived = false;
        boardwidth = -1;
        boardheight = -1;
        notewidth = -1;
        noteheight = -1;
        validcolors = null;

        // disable connect right away so user cant spam it
        connectbutton.setEnabled(false);
        statuslabel.setText("status: connecting...");

        try
        {
            clientconnection = new ClientConnection(new ClientConnection.ClientConnectionListener()
            {
                @Override
                public void online(String line)
                {
                    SwingUtilities.invokeLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            handlelinefromserver(line);
                        }
                    });
                }

                @Override
                public void ondisconnect(String reason)
                {
                    SwingUtilities.invokeLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            handledisconnect(reason);
                        }
                    });
                }
            });

            clientconnection.connect(hosttext, port);

            // connected at socket level
            setconnectedstate(true);
            statuslabel.setText("status: connected (waiting for hello)");
            serverinfolabel.setText("server: " + hosttext + ":" + port);
            appendoutputline("(client) connected to " + hosttext + ":" + port);
        }
        catch (Exception ex)
        {
            // connection failed, go back to disconnected state
            clientconnection = null;
            setconnectedstate(false);
            JOptionPane.showMessageDialog(this, "connect failed: " + ex.getMessage());
        }
    }

    private void dodisconnect()
    {
        // if not connected, just ignore
        if (clientconnection == null || !clientconnection.isconnected())
        {
            setconnectedstate(false);
            return;
        }

        try
        {
            // ask server to disconnect nicely
            appendoutputline("> DISCONNECT");
            clientconnection.sendline("DISCONNECT");
        }
        catch (Exception ex)
        {
            // ignore, disconnecting anyway
        }

        clientconnection.disconnect();
    }

    // post

    private void dopost()
    {
        if (!ensureconnected())
        {
            return;
        }

        Integer x = parserequirednonnegativeint(postxfield, "post x");
        if (x == null)
        {
            return;
        }

        Integer y = parserequirednonnegativeint(postyfield, "post y");
        if (y == null)
        {
            return;
        }

        String color = parsecolorselection(postcolorbox);
        if (color == null)
        {
            return;
        }

        String message = parserequiredsinglelinetext(postmessagefield, "message");
        if (message == null)
        {
            return;
        }

        if (!validatepostfits(x, y))
        {
            return;
        }

        String command = "POST " + x + " " + y + " " + color + " " + message;

        appendoutputline("> " + command);

        try
        {
            clientconnection.sendline(command);
        }
        catch (Exception ex)
        {
            JOptionPane.showMessageDialog(this, "send failed: " + ex.getMessage());
        }
    }

    // get and get pins

    private void doget()
    {
        if (!ensureconnected())
        {
            return;
        }

        if (!validatecontainspair())
        {
            return;
        }

        // build: GET [color=...] [contains=x y] [refersTo=...]
        StringBuilder commandbuilder = new StringBuilder("GET");

        // color is optional, skip if any or placeholder
        Object selected = getcolorbox.getSelectedItem();
        if (selected != null)
        {
            String color = selected.toString().trim();

            if (!color.equals("(any)") && !color.startsWith("(") && color.length() > 0)
            {
                commandbuilder.append(" color=").append(color);
            }
        }

        // contains is optional
        String cxtext = containsxfield.getText().trim();
        String cytext = containsyfield.getText().trim();

        if (cxtext.length() > 0 && cytext.length() > 0)
        {
            Integer cx = parseoptionalnonnegativeint(containsxfield, "contains x");
            if (cx == null)
            {
                return;
            }

            Integer cy = parseoptionalnonnegativeint(containsyfield, "contains y");
            if (cy == null)
            {
                return;
            }

            commandbuilder.append(" contains=").append(cx).append(" ").append(cy);
        }

        // refersto is optional
        String refersto = parseoptionalsinglelinetext(referstofield, "refersto");
        if (refersto == null)
        {
            return;
        }

        if (refersto.length() > 0)
        {
            commandbuilder.append(" refersTo=").append(refersto);
        }

        String command = commandbuilder.toString();

        appendoutputline("> " + command);

        try
        {
            clientconnection.sendline(command);
        }
        catch (Exception ex)
        {
            JOptionPane.showMessageDialog(this, "send failed: " + ex.getMessage());
        }
    }

    private void dogetpins()
    {
        if (!ensureconnected())
        {
            return;
        }

        String command = "GET PINS";

        appendoutputline("> " + command);

        try
        {
            clientconnection.sendline(command);
        }
        catch (Exception ex)
        {
            JOptionPane.showMessageDialog(this, "send failed: " + ex.getMessage());
        }
    }

    // pin and unpin

    private void dopin(boolean ispin)
    {
        if (!ensureconnected())
        {
            return;
        }

        Integer x = parserequirednonnegativeint(pinxfield, "x");
        if (x == null)
        {
            return;
        }

        Integer y = parserequirednonnegativeint(pinyfield, "y");
        if (y == null)
        {
            return;
        }

        // optional bounds check if we know the board size
        if (boardwidth > -1 && boardheight > -1)
        {
            if (x >= boardwidth || y >= boardheight)
            {
                JOptionPane.showMessageDialog(this, "point is outside the board");
                return;
            }
        }

        String command;

        if (ispin)
        {
            command = "PIN " + x + " " + y;
        }
        else
        {
            command = "UNPIN " + x + " " + y;
        }

        appendoutputline("> " + command);

        try
        {
            clientconnection.sendline(command);
        }
        catch (Exception ex)
        {
            JOptionPane.showMessageDialog(this, "send failed: " + ex.getMessage());
        }
    }

    // shake and clear

    private void doshake()
    {
        if (!ensureconnected())
        {
            return;
        }

        String command = "SHAKE";

        appendoutputline("> " + command);

        try
        {
            clientconnection.sendline(command);
        }
        catch (Exception ex)
        {
            JOptionPane.showMessageDialog(this, "send failed: " + ex.getMessage());
        }
    }

    private void doclear()
    {
        if (!ensureconnected())
        {
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "are you sure you want to clear the board?",
                "confirm clear",
                JOptionPane.YES_NO_OPTION
        );

        if (choice != JOptionPane.YES_OPTION)
        {
            return;
        }

        String command = "CLEAR";

        appendoutputline("> " + command);

        try
        {
            clientconnection.sendline(command);
        }
        catch (Exception ex)
        {
            JOptionPane.showMessageDialog(this, "send failed: " + ex.getMessage());
        }
    }

    private void handlelinefromserver(String line)
    {
        // print exactly as received
        appendoutputline(line);

        // try to parse hello once (only for colors + info labels)
        if (!helloreceived)
        {
            if (tryparsehello(line))
            {
                helloreceived = true;
                statuslabel.setText("status: connected");
            }
        }
    }

    private void handledisconnect(String reason)
    {
        appendoutputline("(client) disconnected: " + reason);

        clientconnection = null;
        setconnectedstate(false);

        // reset handshake info
        helloreceived = false;
        boardwidth = -1;
        boardheight = -1;
        notewidth = -1;
        noteheight = -1;
        validcolors = null;
    }

    private boolean tryparsehello(String line)
    {
        // expected: HELLO NBB/1.0 <boardw> <boardh> <notew> <noteh> <color1> ...
        if (line == null)
        {
            return false;
        }

        String trimmed = line.trim();
        if (!trimmed.startsWith("HELLO "))
        {
            return false;
        }

        try
        {
            String[] parts = trimmed.split("\\s+");

            // 0=HELLO, 1=version, 2=boardw, 3=boardh, 4=notew, 5=noteh
            if (parts.length < 6)
            {
                return false;
            }

            boardwidth = Integer.parseInt(parts[2]);
            boardheight = Integer.parseInt(parts[3]);
            notewidth = Integer.parseInt(parts[4]);
            noteheight = Integer.parseInt(parts[5]);

            // colors start at index 6
            if (parts.length > 6)
            {
                validcolors = new String[parts.length - 6];
                for (int i = 6; i < parts.length; i++)
                {
                    validcolors[i - 6] = parts[i];
                }
            }
            else
            {
                validcolors = new String[0];
            }

            // update labels and dropdowns
            serverinfolabel.setText("server: " + boardwidth + "x" + boardheight + " note:" + notewidth + "x" + noteheight);
            updatecolorboxes();

            return true;
        }
        catch (Exception ex)
        {
            // if hello is malformed, dont crash client
            return false;
        }
    }

    private void updatecolorboxes()
    {
        // update post colors
        postcolorbox.removeAllItems();

        // update get colors (keep any option)
        getcolorbox.removeAllItems();
        getcolorbox.addItem("(any)");

        if (validcolors == null || validcolors.length == 0)
        {
            postcolorbox.addItem("(no colors)");
            getcolorbox.addItem("(no colors)");
            return;
        }

        for (String c : validcolors)
        {
            postcolorbox.addItem(c);
            getcolorbox.addItem(c);
        }
    }

    // adds a line to output and scrolls down
    private void appendoutputline(String line)
    {
        outputarea.append(line + "\n");
        outputarea.setCaretPosition(outputarea.getDocument().getLength());
    }

    // validation helpers

    private boolean ensureconnected()
    {
        if (clientconnection == null || !clientconnection.isconnected())
        {
            JOptionPane.showMessageDialog(this, "not connected");
            return false;
        }

        return true;
    }

    private Integer parseoptionalnonnegativeint(JTextField field, String fieldname)
    {
        String text = field.getText().trim();

        // optional means blank is ok
        if (text.length() == 0)
        {
            return null;
        }

        int value;
        try
        {
            value = Integer.parseInt(text);
        }
        catch (Exception ex)
        {
            JOptionPane.showMessageDialog(this, fieldname + " must be a number");
            field.requestFocusInWindow();
            return null;
        }

        if (value < 0)
        {
            JOptionPane.showMessageDialog(this, fieldname + " must be 0 or more");
            field.requestFocusInWindow();
            return null;
        }

        return value;
    }

    private Integer parserequirednonnegativeint(JTextField field, String fieldname)
    {
        String text = field.getText().trim();

        if (text.length() == 0)
        {
            JOptionPane.showMessageDialog(this, fieldname + " is required");
            field.requestFocusInWindow();
            return null;
        }

        int value;
        try
        {
            value = Integer.parseInt(text);
        }
        catch (Exception ex)
        {
            JOptionPane.showMessageDialog(this, fieldname + " must be a number");
            field.requestFocusInWindow();
            return null;
        }

        if (value < 0)
        {
            JOptionPane.showMessageDialog(this, fieldname + " must be 0 or more");
            field.requestFocusInWindow();
            return null;
        }

        return value;
    }

    private String parserequiredsinglelinetext(JTextField field, String fieldname)
    {
        String text = field.getText();

        if (text == null)
        {
            text = "";
        }

        text = text.trim();

        if (text.length() == 0)
        {
            JOptionPane.showMessageDialog(this, fieldname + " is required");
            field.requestFocusInWindow();
            return null;
        }

        // no newlines allowed in protocol line
        if (text.contains("\n") || text.contains("\r"))
        {
            JOptionPane.showMessageDialog(this, fieldname + " must be one line");
            field.requestFocusInWindow();
            return null;
        }

        return text;
    }

    private String parseoptionalsinglelinetext(JTextField field, String fieldname)
    {
        String text = field.getText();

        if (text == null)
        {
            text = "";
        }

        text = text.trim();

        if (text.length() == 0)
        {
            return "";
        }

        if (text.contains("\n") || text.contains("\r"))
        {
            JOptionPane.showMessageDialog(this, fieldname + " must be one line");
            field.requestFocusInWindow();
            return null;
        }

        return text;
    }

    private String parsecolorselection(JComboBox<String> box)
    {
        Object selected = box.getSelectedItem();

        if (selected == null)
        {
            JOptionPane.showMessageDialog(this, "color is required");
            return null;
        }

        String color = selected.toString().trim();

        // block placeholder items like (no colors yet)
        if (color.startsWith("("))
        {
            JOptionPane.showMessageDialog(this, "choose a real color");
            return null;
        }

        return color;
    }

    private boolean validatepostfits(int x, int y)
    {
        // if we dont know dims yet, skip this check
        if (boardwidth < 0 || boardheight < 0 || notewidth < 0 || noteheight < 0)
        {
            return true;
        }

        // note must fit entirely on board
        if (x + notewidth > boardwidth || y + noteheight > boardheight)
        {
            JOptionPane.showMessageDialog(this,
                    "post does not fit on board\n" +
                    "board: " + boardwidth + "x" + boardheight + "\n" +
                    "note: " + notewidth + "x" + noteheight);
            return false;
        }

        return true;
    }

    private boolean validatecontainspair()
    {
        String xtext = containsxfield.getText().trim();
        String ytext = containsyfield.getText().trim();

        // both blank means user is not using contains filter
        if (xtext.length() == 0 && ytext.length() == 0)
        {
            return true;
        }

        // one filled and the other blank is not allowed
        if (xtext.length() == 0 || ytext.length() == 0)
        {
            JOptionPane.showMessageDialog(this, "contains requires both x and y");
            return false;
        }

        // now both are present, they must parse and be non-negative
        Integer cx = parseoptionalnonnegativeint(containsxfield, "contains x");
        if (cx == null)
        {
            return false;
        }

        Integer cy = parseoptionalnonnegativeint(containsyfield, "contains y");
        if (cy == null)
        {
            return false;
        }

        // optional: bounds check if we know board size (contains point must be on board)
        if (boardwidth > -1 && boardheight > -1)
        {
            if (cx >= boardwidth || cy >= boardheight)
            {
                JOptionPane.showMessageDialog(this, "contains point is outside the board");
                return false;
            }
        }

        return true;
    }

    // this sets buttons enabled/disabled based on connection
    private void setconnectedstate(boolean isconnected)
    {
        disconnectbutton.setEnabled(isconnected);

        postbutton.setEnabled(isconnected);
        getbutton.setEnabled(isconnected);
        getpinsbutton.setEnabled(isconnected);
        pinbutton.setEnabled(isconnected);
        unpinbutton.setEnabled(isconnected);
        shakebutton.setEnabled(isconnected);
        clearbutton.setEnabled(isconnected);

        // connect should be disabled only when connected
        connectbutton.setEnabled(!isconnected);

        if (!isconnected)
        {
            statuslabel.setText("status: disconnected");
            serverinfolabel.setText("server: (not connected)");
        }
        else
        {
            statuslabel.setText("status: connected");
        }
    }
}
