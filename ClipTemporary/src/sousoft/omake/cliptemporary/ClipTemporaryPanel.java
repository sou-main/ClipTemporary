package sousoft.omake.cliptemporary;

import static sousoft.omake.cliptemporary.ClipTemporary.*;

import java.awt.Color;
import java.awt.Point;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import soulib.windowLib.CanvasComponentManager;
import soulib.windowLib.CommonGraphics;
import soulib.windowLib.WindowLib;
import soulib.windowLib.CanvasComponents.CPanel;
import soulib.windowLib.CanvasComponents.CanvasComponent;
import sousoft.omake.cliptemporary.ClipTemporary.ReadClipException;
import sousoft.omake.cliptemporary.History.HistoryManager;
import sousoft.omake.cliptemporary.History.HistoryRemove;
import sousoft.omake.cliptemporary.data.Data;
import sousoft.omake.cliptemporary.data.DataImage;
import sousoft.omake.cliptemporary.plugin.IPlugin;

public class ClipTemporaryPanel extends CPanel{
	public HistoryManager history;
	public List<Data> data;
	public int bodyX;
	public boolean imageView;
	private CPanel clickMenu;
	private boolean showClickMenu;
	public CTWindow window;
	//"C:\\Windows\\Media\\Windows Background.wav"
	public ClipTemporaryPanel(CTWindow win,boolean frame){
		super(frame,true);
		window=win;
		data=new ArrayList<Data>();
		history=new HistoryManager(this);
		//data=new Vector<Data>();
		clickMenu=new CPanel();
		clickMenu.ccm=new CanvasComponentManager() {
			/**TODO 追加を上書きに変更*/
			public void addCanvasComponent(CanvasComponent[] cc){
				ccs=cc;
			}
		};
		clickMenu.setPriority(100);
		setName("ClipTemporaryPanel");
	}
	public void draw(CommonGraphics g,Point p) {
		if(active&&window.drawLine&&ccm!=null&&ccm.size()>0) {
			g.setColor(Color.black);
			g.drawLine(getX(),getY(),window.canWidth(),getY());
		}
		super.draw(g,p);
	}
	/**選択したものを移動させる*/
	public void move(int i) {
		move(i,true);
	}
	public void move(int i,boolean addHistory) {
		ArrayList<Integer> indexList=new ArrayList<Integer>();
		for(int j=0;j<data.size();j++) {
			Data e=data.get(i<0?j:data.size()-1-j);
			if(!e.toggle)continue;
			int index=data.indexOf(e)+i;
			if(index<0||index>data.size()-1)continue;
			data.remove(e);
			data.add(index,e);
			indexList.add(index);
		}
		int[] indexArray=new int[indexList.size()];
		int[] NindexArray=new int[indexList.size()];
		for(int j=0;j<indexArray.length;j++) {
			NindexArray[j]=indexList.get(j);
			indexArray[j]=indexArray[j]-i;
		}
		if(addHistory)addHistory(new History.HistoryMove(this,indexArray,NindexArray));
		sort();
	}
	public void remove(Data... arr){
		ArrayList<Data> list=new ArrayList<Data>();
		try{
			for(Data d:arr){
				if(d.toggle)list.add(d);
			}
			addHistory(new HistoryRemove(list));
			for(Data d:list) {
				if(debug)System.out.println("remove@data="+d+"@toggle="+d.toggle);
				getEdit().saved=false;
				data.remove(d);
				removeCanvasComponent(d);
				removeDataEvent(d);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		sort();
		bodyX();
		System.gc();
	}
	public void remove(Data d){
		remove(d,true);
	}
	public void remove(Data d,boolean addHistory){
		if(d.isLock())return;
		if(addHistory)addHistory(new History.HistoryRemove(d));
		getEdit().saved=false;
		data.remove(d);
		removeCanvasComponent(d);
		removeDataEvent(d);
		sort();
		bodyX();
	}
	public void removeAll(){
		addHistory(new History.HistoryRemove(data));
		getEdit().saved=false;
		for(Data d:data)removeDataEvent(d);
		data.clear();
		removeAllCanvasComponent();
		System.gc();
	}
	public void removeDataEvent(Data d) {
		for(IPlugin p:window.plugin){
			p.removeData(d);
		}
	}
	public void readS() throws UnsupportedFlavorException,IOException{
		readS(true);
	}
	public void readS(boolean dialog) throws UnsupportedFlavorException,IOException{
		Data[] read;
		try{
			read=ClipTemporary.readClip(window);
		}catch(ReadClipException e){
			if(dialog)WindowLib.ErrorWindow(e.getMessage(),"読み取り失敗");
			return;
		}
		if(read==null) {
			if(dialog)WindowLib.ErrorWindow("非対応データです","読み取り失敗");
			return;
		}
		getEdit().saved=false;
		data.addAll(Arrays.asList(read));
		addCanvasComponent(read);
		for(Data d:read)addDataEvent(d);
		addHistory(new History.HistoryAdd(read));
		addData();
	}
	public void sort(){
		int h=getY();
		for(int i=0;i<data.size();i++){
			Data d=data.get(i);
			d.setPoint(getX(),h);
			h+=d.getH();
			d.setPriority(-i);
		}
	}
	public void resetHeight() {
		int h=getY();
		for(int i=0;i<data.size();i++){
			Data d=data.get(i);
			d.setPoint(getX(),h);
			h+=d.getH();
		}
	}
	public static void sort(CPanel p){
		int h=p.getY();
		CanvasComponent[] arr=p.getCanvasComponent();
		for(int i=0;i<arr.length;i++){
			CanvasComponent d=arr[i];
			d.setPoint(p.getX(),h);
			h+=d.getH();
			d.setPriority(-i+d.getPriority());
		}
	}
	/**null引数可能
	 * @param e nullでも可
	 * @return 閉じたときtrue*/
	public boolean hideMenu(MouseEvent e){
		if(!ccm.contains(clickMenu))return false;
		if(e!=null&&clickMenu.isInside(e.getPoint()))return false;
		removeCanvasComponent(clickMenu);
		showClickMenu=false;
		return true;
	}
	public void showMenu(Data d){
		//clickMenu.removeAllCanvasComponent();//TODO デフォルトの実装では必要
		ArrayList<CanvasComponent> list=new ArrayList<CanvasComponent>();
		d.openMenu(list);
		for(IPlugin p:window.plugin) {
			try{
				p.openMenu(d,list);
			}catch(NoClassDefFoundError e) {
				e.printStackTrace();
			}
		}
		if(list.size()<1)return;
		CanvasComponent[] arr=new CanvasComponent[list.size()];
		//for(int i=0;i<arr.length;i++)arr[i]=list.get(i);
		arr=list.toArray(arr);
		showClickMenu=true;
		clickMenu.addCanvasComponent(arr);
		addCanvasComponent(clickMenu);
		clickMenu.setPoint(getX()+bodyX,d.getY());
		sort(clickMenu);
	}
	public void bodyX(){
		/*
		CanvasComponent[] arr=getCanvasComponent();
		bodyX=0;
		for(CanvasComponent cc:arr) {
			if(bodyX<cc.getW())bodyX=cc.getW();
		}
		*/
		bodyX=getW();
		if(showClickMenu)bodyX-=clickMenu.getW();
	}
	public EditFile getEdit(){
		return window.getEdit();
	}
	public void addData(int index,Data d){
		addData(index,d,true);
	}
	public void addData(int index,Data d,boolean addHistory){
		if(d==null)throw new NullPointerException();
		if(d instanceof DataImage&&((DataImage)d).isNull())return;
		if(index==data.size()) {
			data.add(d);
			if(addHistory)addHistory(new History.HistoryAdd(d));
			addCanvasComponent(d);
			addDataEvent(d);
			addData();
			return;
		}
		data.add(index,d);
		if(addHistory)addHistory(new History.HistoryAdd(d));
		CanvasComponent[] cc=ccm.getCanvasComponent();
		CanvasComponent[] n=new CanvasComponent[cc.length+1];
		for(int i=0;i<index;i++) {
			n[i]=cc[i];
		}
		n[index]=d;
		for(int i=index+1;i<n.length;i++) {
			n[i]=cc[i-1];
		}
		ccm.setCanvasComponent(n);
		addDataEvent(d);
		ReSize();
		addData();
	}
	public void addData(Data d){
		if(d==null)throw new NullPointerException();
		if(d instanceof DataImage&&((DataImage)d).isNull())return;
		data.add(d);
		addHistory(new History.HistoryAdd(d));
		addCanvasComponent(d);
		addDataEvent(d);
		addData();
	}
	public void addData(List<? extends Data> read){
		addData(read,true);
	}
	public void addData(List<? extends Data> read,boolean addHistory){
		if(read==null)throw new NullPointerException();
		for(Data d:read.toArray(new Data[read.size()])) {
			if(d instanceof DataImage&&((DataImage)d).isNull()) {
				read.remove(d);
			}
		}
		data.addAll(read);
		if(addHistory)addHistory(new History.HistoryAdd(read));
		addCanvasComponent(read.toArray(new Data[read.size()]));
		for(Data d:data)addDataEvent(d);
		addData();
	}
	public void addDataEvent(Data d) {
		for(IPlugin p:window.plugin){
			p.addData(d);
		}
	}
	public void addData() {
		sort();
		ReSize();
		bodyX();
		getEdit().saved=false;
	}
	public void writeS(){
		ArrayList<Data> list=new ArrayList<Data>();
		for(Data d:data){
			if(d.getToggle()) list.add(d);
		}
		writeClip(list.toArray(new Data[list.size()]));
		if(debug) for(Data li:list){
			System.out.println(li.toString());
		}
	}
	public void CelectDelete(){
		if(data.isEmpty()) return;
		if(window.getCTMenuBar().getDelCheck()) if(!WindowLib.Yes_NoWindow("選択した項目を削除しますか?","削除しますか?")) return;
		Data[] arr=data.toArray(new Data[data.size()]);
		remove(arr);
	}
	public void Cut(){
		if(data.isEmpty()) return;
		writeS();
		Data[] arr=data.toArray(new Data[data.size()]);
		remove(arr);
	}
	public void addHistory(History hg){
		if(history!=null)history.add(hg);
	}
}