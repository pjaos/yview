package yview.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

/**
 * @brief SplashWindow to show a message to the user for a short period of time
 * @author pja
 *
 */
class SplashWindow extends JWindow {
	/**
	 * @brief Constructor
	 * @param f The parent frame
	 * @param text 	The text to appear in the splash window
	 * @param waitTimeMilliSeconds The wait time in Milli Seconds
	 */
	public SplashWindow(Frame f, String text, int waitTimeMilliSeconds) {
		super(f);
		JLabel l = new JLabel(text);
		getContentPane().add(l, BorderLayout.CENTER);
		pack();
		Dimension labelSize = l.getPreferredSize();
		Point frameOriginLocation = f.getLocation();
		setLocation( frameOriginLocation.x+(f.getWidth()/2)-(labelSize.width / 2), frameOriginLocation.y+(f.getHeight()/2)-(labelSize.height / 2) );
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				setVisible(false);
				dispose();
			}
		});
		final int pauseMilliSeconds = waitTimeMilliSeconds;
		final Runnable closerRunner = new Runnable() {
			public void run() {
				setVisible(false);
				dispose();
			}
		};
		Runnable waitRunner = new Runnable() {
			public void run() {
				try {
					Thread.sleep(pauseMilliSeconds);
					SwingUtilities.invokeAndWait(closerRunner);
				} catch (Exception e) {
					e.printStackTrace();
					// can catch InvocationTargetException
					// can catch InterruptedException
				}
			}
		};
		setVisible(true);
		Thread splashThread = new Thread(waitRunner, "SplashThread");
		splashThread.start();
	}
}