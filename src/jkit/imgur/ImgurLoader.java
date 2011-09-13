/**
 * 
 */
package jkit.imgur;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Joschi <josua.krause@googlemail.com>
 * 
 */
public class ImgurLoader {

	private static final SAXParserFactory FACTORY = SAXParserFactory
			.newInstance();

	private static enum Mode {
		IGNORE, TITLE, HASH, EXT, WIDTH, HEIGHT;
	}

	private static final String ITEM = "item";

	private static final String TITLE = "title";

	private static final String HASH = "hash";

	private static final String EXT = "ext";

	private static final String WIDTH = "width";

	private static final String HEIGHT = "height";

	private static final Map<String, Mode> TAGS = new HashMap<String, Mode>();

	static {
		TAGS.put(ITEM, Mode.IGNORE);
		TAGS.put(TITLE, Mode.TITLE);
		TAGS.put(HASH, Mode.HASH);
		TAGS.put(EXT, Mode.EXT);
		TAGS.put(WIDTH, Mode.WIDTH);
		TAGS.put(HEIGHT, Mode.HEIGHT);
	}

	public Iterable<ImgurImage> loadImages(String subPage) throws IOException {
		try {
			final List<ImgurImage> res = new LinkedList<ImgurImage>();
			DefaultHandler handler = new DefaultHandler() {

				private String title;

				private String hash;

				private String ext;

				private int width;

				private int height;

				private Mode mode;

				@Override
				public void startElement(String uri, String localName,
						String name, Attributes attributes) throws SAXException {
					if (ITEM.equals(name)) {
						title = null;
						hash = null;
						ext = null;
						width = -1;
						height = -1;
					}
					if (TAGS.containsKey(name)) {
						mode = TAGS.get(name);
					}
				}

				@Override
				public void characters(char[] ch, int start, int length)
						throws SAXException {
					if (mode == Mode.IGNORE)
						return;
					String str = new String(ch, start, length);
					switch (mode) {
					case TITLE:
						title = str;
						break;
					case EXT:
						ext = str;
						break;
					case HASH:
						hash = str;
						break;
					case WIDTH:
						width = number(str);
						break;
					case HEIGHT:
						height = number(str);
						break;
					case IGNORE:
						// no break
					default:
						throw new InternalError();
					}
				}

				private int number(String str) {
					try {
						return Integer.valueOf(str);
					} catch (NumberFormatException e) {
						return -1;
					}
				}

				@Override
				public void endElement(String uri, String localName, String name)
						throws SAXException {
					if (ITEM.equals(name)) {
						if (title == null || hash == null || ext == null) {
							System.err.println("invalid item in xml");
						} else {
							try {
								res.add(new ImgurImage(title, hash, ext, width,
										height));
							} catch (IOException e) {
								System.err.println("invalid item:");
								e.printStackTrace();
							}
						}
					}
					if (TAGS.containsKey(name)) {
						mode = Mode.IGNORE;
					}
				}

			};
			URL url = new URL("http://imgur.com/" + subPage + ".xml");
			SAXParser parser = FACTORY.newSAXParser();
			parser.parse(new BufferedInputStream(url.openStream()), handler);
			return res;
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
}
