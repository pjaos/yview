package yview.controller;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.event.*;

import yview.model.Constants;
import yview.view.MainFrame;

import com.wittams.gritty.Questioner;
import com.wittams.gritty.RequestOrigin;
import com.wittams.gritty.ResizePanelDelegate;
import com.wittams.gritty.Tty;
import com.wittams.gritty.swing.BufferPanel;
import com.wittams.gritty.swing.GrittyTerminal;
import com.wittams.gritty.swing.TermPanel;

import pja.io.SimpleConfig;

/**
 * @brief Responsible for displaying a terminal interface when connected to remote serial devices.
 */
public class WyTermTerminal implements Tty, WindowListener {
	public static Logger        logger = Logger.GetLogger(WyTermTerminal.class);
	public static File 			ConfigFile;
	private JFrame				frame;
	private JFrame 				bufferFrame;
	private GrittyTerminal 		terminal;
	private TermPanel 			termPanel;
	
    private String              host;
    private String              name;
    private String              serialConfig;
	private Socket 				socket;
	private int    				port;
	
	private InputStream 		in 			= 	null;
	private OutputStream 		out 		= 	null;
	
	public static final String 	XPOS		=	"XPOS";
	public static final String 	YPOS		=	"YPOS";
	public static final String 	WIDTH		=	"WIDTH";
	public static final String 	HEIGHT		=	"HEIGHT";

	/**
	 * Constructor
	 * @param host The address of the serial device.
	 * @param port The TCP port number of the device.
	 * @param serialConfig The string that defines the serial config.
	 */
    public WyTermTerminal(String host, int port, String name, String serialConfig) {
	  this.host=host;
	  this.name=name;
	  this.serialConfig=serialConfig;
      this.port=port;
      ConfigFile = new File( SimpleConfig.GetTopLevelConfigPath(Constants.APP_NAME), WyTermTerminal.class.getName());
      init();
      openSession();
	}
    
    /**
     * @brief Init the terminal GUI interface
     * @brief q A Questioner instance.
     * @return Always returns true.
     */
	public boolean init(Questioner q) {
		//Return True to indicate init success
		return true;
	}

	/**
	 * @brief Close a connection to remote serial device.
	 */
	public void close() {
		if (socket != null) {
			try {
				socket.close();
			}
			catch( IOException e) {
				logger.error(e);
			}
			in = null;
			out = null;
			socket = null;
		}
		
	}

	/**
	 * @brief Called when terminal resized.
	 * @param termSize A Dimension instance.
	 * @param pixelSize A Dimension instance.
	 */
	public void resize(Dimension termSize, Dimension pixelSize) {
	}

	/**
	 * @brief Get the name of the terminal.
	 */
	public String getName() {
	  return Constants.APP_NAME+" "+host+":"+port;
	}

	/**
	 * @brief Read data from the remote device.
	 * @param buf A byte buffer to hold the data.
	 * @param offset The offset in the buffer to place the data.
	 * @param length The length of the buffer.
	 * @return The number of bytes received.
	 */
	public int read(byte[] buf, int offset, int length) throws IOException {
		int rc=0;
		if( in != null ) {
			rc = in.read(buf, offset, length);
		}
		return rc;
	}

	/**
	 * @brief write to the remote device.
	 * @param bytes The buffer to write.
	 */
	public void write(byte[] bytes) throws IOException {
		if( out != null ) {	
			out.write(bytes);
			out.flush();
		}
	}

	
    /**
     * @brief Init the terminal GUI interface
     */
    private void init() {
        
		terminal = new GrittyTerminal();
		termPanel = terminal.getTermPanel();
		
        String frameTitle = Constants.APP_NAME+" "+host+" ("+serialConfig+")";
        
	    if( name != null && name.length() > 0 ) {
	      frameTitle = Constants.APP_NAME+" "+host+" ("+name+", "+serialConfig+")";
	    }
	      
		frame = new JFrame(frameTitle);
		frame.addWindowListener(this);

		JMenuBar mb = getJMenuBar();
		frame.setJMenuBar(mb);
		sizeFrameForTerm(frame);
		frame.getContentPane().add("Center", terminal);

		frame.pack();
		termPanel.setVisible(true);
		frame.setVisible(true);

		frame.setResizable(true);

		termPanel.setResizePanelDelegate(new ResizePanelDelegate(){
			public void resizedPanel( Dimension pixelDimension, RequestOrigin origin) {
				if(origin == RequestOrigin.Remote)
					sizeFrameForTerm(frame);
			}
		});
    }
    
    /**
     * @brief Get the menu bar associated with this terminal.
     * @return A JMenuBar instance.
     */
	private JMenuBar getJMenuBar() {
		JMenuBar mb = new JMenuBar();
		JMenu m = new JMenu("File");
		
		m.add(openAction);
		m.add(closeAction);
		mb.add(m);
		JMenu dm = new JMenu("Debug");
		
		dm.add(showBuffersAction);
		dm.add(resetDamage);
		dm.add(drawDamage);

		return mb;
	}
	 
	/**
	 * @brief Show the RX data.
	 */
	private void showBuffers() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				bufferFrame = new JFrame("buffers");
				JPanel panel = new BufferPanel(terminal);
				
				bufferFrame.getContentPane().add(panel);
				bufferFrame.pack();
				bufferFrame.setVisible(true);
				bufferFrame.setSize(800, 600);
				
				bufferFrame.addWindowListener(new WindowAdapter(){
					@Override
					public void windowClosing(WindowEvent e) {
						bufferFrame = null;
					}
				});
			}
		});
	}
	
	/**
	 * @brief Set the frame size to hold the terminal.
	 * @param frame
	 */
	private void sizeFrameForTerm(JFrame frame) {
		Dimension d = terminal.getPreferredSize();
		
		d.width += frame.getWidth() - frame.getContentPane().getWidth();
		d.height += frame.getHeight() - frame.getContentPane().getHeight(); 
		frame.setSize(d);
	}
	
	/**
	 * @brief Reconnect to the tty.
	 */
    private AbstractAction openAction = new AbstractAction("Reconnect"){
	      public void actionPerformed(ActionEvent e) {
	          close();
	          frame.setVisible(false);
	          init();
	          openSession();
	      }
	  };
  
    /**
     * @brief CAlled when the tty is closed.
     */
    private AbstractAction closeAction = new AbstractAction("Quit"){
      public void actionPerformed(ActionEvent e) {
        close();
        frame.setVisible(false);
      }
    };

  
    /**
     * brief Open a tty sesson.
     */
	public void openSession() {

		if(!terminal.isSessionRunning()){
			try {
				socket = new Socket(host, port);
				in = socket.getInputStream();
				out = socket.getOutputStream();
				terminal.setTty(this);
				terminal.start();
			}
			catch( IOException e ) {
				logger.error(e);
			}
		}
	}

	/**
	 * @brief Called tty resetDamage() method.
	 */
	private AbstractAction resetDamage = new AbstractAction("Reset damage") {
		public void actionPerformed(ActionEvent e) {
			if(termPanel != null)
				termPanel.getBackBuffer().resetDamage();
		}
	};

	/**
	 * @brief Call tty redrawFromDamage method.
	 */
	private AbstractAction drawDamage = new AbstractAction("Draw from damage") {
		public void actionPerformed(ActionEvent e) {
			if(termPanel != null)
				termPanel.redrawFromDamage();
		}
	};

	/**
	 * @brief Show RX buffers.  
	 */
	private AbstractAction showBuffersAction = new AbstractAction("Show buffers") {
		public void actionPerformed(ActionEvent e) {
			if(bufferFrame == null)
				showBuffers();
		}
	};
	//************************************************************************************

	  /**
	   * Load the window details
	   */
	  public boolean loadWindowDetails() {
		boolean loadedWindowDetails=false;
		  try {
			  Properties	windowProperties = new Properties();
			  FileInputStream fis = new FileInputStream( WyTermTerminal.ConfigFile );
			  windowProperties.load(fis);
			  int xpos = (int)Double.parseDouble( windowProperties.getProperty(MainFrame.XPOS) );
			  int ypos = (int)Double.parseDouble( windowProperties.getProperty(MainFrame.YPOS) );
			  int width = (int)Double.parseDouble( windowProperties.getProperty(MainFrame.WIDTH) );
			  int height = (int)Double.parseDouble( windowProperties.getProperty(MainFrame.HEIGHT) );
			  frame.setSize( width, height );
			  frame.setLocation( xpos, ypos );
			  fis.close();
			  loadedWindowDetails=true;
		  }
		  catch(Exception e) {
		  }
		  return loadedWindowDetails;
	  }
	  
	  /**
	   * Save the window size and position so that it can be loaded on restart.
	   */
	  public void saveWindowDetails() {
		  try {
			  Properties	windowProperties = new Properties();
			  FileWriter fw = new FileWriter( WyTermTerminal.ConfigFile );
			  windowProperties.put(MainFrame.XPOS, ""+frame.getLocation().getX());
			  windowProperties.put(MainFrame.YPOS, ""+frame.getLocation().getY());
			  windowProperties.put(MainFrame.WIDTH, ""+frame.getSize().getWidth());
			  windowProperties.put(MainFrame.HEIGHT, ""+frame.getSize().getHeight());
			  windowProperties.store(fw, "");
			  fw.close();
		  }
		  catch(Exception e) {
			  e.printStackTrace();
		  }
	  }

	public void windowActivated(WindowEvent arg0) {}
	public void windowClosed(WindowEvent arg0) {}
	public void windowClosing(WindowEvent arg0) {
		saveWindowDetails();
	}
	public void windowDeactivated(WindowEvent arg0) {}
	public void windowDeiconified(WindowEvent arg0) {}
	public void windowIconified(WindowEvent arg0) {}
	public void windowOpened(WindowEvent arg0) {
		loadWindowDetails();
	}
	  
}
