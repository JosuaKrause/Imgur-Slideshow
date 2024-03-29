/**
 * 
 */
package jkit.imgur;

import java.awt.Dimension;
import java.awt.Image;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;

/**
 * @author Joschi <josua.krause@googlemail.com>
 * 
 */
public class ImgurImage {

	private final String title;

	private final URL url;

	private final Dimension dim;

	public ImgurImage(final String title, final String hash, final String ext,
			final int width, final int height) throws MalformedURLException {
		this.title = title != null ? title : "Slideshow - Imgur";
		url = new URL("http://i.imgur.com/" + hash + ext);
		dim = new Dimension(width, height);
	}

	public String getTitle() {
		return title;
	}

	public URL getUrl() {
		return url;
	}

	private Image img = null;

	public Image getImage() throws IOException {
		if (img == null) {
			try {
				img = ImageIO.read(url);
			} catch (final OutOfMemoryError e) {
				img = null;
			}
		}
		return img;
	}

	public boolean isValidDimension() {
		return dim.width > 0 && dim.height > 0;
	}

	public Dimension getDimension() {
		return dim;
	}

	public void dispose() {
		if (img != null) {
			img.flush();
			img = null;
		}
	}

}
