/*
 * TileEdProView.java
 */
package tileedpro;

import java.awt.Dialog.ModalityType;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Point;
import java.beans.PropertyChangeEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import tileedpro.tilemap.OpenTilemapAction;
import tileedpro.tilemap.SaveTilemapAction;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import javax.swing.JComponent;
import tileedpro.tileset.OpenTilesetAction;
import tileedpro.tileset.SaveTilesetAction;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import tileedpro.tilemap.NewTileMapAction;
import tilelib.tilemap.Tilemap;
import tileedpro.tilemap.TilemapPanel;
import tileedpro.tileset.EditMasterTilePanel;
import tileedpro.tileset.NewTilesetFromDirectoryAction;
import tilelib.tilemap.Tilemap.Tile;
import tilelib.tileset.Tileset;
import tileedpro.tileset.TilesetListCellRenderer;
import tilelib.tileset.Tileset.Layer;
import tilelib.util.u;

/**
 * The application's main frame.
 */
public class TileEdProView extends FrameView {

    private Tileset tileset;
    private BackgroundColorDialog backgroundColorDialog;

    public Tileset getTileset() {
        return tileset;
    }

    public void setTileset(Tileset ts) {
        Tileset old = this.tileset;
        this.tileset = ts;
        firePropertyChange("tileset", old, getTileset());
    }

    public TileEdProView(SingleFrameApplication app) {
        super(app);

        initComponents();

        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String) (evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer) (evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });


        this.availableTileList.setCellRenderer(new TilesetListCellRenderer());
        this.availableTileList.setTransferHandler(new TransferHandler("tileset") {

            @Override
            public int getSourceActions(JComponent arg0) {
                return TransferHandler.COPY;
            }

            @Override
            protected Transferable createTransferable(JComponent comp) {
                return new Transferable() {

                    public DataFlavor[] getTransferDataFlavors() {
                        //System.out.println("returning dfs");
                        DataFlavor[] dfs = {Tileset.TILE_DND_FLAVOR};
                        return dfs;
                    }

                    public boolean isDataFlavorSupported(DataFlavor df) {
                        //System.out.println("checking if supported: " + df);
                        return df.match(Tileset.TILE_DND_FLAVOR);
                    }

                    public Object getTransferData(DataFlavor df) throws UnsupportedFlavorException, IOException {
                        if (isDataFlavorSupported(df)) {
                            Tileset.Tile tile = (Tileset.Tile) availableTileList.getSelectedValue();
                            //System.out.println("transferring : " + tile);
                            return tile;
                        }
                        return null;
                    }
                };
            }
        });
        
        
        tilemapPanel.addPropertyChangeListener("dirty",new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                System.out.println("dirty = : " + tilemapPanel.isDirty());
                getFrame().getRootPane().putClientProperty("Window.documentModified", Boolean.valueOf(tilemapPanel.isDirty()));
            }
        });
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = TileEdProApp.getApplication().getMainFrame();
            aboutBox = new TileEdProAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        TileEdProApp.getApplication().show(aboutBox);
    }

    TilemapPanel getTilemapPanel() {
        return this.tilemapPanel;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        mainPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        tilemapPanel = new tileedpro.tilemap.TilemapPanel();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        availableTileList = new javax.swing.JList();
        jPanel4 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jButton8 = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jButton5 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jCheckBox1 = new javax.swing.JCheckBox();
        jLabel11 = new javax.swing.JLabel();
        jCheckBox2 = new javax.swing.JCheckBox();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenuItem7 = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem6 = new javax.swing.JMenuItem();
        jMenuItem5 = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem14 = new javax.swing.JMenuItem();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem9 = new javax.swing.JMenuItem();
        jMenuItem12 = new javax.swing.JMenuItem();
        jMenuItem11 = new javax.swing.JMenuItem();
        jMenuItem8 = new javax.swing.JMenuItem();
        jMenuItem13 = new javax.swing.JMenuItem();
        jMenuItem10 = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();

        mainPanel.setName("mainPanel"); // NOI18N

        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.PAGE_AXIS));

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(tileedpro.TileEdProApp.class).getContext().getActionMap(TileEdProView.class, this);
        jButton1.setAction(actionMap.get("flipHorz")); // NOI18N
        jButton1.setName("jButton1"); // NOI18N
        jPanel1.add(jButton1);

        jButton2.setAction(actionMap.get("flipVert")); // NOI18N
        jButton2.setName("jButton2"); // NOI18N
        jPanel1.add(jButton2);

        jButton3.setAction(actionMap.get("deleteTile")); // NOI18N
        jButton3.setName("jButton3"); // NOI18N
        jPanel1.add(jButton3);

        jButton6.setAction(actionMap.get("startBrushMode")); // NOI18N
        jButton6.setName("jButton6"); // NOI18N
        jPanel1.add(jButton6);

        jButton7.setAction(actionMap.get("endBrushMode")); // NOI18N
        jButton7.setName("jButton7"); // NOI18N
        jPanel1.add(jButton7);

        jSplitPane1.setBorder(null);
        jSplitPane1.setDividerLocation(300);
        jSplitPane1.setResizeWeight(1.0);
        jSplitPane1.setContinuousLayout(true);
        jSplitPane1.setName("jSplitPane1"); // NOI18N

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        tilemapPanel.setName("tilemapPanel"); // NOI18N

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${tileset.tileHeight}"), tilemapPanel, org.jdesktop.beansbinding.BeanProperty.create("tileHeight"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${tileset}"), tilemapPanel, org.jdesktop.beansbinding.BeanProperty.create("tileset"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${tileset.tileWidth}"), tilemapPanel, org.jdesktop.beansbinding.BeanProperty.create("tileWidth"));
        bindingGroup.addBinding(binding);

        javax.swing.GroupLayout tilemapPanelLayout = new javax.swing.GroupLayout(tilemapPanel);
        tilemapPanel.setLayout(tilemapPanelLayout);
        tilemapPanelLayout.setHorizontalGroup(
            tilemapPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 438, Short.MAX_VALUE)
        );
        tilemapPanelLayout.setVerticalGroup(
            tilemapPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 416, Short.MAX_VALUE)
        );

        jScrollPane2.setViewportView(tilemapPanel);

        jSplitPane1.setLeftComponent(jScrollPane2);

        jPanel3.setName("jPanel3"); // NOI18N

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        availableTileList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        availableTileList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        availableTileList.setDragEnabled(true);
        availableTileList.setLayoutOrientation(javax.swing.JList.HORIZONTAL_WRAP);
        availableTileList.setName("availableTileList"); // NOI18N
        availableTileList.setVisibleRowCount(-1);

        org.jdesktop.beansbinding.ELProperty eLProperty = org.jdesktop.beansbinding.ELProperty.create("${tileset.tiles}");
        org.jdesktop.swingbinding.JListBinding jListBinding = org.jdesktop.swingbinding.SwingBindings.createJListBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, this, eLProperty, availableTileList);
        bindingGroup.addBinding(jListBinding);

        jScrollPane1.setViewportView(availableTileList);

        jPanel4.setName("jPanel4"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(tileedpro.TileEdProApp.class).getContext().getResourceMap(TileEdProView.class);
        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N

        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N

        jLabel5.setText(resourceMap.getString("jLabel5.text")); // NOI18N
        jLabel5.setName("jLabel5"); // NOI18N

        jLabel7.setName("jLabel7"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, availableTileList, org.jdesktop.beansbinding.ELProperty.create("${selectedElement.xoff}"), jLabel7, org.jdesktop.beansbinding.BeanProperty.create("text"));
        binding.setSourceNullValue("N/A");
        binding.setSourceUnreadableValue("N/A");
        bindingGroup.addBinding(binding);

        jLabel8.setName("jLabel8"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, availableTileList, org.jdesktop.beansbinding.ELProperty.create("${selectedElement.yoff}"), jLabel8, org.jdesktop.beansbinding.BeanProperty.create("text"));
        binding.setSourceNullValue("N/A");
        binding.setSourceUnreadableValue("N/A");
        bindingGroup.addBinding(binding);

        jLabel9.setText(resourceMap.getString("jLabel9.text")); // NOI18N
        jLabel9.setName("jLabel9"); // NOI18N

        jLabel10.setName("jLabel10"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, availableTileList, org.jdesktop.beansbinding.ELProperty.create("${selectedElement.id}"), jLabel10, org.jdesktop.beansbinding.BeanProperty.create("text"));
        binding.setSourceNullValue("N/A");
        binding.setSourceUnreadableValue("N/A");
        bindingGroup.addBinding(binding);

        jLabel12.setName("jLabel12"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, availableTileList, org.jdesktop.beansbinding.ELProperty.create("${selectedElement.filename}"), jLabel12, org.jdesktop.beansbinding.BeanProperty.create("text"));
        binding.setSourceNullValue("N/A");
        binding.setSourceUnreadableValue("N/A");
        bindingGroup.addBinding(binding);

        jLabel13.setText(resourceMap.getString("jLabel13.text")); // NOI18N
        jLabel13.setName("jLabel13"); // NOI18N

        jLabel14.setText(resourceMap.getString("jLabel14.text")); // NOI18N
        jLabel14.setName("jLabel14"); // NOI18N

        jLabel15.setName("jLabel15"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, availableTileList, org.jdesktop.beansbinding.ELProperty.create("${selectedElement.tileWidth}"), jLabel15, org.jdesktop.beansbinding.BeanProperty.create("text"));
        binding.setSourceNullValue("N/A");
        binding.setSourceUnreadableValue("N/A");
        bindingGroup.addBinding(binding);

        jLabel16.setName("jLabel16"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, availableTileList, org.jdesktop.beansbinding.ELProperty.create("${selectedElement.tileHeight}"), jLabel16, org.jdesktop.beansbinding.BeanProperty.create("text"));
        binding.setSourceNullValue("N/A");
        binding.setSourceUnreadableValue("N/A");
        bindingGroup.addBinding(binding);

        jButton8.setAction(actionMap.get("editMasterTile")); // NOI18N
        jButton8.setName("jButton8"); // NOI18N

        jLabel6.setText(resourceMap.getString("jLabel6.text")); // NOI18N
        jLabel6.setName("jLabel6"); // NOI18N

        jLabel17.setName("jLabel17"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, availableTileList, org.jdesktop.beansbinding.ELProperty.create("${selectedElement.layer.name}"), jLabel17, org.jdesktop.beansbinding.BeanProperty.create("text"));
        binding.setSourceNullValue("N/A");
        binding.setSourceUnreadableValue("N/A");
        bindingGroup.addBinding(binding);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel14)
                    .addComponent(jLabel13)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4)
                    .addComponent(jLabel6)
                    .addComponent(jLabel5)
                    .addComponent(jLabel9))
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel17)
                    .addComponent(jLabel8)
                    .addComponent(jLabel15)
                    .addComponent(jLabel16)
                    .addComponent(jLabel7)
                    .addComponent(jLabel10)
                    .addComponent(jLabel12)))
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jButton8))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jLabel12))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addGap(40, 40, 40)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(jLabel7))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4)
                            .addComponent(jLabel8)))
                    .addComponent(jLabel10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel15)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel16))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel13)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel14)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jLabel17))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton8))
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 167, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jSplitPane1.setRightComponent(jPanel3);

        jLabel1.setName("jLabel1"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${tileset.name}"), jLabel1, org.jdesktop.beansbinding.BeanProperty.create("text"));
        binding.setSourceNullValue("no tileset selected");
        bindingGroup.addBinding(binding);

        jPanel2.setName("jPanel2"); // NOI18N

        jButton5.setAction(actionMap.get("zoomOut")); // NOI18N
        jButton5.setText(resourceMap.getString("jButton5.text")); // NOI18N
        jButton5.setMaximumSize(new java.awt.Dimension(30, 30));
        jButton5.setMinimumSize(new java.awt.Dimension(30, 30));
        jButton5.setName("jButton5"); // NOI18N
        jButton5.setPreferredSize(new java.awt.Dimension(30, 30));
        jButton5.putClientProperty("JButton.buttonType", "square");

        jButton4.setAction(actionMap.get("zoomIn")); // NOI18N
        jButton4.setText(resourceMap.getString("jButton4.text")); // NOI18N
        jButton4.setMaximumSize(new java.awt.Dimension(30, 30));
        jButton4.setMinimumSize(new java.awt.Dimension(30, 30));
        jButton4.setName("jButton4"); // NOI18N
        jButton4.setPreferredSize(new java.awt.Dimension(30, 30));
        jButton4.putClientProperty("JButton.buttonType", "square");

        jCheckBox1.setText(resourceMap.getString("jCheckBox1.text")); // NOI18N
        jCheckBox1.setName("jCheckBox1"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, tilemapPanel, org.jdesktop.beansbinding.ELProperty.create("${gridVisible}"), jCheckBox1, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);

        jLabel11.setName("jLabel11"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, tilemapPanel, org.jdesktop.beansbinding.ELProperty.create("Brush Mode: ${brushMode}"), jLabel11, org.jdesktop.beansbinding.BeanProperty.create("text"));
        binding.setSourceNullValue("???");
        binding.setSourceUnreadableValue("???");
        bindingGroup.addBinding(binding);

        jCheckBox2.setText(resourceMap.getString("jCheckBox2.text")); // NOI18N
        jCheckBox2.setName("jCheckBox2"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, tilemapPanel, org.jdesktop.beansbinding.ELProperty.create("${coordsVisible}"), jCheckBox2, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel11, javax.swing.GroupLayout.DEFAULT_SIZE, 145, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jCheckBox2, javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(jCheckBox1))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel11)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 92, Short.MAX_VALUE)
                .addComponent(jCheckBox1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBox2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 169, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 643, Short.MAX_VALUE)))
        );

        mainPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jPanel1, jPanel2});

        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 402, Short.MAX_VALUE))
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 203, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        jMenuItem2.setAction(actionMap.get("newTilemap")); // NOI18N
        jMenuItem2.setName("jMenuItem2"); // NOI18N
        fileMenu.add(jMenuItem2);

        jMenuItem4.setAction(actionMap.get("openTilemap")); // NOI18N
        jMenuItem4.setName("jMenuItem4"); // NOI18N
        fileMenu.add(jMenuItem4);

        jMenuItem3.setAction(actionMap.get("saveTilemap")); // NOI18N
        jMenuItem3.setName("jMenuItem3"); // NOI18N
        fileMenu.add(jMenuItem3);

        jMenuItem7.setAction(actionMap.get("saveTilemapAs")); // NOI18N
        jMenuItem7.setName("jMenuItem7"); // NOI18N
        fileMenu.add(jMenuItem7);

        jSeparator1.setName("jSeparator1"); // NOI18N
        fileMenu.add(jSeparator1);

        jMenuItem1.setAction(actionMap.get("newTilesetFromDirectory")); // NOI18N
        jMenuItem1.setName("jMenuItem1"); // NOI18N
        fileMenu.add(jMenuItem1);

        jMenuItem6.setAction(actionMap.get("openTileset")); // NOI18N
        jMenuItem6.setName("jMenuItem6"); // NOI18N
        fileMenu.add(jMenuItem6);

        jMenuItem5.setAction(actionMap.get("saveTilest")); // NOI18N
        jMenuItem5.setName("jMenuItem5"); // NOI18N
        fileMenu.add(jMenuItem5);

        jSeparator2.setName("jSeparator2"); // NOI18N
        fileMenu.add(jSeparator2);

        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        jMenu3.setText(resourceMap.getString("jMenu3.text")); // NOI18N
        jMenu3.setName("jMenu3"); // NOI18N
        menuBar.add(jMenu3);

        jMenu2.setText(resourceMap.getString("jMenu2.text")); // NOI18N
        jMenu2.setName("jMenu2"); // NOI18N

        jMenuItem14.setAction(actionMap.get("moveTileBackward")); // NOI18N
        jMenuItem14.setName("jMenuItem14"); // NOI18N
        jMenu2.add(jMenuItem14);

        menuBar.add(jMenu2);

        jMenu1.setText(resourceMap.getString("jMenu1.text")); // NOI18N
        jMenu1.setName("jMenu1"); // NOI18N

        jMenuItem9.setAction(actionMap.get("removeDuplicates")); // NOI18N
        jMenuItem9.setName("jMenuItem9"); // NOI18N
        jMenu1.add(jMenuItem9);

        jMenuItem12.setAction(actionMap.get("reloadTilesetImages")); // NOI18N
        jMenuItem12.setName("jMenuItem12"); // NOI18N
        jMenu1.add(jMenuItem12);

        jMenuItem11.setAction(actionMap.get("setBackgroundColor")); // NOI18N
        jMenuItem11.setName("jMenuItem11"); // NOI18N
        jMenu1.add(jMenuItem11);

        jMenuItem8.setAction(actionMap.get("normalizeCoords")); // NOI18N
        jMenuItem8.setName("jMenuItem8"); // NOI18N
        jMenu1.add(jMenuItem8);

        jMenuItem13.setAction(actionMap.get("resortByLocation")); // NOI18N
        jMenuItem13.setName("jMenuItem13"); // NOI18N
        jMenu1.add(jMenuItem13);

        jMenuItem10.setAction(actionMap.get("exportAsImage")); // NOI18N
        jMenuItem10.setName("jMenuItem10"); // NOI18N
        jMenu1.add(jMenuItem10);

        menuBar.add(jMenu1);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 812, Short.MAX_VALUE)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 648, Short.MAX_VALUE)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusAnimationLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusMessageLabel)
                    .addComponent(statusAnimationLabel)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(3, 3, 3))
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);

        bindingGroup.bind();
    }// </editor-fold>//GEN-END:initComponents

    @Action
    public void flipHorz() {
        if(tilemapPanel.isTileSelected()) {
            Tilemap.Tile tile = tilemapPanel.getSelectedTile();
            tile.setFlippedHorz(!tile.isFlippedHorz());
            tilemapPanel.repaint();
        }
    }

    @Action
    public void flipVert() {
        if(tilemapPanel.isTileSelected()) {
            Tilemap.Tile  tile = tilemapPanel.getSelectedTile();
            tile.setFlippedVert(!tile.isFlippedVert());
            tilemapPanel.repaint();
        }
    }

    @Action
    public void newTilesetFromDirectory() {
        NewTilesetFromDirectoryAction action = new NewTilesetFromDirectoryAction();
        setTileset(action.execute(this.getComponent()));
    }

    @Action
    public void newTilemap() {
        NewTileMapAction action = new NewTileMapAction();
        ApplicationStateModel model = TileEdProApp.getApplication().getModel();
        action.setModel(model);
        Tilemap map = action.createNewTilemap(getFrame());
        this.setTilemap(map);
    }

    @Action
    public void saveTilemap() {
        SaveTilemapAction action = new SaveTilemapAction();
        if(action.save(this.getComponent(),tilemapPanel.getTilemap())) {
            tilemapPanel.setDirty(false);
        }
    }


    @Action
    public void saveTilemapAs() {
        SaveTilemapAction action = new SaveTilemapAction();
        action.save(this.getComponent(),tilemapPanel.getTilemap(),true);
    }
    
    @Action
    public void openTilemap() {
        OpenTilemapAction action = new OpenTilemapAction();
        Tilemap map = action.open(this.getComponent());
        if(map != null) {
            this.getFrame().setTitle("TileEdPro: " + map.getFilename());
            tilemapPanel.setTilemap(map);
            setTileset(map.getTileset());
        }
    }

    @Action
    public void saveTilest() {
        SaveTilesetAction action = new SaveTilesetAction();
        action.save(this.getComponent(),this.getTileset());
    }

    @Action
    public void openTileset() {
        OpenTilesetAction action = new OpenTilesetAction();
        Tileset tileset = action.open(this.getComponent());
        if(tileset != null) {
            setTileset(tileset);
        }
        
    }

    @Action
    public void deleteTile() {
        if(tilemapPanel.isTileSelected()) {
            tilemapPanel.removeTile(tilemapPanel.getSelectedTile());
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JList availableTileList;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBox jCheckBox2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem10;
    private javax.swing.JMenuItem jMenuItem11;
    private javax.swing.JMenuItem jMenuItem12;
    private javax.swing.JMenuItem jMenuItem13;
    private javax.swing.JMenuItem jMenuItem14;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JMenuItem jMenuItem7;
    private javax.swing.JMenuItem jMenuItem8;
    private javax.swing.JMenuItem jMenuItem9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private tileedpro.tilemap.TilemapPanel tilemapPanel;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables

    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;

    private JDialog aboutBox;
    
    
    public Tilemap getCurrentTilemap() {
        return tilemapPanel.getTilemap();
    }
    
    void setTilemap(Tilemap map) {
        if(map != null) {
            tilemapPanel.setTilemap(map);
            setTileset(map.getTileset());
        }
    }

    @Action
    public void zoomIn() {
        tilemapPanel.setZoom(tilemapPanel.getZoom()+1);
    }

    @Action
    public void zoomOut() {
        tilemapPanel.setZoom(tilemapPanel.getZoom()-1);
    }

    @Action
    public void normalizeCoords() {
        Point min = tilemapPanel.getTilemap().getMinimumCoords();
        u.p("min = " + min);
        for(Tilemap.Tile tile : tilemapPanel.getTilemap().getTiles()) {
            tile.setX(tile.getX() - min.x);
            tile.setY(tile.getY() - min.y);
        }
        tilemapPanel.repaint();
    }

    /** look for any duplicate tiles, meaning two tiles on
     * the same square.
     */
    @Action
    public void removeDuplicates() {
        Tilemap map = tilemapPanel.getTilemap();
        Point min = map.getMinimumCoords();
        Point max = map.getMaximumCoords();
        for(int x = min.x; x<max.x; x++) {
            for(int y = min.y; y<max.y; y++) {
                List<Tilemap.Tile> tiles = map.getTiles(x,y);
                if(tiles != null && tiles.size() >1) {
                    List<Tilemap.Tile> dups = findDups(tiles);
                    for(Tilemap.Tile t : dups) {
                        map.remove(t);
                    }
                }
            }
        }
        tilemapPanel.repaint();
    }
    
    private List<Tilemap.Tile> findDups(List<Tilemap.Tile> tiles) {
        List<Tilemap.Tile> dups = new ArrayList<Tilemap.Tile>();
        for(Tilemap.Tile src : tiles) {
            for(Tilemap.Tile dst : tiles) {
                if(src != dst && src.getImg() == dst.getImg() && !dups.contains(dst) && !dups.contains(src)) {
                    dups.add(dst);
                }
            }
        }
        return dups;
    }

    @Action
    public void exportAsImage() {
        try {
        FileDialog fd = new FileDialog((Frame)null);
        fd.setMode(FileDialog.SAVE);
        fd.setVisible(true);
        if(fd.getFile() != null) {
            File file = new File(fd.getDirectory(),fd.getFile());
            Tilemap map = tilemapPanel.getTilemap();
            Point min = map.getMinimumCoords();
            int w = (map.getGridWidth()+1)*map.getTileset().getTileWidth();
            int h = (map.getGridHeight()+1)*map.getTileset().getTileHeight();
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            for(Tilemap.Tile tile : map.getTiles()) {
                g.drawImage(
                        tile.getImg(), 
                        tile.getX()*map.getTileset().getTileWidth()+tile.getXoff(), 
                        tile.getY()*map.getTileset().getTileHeight()+tile.getYoff(),
                        null);
            }
            
            try {
                if(file.getName().toLowerCase().endsWith("png")) {
                    ImageIO.write(img, "png", file);
                }
                if(file.getName().toLowerCase().endsWith("jpg")) {
                    ImageIO.write(img, "jpg", file);
                }
                if(file.getName().toLowerCase().endsWith("bmp")) {
                    ImageIO.write(img, "bmp", file);
                }
            } catch (IOException ex) {
                Logger.getLogger(TileEdProView.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        } catch (Exception ex) {
            u.p(ex);
        }
    }

    @Action
    public void startBrushMode() {
        if(tilemapPanel.getSelectedTile() != null) {
            tilemapPanel.setBrushMode(true);
            tilemapPanel.setBrush(tilemapPanel.getSelectedTile());
        }
    }

    @Action
    public void endBrushMode() {
        tilemapPanel.setBrushMode(false);
        tilemapPanel.setBrush(null);
    }

    @Action
    public void setBackgroundColor() {
        if (backgroundColorDialog == null) {
            JFrame mainFrame = TileEdProApp.getApplication().getMainFrame();
            backgroundColorDialog = new BackgroundColorDialog(mainFrame,true);
            backgroundColorDialog.setLocationRelativeTo(mainFrame);
            backgroundColorDialog.setTilemapPanel(this.tilemapPanel);
        }
        TileEdProApp.getApplication().show(backgroundColorDialog);
    }

    @Action
    public void reloadTilesetImages() {
        try {
            getTileset().reload();
            tilemapPanel.repaint();
        } catch (Exception ex) {
            Logger.getLogger(TileEdProView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Action
    public void editMasterTile() {
        JDialog dialog = new JDialog(SwingUtilities.windowForComponent(getComponent()),
                "Edit Tile",ModalityType.APPLICATION_MODAL);
        EditMasterTilePanel panel = new EditMasterTilePanel();
        panel.setTile((Tileset.Tile)availableTileList.getSelectedValue());
        dialog.getRootPane().setDefaultButton(panel.okayButton);
        dialog.add(panel);
        dialog.pack();
        dialog.setVisible(true);
    }

    @Action
    public void resortByLocation() {
        Tilemap map = getTilemapPanel().getTilemap();
        List<Layer> layers = map.getOrderedLayers();
        for(Layer l : layers) {
            List<Tilemap.Tile> tiles = new ArrayList<Tilemap.Tile>();
            //create a copy list
            for(Tilemap.Tile t : map.getTilesForLayer(l)) {
                tiles.add(t);
            }
            Collections.sort(tiles, new Comparator<Tilemap.Tile>() {
                public int compare(Tile o1, Tile o2) {
                    if(o1 == o2) return 0;
                    if(o1.getY() < o2.getY()) return -1;
                    if(o1.getY() > o2.getY()) return 1;
                    if(o1.getX() < o2.getX()) return -1;
                    if(o1.getX() > o2.getX()) return 1;
                    return 0;
                }
            });
            for(Tilemap.Tile t : tiles) {
                map.remove(t);
                map.add(t);
            }
        }
    }

    @Action
    public void moveTileBackward() {
        Tilemap.Tile tile = getTilemapPanel().getSelectedTile();
        if(tile == null) return;
        Tilemap map = getTilemapPanel().getTilemap();
        List<Tilemap.Tile> tiles = map.getOverlappingTiles(tile);
        int origLocalIndex = tiles.indexOf(tile);
        if(origLocalIndex == 0) return;
        
        int newLocalIndex = origLocalIndex-1;
        Tilemap.Tile targetTile = tiles.get(newLocalIndex);
        map.remove(tile);
        int newGlobalIndex = map.getIndexOfTile(targetTile);
        map.add(newGlobalIndex,tile);

        
        /*
        
        List<Tilemap.Tile> otherTiles = new ArrayList<Tile>();
        // get all tiles in a local copy
        u.p("before");
        for(Tilemap.Tile t : tiles) {
            otherTiles.add(t); 
            u.p("tile = " + t.hashCode());
        }
        //check if already at the bottom
        if(otherTiles.get(0) == tile) return;
        
        int n = otherTiles.indexOf(tile);
        u.p("moving item at n: " + n);
        if(n == 0) return;
        Collections.rotate(otherTiles.subList(n-1, n+1), -1);
        
        // remove all tiles
        for(Tilemap.Tile t : tiles) {
            map.remove(t);
        }
        
        u.p("after");
        // add them back in the new order
        for(Tilemap.Tile t : otherTiles) {
            u.p("tile = " + t.hashCode());
            map.add(t);
        }
        */
        getTilemapPanel().repaint();
        
    }

}
