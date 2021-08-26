package tileedpro.tilemap;

import tilelib.tilemap.Tilemap;
import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author joshua.marinacci@sun.com
 */
public class SaveTilemapAction {
    public boolean save(Component parent, Tilemap tilemap) {
        return save(parent, tilemap, false);
    }
    
    public boolean save(Component parent, Tilemap tilemap, boolean forceSaveAs) {
        if(tilemap.getSource() == null || forceSaveAs) {
            FileDialog fd = new FileDialog((Frame)null);
            fd.setMode(FileDialog.SAVE);
            fd.setVisible(true);
            if(fd.getFile() == null) {
                return false;
            }
            
            tilemap.setSource(new File(fd.getDirectory(),fd.getFile()).toURI());
        }
        
        
        try {
            tilemap.save();
            return true;
        } catch (Exception ex) {
            Logger.getLogger(SaveTilemapAction.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        
    }

}
