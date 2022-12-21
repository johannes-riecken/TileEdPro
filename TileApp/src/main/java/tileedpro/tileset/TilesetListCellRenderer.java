/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tileedpro.tileset;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import tilelib.tileset.Tileset;

/**
 *
 * @author joshua.marinacci@sun.com
 */
public class TilesetListCellRenderer extends DefaultListCellRenderer{

    @Override
    public Component getListCellRendererComponent(JList jlist, Object object, int arg2, boolean arg3, boolean arg4) {
        Component comp = super.getListCellRendererComponent(jlist, object, arg2, arg3, arg4);
        Tileset.Tile tile;
        if(comp instanceof JLabel && object instanceof Tileset.Tile) {
            JLabel label = (JLabel) comp;
            label.setText("");
            tile = (Tileset.Tile)object;
            label.setIcon(new ImageIcon(tile.getImage()));
        }
        return comp;
    }

}
