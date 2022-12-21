package tileedpro;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.EventObject;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.jdesktop.application.Application;
import org.jdesktop.application.LocalStorage;
import org.jdesktop.application.SingleFrameApplication;
import tilelib.tilemap.Tilemap;
import tilelib.tileset.Tileset;

/**
 * The main class of the application.
 */
public class TileEdProApp extends SingleFrameApplication {

    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {

        
        
        final TileEdProView vw = new TileEdProView(this);
        loadPrefs(vw);
        
        this.addExitListener(new ExitListener() {
            public boolean canExit(EventObject arg0) {
                if(vw.getTilemapPanel().isDirty()) {
                    int retval = JOptionPane.showConfirmDialog(vw.getComponent(), "The tilemap has unsaved changes.\n"+
                            " Are you sure you want to quit?", "Unsaved Changes", JOptionPane.OK_CANCEL_OPTION);
                    if(retval == JOptionPane.CANCEL_OPTION) return false;
                }
                return true;
            }

            public void willExit(EventObject arg0) {
                System.out.println("exiting and saving current tileset: " + vw.getTileset().getName());
                if(vw.getTileset() != null) {
                    savePrefs(vw);
                }
            }

        });
        //MacUtil.setupMac(vw);
        show(vw);
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of TileEdProApp
     */
    public static TileEdProApp getApplication() {
        return Application.getInstance(TileEdProApp.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        launch(TileEdProApp.class, args);
    }

    private ApplicationStateModel model = new ApplicationStateModel();
    ApplicationStateModel getModel() {
        return model;
    }

    private void loadPrefs(final TileEdProView view) {

        try {
            LocalStorage storage = getApplication().getContext().getLocalStorage();
            Properties prefs = new Properties();
            prefs.load(storage.openInputFile("main_prefs.properties"));
            
            if (prefs.getProperty("current.tileset") != null) {
                System.out.println("current.tileset = " + prefs.getProperty("current.tileset"));
                File file = new File(new File(new URI(prefs.getProperty("current.tileset"))), "tileset.xml");
                p("file = " + file);
                Tileset ts = Tileset.open(file);
                getModel().addTileset(ts);
                if (ts != null) {
                    System.out.println("set tileset: " + ts);
                    view.setTileset(ts);
                }
            }
            
            
            if(prefs.getProperty("mapview.background") != null) {
                Color color = new Color(Integer.parseInt(prefs.getProperty("mapview.background"),16));
                view.getTilemapPanel().setBackground(color);
            }
            
            String currentTilemap = prefs.getProperty("current.tilemap");
            if(currentTilemap != null) {
                Tilemap map = Tilemap.open(new URI(currentTilemap));
                if(map != null) {
                    view.setTilemap(map);
                    view.getFrame().setTitle("TileEdPro: " + map.getFilename());
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void p(String string) {
        System.out.println(string);
    }
    
    private void savePrefs(final TileEdProView view) {
        OutputStream out = null;
        try {
            LocalStorage storage = getApplication().getContext().getLocalStorage();
            Properties prefs = new Properties();
            String tileset = view.getTileset().getBasedir().toASCIIString();
            System.out.println("tileset = " + tileset);
            prefs.setProperty("current.tileset", tileset);
            Tilemap map = view.getCurrentTilemap();
            if(map != null && map.getSource() != null) {
                prefs.setProperty("current.tilemap", map.getSource().toASCIIString());
            }
            prefs.setProperty("mapview.background", Integer.toHexString(view.getTilemapPanel().getBackground().getRGB()).substring(2));
            out = storage.openOutputFile("main_prefs.properties");
            prefs.store(out, "default properties");
        } catch (IOException ex) {
            Logger.getLogger(TileEdProApp.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        } finally {
            try {
                if(out != null) {
                    out.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(TileEdProApp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    

}
