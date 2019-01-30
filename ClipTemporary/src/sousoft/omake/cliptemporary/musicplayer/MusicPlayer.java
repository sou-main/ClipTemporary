package sousoft.omake.cliptemporary.musicplayer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;

import soulib.lib.ConfigBase;
import soulib.windowLib.FontManager;
import soulib.windowLib.WindowLib;
import soulib.windowLib.WindowMode;
import soulib.windowLib.CanvasComponents.CButton;
import soulib.windowLib.CanvasComponents.CLabel;
import soulib.windowLib.CanvasComponents.CPanel;
import soulib.windowLib.CanvasComponents.CScrollbar;
import soulib.windowLib.CanvasComponents.CanvasComponent;
import sousoft.omake.cliptemporary.CTWindow;
import sousoft.omake.cliptemporary.data.DataFile;
import sousoft.omake.cliptemporary.musicplayer.ISoundBase.EndEvent;

public class MusicPlayer extends CPanel{
	private static class VolScrollbar extends CScrollbar{
		private MusicPlayer mp;
		public VolScrollbar(MusicPlayer m){
			super(true);
			mp=m;
		}
		public void setCursor(int cursor){
			if(cursor<0){
				cursor=0;
			}else if(cursor>getMax()){
				cursor=getMax();
			}
			this.cursor=cursor;
			getBar().setX(getX()+cursor);
			int volN=getFomat(0,100);
			if(mp!=null)mp.setVol(volN);
		}
		/** 見た目の長さを変更(ボタンを含まない) */
		@Override
		public void setH(int max){
			this.h=max;
			w=max;
			getFrame().setW(max);
			getFrame().setH(getWidth());
			setX(getX());
			setY(getY());
		}
		@Override
		public void setX(int x){
			super.setX(x);
			getUp().setX(x);
			getDown().setX(x+h-getWidth());
			getFrame().setX(x);
			getBar().setX(x+getCursor());
		}
		protected void setCursor(Point p){
			setCursor(p.x-getX()-getBar().getW()/2);
		}
		public int getMax(){
			return getHeight()-getBar().getW();
		}
	}
	private ArrayList<Line> list=new ArrayList<Line>();
	private int y;
	public boolean win;
	public MPWindow window;
	private CButton Wbt;
	public final CTWindow ctWindow;
	private int posX,posY;
	private CLabel label=new CLabel("MusicPlayer");
	private CButton next;
	public final CScrollbar vol;
	private boolean SequentialPlay;

	public MusicPlayer(CTWindow ctWindow) {
		super(true,true);
		setBackground(Color.white);
		setPriority(100);
		this.ctWindow=ctWindow;
		active=false;
		label.setFont(WindowLib.getPLAIN_Font(20));
		Wbt=new CButton("ウィンドウ化");
		Wbt.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e){
				win=!win;
				if(win)toWindow();
				else toPanel();
			}
		});
		next=new CButton("次の曲");
		next.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e){
				if(!SequentialPlay)return;
				Line line=null;
				int i=0;
				for(Line l:list) {
					if(!l.sb.isStop()) {
						line=l;
						i++;
					}
					if(i>1)break;
				}
				if(line==null&&i<2)return;
				ISoundBase sb=line.sb;
				sb.End();
				EndEvent event=sb.getEndEvent();
				if(event!=null)event.end(sb);
				//System.out.println(sb.isStop());
			}
		});
		this.addCanvasComponent(Wbt);
		vol=new VolScrollbar(this);
		int wbth;
		vol.getBar().setW(20);
		vol.setW(wbth=Wbt.getH());
		vol.setH(200+vol.getBarSize());
		vol.setCursor(50+wbth);
		vol.getUp().active=false;
		vol.getDown().active=false;
		this.addCanvasComponent(vol,Wbt.getW(),0);
		this.addCanvasComponent(label,Wbt.getW()+vol.getW(),0);
		this.addCanvasComponent(next,Wbt.getW()+vol.getW()+label.getW(),0);
		y=Wbt.getH();
	}
	@Override
	public void keyPressed(KeyEvent e){
		super.keyPressed(e);
		int keycode=e.getKeyCode();
		if(keycode==KeyEvent.VK_SPACE){
			if(win&&list.size()==1) {
				Line line=list.get(0);
				ISoundBase sb=line.sb;
				line.name.setBackground(sb.isStop()?Color.orange:Color.white);
				sb.stop(!sb.isStop());
				//System.out.println(sb.isStop());
			}
		}
	}
	public synchronized void toWindow(){
		WindowMode wm=new WindowMode(100,100);
		wm.name=ctWindow.getTitle()+" - MusicPlayer";
		wm.icon=ctWindow.getWindowMode().icon;
		window=new MPWindow(wm,MusicPlayer.this);
		if(list.isEmpty()) {
			window.setVisible(false);
		}
		Wbt.name="パネル化";
		frame=false;
		move=false;
		posX=getX();
		posY=getY();
		setPoint(0,0);
		vol.setX(Wbt.getW());
		label.setX(Wbt.getW()+vol.getW());
		next.setX(Wbt.getW()+vol.getW()+label.getW());
		ctWindow.getCanvasComponentManager().removeCanvasComponent(MusicPlayer.this);
		win=true;
	}
	public synchronized void toPanel() {
		setBackground(Color.white);
		setPriority(100);
		setPoint(posX,posY);
		ConfigBase conf=ctWindow.getConfig();
		if(window!=null&&conf!=null){
			Point p=window.getLocation();
			conf.setConfigDataInt("MPウィンドウX座標",p.x);
			conf.setConfigDataInt("MPウィンドウY座標",p.y);
		}
		Wbt.name="ウィンドウ化";
		vol.setX(getX()+Wbt.getW());
		label.setX(Wbt.getW()+getX()+vol.getW());
		next.setX(Wbt.getW()+getX()+vol.getW()+label.getW());
		frame=true;
		move=true;
		ctWindow.getCanvasComponentManager().addCanvasComponent(this);
		window.close();
		window=null;
		win=false;
	}
	public void setVol(float vol) {
		for(Line l:list) {
			if(l.sb!=null)l.sb.setVol(vol);
		}
	}
	public void setSize(int w,int h){
		super.setW(w);
		super.setH(h);
		if(window!=null)window.setSize(getW()+window.spaceX,getH()+window.spaceY);
	}
	public void setW(int i) {
		super.setW(i);
		if(window!=null)window.setSize(getW()+window.spaceX,getH()+window.spaceY);
	}
	public void setH(int i) {
		super.setH(i);
		if(window!=null)window.setSize(getW()+window.spaceX,getH()+window.spaceY);
	}
	public synchronized void remove(Line line){
		int index=list.indexOf(line);
		if(index<0)return;
		list.remove(index);
		y-=line.getH();
		ccm.removeCanvasComponent(line.arr);
		if(line.sb!=null) try{
			line.sb.close();
		}catch(IOException e){
			e.printStackTrace();
		}
		line.sb=null;
		w=0;
		for(int i=index;i<list.size();i++) {
			Line l=list.get(i);
			for(CanvasComponent cc:l.arr) {
				cc.setPoint(cc.getX(),cc.getY()-line.getH());
			}
		}
		for(Line l:list) {
			int lw=l.getW();
			if(lw>w)w=lw;
		}
		if(list.isEmpty()) {
			this.active=false;
			if(window!=null)window.setVisible(active);
		}
	}
	public synchronized Line add(ISoundBase sb,String name, DataFile dataFile) {
		Line line=new Line(sb,name);
		if(sb!=null)sb.setVol(vol.getFomat(0,100));
		sb.setEndEvent(new SequentialPlayEvent(this,line,dataFile));
		line.mp=this;
		line.name.setFont(ctWindow.getFontManager().get(Font.PLAIN,20));
		line.setFont(ctWindow.getFontManager());
		list.add(line);
		line.setPos(y);
		ccm.addCanvasComponent(line.arr);
		y+=line.getH();
		for(Line l:list) {
			int lw=l.getW();
			if(lw>w)w=lw;
		}
		if(!active) {
			this.active=true;
			if(window!=null)window.setVisible(active);
		}
		return line;
	}
	public boolean isSequentialPlay(){
		return SequentialPlay;
	}
	public void setSequentialPlay(boolean b) {
		SequentialPlay=b;
		next.active=b;
	}
	public int getH() {
		return y;
	}
	public void setFont(FontManager f){
		if(window!=null)window.setFont(f);
		Font now=getFont();
		super.setFont(f.get(now.getStyle(),now.getSize()));
		Wbt.setFont(f.get(Wbt.getFont().getStyle(),Wbt.getFont().getSize()));
		vol.setX(getX()+Wbt.getW());
		y=Wbt.getH();
		vol.setW(Wbt.getH());
		label.setX(Wbt.getW()+getX()+vol.getW());
		label.setFont(f.get(label.getFont().getStyle(),label.getFont().getSize()));
		next.setX(Wbt.getW()+getX()+vol.getW()+label.getW());
		next.setFont(f.get(next.getFont().getStyle(),next.getFont().getSize()));
		for(Line line:list) {
			line.setFont(f);
			line.setPos(y);
			y+=line.name.getH();
		}
	}
	public synchronized void close(CTWindow ctWindow2){
		if(window!=null)window.close();
		window=null;
		for(Line l:list) {
			try{
				l.sb.End();
				l.sb.close();
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	public int getPanelX(){
		return posX;
	}
	public int getPanelY(){
		return posY;
	}
}
