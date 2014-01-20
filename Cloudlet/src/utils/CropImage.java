package utils;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.image.CropImageFilter;
import java.awt.image.FilteredImageSource;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

public class CropImage extends JFrame {
	Image image;
	Insets insets;

	public CropImage() {
		 super("Crop Image");
		 ImageIcon icon = new ImageIcon("./group.jpg");
		 image = icon.getImage();
		 image = createImage(new FilteredImageSource(image.getSource(),
		 new CropImageFilter(75, 70, 140, 150)));
	}

	public void paint(Graphics g) {
		super.paint(g);
		if (insets == null) {
			insets = getInsets();
		}
		g.drawImage(image, insets.left, insets.top, this);
		
		
	}

	public static void main(String args[]) {
		JFrame frame = new CropImage();
		frame.setSize(250, 250);
		frame.show();
	}

	
}