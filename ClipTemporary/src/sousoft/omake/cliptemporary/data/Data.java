package sousoft.omake.cliptemporary.data;

import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.SortedMap;

import javax.swing.JOptionPane;

import soulib.lib.MapConfig;
import soulib.windowLib.CommonGraphics;
import soulib.windowLib.CanvasComponents.CToggleButton;
import soulib.windowLib.CanvasComponents.CanvasComponent;
import sousoft.omake.cliptemporary.CTMenu;
import sousoft.omake.cliptemporary.CTWindow;
import sousoft.omake.cliptemporary.ClipTemporary;
import sousoft.omake.cliptemporary.ClipTemporaryPanel;

public abstract class Data extends CToggleButton{
	public final String type;
	private CTWindow window;
	/**ロックしたら必ず解除すること finally*/
	protected boolean lock;
	public abstract int getID();
	public Data(CTWindow win,String t){
		this.newthread=false;
		type=t;
		name="謎データ";
		window=win;
		this.setFont(win.getFont());
	}
	public void draw(CommonGraphics g,Point p) {
		super.draw(g,p);
		if(!window.drawLine)return;
		int y=getY()+getH();
		g.drawLine(getX(),y,window.canWidth(),y);
	}
	@Override
	public int getW() {
		return Math.max(super.getW(),window.getMainPanel().bodyX);
	}
	@Override
	public void click(MouseEvent e){
		if(e.getButton()==MouseEvent.BUTTON3)getPanel().showMenu(this);
		else super.click(e);
	}
	public ClipTemporaryPanel getPanel(){
		return window.ct;
	}
	@Override
	public void mousePressed(MouseEvent e){
		if(e.getButton()!=MouseEvent.BUTTON3)super.mousePressed(e);
		getPanel().hideMenu(e);
	}
	@Override
	public String toString(){
		return "type="+type;
	}
	public CTWindow getWindow(){
		return window;
	}
	protected void finalize() throws Throwable {
		if(ClipTemporary.debug)System.out.println("DataFinalize="+toString());
		super.finalize();
	}
	public void openMenu(ArrayList<CanvasComponent> list) {
		list.add(new CTMenu(window, "コピー"){
			public void click(){
				ClipTemporary.writeClip(Data.this);
			}
		});
		list.add(new CTMenu(window,"切り取り"){
			public void click(){
				ClipTemporary.writeClip(Data.this);
				getPanel().remove(Data.this);
			}
		});
		list.add(new CTMenu(window,"削除"){
			public void click(){
				getPanel().remove(Data.this);
			}
		});
	}
	public abstract long write(DataOutputStream out)throws Exception;
	public abstract long read(DataInputStream in) throws Exception;
	/**取り消しされた場合null*/
	public static Charset CelectEncode() {
		SortedMap<String, Charset> map=Charset.availableCharsets();
		ArrayList<String> keys=MapConfig.getAllKeys(map);
		sort(keys);
		keys.trimToSize();
		String title="文字コードを選択";
		String celect=(String)JOptionPane.showInputDialog(null,title,title,
				JOptionPane.QUESTION_MESSAGE,null,keys.toArray(),keys.get(0));
		if(celect==null)return null;
		return map.getOrDefault(celect,StandardCharsets.UTF_8);
	}
	private static void sort(ArrayList<String> keys) {
		move(keys,StandardCharsets.ISO_8859_1.name());
		move(keys,StandardCharsets.UTF_16BE.name());
		move(keys,"EUC-JP");
		move(keys,"Shift_JIS");
		move(keys,StandardCharsets.UTF_16LE.name());
		move(keys,StandardCharsets.UTF_8.name());
	}
	private static void move(ArrayList<String> keys,String key) {
		if(keys.remove(key)) {
			keys.add(0,key);
		}
	}
	public String getUsedTempFile(){
		return null;
	}
	public boolean isLock(){
		return lock;
	}
	public abstract Transferable writeClip(Data[] arr);
}