/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tileedpro;

import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import tilelib.tilemap.Tilemap;
import tilelib.tileset.Tileset;

/**
 *
 * @author joshua.marinacci@sun.com
 */
public class ApplicationStateModel {
    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private List<Tileset> knownTilesets = new ArrayList<Tileset>();
    private Tilemap currentTilemap;

    public List<Tileset> getKnownTilesets() {
        System.out.println("getting known tilesets");
        for(Tileset t : knownTilesets) {
            System.out.println("tileset: " + t);
        }
        return knownTilesets;
    }

    public void setKnownTilesets(List<Tileset> knownTilesets) {
        List<Tileset> old = getKnownTilesets();
        this.knownTilesets = knownTilesets;
        pcs.firePropertyChange("knownTilesets", old, getKnownTilesets());
    }

    public Tilemap getCurrentTilemap() {
        return currentTilemap;
    }

    public void setCurrentTilemap(Tilemap currentTilemap) {
        Tilemap old = this.getCurrentTilemap();
        this.currentTilemap = currentTilemap;
        pcs.firePropertyChange("currentTilemap", old, this.getCurrentTilemap());
    }

    void addTileset(Tileset ts) {
        knownTilesets.add(ts);
    }
}
