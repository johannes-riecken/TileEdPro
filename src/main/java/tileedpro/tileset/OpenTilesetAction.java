package tileedpro.tileset;

import tilelib.tileset.Tileset;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 *
 * @author joshua.marinacci@sun.com
 */
public class OpenTilesetAction {

    public Tileset open(JComponent component) {
        try {
            FileDialog fd = new FileDialog((Frame)SwingUtilities.windowForComponent(component));
            
            fd.setMode(FileDialog.LOAD);
            fd.setVisible(true);
            if(fd.getFile() == null) return null;

            return Tileset.open(new File(fd.getDirectory(),fd.getFile()));
        } catch (Exception ex) {
            Logger.getLogger(OpenTilesetAction.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

}
