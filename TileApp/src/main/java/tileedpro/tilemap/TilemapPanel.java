package tileedpro.tilemap;

import java.awt.BasicStroke;
import java.beans.PropertyChangeEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.Scrollable;
import javax.swing.TransferHandler;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;

import tilelib.tilemap.Tilemap;
import tilelib.tileset.Tileset;
import tilelib.tilemap.Tilemap.Tile;
import tilelib.tileset.Tileset.Layer;

/**
 *
 * @author joshua.marinacci@sun.com
 */
public class TilemapPanel extends JComponent implements Scrollable {

    private int tileWidth = 128;
    private int tileHeight = 128;
    private Tilemap.Tile dragTile = null;
    private int dragTileCanvasX;
    private int dragTileCanvasY;
    private Tilemap.Tile selectedTile = null;
    private Tilemap tilemap = new Tilemap();
    private int originX;
    private int originY;
    private boolean gridVisible = true;
    private boolean coordsVisible = false;
    private int zoom = 1;
    private boolean brushMode = false;
    private Tilemap.Tile brush;

    public TilemapPanel() {
        this.setTransferHandler(new TilemapTransferHandler("tilemappanel"));
        MouseInputListener mil = new DragListener();
        this.addMouseListener(mil);
        this.addMouseMotionListener(mil);
        this.addKeyListener(new TilemapKeyListener());
    }

    public Tilemap getTilemap() {
        return this.tilemap;
    }

    public int getZoom() {
        return zoom;
    }

    public void setBrush(Tile selectedTile) {
        Tile old = getBrush();
        this.brush = selectedTile;
        firePropertyChange("brush", old, getBrush());
    }

    public void setZoom(int zoom) {
        int old = getZoom();
        this.zoom = zoom;
        firePropertyChange("zoom", old, getZoom());
        recalcPreferredSize();
        repaint();
    }

    public void setTilemap(Tilemap map) {
        this.tilemap = map;
        selectedTile = null;
        dragTile = null;
        recalcPreferredSize();
        repaint();
    }

    private Point convertScreenPointToTile(Point dropPoint) {
        Point pt = new Point();
        double scale = getScale();
        int spx = (int) (dropPoint.x / scale);
        pt.x = spx / getTileWidth() + originX;
        pt.y = (int) (dropPoint.y / scale) / getTileHeight() + originY;
        return pt;
    }

    private void drawTile(Tile t, double scale, Rectangle tbounds, Rectangle clip, Graphics2D g) {
        BufferedImage img = t.getScaledImg(getZoom(), t.isFlippedHorz(), t.isFlippedVert());
        int tx = (int) (t.getX() * getTileWidth() * scale);
        int ty = (int) (t.getY() * getTileHeight() * scale);
        tbounds.x = tx - 10;
        tbounds.y = ty - 10;
        tbounds.width = img.getWidth() + 20;
        tbounds.height = img.getHeight() + 20;
        if (tbounds.intersects(clip) || DRAW_ALL_TILES) {
            g.translate(tx, ty);
            Graphics2D gx = (Graphics2D) g.create();
            gx.translate(t.getXoff() * scale, t.getYoff() * scale);
            gx.drawImage(img, 0, 0, null);
            if (isCoordsVisible()) {
                gx.setFont(gx.getFont().deriveFont((float) 8));
                gx.setPaint(Color.DARK_GRAY);
                gx.translate(2, 10);
                gx.drawString(t.getX() + "," + t.getY(), 1, 1);
                gx.setPaint(Color.WHITE);
                gx.drawString(t.getX() + "," + t.getY(), 0, 0);
            }
            gx.dispose();
            g.translate(-tx, -ty);
        }
    }

    private Tile findTileAt(Point point) {
        int x = point.x;
        int y = point.y;
        List<Tile> tiles = new ArrayList<Tile>();
        for (Tile t : tilemap.getTiles()) {
            tiles.add(t);
        }
        Collections.reverse(tiles);

        for (Tile t : tiles) {
            if(x>=t.getX() && x <t.getX()+t.getTileWidth()) {
                if(y>=t.getY() && y <t.getY()+t.getTileHeight()) {
                    return t;
                }                
            }
        }
        return null;
    }

    public void addTile(Tileset.Tile ti, Point dropPoint) {
        Point tp = convertScreenPointToTile(dropPoint);
        Tile tile = getTilemap().createTile(tp.x,tp.y,ti);
        tilemap.add(tile);
        recalcPreferredSize();
        setDirty(true);
        repaint();
    }

    public void removeTile(Tile selectedTile) {
        if (this.selectedTile == selectedTile) {
            this.selectedTile = null;
        }
        tilemap.remove(selectedTile);
        setDirty(true);
        repaint();
    }
    
    private static final boolean DRAW_GRID_BOUNDARIES = true;
    private static final boolean DRAW_TILES = true;
    private static final boolean DRAW_GRID = true;
    private static final boolean DRAW_SELECTION = true;
    private static final boolean DRAW_ALL_TILES = false;

    @Override
    protected void paintComponent(Graphics gfx) {
        //u.startTimer();
        super.paintComponent(gfx);
        
        
        Graphics2D g = (Graphics2D) gfx.create();
        g.setPaint(getBackground());
        g.fill(gfx.getClipBounds());
        
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        double scale = getScale();
        if (DRAW_TILES) {
            g.translate(-originX * getTileWidth()*scale, -originY * getTileHeight()*scale);

            Rectangle clip = g.getClipBounds();
            Rectangle tbounds = new Rectangle();
            for (Layer l : tilemap.getOrderedLayers()) {
                for(Tile t : tilemap.getTilesForLayer(l)) {
                    drawTile(t, scale, tbounds, clip, g);
                }
            }
            
            g.translate(+originX * getTileWidth()*scale, +originY * getTileHeight()*scale);
        }

        // draw the grid 
        if (DRAW_GRID && isGridVisible()) {
            g.setPaint(new Color(100, 100, 100, 100));
            Rectangle clip = g.getClipBounds();
            int tw = getTileWidth();
            int th = getTileHeight();
            int minx = (clip.x / tw) * tw;
            int maxx = minx + (clip.width / tw) * tw + tw;
            int miny = (clip.y / th) * th;
            int maxy = miny + (clip.height / th) * th + th;
            for (int x = minx; x < maxx; x += (int) (tw * scale)) {
                g.drawLine(x, clip.y-1, x, clip.y + clip.height+1);
            }
            for (int y = miny; y < maxy; y += (int) (th * scale)) {
                g.drawLine(clip.x-1, y, clip.x + clip.width+1, y);
            }
        }

        if(DRAW_GRID_BOUNDARIES) {
            if(tilemap.isFixedGrid()) {
                g.setColor(Color.ORANGE);
                int w = tilemap.getFixedGridWidth();
                int h = tilemap.getFixedGridHeight();
                g.drawRect(0, 0, getTileWidth()*w,getTileHeight()*h);
                g.setStroke(new BasicStroke(1));
            }
        }
        
        if (DRAW_SELECTION) {
            
            if (getSelectedTile() != null) {
                g.setPaint(Color.RED);
                g.setStroke(new BasicStroke(3));
                g.drawRect(
                        (int) ((getSelectedTile().getX() - originX) * getTileWidth() * scale),
                        (int) ((getSelectedTile().getY() - originY) * getTileHeight() * scale),
                        (int) (getSelectedTile().getTileWidth()  * getTileWidth()  * scale),
                        (int) (getSelectedTile().getTileHeight() * getTileHeight() * scale));
                g.setStroke(new BasicStroke(1));
            }

            if (dragTile != null) {
                g.translate(dragTileCanvasX, dragTileCanvasY);
                BufferedImage img = dragTile.getScaledImg(getZoom(), dragTile.isFlippedHorz(), dragTile.isFlippedVert());
                g.drawImage(img, dragTile.getXoff(), dragTile.getYoff(), null);
            }
            
        }
        
        g.dispose();
        //u.stopTimer();
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public void setTileWidth(int tileWidth) {
        if (tileWidth <= 0) {
            return;
        }
        int old = getTileWidth();
        this.tileWidth = tileWidth;
        firePropertyChange("tileWidth", old, getTileWidth());
        repaint();
    }

    public int getTileHeight() {
        return tileHeight;
    }

    public void setTileHeight(int tileHeight) {
        if (tileHeight <= 0) {
            return;
        }
        this.tileHeight = tileHeight;
        int old = getTileHeight();
        firePropertyChange("tileHeight", old, getTileHeight());
        repaint();
    }

    public Tile getSelectedTile() {
        return selectedTile;
    }

    public boolean isTileSelected() {
        if (selectedTile == null) {
            return false;
        }
        return true;
    }

    public void setTileset(Tileset tileset) {
        Tileset old = getTileset();
        this.tilemap.setTileset(tileset);
        if(tileset != null) {
            tileset.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    System.out.println("tileset changed");
                    repaint();
                }
            });
        }
        firePropertyChange("tileset", old, getTileset());
    }

    public Tileset getTileset() {
        return this.tilemap.getTileset();
    }

    public Tile getBrush() {
        return this.brush;
    }

    private double getScale() {
        return Math.pow(2, zoom - 1);
    }

    public void setBrushMode(boolean brushMode) {
        boolean old = isBrushMode();
        this.brushMode = brushMode;
        firePropertyChange("brushMode", old, isBrushMode());
    }

    public boolean isBrushMode() {
        return brushMode;
    }

    private void recalcPreferredSize() {
        Point min = tilemap.getMinimumCoords();
        Point max = tilemap.getMaximumCoords();
        
        int w = max.x - min.x;
        int h = max.y - min.y;
        if (w < 5) {
            w = 5;
        }
        if (h < 5) {
            h = 5;
        }
        originX = min.x;
        originY = min.y;

        double scale = getScale();
        setPreferredSize(new Dimension(
                (int) (w * getTileWidth() * scale),
                (int) (h * getTileHeight() * scale)));
        this.revalidate();
    }

    public boolean isGridVisible() {
        return gridVisible;
    }

    public void setGridVisible(boolean gridVisible) {
        boolean old = isGridVisible();
        this.gridVisible = gridVisible;
        firePropertyChange("gridVisible", old, isGridVisible());
        repaint();
    }

    public boolean isCoordsVisible() {
        return coordsVisible;
    }

    public void setCoordsVisible(boolean coordsVisible) {
        boolean old = isCoordsVisible();
        this.coordsVisible = coordsVisible;
        firePropertyChange("coordsVisible", old, isCoordsVisible());
        repaint();
    }

    private Rectangle getTileBoundsInScreenCoords(Tilemap.Tile tile) {
        Rectangle bounds = new Rectangle();
        bounds.x = (int) ((tile.getX() - originX) * getTileWidth() * getScale() + tile.getXoff() * getScale());
        bounds.y = (int) ((tile.getY() - originY) * getTileHeight() * getScale() + tile.getYoff() * getScale());
        BufferedImage img = tile.getScaledImg(getZoom(), tile.isFlippedHorz(), tile.isFlippedVert());
        bounds.width = (int) (img.getWidth() + 2);
        bounds.height = (int) (img.getHeight() + 2);
        return bounds;
    }
    private class DragListener extends MouseInputAdapter {

        private int xoff;
        private int yoff;
        private boolean doDrag;

        public DragListener() {
        }

        @Override
        public void mousePressed(MouseEvent e) {
            requestFocus();
            Tile tile = findTileAt(convertScreenPointToTile(e.getPoint()));
            if (tile != null) {
                double scale = getScale();
                xoff = e.getX() + (int) ((-tile.getX() * getTileWidth() * scale) +
                        originX * getTileWidth() * scale);
                yoff = e.getY() + (int) (-tile.getY() * getTileHeight() * scale +
                        originY * getTileHeight() * scale);
                if(selectedTile != null) {
                    repaint(getTileBoundsInScreenCoords(selectedTile));
                }
                selectedTile = tile;
                doDrag = true;
                repaint(getTileBoundsInScreenCoords(selectedTile));
            } else {
                doDrag = false;
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if(isBrushMode() && getBrush() != null) {
                Tile tile = findTileAt(convertScreenPointToTile(e.getPoint()));
                if(tile != null) {
                    getTilemap().remove(tile);
                    setDirty(true);
                }
                
                Tileset.Tile tst = getTileset().getTileForImage(getBrush().getImg());
                addTile(tst, e.getPoint());
                return;
            }
            if(doDrag) {
                boolean doCopy = e.isAltDown();
                if (getSelectedTile() != null) {
                    if (doCopy) {
                        dragTile = getSelectedTile().copy();
                        selectedTile = null;
                    } else {
                        tilemap.remove(getSelectedTile());
                        setDirty(true);
                        dragTile = getSelectedTile();
                        selectedTile = null;
                    }
                }
                if (dragTile != null) {
                    Rectangle oldBounds = getTileBoundsInScreenCoords(dragTile);
                    oldBounds.x = dragTileCanvasX-1;
                    oldBounds.y = dragTileCanvasY-1;
                    oldBounds.width+=2;
                    oldBounds.height+=2;
                    //repaint(oldBounds);
                    
                    Point pt = convertScreenPointToTile(e.getPoint());
                    dragTileCanvasX = e.getX() - xoff;
                    dragTileCanvasY = e.getY() - yoff;
                    Rectangle newBounds = getTileBoundsInScreenCoords(dragTile);
                    newBounds.x = dragTileCanvasX-1 + dragTile.getXoff();
                    newBounds.y = dragTileCanvasY-1 + dragTile.getYoff();
                    newBounds.width+=2;
                    newBounds.height+=2;
                    //repaint(newBounds);
                    repaint(oldBounds.union(newBounds));
                }
                return;
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            xoff = -1;
            yoff = -1;
            if (dragTile != null) {
                Point pt = convertScreenPointToTile(e.getPoint());
                dragTile.setX(pt.x);
                dragTile.setY(pt.y);
                tilemap.add(dragTile);
                setDirty(true);
                recalcPreferredSize();
                selectedTile = dragTile;
                dragTile = null;
                repaint();
            }
            doDrag = false;
        }
    }

    public Dimension getPreferredScrollableViewportSize() {
        Dimension dim = new Dimension(
                10 * getTileWidth(),
                10 * getTileHeight());
        return dim;
    }

    public int getScrollableUnitIncrement(Rectangle arg0, int arg1, int arg2) {
        return getTileWidth();
    }

    public int getScrollableBlockIncrement(Rectangle arg0, int arg1, int arg2) {
        return getTileWidth() * 3;
    }

    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
    
    
    private boolean dirty = false;
    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        boolean old = isDirty();
        this.dirty = dirty;
        firePropertyChange("dirty", old, isDirty());
    }

    private class TilemapTransferHandler extends TransferHandler {

        public TilemapTransferHandler(String property) {
            super(property);
        }

        @Override
        public boolean canImport(TransferSupport ts) {
            //System.out.println("ts = " +ts.getDropLocation());
            return true;
        }

        @Override
        public boolean importData(TransferSupport ts) {
            for (DataFlavor df : ts.getTransferable().getTransferDataFlavors()) {
                //System.out.println("flavor = " + df);
            }
            if (ts.isDataFlavorSupported(Tileset.TILE_DND_FLAVOR)) {
                try {
                    Object obj = ts.getTransferable().getTransferData(Tileset.TILE_DND_FLAVOR);
                    //System.out.println("got data: " + obj);
                    Tileset.Tile ti = (Tileset.Tile) obj;
                    addTile(ti, ts.getDropLocation().getDropPoint());
                } catch (Exception ex) {
                    Logger.getLogger(TilemapPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return true;
        }
    }

    private class TilemapKeyListener implements KeyListener {

        public TilemapKeyListener() {
        }

        public void keyTyped(KeyEvent e) {
        }

        public void keyPressed(KeyEvent e) {
        }

        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                if (isTileSelected()) {
                    removeTile(getSelectedTile());
                }
            }
        }
    }
    
}
