package yview.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Responsible for displaying the status line (typically at the bottom of a frame)
 * Maybe double clicked to show the history.
 * 
 * @author Paul Austen
 */
public class StatusBar extends JPanel implements ActionListener, MouseListener
{
  private JLabel statusBarLabel = new JLabel();
  private JFrame historyFrame;
  private JTextArea messageField;
  public  static String ToolTipOpen="Double click for message history window"; 
  public  static String ToolTipClose="Double click to close message history window"; 
  private Timer t;
  boolean historyFrameVisibleFirsttime = true;
  private JScrollPane jScrollPane;
  
  public StatusBar()
  {
    this.setLayout(new FlowLayout(FlowLayout.LEFT));
    add(statusBarLabel);
    setBorder( BorderFactory.createEtchedBorder());
    status(" ",false);

    historyFrame = new JFrame("Status message history");
    messageField = new JTextArea(24,80);
    messageField.setFont( new Font("Monospaced", Font.PLAIN, 11) );
    jScrollPane = new JScrollPane(messageField);
    historyFrame.getContentPane().add( jScrollPane );
    historyFrame.pack();
	
    statusBarLabel.addMouseListener(this);
    addMouseListener(this);
    statusBarLabel.setToolTipText(ToolTipOpen);
    setToolTipText(ToolTipOpen);

  }
  
  private void status(String line, boolean clearDown)
  {
    statusBarLabel.setText(line);
    if( messageField != null )
    {
      messageField.append(line+"\n");
      if( messageField.getText().length() > 65536 )
      {
        //Chop it down so we don't use to much memory
        messageField.replaceRange("", 0, 32768);
      }
    }
    if( clearDown )
    {
      if( t == null )
      {
        t = new Timer(3000,this);
      }
      t.restart();
    }
  }
  
  public void println(String line)
  {
    status(line, true);
  }
  
  public void println_persistent(String line)
  {
    status(line, false);
  }
  
  //Called to clear down the last message
  public void actionPerformed(ActionEvent e) 
  {
    //Clear status text, keep space as this ensures that the vertical size of the status bar does not change.
    statusBarLabel.setText(" ");
  }
  
  /**
   * @brief Hide the history frame if visible
   */
  public void close() {
	  if( historyFrame.isVisible() ) {
		  historyFrame.setVisible(false);
	  }
  }

  public void mouseClicked(MouseEvent e) 
  {

    //If double click 
    if( e.getClickCount() == 2 )
    {          	
    	if( historyFrame.isVisible() ) {
    		historyFrame.setVisible(false);   
    	    statusBarLabel.setToolTipText(ToolTipOpen);
    	    setToolTipText(ToolTipOpen);
    	}
    	else {
    		
    		if( historyFrameVisibleFirsttime ) {
    			historyFrame.setLocationRelativeTo( getParent() );
    			historyFrameVisibleFirsttime=false;
    		}
    	    statusBarLabel.setToolTipText(ToolTipClose);
    	    setToolTipText(ToolTipClose);
    	    historyFrame.toFront();
    	    historyFrame.setVisible(true);

    	}
    }
  }

  public void mouseEntered(MouseEvent e) {}
  public void mouseExited(MouseEvent e) {}
  public void mousePressed(MouseEvent e) {}
  public void mouseReleased(MouseEvent e) {}
 
}