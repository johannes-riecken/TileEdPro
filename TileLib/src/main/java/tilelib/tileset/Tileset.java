/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tilelib.tileset;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
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
import org.xml.sax.SAXException;
import tilelib.util.u;

/**
 *
 * @author joshua.marinacci@sun.com
 */
public class Tileset {

    private URI basedir;
    private List<Tile> tiles;
    private String name;
    private int tileWidth;
    private int tileHeight;
    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    public static DataFlavor TILE_DND_FLAVOR = new DataFlavor(Tileset.class,"bob");
    private List<Layer> layers;

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }


    public int getTileWidth() {
        return tileWidth;
    }

    public void setTileWidth(int tileWidth) {
        this.tileWidth = tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }

    public void setTileHeight(int tileHeight) {
        this.tileHeight = tileHeight;
    }

    
    public static class Tile {
        private int id = -1;
        private URI file;
        private BufferedImage image;
        private int xoff = 0;
        private int yoff = 0;
        private String filename;
        private int tileWidth = 1;
        private int tileHeight = 1;
        private boolean blocking;
        private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
        
        private Map<String,BufferedImage> cache = new HashMap<String, BufferedImage>();
        private Layer layer;
        private Tileset tileset;

        public BufferedImage getScaledImg(int zoom, boolean flippedHorz, boolean flippedVert) {
            String key = "img:zoom="+zoom+"_hflip="+flippedHorz+"vflip="+flippedVert;
            if(!cache.containsKey(key)) {
                double scale = Math.pow(2,zoom-1);
                int w = (int) (getImage().getWidth() * scale);
                int h = (int) (getImage().getHeight() * scale);
                BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = img.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.scale(scale, scale);
                    //TODO fix the flipped stuff
                g.translate( w / 2.0,  h / 2.0);
                g.scale(flippedHorz ? -1 : 1, flippedVert ? -1 : 1);
                g.translate(-w / 2.0, -h / 2.0);
                g.drawImage(getImage(),0,0,null);
                g.dispose();
                cache.put(key, img);
            }
            
            return cache.get(key);
        }

        @Override
        public String toString() {
            return "tile " + getId();
        }

        public URI getFile() {
            return file;
        }

        public void setFile(URI file) {
            this.file = file;
        }

        public int getXoff() {
            return xoff;
        }

        public void setXoff(int xoff) {
            int old = getXoff();
            this.xoff = xoff;
            pcs.firePropertyChange("xoff",old,getXoff());
        }

        public int getYoff() {
            return yoff;
        }

        public void setYoff(int yoff) {
            int old = getYoff();
            this.yoff = yoff;
            pcs.firePropertyChange("yoff", old, getYoff());
        }
        
        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public void addPropertyChangeListener(String property, PropertyChangeListener tileListener) {
            this.pcs.addPropertyChangeListener(property, tileListener);
        }

        public void addPropertyChangeListener(PropertyChangeListener tileListener) {
            this.pcs.addPropertyChangeListener(tileListener);
        }

        private void clearCache() {
            this.cache.clear();
        }

        public int getTileWidth() {
            return tileWidth;
        }

        public void setTileWidth(int tileWidth) {
            int old = getTileWidth();
            this.tileWidth = tileWidth;
            pcs.firePropertyChange("tileWidth", old, getTileWidth());
        }

        public int getTileHeight() {
            return tileHeight;
        }

        public void setTileHeight(int tileHeight) {
            int old = getTileHeight();
            this.tileHeight = tileHeight;
            pcs.firePropertyChange("tileHeight", old, getTileHeight());
        }

        public boolean isBlocking() {
            return blocking;
        }

        public void setBlocking(boolean blocking) {
            boolean old = blocking;
            this.blocking = blocking;
            pcs.firePropertyChange("blocking", old, isBlocking());
        }

        public BufferedImage getImage() {
            return image;
        }

        public void setImage(BufferedImage image) {
            this.image = image;
        }
        
        public Layer getLayer() {
            return this.layer;
        }
        
        public Tileset getTileset() {
            return this.tileset;
        }

        private void setLayer(Layer layer) {
            Layer old = getLayer();
            this.layer = layer;
            pcs.firePropertyChange("layer", old, getLayer());
        }
        
    }

    
    public static class Layer {
        private String name;
        private int order;

        public Layer(String name, int order) {
            setName(name);
            this.order = order;
        }

        
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getOrder() {
            return this.order;
        }
    }
    
    public Tileset() {
        tiles = new ArrayList<Tile>();
        layers = new ArrayList<Layer>();
        layers.add(new Layer("misc",1000000));
    }
    
    
    public List<Layer> getLayers() {
        return layers;
    }
    
    private void addLayer(Layer layer) {
        for(Layer l : layers) {
            if(l.getName().equals(layer.getName())) {
                return;
            }
        }
        layers.add(layer);
    }
    
    private Layer getLayer(String name) {
        for(Layer l : layers) {
            if(l.getName().equals(name)) {
                return l;
            }
        }
        return null;
    }
    
    public Tile addImage(int id, URL baseURL, String filename) throws IOException {
        Tile tile = new Tile();
        tile.setLayer(this.getLayers().get(0));
        tile.tileset = this;
        tile.setId(id);
        tile.setFilename(filename);
        tile.setImage(ImageIO.read(new URL(baseURL, filename)));
        this.tiles.add(tile);
        tile.addPropertyChangeListener(tileListener);
        return tile;
    }
    private PropertyChangeListener tileListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            pcs.firePropertyChange("tiles",null,getTiles());
        }
    };
    

    public List<Tileset.Tile> getTiles() {
        return this.tiles;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        String old = this.getName();
        this.name = name;
        pcs.firePropertyChange("name", old, getName());
    }

    public URI getBasedir() {
        return basedir;
    }
    
    public void setBasedir(URI basedir) {
        this.basedir = basedir;
    }
    
    public String getFileForImage(BufferedImage img) {
        for(Tile t : tiles) {
            if(t.getImage() == img) return t.getFilename();
        }
        return null;
    }
    
    public BufferedImage getImageForFile(String filename) {
        for(Tile t : tiles) {
            if(t.getFilename().equals(filename)) {
                return t.getImage();
            }
        }
        return null;
    }
    
    public Tile getTileForFile(String filename) {
        //System.out.println("looking up image for: " + filename);
        for(Tile t : tiles) {
            if(t.getFilename().equals(filename)) {
                return t;
            }
        }
        return null;
    }
    public Tile getTileForImage(BufferedImage img) {
        for(Tile t : tiles) {
            if(t.getImage() == img) {
                return t;
            }
        }
        return null;
    }

    public Tile getTileById(int id) {
        for(Tile t : tiles) {
            if(t.getId() == id) {
                return t;
            }
        }
        return null;
    }
    
    public static Tileset open(File file) throws Exception {
        return open(file.toURI());
    }
    public static Tileset open(URI uri) throws Exception {
        System.out.println("loading: " + uri);
        URL baseURL = uri.toURL();
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(baseURL.openStream());
        Tileset tileset = new Tileset();
        Element root = doc.getDocumentElement();
        tileset.setName(root.getAttribute("name"));
        tileset.setBasedir(uri.resolve(""));
        tileset.setTileWidth(Integer.parseInt(root.getAttribute("tilewidth")));
        tileset.setTileHeight(Integer.parseInt(root.getAttribute("tileheight")));
        
        NodeList layers = root.getElementsByTagName("layer");
        for(int i=0; i<layers.getLength(); i++) {
            Element layer = (Element)layers.item(i);
            String name = layer.getAttribute("name");
            int order = Integer.parseInt(layer.getAttribute("order"));
            tileset.addLayer(new Layer(name, order));
        }
        NodeList tiles = root.getElementsByTagName("tile");
        for(int i=0; i<tiles.getLength(); i++) {
            Element tileElement = (Element) tiles.item(i);
            int id = Integer.parseInt(tileElement.getAttribute("id"));
            String filename = tileElement.getAttribute("src");
            URL srcURL = new URL(baseURL,filename);
            if(isValidTileImage(srcURL)) {
                Tileset.Tile tile = tileset.addImage(id, baseURL, filename);
                loadAttributes(tileElement, tile, tileset);
            }
        }
        return tileset;
    }
    
    public static boolean isValidTileImage(URL f) {
        if(f.toString().toLowerCase().endsWith("png")) {
            return true;
        }
        if(f.toString().toLowerCase().endsWith("bmp")) {
            return true;
        }
        if(f.toString().toLowerCase().endsWith("jpg")) {
            return true;
        }
        if(f.toString().toLowerCase().endsWith("gif")) {
            return true;
        }
        return false;
    }

    
    public void save() throws ParserConfigurationException, TransformerConfigurationException, TransformerException {
        File xml = new File(new File(getBasedir()),"tileset.xml");
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root = doc.createElement("tileset");
        //root.setAttribute("basedir", getBasedir().toASCIIString());//.getPath()+"/"+getBasedir().getPath());
        root.setAttribute("name", getName());
        root.setAttribute("tilewidth", ""+getTileWidth());
        root.setAttribute("tileheight", ""+getTileHeight());
        doc.appendChild(root);
        for(Layer layer : getLayers()) {
            Element layerElement = doc.createElement("layer");
            layerElement.setAttribute("name", layer.getName());
            layerElement.setAttribute("order", ""+layer.getOrder());
            root.appendChild(layerElement);
        }
        for(Tile tile : getTiles()) {
            Element tileElement = doc.createElement("tile");
            saveAttributes(tileElement,tile);
            root.appendChild(tileElement);
        }
        

        Transformer trans = TransformerFactory.newInstance().newTransformer();
        trans.transform(new DOMSource(doc), new StreamResult(System.out));
        trans.transform(new DOMSource(doc), new StreamResult(xml));
    }
    
    public void reload() throws IOException, SAXException, ParserConfigurationException {
        URL xml = new URL(getBasedir().toURL(),"tileset.xml");
        u.p("loading: " + xml);
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml.openStream());
        Element root = doc.getDocumentElement();
        NodeList tileNodes = root.getElementsByTagName("tile");
        for(int i=0; i<tileNodes.getLength(); i++) {
            Element tileElement = (Element) tileNodes.item(i);
            int id = Integer.parseInt(tileElement.getAttribute("id"));
            for(Tile tile : getTiles()) {
                if(tile.getId() == id) {
                    //reload the offsets
                    loadAttributes(tileElement,tile, this);
                    //reload the images
                    tile.clearCache();
                    tile.setImage(ImageIO.read(new URL(getBasedir().toURL(), tile.getFilename())));
                }
            }
        }

    }

    private static void loadAttributes(Element tileElement, Tile tile, Tileset tileset) {
        String xoff = tileElement.getAttribute("xoff");
        if(xoff != null && !xoff.trim().isEmpty()) {
            tile.setXoff(Integer.parseInt(xoff));
        } else {
            tile.setXoff(0);
        }
        String yoff = tileElement.getAttribute("yoff");
        if(yoff != null && !yoff.trim().isEmpty()) {
            tile.setYoff(Integer.parseInt(yoff));
        } else {
            tile.setYoff(0);
        }
        String tileWidth = tileElement.getAttribute("tilewidth");
        if(tileWidth != null && !tileWidth.trim().isEmpty()) {
            tile.setTileWidth(Integer.parseInt(tileWidth));
        }
        String tileHeight = tileElement.getAttribute("tileheight");
        if(tileHeight != null && !tileHeight.trim().isEmpty()) {
            tile.setTileHeight(Integer.parseInt(tileHeight));
        }
        String blocking = tileElement.getAttribute("blocking");
        if(blocking != null && !blocking.trim().isEmpty()) {
            tile.setBlocking(true);
        }
        String layer = tileElement.getAttribute("layer");
        if(layer == null || layer.trim().isEmpty()) {
            tile.setLayer(tileset.getLayer("misc"));
        } else {
            tile.setLayer(tileset.getLayer(layer));
        }
    }

    private void saveAttributes(Element tileElement, Tile tile) {
        tileElement.setAttribute("id", tile.getId()+"");
        tileElement.setAttribute("src", tile.getFilename());
        if(tile.getXoff() != 0) {
            tileElement.setAttribute("xoff", ""+tile.getXoff());
        }
        if(tile.getYoff() != 0) {
            tileElement.setAttribute("yoff", ""+tile.getYoff());
        }
        if(tile.getTileWidth() != 1) {
            tileElement.setAttribute("tilewidth", ""+tile.getTileWidth());
        }
        if(tile.getTileHeight() != 1) {
            tileElement.setAttribute("tileheight", ""+tile.getTileHeight());
        }
        if(tile.isBlocking()) {
            tileElement.setAttribute("blocking", ""+tile.isBlocking());
        }
        tileElement.setAttribute("layer", tile.getLayer().getName());
    }

}
