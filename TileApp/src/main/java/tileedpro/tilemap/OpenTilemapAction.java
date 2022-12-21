package tileedpro.tilemap;

import tilelib.tilemap.Tilemap;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import tilelib.util.u;

/**
 *
 * @author joshua.marinacci@sun.com
 */
public class OpenTilemapAction {

    public Tilemap open(JComponent component) {
        try {
            FileDialog fd = new FileDialog((Frame)null);
            fd.setMode(FileDialog.LOAD);
            fd.setVisible(true);
            if(fd.getFile() == null) {
                return null;
            }
            File file = new File(fd.getDirectory(),fd.getFile());
            u.p("opening file: " + file.getAbsolutePath());
            return Tilemap.open(file);
        } catch (Exception ex) {
            Logger.getLogger(OpenTilemapAction.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

}
