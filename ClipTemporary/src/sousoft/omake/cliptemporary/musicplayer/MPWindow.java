package sousoft.omake.cliptemporary.musicplayer;

import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import soulib.lib.ConfigBase;
import soulib.lib.DataEditor;
import soulib.windowLib.FontManager;
import soulib.windowLib.SwingWindow;
import soulib.windowLib.WindowLib;
import soulib.windowLib.WindowMode;
import sousoft.omake.cliptemporary.CTWindow;

public class MPWindow extends SwingWindow{
	private MusicPlayer mp;
	public int spaceX=50,spaceY=80;
	public void setFont(FontManager font) {
		JMenuBar mb=getJMenuBar();
		int len=mb.getMenuCount();
		for(int index=0;index<len;index++) {
			JMenu m=mb.getMenu(index);
			int ml=m.getItemCount();
			for(int i=0;i<ml;i++) {
				CTWindow.setFont(m.getMenuComponent(i),font);
			}
			Font f=m.getFont();
			m.setFont(font.get(f.getStyle(),f.getSize()));
		}
	}
	public MPWindow(WindowMode wm, MusicPlayer mp){
		super(wm);
		ccm.addCanvasComponent(mp);
		this.mp=mp;
		setResizable(false);
		JMenuBar mb=new JMenuBar();
		JMenu m=new JMenu("設定");
		ConfigBase conf=mp.ctWindow.getConfig();
		{
			final JCheckBoxMenuItem mi=new JCheckBoxMenuItem("常に最前面");
			mi.setState(conf.getConfigDataBoolean("MP常に最前面"));
			if(mi.getState())setAlwaysOnTop(true);
			mi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e){
					//System.out.println(isAlwaysOnTopSupported());
					ConfigBase conf=mp.ctWindow.getConfig();
					conf.setConfigDataBoolean("MP常に最前面",mi.getState());
					setAlwaysOnTop(mi.getState());
				}
			});
			m.add(mi);
		}
		if(conf.contains("MPウィンドウX軸余白")) {
			spaceX=conf.getConfigDataInt("MPウィンドウX軸余白");
		}
		{
			JMenuItem mi=new JMenuItem("ウィンドウX軸余白");
			mi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e){
					String size=WindowLib.InputWindow("ウィンドウX軸余白",Integer.toString(spaceX));
					if(size==null)return;
					int i=(int) DataEditor.parseNumber(size,-1);
					if(i<0)return;
					spaceX=i;
					ConfigBase conf=mp.ctWindow.getConfig();
					conf.setConfigDataInt("MPウィンドウX軸余白",i);
				}
			});
			m.add(mi);
		}
		if(conf.contains("MPウィンドウY軸余白")) {
			spaceY=conf.getConfigDataInt("MPウィンドウY軸余白");
		}
		{
			JMenuItem mi=new JMenuItem("ウィンドウY軸余白");
			mi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e){
					String size=WindowLib.InputWindow("ウィンドウY軸余白",Integer.toString(spaceY));
					if(size==null)return;
					int i=(int) DataEditor.parseNumber(size,-1);
					if(i<0)return;
					spaceY=i;
					ConfigBase conf=mp.ctWindow.getConfig();
					conf.setConfigDataInt("MPウィンドウY軸余白",i);
				}
			});
			m.add(mi);
		}
		mb.add(m);
		this.setJMenuBar(mb);
		Point p=getLocation();
		setLocation(conf.getConfigDataInt("MPウィンドウX座標",p.x),conf.getConfigDataInt("MPウィンドウY座標",p.y));
	}
	@Override
	public WindowListener setWindowListener(){
		return new WindowAdapter(){
			@Override
			public void windowClosing(WindowEvent evt){
				mp.toPanel();
			}
		};
	}
}