package sousoft.omake.cliptemporary.data;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ServiceLoader;

import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;

import soulib.fileLib.FileEditor;
import soulib.windowLib.CommonGraphics;
import soulib.windowLib.WindowLib;
import soulib.windowLib.CanvasComponents.CanvasComponent;
import sousoft.omake.cliptemporary.CTMenu;
import sousoft.omake.cliptemporary.CTWindow;
import sousoft.omake.cliptemporary.ClipTemporary;
import sousoft.omake.cliptemporary.FileSelection;
import sousoft.omake.cliptemporary.musicplayer.ISoundBase;
import sousoft.omake.cliptemporary.musicplayer.ISoundBase.BasicPlayer;
import sousoft.omake.cliptemporary.musicplayer.ISoundBase.MP3Player;
import sousoft.omake.cliptemporary.musicplayer.Line;
import sousoft.omake.cliptemporary.musicplayer.MusicPlayer;

public class DataFile extends Data{//TODO ファイルデータ
	public static int id=1;
	@Override
	public int getID(){
		return id;
	}
	private File file;
	private String path,Filename;
	private boolean exists;
	private File binary;
	public Line sound;
	public DataFile(CTWindow panel,File file){
		super(panel, "file");
		if(!file.isAbsolute()) {
			this.file=file.getAbsoluteFile();
		}else this.file=file;
		if(file!=null)exists=file.exists();
	}
	/**ファイルから読み込むときに使われる*/
	public DataFile(CTWindow panel) throws Exception{
		super(panel, "file");
	}
	public File getData(){
		if(!exists&&binary!=null) {
			try{
				File to=new File(binary.getParentFile(),file.getName());
				Files.move(binary.toPath(),to.toPath());
				return binary=to;
			}catch(IOException e){
				e.printStackTrace();
			}
			return binary;
		}
		return file;
	}
	{
		name="ファイル";
		setBackground(Color.green);
	}
	private void setBackgroundF(Color c) {
		this.setBackground(c);
	}
	@Override
	public void draw(CommonGraphics g,Point p){
		super.draw(g,p);
		if(exists)g.setColor(WindowLib.darkPaple);
		else g.setColor(Color.red);
		if(isInside(p))g.setFont(this.getWindow().getBigFont());
		if(Filename==null) {
			path=file.getAbsolutePath();
			Filename=file.getName();
		}
		boolean isFullPath=this.getWindow().getCTMenuBar().isFullPath();
		g.drawString(isFullPath?path:Filename,getPanel().bodyX+getX(),getY()+getH());
	}
	@Override
	public String toString(){
		return super.toString()+"@path="+file.getAbsolutePath();
	}
	@Override
	public void openMenu(ArrayList<CanvasComponent> list) {
		super.openMenu(list);
		list.add(new CTMenu(getWindow(),"パスをコピー"){
			public void click(){
				if(file==null)return;
				try{
					Clipboard clipboard=Toolkit.getDefaultToolkit().getSystemClipboard();
					clipboard.setContents( new StringSelection(file.getAbsolutePath()),null);
				}catch(Exception e) {

				}
			}
		});
		list.add(new CTMenu(getWindow(),"開く"){
			public void click(){
				try{
					Desktop d=Desktop.getDesktop();
					if(file.isDirectory()||WindowLib.Yes_NoWindow("確認","このファイルを開きますか?",file.getAbsolutePath(),file.getName())) {
						try{
							d.open(file);
						}catch(IOException e) {
							WindowLib.InfoWindow("ファイルを開けません","失敗");
						}
					}
				}catch(Exception e) {

				}
			}
		});
		/*
		list.add(new CTMenu(getWindow(), "画像読込"){
			public void click(){
				try{
					if(WindowLib.Yes_NoWindow("確認","このファイルを画像として読み込みますか?",file.getAbsolutePath(),file.getName())) {
						Image img= ImageIO.read(file);
						getPanel().addData(new DataImage(getWindow(),img));
					}
				}catch(Exception e) {
					WindowLib.ErrorWindow("非対応データです","読み取り失敗");
				}
			}
		});
		list.add(new CTMenu(getWindow(),"文字読込"){
			public void click(){
				if(WindowLib.Yes_NoWindow("確認","このファイルをテキストとして読み込みますか?",file.getAbsolutePath(),file.getName()))try{
					Charset c=CelectEncode();
					if(c==null)return;
					String text;
					InputStream is=null;
					try{
						is=new FileInputStream(file);
						text=new String(FileEditor.toByteArray(is),c);
					}finally{
						if(is!=null) try{
							is.close();
						}catch(IOException e){
							e.printStackTrace();
						}
					}
					getPanel().addData(new DataText(getWindow(),text));
				}catch(NullPointerException e) {

				}catch(Exception e) {
					e.printStackTrace();
					WindowLib.ErrorWindow("非対応データです","読み取り失敗");
				}
			}
		});
		*/
		list.add(new CTMenu(getWindow(), binary==null?"バイナリ読込":"バイナリ無読込"){
			public void click(){
				if(binary==null) {
					if(!file.exists()) {
						exists=false;
						WindowLib.WarningWindow("ファイルが存在しません","ファイルが存在しません");
						return;
					}else exists=true;
					if(file.isFile()&&WindowLib.Yes_NoWindow("確認","このファイルのデータを読み込みますか?",file.getAbsolutePath(),file.getName())) {
						File dir=new File(System.getProperty("java.io.tmpdir")+"/ClipTemporary");
						dir.mkdirs();
						try{
							binary=File.createTempFile("File",".dat",dir);
							FileEditor.FE.copyFileToFile(file,binary);
							getWindow().getEdit().saved=false;
						}catch(IOException e){
							e.printStackTrace();
						}
					}
				}else {
					if(WindowLib.Yes_NoWindow("確認","このファイルをファイルパスとして読み込みますか?",file.getAbsolutePath(),file.getName())) {
						binary.delete();
						getWindow().getEdit().saved=false;
						binary=null;
					}
				}
				if(binary!=null)setBackgroundF(Color.orange);
				else setBackgroundF(Color.green);
			}
		});
		list.add(new CTMenu(getWindow(),"場所を開く"){
			public void click(){
				try{
					Desktop d=Desktop.getDesktop();
					File dir=file.getParentFile();
					if(dir!=null&&dir.isDirectory()) {
						try{
							d.open(dir);
						}catch(IOException e) {
							WindowLib.InfoWindow("ファイルを開けません","失敗");
						}
					}
				}catch(Exception e) {

				}
			}
		});
		if(isSound())list.add(new CTMenu(getWindow(), sound==null||sound.sb==null?"音楽再生":"再生終了"){
			public void click(){
				try{
					MusicPlayer mp=getWindow().musicPlayer;
					if(sound==null||sound.sb==null) {
						ISoundBase sb=getSound();
						if(sb==null)return;
						sound=mp.add(sb,Filename,DataFile.this);
						sb.play();
					}else if(sound.sb!=null){
						sound.sb.End();
						mp.remove(sound);
						sound=null;
					}
				}catch(Exception e) {
					e.printStackTrace();
					WindowLib.ErrorWindow("非対応データです","再生失敗");
				}
			}
		});
	}
	public void binary(boolean b) {
		if(!b&&binary==null)return;
		if(b&&binary!=null)return;
		if(binary==null) {
			if(!file.exists()) {
				exists=false;
				//WindowLib.WarningWindow("ファイルが存在しません","ファイルが存在しません");
				return;
			}else exists=true;
			if(file.isFile()) {
				File dir=new File(System.getProperty("java.io.tmpdir")+"/ClipTemporary");
				dir.mkdirs();
				try{
					binary=File.createTempFile("File",".dat",dir);
					FileEditor.FE.copyFileToFile(file,binary);
					getWindow().getEdit().saved=false;
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		}else {
			binary.delete();
			getWindow().getEdit().saved=false;
			binary=null;
		}
		if(binary!=null)setBackgroundF(Color.orange);
		else setBackgroundF(Color.green);
	}
	public void playSound() throws Exception{
		MusicPlayer mp=getWindow().musicPlayer;
		ISoundBase sb=getSound();
		if(sb==null)return;
		sound=mp.add(sb,Filename,DataFile.this);
		sb.play();
	}
	public boolean isSound() {
		try{
			ArrayList<AudioFileReader> arrayList = new ArrayList<AudioFileReader>(7);
			PrivilegedAction<Iterator<AudioFileReader>> privilegedAction = new PrivilegedAction<Iterator<AudioFileReader>>(){
				@Override
				public Iterator<AudioFileReader> run() {
					return ServiceLoader.load(AudioFileReader.class).iterator();
				}
			};
			final Iterator<AudioFileReader> iterator = (Iterator<AudioFileReader>)AccessController.doPrivileged(privilegedAction);
			PrivilegedAction<Boolean> privilegedAction2 = new PrivilegedAction<Boolean>(){
				@Override
				public Boolean run() {
					return iterator.hasNext();
				}
			};
			while (((Boolean)AccessController.doPrivileged(privilegedAction2)).booleanValue()) {
				try {
					AudioFileReader e = iterator.next();
					if (!AudioFileReader.class.isInstance(e)) continue;
					arrayList.add(0, e);
				}
				catch (Throwable throwable) {}
			}
			for(int i=0;i<arrayList.size();i++) {
				AudioFileReader r=arrayList.get(i);
				try{
					r.getAudioFileFormat(getData());
					//AudioInputStream stream=r.getAudioInputStream(getData());
					//stream.close();
					if(ClipTemporary.debug)System.out.println("AudioReader "+r.getClass().getName());
					return true;
				}catch(UnsupportedAudioFileException e) {

				}
				//System.out.println(r.getClass());
			}
		}catch(FileNotFoundException f) {
			return false;
		}catch(Throwable t) {
			t.printStackTrace();
		}
		if(path.endsWith(".mp3")) {
			if(ClipTemporary.debug)System.out.println("AudioReader javazoom.jl.decoder.Decoder");
			return true;
		}else return false;
		//return path.endsWith(".mid")||path.endsWith(".wav")||path.endsWith(".mp3")||path.endsWith(".aiff");
	}
	public ISoundBase getSound() throws Exception{
		if(!isSound())return null;
		if(path.endsWith(".mp3")) {
			return new MP3Player(getData(), getWindow());
		}else return new BasicPlayer(getData(), getWindow());
	}
	@Override
	public String getUsedTempFile() {
		if(binary==null)return null;
		return binary.getAbsolutePath();
	}
	/*バイナリデータ構造
	 * 1byte種類
	 * 4byteデータサイズ
	 * ?byte UTF8文字データ(ファイルパス)
	 * */
	@Override
	public long write(DataOutputStream out)throws Exception{
		long all=0;
		byte[] arr=file.getAbsolutePath().getBytes(StandardCharsets.UTF_8);
		if(binary!=null) {
			out.writeInt(arr.length*-1);
			out.writeLong(binary.length());
			FileInputStream in=new FileInputStream(binary);
			byte[] b=new byte[512];
			int size;
			try {
				while((size=in.read(b))>0) {
					out.write(b,0,size);
					all+=size;
				}
			}finally {
				in.close();
			}
		}else out.writeInt(arr.length);
		out.write(arr);
		return arr.length+4+all;
	}
	@Override
	public long read(DataInputStream in) throws Exception{
		long allD=0;
		int size=in.readInt();
		if(size<0) {
			File dir=new File(System.getProperty("java.io.tmpdir")+"/ClipTemporary");
			dir.mkdirs();
			binary=File.createTempFile("File",".dat",dir);
			FileOutputStream out=new FileOutputStream(binary);
			byte[] b=new byte[512];
			int re;
			long all=in.readLong();
			long nokori=all;
			try {
				while(true) {
					if(b.length<nokori)re=b.length;
					else re=(int) nokori;
					if(re<=0)break;
					re=in.read(b,0,re);
					if(re<=0)break;
					out.write(b,0,re);
					nokori-=re;
					allD+=re;
				}
			}finally {
				out.close();
			}
		}
		byte[] arr=new byte[Math.abs(size)];
		size=in.read(arr);
		String path=new String(arr,0,size,StandardCharsets.UTF_8);
		file=new File(path);
		if(file!=null)exists=file.exists();
		if(binary!=null)setBackgroundF(Color.orange);
		else setBackgroundF(Color.green);
		if(!exists)setBackgroundF(getBackground().darker());
		return size+4+allD;
	}
	public File getFile(){
		return file;
	}
	@Override
	public Transferable writeClip(Data[] arr){
		ArrayList<File> file=new ArrayList<File>();
		for(Data data:arr){
			if(data instanceof DataFile) file.add(((DataFile) data).getData());
		}
		return new FileSelection(file);
	}
}
