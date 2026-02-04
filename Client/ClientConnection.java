import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientConnection
{
    // this lets the GUI receive lines and disconnect events
    public interface ClientConnectionListener
    {
        void online(String line);
        void ondisconnect(String reason);
    }

    private ClientConnectionListener listener;

    private Socket socket;
    private BufferedReader inreader;
    private PrintWriter outwriter;

    private Thread readerthread;

    private volatile boolean connectedflag;
    private volatile boolean disconnectnotified;

    public ClientConnection(ClientConnectionListener listener)
    {
        this.listener = listener;
        this.connectedflag = false;
        this.disconnectnotified = false;
    }

    public boolean isconnected()
    {
        return connectedflag;
    }

    public void connect(String host, int port) throws IOException
    {
        // if already connected, do nothing
        if (connectedflag)
        {
            return;
        }

        socket = new Socket(host, port);
        inreader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        outwriter = new PrintWriter(socket.getOutputStream(), true);

        connectedflag = true;

        // start the reader thread
        readerthread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                readerloop();
            }
        });
        readerthread.start();
    }

    private void readerloop()
    {
        try
        {
            String line;

            while (connectedflag && (line = inreader.readLine()) != null)
            {
                // pass the line to GUI exactly as received
                if (listener != null)
                {
                    listener.online(line);
                }
            }

            // if we drop out of loop naturally, server closed connection
            notifydisconnect("server closed connection");
        }
        catch (IOException ex)
        {
            // connection broke or read failed
            notifydisconnect("connection error: " + ex.getMessage());
        }
        catch (Exception ex)
        {
            // safety net, never crash the app
            notifydisconnect("unexpected error: " + ex.getMessage());
        }
        finally
        {
            // cleanup
            closeeverything();
        }
    }

    public synchronized void sendline(String line) throws IOException
    {
        if (!connectedflag || socket == null || socket.isClosed())
        {
            throw new IOException("not connected");
        }

        // send exactly one line
        outwriter.println(line);
        outwriter.flush();
    }

    public void disconnect()
    {
        // if already disconnected, do nothing
        if (!connectedflag)
        {
            return;
        }

        connectedflag = false;

        // try to close, this will cause reader thread to end too
        closeeverything();

        notifydisconnect("disconnected");
    }

    private synchronized void notifydisconnect(String reason)
    {
        if (disconnectnotified)
        {
            return;
        }

        disconnectnotified = true;
        connectedflag = false;

        if (listener != null)
        {
            listener.ondisconnect(reason);
        }
    }

    private void closeeverything()
    {
        try
        {
            if (inreader != null)
            {
                inreader.close();
            }
        }
        catch (Exception ex)
        {
            // ignore
        }

        try
        {
            if (outwriter != null)
            {
                outwriter.close();
            }
        }
        catch (Exception ex)
        {
            // ignore
        }

        try
        {
            if (socket != null && !socket.isClosed())
            {
                socket.close();
            }
        }
        catch (Exception ex)
        {
            // ignore
        }
    }
}
