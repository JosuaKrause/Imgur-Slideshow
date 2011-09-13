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
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

/**
 * @author Joschi <josua.krause@googlemail.com>
 * 
 */
public class MainWindow extends JFrame {

	private static final long serialVersionUID = 6503228194260524079L;

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

	private boolean exclusiveFullscreen;

	public MainWindow() {
		run = null;
		display = null;
		displayDim = null;
		loader = new ImgurLoader();
		delay = 15000;
		subPage = "r/earthporn";
		setLayout(new BorderLayout());
		setBackground(Color.BLACK);
		view.setPreferredSize(new Dimension(800, 600));
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
					setExtendedState(Frame.MAXIMIZED_BOTH);
				} else {
					setExtendedState(Frame.NORMAL);
				}
				repaint();
			}

		});
		aMap.put("x", new AbstractAction() {

			private static final long serialVersionUID = -2949886933502495995L;

			@Override
			public void actionPerformed(ActionEvent e) {
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
		setLocationRelativeTo(null);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
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
