
import javax.swing.SwingUtilities;

public class ClientMain
{
    public static void main(String[] args)
    {
        // start swing
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                ClientGUI clientgui = new ClientGUI();
                clientgui.setVisible(true);
            }
        });
    }
}