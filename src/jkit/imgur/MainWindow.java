/**
 * 
 */
package jkit.imgur;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

import jkit.ini.IniReader;

/**
 * @author Joschi <josua.krause@googlemail.com>
 * 
 */
public class MainWindow extends JFrame {

	private static final long serialVersionUID = 6503228194260524079L;

	public static final IniReader INI = IniReader.createFailProofIniReader(
			new File("iss.ini"), true);

	public static final String SUBPAGE = INI.get("imgur", "subPage",
			"r/earthporn");

	public static final long DELAY = INI.getLong("slideShow", "delay", 60000);

	public static int X = INI.getInteger("window", "left", -1);

	public static int Y = INI.getInteger("window", "top", -1);

	public static int WIDTH = INI.getInteger("window", "width", 800);

	public static int HEIGHT = INI.getInteger("window", "height", 600);

	public static final boolean EXCLUSIVE = INI.getBoolean("window",
			"exclusiveFullScreen");

	public static final boolean FULLSCREEN = INI.getBoolean("window",
			"fullScreen");

	public static void savePosition(MainWindow wnd) {
		Point p = wnd.getLocation();
		X = p.x;
		Y = p.y;
		Dimension dim = wnd.view.getSize();
		WIDTH = dim.width;
		HEIGHT = dim.height;
	}

	private final ImgurLoader loader;

	private final long delay;

	private final String subPage;

	private final JComponent view = new JComponent() {

		private static final long serialVersionUID = -4589272975041099995L;

		private Dimension lastDim;

		private Image last = null;

		private Image scaled = null;

		private double pos;

		private boolean vertical;

		@Override
		protected void paintComponent(Graphics gfx) {
			Graphics2D g = (Graphics2D) gfx.create();
			Dimension own = getSize();
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, own.width + 1, own.height + 1);
			if (display == null) {
				return;
			}
			if (display != last || lastDim != own) {
				Dimension pic = displayDim == null ? new Dimension(display
						.getWidth(this), display.getHeight(this)) : displayDim;
				double verWidth = (double) pic.width * (double) own.height
						/ pic.height;
				vertical = verWidth <= own.width;
				if (vertical) {
					scaled = display.getScaledInstance(-1, own.height,
							Image.SCALE_SMOOTH);
					pos = (own.width - scaled.getWidth(this)) * 0.5;
				} else {
					scaled = display.getScaledInstance(own.width, -1,
							Image.SCALE_SMOOTH);
					pos = (own.height - scaled.getHeight(this)) * 0.5;
				}
				last = display;
				lastDim = own;
			}
			if (vertical) {
				g.translate(pos, 0);
			} else {
				g.translate(0, pos);
			}
			g.drawImage(scaled, 0, 0, this);
			g.dispose();
		}

	};

	private ImgurImage cur;

	private Dimension displayDim;

	private Image display;

	private Thread run;

	private boolean fullScreen;

	private boolean exclusiveFullscreen;

	public MainWindow() {
		run = null;
		display = null;
		displayDim = null;
		loader = new ImgurLoader();
		delay = DELAY;
		subPage = SUBPAGE;
		setLayout(new BorderLayout());
		setBackground(Color.BLACK);
		view.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		add(view, BorderLayout.CENTER);
		pack();
		addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				nextImage();
			}

		});
		ActionMap aMap = new ActionMap();
		aMap.put("next", new AbstractAction() {

			private static final long serialVersionUID = -7372695232568761641L;

			@Override
			public void actionPerformed(ActionEvent e) {
				nextImage();
			}
		});
		aMap.put("quit", new AbstractAction() {

			private static final long serialVersionUID = 2266843640938062469L;

			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
				dispose();
			}

		});
		aMap.put("fs", new AbstractAction() {

			private static final long serialVersionUID = -2109281969355293043L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (exclusiveFullscreen) {
					return;
				}
				if (getExtendedState() != Frame.MAXIMIZED_BOTH) {
					savePosition(MainWindow.this);
					setExtendedState(Frame.MAXIMIZED_BOTH);
					fullScreen = true;
				} else {
					setExtendedState(Frame.NORMAL);
					fullScreen = false;
				}
				repaint();
			}

		});
		aMap.put("x", new AbstractAction() {

			private static final long serialVersionUID = -2949886933502495995L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (!fullScreen) {
					savePosition(MainWindow.this);
				}
				GraphicsEnvironment ge = GraphicsEnvironment
						.getLocalGraphicsEnvironment();
				GraphicsDevice[] gs = ge.getScreenDevices();
				GraphicsDevice gd = gs[0];
				if (!exclusiveFullscreen) {
					setResizable(false);
					gd.setFullScreenWindow(MainWindow.this);
					exclusiveFullscreen = true;
				} else {
					setResizable(true);
					gd.setFullScreenWindow(null);
					exclusiveFullscreen = false;
				}
				repaint();
			}

		});
		InputMap iMap = new InputMap();
		iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), "fs");
		iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, 0), "next");
		iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, 0), "quit");
		iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, 0), "x");
		view.setActionMap(aMap);
		view.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, iMap);
		view.setFocusable(true);
		view.grabFocus();
		setFocusable(true);
		if (X < 0 || Y < 0) {
			setLocationRelativeTo(null);
		} else {
			setLocation(X, Y);
		}
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		if (FULLSCREEN) {
			aMap.get("fs").actionPerformed(null);
		}
		if (EXCLUSIVE) {
			aMap.get("x").actionPerformed(null);
		}
	}

	public void startSlideshow() {
		if (run != null)
			throw new IllegalStateException("already sliding");
		run = new Thread() {

			private Iterator<ImgurImage> images = null;

			@Override
			public void run() {
				while (!isInterrupted()) {
					try {
						if (images == null) {
							images = loader.loadImages(subPage).iterator();
						}
						if (images.hasNext()) {
							if (!exclusiveFullscreen) {
								MainWindow.this
										.setTitle("Loading next image...");
							}
							ImgurImage img = images.next();
							images.remove();
							setCurrent(img);
							if (img.isValidDimension()) {
								displayDim = img.getDimension();
							} else {
								displayDim = null;
							}
							display = img.getImage();
							view.repaint();
							if (!exclusiveFullscreen) {
								MainWindow.this.setTitle(img.getTitle());
							}
							synchronized (this) {
								wait(delay);
							}
						} else {
							images = null;
						}
					} catch (IOException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						interrupt();
					}
				}
			}

		};
		run.start();
	}

	private void setCurrent(ImgurImage img) {
		if (cur != null) {
			cur.dispose();
		}
		cur = img;
	}

	public void nextImage() {
		if (run == null) {
			throw new IllegalStateException("not running");
		}
		synchronized (run) {
			run.notify();
		}
	}

	@Override
	public void dispose() {
		if (!fullScreen && !exclusiveFullscreen) {
			savePosition(this);
		}
		INI.setInteger("window", "left", X);
		INI.setInteger("window", "top", Y);
		INI.setInteger("window", "width", WIDTH);
		INI.setInteger("window", "height", HEIGHT);
		INI.setBoolean("window", "fullScreen", fullScreen);
		INI.setBoolean("window", "exclusiveFullScreen", exclusiveFullscreen);
		try {
			INI.writeIni();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (run != null) {
			run.interrupt();
			run = null;
		}
		super.dispose();
	}

	public static void main(String[] args) {
		MainWindow mw = new MainWindow();
		mw.startSlideshow();
		mw.setVisible(true);
	}

}
