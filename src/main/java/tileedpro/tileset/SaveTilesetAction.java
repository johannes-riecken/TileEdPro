/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tileedpro.tileset;

import tilelib.tileset.Tileset;
import java.awt.Component;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import tilelib.util.u;

/**
 *
 * @author joshua.marinacci@sun.com
 */
public class SaveTilesetAction {

    public void save(Component comp, Tileset tileset) {
        try {
            System.out.println("basedir = " + tileset.getBasedir());
            File xml = new File(new File(tileset.getBasedir()),"tileset.xml");
            u.p("xml file = " + xml.getAbsolutePath());
            if(!xml.canWrite() && (!xml.getParentFile().exists() || xml.getParentFile().canWrite())) {
                JOptionPane.showMessageDialog(comp, "Couldn't write to:\n" + xml.getAbsolutePath());
                //return;
            }
            
            tileset.save();
            
            
        } catch (Exception ex) {
            Logger.getLogger(SaveTilesetAction.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
    }

}
