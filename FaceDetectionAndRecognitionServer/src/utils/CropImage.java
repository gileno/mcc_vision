package utils;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;

import javax.swing.JPanel;

public class CropImage extends JPanel {
	Image image;
	Insets insets;

	public void paint(Graphics g) {
		super.paint(g);
		if (insets == null) {
			insets = getInsets();
		}
		g.drawImage(image, insets.left, insets.top, this);
	}
}