package tilelib.tilemap;

import tilelib.tileset.Tileset.Layer;
import tilelib.util.u;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import tilelib.tileset.Tileset;

/**
 *
 * @author joshua.marinacci@sun.com
 */
public class Tilemap {
    private Tileset tileset = null;
    private String filename = "Untitled";
    private boolean fixedGrid;
    private int width;
    private int height;
    
    
    public static class Tile {
        private int x;
        private int y;
        private boolean flippedHorz = false;
        private boolean flippedVert = false;
        
        private Tilemap map;
        private Tileset.Tile masterTile;
        
        private Tile(Tilemap map, Tileset.Tile masterTile) {
            this.map = map;
            this.masterTile = masterTile;
        }

        @Override
        public String toString() {
            return "Tilemap.Tile: x="+getX() + "," + getY() + " " + getTileWidth()+"x"+getTileHeight();
        }

        public Tile copy() {
            Tile t = new Tile(this.map,this.masterTile);
            t.setX(getX());
            t.setY(getY());
            t.setFlippedHorz(isFlippedHorz());
            t.setFlippedVert(isFlippedVert());
            return t;
        }

        public BufferedImage getImg() {
            return masterTile.getImage();
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getXoff() {
            return masterTile.getXoff();
        }

        public int getYoff() {
            return masterTile.getYoff();
        }

        public boolean isFlippedHorz() {
            return flippedHorz;
        }

        public void setFlippedHorz(boolean flippedHorz) {
            this.flippedHorz = flippedHorz;
        }

        public boolean isFlippedVert() {
            return flippedVert;
        }

        public void setFlippedVert(boolean flippedVert) {
            this.flippedVert = flippedVert;
        }

        public BufferedImage getScaledImg(int zoom, boolean flippedHorz, boolean flippedVert) {
            return this.masterTile.getScaledImg(zoom,flippedHorz,flippedVert);
        }

        public int getTileWidth() {
            return this.masterTile.getTileWidth();
        }

        public int getTileHeight() {
            return this.masterTile.getTileHeight();
        }
        
        public Tileset.Tile getMaster() {
            return this.masterTile;
        }
        
        public int getId() {
            return this.masterTile.getId();
        }
        
    }

    
    
    private List<Tile> tiles = new ArrayList<Tile>();

    private URI source;

    public void add(int newGlobalIndex, Tile tile) {
        this.tiles.add(newGlobalIndex, tile);
    }


    public String getFilename() {
        return this.filename;
    }

    public int getIndexOfTile(Tile targetTile) {
        return this.tiles.indexOf(targetTile);
    }

    
    public List<Layer> getOrderedLayers() {
        if(this.tileset == null) {
            return new ArrayList<Layer>();
        }
        List<Layer> layers = this.tileset.getLayers();
        
        Collections.sort(layers, new Comparator<Layer>() {
            @Override
            public int compare(Layer o1, Layer o2) {
                if(o1.getOrder() == o2.getOrder()) {
                    return 0;
                }
                if(o1.getOrder() < o2.getOrder()) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
        
        return layers;
    }

    public List<Tile> getOverlappingTiles(Tile tile) {
        List<Tile> matchingTiles = new ArrayList<Tile>();
        for(Tile t : getTilesForLayer(tile.getMaster().getLayer())) {
            if(t.getX() >= tile.getX() && t.getX() < tile.getX()+tile.getTileWidth()) {
                if(t.getY() >= tile.getY() && t.getY() < tile.getY()+tile.getTileHeight()) {
                    matchingTiles.add(t);
                    continue;
                }                
            }
            if(tile.getX() >= t.getX() && tile.getX() < t.getX()+t.getTileWidth()) {
                if(tile.getY() >= t.getY() && tile.getY() < t.getY()+t.getTileHeight()) {
                    matchingTiles.add(t);
                    continue;
                }                
            }
        }
        return matchingTiles;
    }

    public List<Tile> getTiles(int x, int y, Layer layer) {
        List<Tile> matchingTiles = new ArrayList<Tile>();
        for(Tile t : getTilesForLayer(layer)) {
            if(t.getX() == x && t.getY() == y) {
                matchingTiles.add(t);
            }
        }
        return matchingTiles;
    }

    public Iterable<Tile> getTilesForLayer(Layer l) {
        List<Tile> layerTiles = new ArrayList<Tile>();
        for(Tile t : tiles) {
            if(t.getMaster().getLayer() == l) {
                layerTiles.add(t);
            }
        }
        return layerTiles;
    }
    
    
    public void setFilename(String filename) {
        this.filename = filename;
    }

    public URI getSource() {
        return source;
    }

    public Tile getTile(int x, int y) {
        for(Tile tile : getTiles()) {
            if(tile.getX() == x && tile.getY() == y) {
                return tile;
            }
        }
        return null;
    }

    public List<Tile> getTiles(int x, int y) {
        List<Tile> tiles = new ArrayList<Tile>();
        for(Tile tile : getTiles()) {
            if(tile.getX() == x && tile.getY() == y) {
                tiles.add(tile);
            }
        }
        return tiles;
    }

    public boolean isFixedGrid() {
        return fixedGrid;
    }
    public void setFixedGrid(boolean b) {
        this.fixedGrid = b;
    }


    public void setFixedGridWidth(int width) {
        this.width = width;
    }
    public int getFixedGridWidth() {
        return this.width;
    }
    
    public int getFixedGridHeight() {
        return this.height;
    }
    public void setFixedGridHeight(int height) {
        this.height = height;
    }


    public void setSource(URI file) {
        this.source = file;
    }
    
    public void add(Tile tile) {
        tiles.add(tile);
    }

    public Iterable<Tile> getTiles() {
        return tiles;
    }

    public void remove(Tile tile) {
        tiles.remove(tile);
    }

    public void setTileset(Tileset tileset) {
        this.tileset = tileset;
    }

    public Tileset getTileset() {
        return this.tileset;
    }
    
    
    public Point getMinimumCoords() {
        if(isFixedGrid()) {
            return new Point(0,0);
        }
        /*if(this.tiles.isEmpty()) {
            return new Point(0,0);
        }*/
        int minx = Integer.MAX_VALUE;
        int miny = Integer.MAX_VALUE;
        for(Tilemap.Tile t : getTiles()) {
            if(t.getX() < minx) minx = t.getX();
            if(t.getY() < miny) miny = t.getY();
        }
        
        if(minx == Integer.MAX_VALUE) { minx = 0; }
        if(miny == Integer.MAX_VALUE) { miny = 0; }
        //don't let it get greater than zero
        if(minx > 0) minx = 0;
        if(miny > 0) miny = 0;
        
        return new Point(minx,miny);
    }
    
    public Point getMaximumCoords() {
        /*if(this.tiles.isEmpty()) {
            return new Point(5,5);
        }*/
        int maxx = Integer.MIN_VALUE;
        int maxy = Integer.MIN_VALUE;
        for(Tilemap.Tile t : getTiles()) {
            if(t.getX()+t.getTileWidth() > maxx) maxx = t.getX()+t.getTileWidth()-1;
            if(t.getY()+t.getTileHeight() > maxy) maxy = t.getY()+t.getTileHeight()-1;
        }
        
        if(maxx == Integer.MIN_VALUE) { maxx = 5; }
        if(maxy == Integer.MIN_VALUE) { maxy = 5; }
        Point min = getMinimumCoords();
        u.p("min = " + min);
        u.p("setting to higher: " + getFixedGridWidth());
        u.p("maxx = " + maxx);
        if(maxx <  min.x + getFixedGridWidth() && isFixedGrid()) {
            u.p("setting");
            maxx = min.x + getFixedGridWidth()-1;
        }
        if(maxy <  min.y + getFixedGridHeight() && isFixedGrid()) {
            maxy = min.y + getFixedGridHeight()-1;
        }
        if(!isFixedGrid()) {
            maxx += 2;
        }
        u.p("max = " + maxx + " " + maxy);
        return new Point(maxx,maxy);
    }
    
    /* get the height of the grid in tiles */
    public int getGridHeight() {
        return getMaximumCoords().y + 1;
    }

    /* get the width of the grid in tiles */
    public int getGridWidth() {
        return getMaximumCoords().x + 1;
    }
    
    public boolean canPlayerEnter(int x, int y) {
        Tile t = getTile(x, y);
        if(t == null) return false;
        return !t.getMaster().isBlocking();
    }

    public static Tilemap open(File file) throws Exception {
        return open(file.toURI(),file.getName());
    }

    public static Tilemap open(URI uri) throws Exception {
        return open(uri,"unknown");
    }
    public static Tilemap open(URI uri, String fname) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(uri.toURL().openStream());
        
        Tilemap tilemap = new Tilemap();
        tilemap.setSource(uri);
        tilemap.setFilename(fname);
        Element root = doc.getDocumentElement();
        System.out.println("root = " + root);
        for(int i = 0; i<root.getAttributes().getLength(); i++) {
            System.out.println("attr = " + root.getAttributes().item(i));
        }
        
        u.p("resolving: uri = " + uri);
        URL newURL = new URL(uri.toURL(),"foo.xml");
        u.p("new url = " + newURL);
        String newPart = root.getAttribute("tileset");
        if(!newPart.endsWith("/")) {
            newPart = newPart +"/";
        }
        newPart = newPart+"tileset.xml";
        
        u.p("new part = " + newPart);
        u.p("t1 = " + uri.resolve(""));
        u.p("t2 = " + uri.relativize(new URI("")));
        URI tilesetFile = uri.resolve(newPart);
        tilesetFile = tilesetFile.normalize();
        u.p("resolved tilesetFile = " + tilesetFile);
        
        if(uri.toASCIIString().startsWith("jar:")) {
            tilesetFile = new URL(uri.toURL(),newPart).toURI();
        }
        
        tilesetFile = tilesetFile.normalize();
        System.out.println("loading tileset: " + tilesetFile);
        Tileset tileset = Tileset.open(tilesetFile);
        tilemap.setTileset(tileset);

        String fixedGrid = root.getAttribute("fixedGrid");
        if(fixedGrid != null && !fixedGrid.isEmpty()) {
            tilemap.setFixedGrid(Boolean.parseBoolean(fixedGrid));
            if(tilemap.isFixedGrid()) {
                tilemap.setFixedGridWidth(Integer.parseInt(root.getAttribute("fixedGridWidth")));
                tilemap.setFixedGridHeight(Integer.parseInt(root.getAttribute("fixedGridHeight")));
            }
        }
        NodeList tiles = root.getElementsByTagName("tile");
        for(int i=0; i<tiles.getLength(); i++) {
            Element tileElement = (Element) tiles.item(i);
            
            String filename = tileElement.getAttribute("img");
            BufferedImage img = tileset.getImageForFile(filename);
            Tileset.Tile masterTile = tileset.getTileForImage(img);
            Tilemap.Tile tile = new Tilemap.Tile(tilemap,masterTile);
            
            tile.setX(Integer.parseInt(tileElement.getAttribute("x")));
            tile.setY(Integer.parseInt(tileElement.getAttribute("y")));
            tile.setFlippedHorz(Boolean.parseBoolean(tileElement.getAttribute("flippedhorz")));
            tile.setFlippedVert(Boolean.parseBoolean(tileElement.getAttribute("flippedvert")));
            tilemap.add(tile);
        }

        return tilemap;

    }
    
    public void save() throws ParserConfigurationException, TransformerConfigurationException, TransformerException {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root = doc.createElement("tilemap");
        URI tilesetURI = getTileset().getBasedir();
        URI tilemapURI = this.getSource().resolve(""); //chop off the filename
        URI local = tilesetURI.relativize(tilemapURI);
        tilesetURI = tilemapURI.normalize().relativize(tilesetURI.normalize());
        root.setAttribute("tileset", tilesetURI.toASCIIString());
        root.setAttribute("fixedGrid", ""+this.isFixedGrid());
        if(isFixedGrid()) {
            root.setAttribute("fixedGridWidth", ""+this.getFixedGridWidth());
            root.setAttribute("fixedGridHeight", ""+this.getFixedGridHeight());
        }
        doc.appendChild(root);
        for(Tilemap.Tile tile : getTiles()) {
            Element tileElement = doc.createElement("tile");
            tileElement.setAttribute("x", ""+tile.getX());
            tileElement.setAttribute("y", ""+tile.getY());
            tileElement.setAttribute("flippedhorz", ""+tile.isFlippedHorz());
            tileElement.setAttribute("flippedvert", ""+tile.isFlippedVert());
            tileElement.setAttribute("img",getTileset().getFileForImage(tile.getImg()));
            root.appendChild(tileElement);
        }

        Transformer trans = TransformerFactory.newInstance().newTransformer();
        //trans.transform(new DOMSource(doc), new StreamResult(System.out));
        File file = new File(getSource());
        trans.transform(new DOMSource(doc), new StreamResult(file));
    }

    public Tile createTile(int x, int y, Tileset.Tile ti) {
        Tile tile = new Tile(this,ti);
        tile.setX(x);
        tile.setY(y);
        return tile;
    }

    
    
}
