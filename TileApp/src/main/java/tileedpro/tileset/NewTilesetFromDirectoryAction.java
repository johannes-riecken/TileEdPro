/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tileedpro.tileset;

import tilelib.tileset.Tileset;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;

/**
 *
 * @author joshua.marinacci@sun.com
 */
public class NewTilesetFromDirectoryAction {
    private File lastDirectory = null;
    public Tileset execute(Component parent) {
        JDialog dialog = new JDialog(SwingUtilities.windowForComponent(parent),"Create New Tileset from Directory",ModalityType.APPLICATION_MODAL);
        NewTilesetFromDirectoryPanel panel = new NewTilesetFromDirectoryPanel();
        dialog.getRootPane().setDefaultButton(panel.okayButton);
        dialog.add(panel);
        dialog.pack();
        dialog.setVisible(true);
        
        if(!panel.isApproved()) {
            return null;
        }
        
        File targetDirectory = new File(panel.getDirectory());
        System.out.println("creating a tileset at: " + panel.getTilesetName() + " " + targetDirectory);
        Tileset set = new Tileset();
        set.setName(panel.getTilesetName());
        set.setBasedir(targetDirectory.toURI());
        set.setTileWidth(panel.getTileWidth());
        set.setTileHeight(panel.getTileHeight());
        int id = 10;
        for(File f : targetDirectory.listFiles()) {
            try {
                System.out.println("getting tile: " + f);
                if(Tileset.isValidTileImage(f.toURL())) {
                    set.addImage(id, targetDirectory.toURL(),f.getName());
                }
            } catch (IOException ex) {
                Logger.getLogger(NewTilesetFromDirectoryAction.class.getName()).log(Level.SEVERE, null, ex);
            }
            id+=10;
        }
        return set;

        
    }

    private void p(String string) {
        System.out.println(string);
    }
}
