package sousoft.omake.cliptemporary.data;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Point;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import soulib.fileLib.FileEditor;
import soulib.windowLib.CommonGraphics;
import soulib.windowLib.WindowLib;
import soulib.windowLib.CanvasComponents.CanvasComponent;
import sousoft.omake.cliptemporary.CTMenu;
import sousoft.omake.cliptemporary.CTWindow;
import sousoft.omake.cliptemporary.History.EditTextHistory;
import sousoft.omake.cliptemporary.History.HistoryAdd;
import sousoft.omake.cliptemporary.History.HistoryGroup;

/**文字データ*/
public class DataText extends Data{
	public static int id=0;
	@Override
	public int getID(){
		return id;
	}
	private String data;
	//private int maxLen=127;
	private String decodeData;
	public DataText(CTWindow panel,String text){
		super(panel, "text");
		data=text;
	}
	public DataText(CTWindow panel){
		super(panel, "text");
	}
	{
		name="文字";
		setBackground(Color.cyan);
	}
	@Override
	public void draw(CommonGraphics g,Point p){
		super.draw(g,p);
		g.setColor(Color.black);
		//int max;
		if(isInside(p))// {
			g.setFont(getWindow().getBigFont());
		//	max=maxLen*4;
		//}else max=maxLen;
		String ra;
		if(data!=null&&data.indexOf("http")==0&&getWindow().getConfig().getConfigDataBoolean("URLエンコード解除",true)) {
			if(decodeData==null)try{
				decodeData=URLDecoder.decode(data,"utf-8");
			}catch(UnsupportedEncodingException e){
				e.printStackTrace();//ここは実行されないはず
			}
			ra=decodeData;
		}
		else ra=String.valueOf(data);
		if(getWindow().getCTMenuBar().isPaintCRLF()&&data!=null&&decodeData==null){
			StringBuilder sb=new StringBuilder(ra.length());
			boolean cr=false;
			for(char c:ra.toCharArray()) {
				if(c=='\t')sb.append("→");
				else if(c=='\r')cr=true;
				else if(c=='\n') {
					if(cr)sb.append("↵");
					else sb.append("↓");
					cr=false;
				}
				else if(cr)sb.append("←");
				else sb.append(c);
			}
			ra=sb.toString();
		}
		/*
		ra=ra.replaceAll("\t",Matcher.quoteReplacement(" → "));
		ra=ra.replaceAll("\r\n",Matcher.quoteReplacement("↲"));
		ra=ra.replaceAll("\r",Matcher.quoteReplacement("↲"));
		ra=ra.replaceAll("\n",Matcher.quoteReplacement("↲"));
		*/
		/*
		ra=ra.replaceAll("\t",Matcher.quoteReplacement(" \\t "));
		ra=ra.replaceAll("\r",Matcher.quoteReplacement(" \\r "));
		ra=ra.replaceAll("\n",Matcher.quoteReplacement(" \\n "));
		*/
		g.drawString(ra,getPanel().bodyX+getX(),getY()+getH());
		/*
		if(g instanceof GraphicsAWT) {
			((GraphicsAWT) g).g.fillOval(getPanel().bodyX+getX(),getY(),getH(),getH());
		}
		/*
		String ry=null;
		if(ra.length()>max) {
			ra=ra.substring(0,max);
			ry="(以下省略)";
		}
		int x,y;
		int ryW=g.drawString(ra,x=getPanel().bodyX+getX(),y=getY()+getH());
		if(ry!=null&&x+ryW<getWindow().canWidth())g.drawString(ry,x+ryW,y);
		*/
	}
	public String getData(){
		return data;
	}
	@Override
	public void openMenu(ArrayList<CanvasComponent> list) {
		super.openMenu(list);
		list.add(new CTMenu(getWindow(),"ファイルに書き出す"){
			public void click(){
				FileNameExtensionFilter filter=new FileNameExtensionFilter("テキスト形式","txt");
				JFileChooser fc=new JFileChooser((File)null);
				fc.setFileSelectionMode(JFileChooser.FILES_ONLY); // Modeで指定した物のみを選択可能にする
				fc.setDialogTitle("テキスト形式で保存");
				fc.addChoosableFileFilter(filter);
				fc.setFileFilter(filter);
				int ret=fc.showSaveDialog(null);
				if(ret!=JFileChooser.APPROVE_OPTION)return; // 何もせずに戻る
				File f=fc.getSelectedFile();
				try{
					Charset c=CelectEncode();
					if(c==null)return;
					OutputStream os=null;
					try{
						os=new FileOutputStream(f);
						os.write(getData().getBytes(c));
					}finally{
						if(os!=null) try{
							os.close();
						}catch(IOException e){
							e.printStackTrace();
						}
					}
				}catch(NullPointerException e) {

				}catch(Exception e) {
					e.printStackTrace();
					WindowLib.ErrorWindow("書き込みできませんでした。","失敗");
				}
			}
		});
		list.add(new CTMenu(getWindow(),"編集"){
			public void click(){
				editWindow();
			}
		});
		list.add(new CTMenu(getWindow(),"行で分割"){
			public void click(){
				if(lock) {
					WindowLib.InfoWindowStop("編集ウィンドウが開いています","分割失敗");
					return;
				}
				String[] arr=FileEditor.FE.StringToStrings(data);
				if(arr.length<2)return;
				HistoryGroup hg=new HistoryGroup();
				hg.add(new EditTextHistory(DataText.this,arr[0]));
				setData(arr[0], false);
				ArrayList<DataText> dtl=new ArrayList<DataText>();
				for(int i=1;i<arr.length;i++) {
					dtl.add(new DataText(getWindow(),arr[i]));
				}
				getWindow().ct.addData(dtl,false);
				hg.add(new HistoryAdd(dtl));
				getWindow().ct.addHistory(hg);
			}
		});
		if(decodeData!=null)list.add(new CTMenu(getWindow(),"URLを開く"){
			public void click(){
				try{
					Desktop.getDesktop().browse(new URI(data));
				}catch(Exception e){
					e.printStackTrace();
					WindowLib.ErrorWindow("開けませんでした。","失敗");
				}
			}
		});
	}
	public void editWindow(){
		getWindow().ct.hideMenu(null);
		if(lock) {
			WindowLib.InfoWindowStop("既に編集ウィンドウが開いています","編集ウィンドウ生成失敗");
			return;
		}
		try{
			lock=true;
			String ret=WindowLib.InputArrayWindow("テキスト編集",data);
			if(ret!=null) {
				setData(ret,true);
				getWindow().getEdit().saved=false;
				decodeData=null;
			}
		}catch(Exception e) {
			e.printStackTrace();
			WindowLib.ErrorWindow("編集できませんでした。","失敗");
		}finally {
			lock=false;
		}
	}
	@Override
	public String toString(){
		return super.toString()+"@data="+data;
	}
	/*バイナリデータ構造
	 * 1byte種類
	 * 4byteデータサイズ
	 * ?byte UTF-8文字データ
	 * */
	@Override
	public long write(DataOutputStream out)throws Exception{
		byte[] arr=data.getBytes(StandardCharsets.UTF_8);
		out.writeInt(arr.length);
		out.write(arr);
		return arr.length+4;
	}
	@Override
	public long read(DataInputStream in)throws Exception{
		int size=in.readInt();
		byte[] arr=new byte[size];
		size=in.read(arr);
		data=new String(arr,0,size,StandardCharsets.UTF_8);
		return size+4;
	}
	@Override
	public Transferable writeClip(Data[] arr){
		StringBuilder str=new StringBuilder();
		for(Data data:arr){
			if(data instanceof DataText) str.append(((DataText) data).getData());
		}
		//String str=((text) d).getData();
		return new StringSelection(str.toString());
	}
	public void setData(String d,boolean addHistory){
		if(addHistory)getWindow().ct.history.add(new EditTextHistory(this,d));
		data=d;
	}
}