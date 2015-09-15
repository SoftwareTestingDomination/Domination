package net.yura.swing;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.net.URL;

public class ImageIcon extends javax.swing.ImageIcon {
    
    public ImageIcon (String filename) {
        super(filename);
    }

    public ImageIcon(URL location) {
        super(location);
    }

    public ImageIcon(Image image) {
        super(image);
    }

    @Override
    public int getIconWidth() {
        return GraphicsUtil.scale(super.getIconWidth());
    }
    
    @Override
    public int getIconHeight() {
        return GraphicsUtil.scale(super.getIconHeight());
    }

    @Override
    public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
        g.drawImage(super.getImage(), x, y, getIconWidth(), getIconHeight(), c);
    }

    /**
     * Swing uses this to get the disabled grey icon for disabled buttons. 
     */
    @Override
    public Image getImage() {
        if (getIconWidth() == super.getIconWidth()) {
            return super.getImage();
        }
        return super.getImage().getScaledInstance(getIconWidth(), getIconHeight(), Image.SCALE_SMOOTH);
    }
}
