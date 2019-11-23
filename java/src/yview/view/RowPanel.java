package yview.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.JPanel;

public class RowPanel extends JPanel
{
  int rowCount=0;
  JPanel leftPanel;
  JPanel rightPanel;
  JPanel topPanel;

  public RowPanel()
  {
    setRowPanel(0);
  }

  public RowPanel(int rowCount)
  {
    setRowPanel(rowCount);
  }
  
  public void setRowPanel(int rowCount)
  {
    this.rowCount=rowCount;
    setLayout(new FlowLayout(FlowLayout.LEFT));
    leftPanel = new JPanel( new GridLayout(rowCount,1) );
    rightPanel = new JPanel( new GridLayout(rowCount,1) );
    topPanel = new JPanel( new BorderLayout(5,5) );
    topPanel.add(leftPanel,BorderLayout.WEST);
    topPanel.add(rightPanel,BorderLayout.EAST);
    add(topPanel);
  }
  
  public void add(Component leftComponent, Component rightComponent)
  {
    leftPanel.add(leftComponent);
    rightPanel.add(rightComponent);
  }
  
  public void replaceLeftComponent(Component leftComponent,int index)
  {
    leftPanel.remove(index);
    leftPanel.add(leftComponent,index);
  }
  
  public void replaceRightComponent(Component leftComponent,int index)
  {
    rightPanel.remove(index);
    rightPanel.add(leftComponent,index);
  }
  
  public void add(Component leftComponent, Component rightComponent,int index)
  {
    leftPanel.add(leftComponent,index);
    rightPanel.add(rightComponent,index);
  }
  
  public void removeLeftComponent(Component leftComponent)
  {
    leftPanel.remove(leftComponent);
  }
  
  public void removeRightComponent(Component rightComponent)
  {
    rightPanel.remove(rightComponent);
  }
  
}
