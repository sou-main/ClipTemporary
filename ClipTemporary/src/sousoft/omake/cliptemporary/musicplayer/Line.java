package sousoft.omake.cliptemporary.musicplayer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

import soulib.windowLib.CommonGraphics;
import soulib.windowLib.FontManager;
import soulib.windowLib.WindowLib;
import soulib.windowLib.CanvasComponents.CButton;
import soulib.windowLib.CanvasComponents.CLabel;
import soulib.windowLib.CanvasComponents.CToggleButton;
import soulib.windowLib.CanvasComponents.CanvasComponent;

public class Line{

	class LoadFrame extends CanvasComponent{
		private int h=0;
		private ISoundBase sb;
		private DecimalFormat f;
		private String text;
		public LoadFrame(ISoundBase s) {
			sb=s;
			f=new DecimalFormat("0,000");
			setFont(WindowLib.getBOLD_Font(15));
		}
		public int getW() {
			if(text==null)return 0;
			return WindowLib.getPaintWidth(getFont(),text);
		}
		public int getH() {
			if(h!=0)return h;
			return h=WindowLib.getPaintHeight(getFont());
		}
		@Override
		public void draw(CommonGraphics g,Point p){
			long len=sb.getFrameLength();
			text=f.format(sb.getLoadFrame());
			if(len>0)text+=" / "+f.format(len);
			g.setColor(Color.black);
			g.drawLine(mp.getX(),getY(),name.getX()+mp.getW(),getY());
			g.setColor(getColor());
			g.setFont(getFont());
			g.drawString(text,getX(),getY()+getH()+2);
			if(sb.isEnd()&&!sb.isRoop())name.setBackground(Color.white);
		}
	}
	public MusicPlayer mp;
	public ISoundBase sb;
	private CButton bt,bt0,bt1;
	public CLabel name;
	public final CanvasComponent[] arr;
	private CToggleButton bt2;
	private LoadFrame frame;
	public Line(ISoundBase s,String s1) {
		sb=s;
		name=new CLabel(s1);
		name.setTooltip(s.getFile().getAbsolutePath());
		name.setBackground(Color.orange);
		bt=new CButton("一時停止");
		bt.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e){
				sb.stop(true);
				name.setBackground(Color.white);
			}
		});
		bt0=new CButton("再開");
		bt0.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e){
				sb.stop(false);
				if(sb.isEnd())sb.play();
				name.setBackground(Color.orange);

			}
		});
		bt1=new CButton("停止");
		bt1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e){
				sb.End();
				mp.remove(Line.this);
			}
		});
		bt2=new CToggleButton("ループ再生");
		bt2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e){
				sb.setLoop(bt2.getToggle());
			}
		});
		frame=new LoadFrame(sb);
		frame.setPriority(100);
		arr= new CanvasComponent[]{name,bt,bt0,bt1,bt2,frame};
	}
	public void setPos(int y) {
		int w=mp.getX();
		for(CanvasComponent cc:arr) {
			cc.setPoint(w,y+mp.getY());
			w+=cc.getW();
		}
	}
	public int getH() {
		int h=0;
		for(CanvasComponent cc:arr) {
			int cch=cc.getH();
			if(h<cch)h=cch;
		}
		return h;
	}
	public int getW() {
		int w=0;
		for(CanvasComponent cc:arr) {
			w+=cc.getW();
		}
		return w;
	}
	public void setFont(FontManager f) {
		for(CanvasComponent cc:arr) {
			Font n=cc.getFont();
			cc.setFont(f.get(n.getStyle(),n.getSize()));
		}
	}
}
