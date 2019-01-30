package sousoft.omake.cliptemporary;

import static sousoft.omake.cliptemporary.ClipTemporary.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import soulib.angou.CommonKeyAngou;
import soulib.fileLib.FileEditor;
import soulib.lib.DataEditor;
import soulib.windowLib.StandardWindow;
import soulib.windowLib.WindowLib;
import sousoft.omake.cliptemporary.data.Data;
import sousoft.omake.cliptemporary.data.DataFile;
import sousoft.omake.cliptemporary.data.DataText;

public class EditFile{
	public ClipTemporaryPanel ct;
	public CTWindow window;
	private String filePath;
	public boolean cancel;
	public static HashMap<Integer,Class<? extends Data>> registry=new HashMap<Integer,Class<? extends Data>>();
	public String getFilePath(){
		return filePath;
	}
	private void setFilePath(String filePath){
		this.filePath=filePath;
	}
	public boolean saved=true;
	public EditFile(CTWindow win){
		this.ct=win.ct;
		window=win;
	}
	public void readFile(){
		FileNameExtensionFilter fnef=new FileNameExtensionFilter("専用拡張子",ClipTemporary.ex[0]);
		FileNameExtensionFilter fnef2=new FileNameExtensionFilter("専用拡張子(暗号化)",ClipTemporary.ex[1]);
		StandardWindow.setLookAndFeel(null,null);
		//File f=WindowLib.OpenFileWindow(null,JFileChooser.FILES_ONLY,fnef);
		File f=new File(".");
		JFileChooser fc=new JFileChooser(f);
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setDialogTitle("開く");
		fc.addChoosableFileFilter(fnef);
		fc.addChoosableFileFilter(fnef2);
		fc.setFileFilter(fnef);
		int ret=fc.showOpenDialog(null);
		if(ret!=JFileChooser.APPROVE_OPTION)f=null;
		else f=fc.getSelectedFile();
		readFile(f);
	}
	public void readFile(File f){
		readFile(f,false);
	}
	private InputStream readcom(File f,boolean c) throws FileNotFoundException, CancelException{
		boolean r=ct.data.isEmpty();
		if(r||c){
			//Skip MassageAndDelete
		}else{
			Boolean b=WindowLib.Yes_No_CancelWindow("上書き",
					"Yesは上書き読み込み",
					"Noは追加読み込み",
					"Cancelは取り消し");
			if(b==null)throw new CancelException();
			if(r=b){
				ct.data.clear();
				ct.removeAllCanvasComponent();
			}
		}
		setFilePath(f.getAbsolutePath());
		window.setMassage("読み込中 NowLoading");
		return new FileInputStream(f);
	}
	/**
	 * @param c
	 *            NOcheck
	 */
	public void readFile(File f,boolean c){
		if(f==null||!f.exists()||!f.isFile()) return;
		if(f.getAbsolutePath().endsWith(".aes")) {
			readFile(f,c,null);
			return;
		}
		try{
			new read(readcom(f,c)).start();
		}catch(Exception e){
			window.setMassage("読み込み失敗 Load Failed");
			e.printStackTrace();
		}
	}
	/**
	 * 暗号化
	 *
	 * @param c
	 *            NOcheck
	 * @param pass
	 *            暗号化キー
	 */
	public void readFile(File f,boolean c,String pass){
		if(f==null||!f.exists()||!f.isFile()) return;
		InputStream is=null;
		try{
			is=readcom(f,c);
			if(pass==null||pass.isEmpty()) {
				pass=WindowLib.InputWindow("パスワード入力");
			}
			if(pass==null)throw new CancelException();
			CommonKeyAngou cka=new CommonKeyAngou(pass);
			is=cka.inStream(is);
			byte[] k="ClipTemp".getBytes(StandardCharsets.UTF_8);
			byte[] b=new byte[k.length];
			int len=is.read(b);
			if(len!=k.length||!Arrays.equals(b,k))throw new Exception("パスワードが間違っています");
			new read(is).start();
		}catch(Exception e){
			try{
				if(is!=null)is.close();
			}catch(IOException e1){
				e1.printStackTrace();
			}
			window.setMassage("読み込み失敗 Load Failed - "+e.getLocalizedMessage());
			e.printStackTrace();
		}
	}
	private class read extends Thread{
		private InputStream is=null;
		public read(InputStream i){
			is=i;
		}
		@Override
		public void run(){
			try{
				boolean r=ct.data.isEmpty();
				readFile(is);
				if(r) saved=true;
				else saved=false;
			}catch(CancelException e){
				window.setMassage("読み込み中断");
			}catch(Exception e){
				e.printStackTrace();
				window.setMassage("読み込み失敗 Load Failed");
				String[] arr= { "何故か読み込めません", "理由"+e.toString(), "詳細情報を保存しますか?" };
				if(JOptionPane.showConfirmDialog(null,arr,"読み込み失敗",JOptionPane.YES_NO_OPTION,
						JOptionPane.WARNING_MESSAGE)==0){
					saveErrMassage(e);
				}
			}finally{
				try{
					if(is!=null) is.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		}
	}
	private class CancelException extends Exception{
		public CancelException(){
			super("ユーザーによって取り消しされました");
			cancel=false;
		}
	}
	public long readFile(InputStream is) throws Exception{
		//deleteTemp();
		DataInputStream dis=new DataInputStream(is);
		ArrayList<Data> read=null;
		if(window.getConfig().getConfigDataBoolean("一括読み取り処理",true)) read=new ArrayList<Data>();
		else saved=false;
		long size=0;
		try{
			while(true){
				int id=dis.read();
				if(id<0)break;
				if(id==255) {
					id=dis.readInt();
				}
				Data d=null;
				Class<? extends Data> c=registry.get(id);
				if(c==null)continue;
				try{
					Constructor<? extends Data> co=c.getConstructor(CTWindow.class);
					d=co.newInstance(window);
				}catch(NoSuchMethodException e) {
					continue;
				}
				/*
				switch(type){
					case id_text:
						d=new DataText(window);
						break;
					case id_image:
						d=new DataImage(window);
						break;
					case id_file:
						d=new DataFile(window);
						break;
					case id_unknown:
						d=new DataUnknown(window);
						break;
				}
				*/
				if(d!=null){
					size+=d.read(dis);
					if(read!=null) read.add(d);
					else ct.addData(d);
				}
				StringBuilder sb=new StringBuilder("読み込中 NowLoading@読み込み済");
				window.setMassage(DataEditor.format(size,sb,1024).append("byte").toString());
				if(cancel) throw new CancelException();
			}
		}catch(EOFException e){

		}
		StringBuilder sb=new StringBuilder("読み込み成功 Successfully Loaded@");
		window.setMassage(DataEditor.format(size,sb,1024).append("byte").toString());
		if(read!=null) ct.addData(read);
		return size;
	}
	public void writeFile(boolean uwagaki,boolean angou){
		if(uwagaki&&filePath!=null){
			File f=new File(filePath);
			if(f.exists()){
				f.delete();
				writeFile(f,false);
				return;
			}
		}
		FileNameExtensionFilter fnef=new FileNameExtensionFilter("専用拡張子",ex);
		StandardWindow.setLookAndFeel(null,null);
		String name=filePath!=null ? filePath : new File("無題."+ex[0]).getAbsolutePath();
		if(angou&&!name.endsWith(".aes"))name+=".aes";
		File f=WindowLib.SaveFileWindow(new File(name),JFileChooser.FILES_ONLY,fnef);
		if(f==null) return;
		String fp=f.getAbsolutePath();
		if(f.getName().indexOf(".")==-1) fp+="."+ex[0];
		if(angou&&!fp.endsWith(".aes"))fp+=".aes";
		writeFile(new File(fp),true);
	}
	private OutputStream writecom(File f,boolean check) throws FileNotFoundException, CancelException {
		if(check&&f.exists()){
			boolean b=WindowLib.Yes_NoWindow("上書き","ファイルが既に存在します","上書きしますか?");
			if(b) f.delete();
			else throw new CancelException();
		}
		setFilePath(f.getAbsolutePath());
		window.setMassage("書き込中 NowSaveing");
		return new FileOutputStream(f);
	}
	public void writeFile(File f,boolean check){
		if(f==null) return;
		if(f.getAbsolutePath().endsWith(".aes")) {
			writeFile(f,check,null);
			return;
		}
		try{
			new write(writecom(f,check)).start();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public void writeFile(File f,boolean check,String pass){
		if(f==null) return;
		OutputStream out=null;
		try{
			out=writecom(f,check);
			if(pass==null||pass.isEmpty()) {
				pass=WindowLib.InputWindow("パスワード入力");
			}
			if(pass==null)throw new CancelException();
			CommonKeyAngou cka=new CommonKeyAngou(pass);
			out=cka.outStream(out);
			out.write("ClipTemp".getBytes(StandardCharsets.UTF_8));
			new write(out).start();
		}catch(Exception e){
			if(out!=null) try{
				out.close();
			}catch(IOException e1){
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}

	private class write extends Thread{
		private OutputStream out=null;
		public write(OutputStream o){
			out=o;
		}
		@Override
		public void run(){
			try{
				writeFile(out);
				//window.setMassage("書き込み成功 Successfully Saved");
			}catch(CancelException e){
				window.setMassage("書き込み中断");
			}catch(Exception e){
				e.printStackTrace();
				window.setMassage("書き込み失敗 Failed Save");
				String[] arr= { "何故かファイルに書き込めません", "理由"+e.toString(), "詳細情報を保存しますか?" };
				if(JOptionPane.showConfirmDialog(null,arr,"保存失敗",JOptionPane.YES_NO_OPTION,
						JOptionPane.WARNING_MESSAGE)==0){
					saveErrMassage(e);
				}
			}finally{
				try{
					if(out!=null) out.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		}
	}
	public void saveErrMassage(Exception e){
		File err=WindowLib.SaveFileWindow(new File("err.txt"));
		if(err!=null){
			StringWriter sw=new StringWriter();
			PrintWriter pw=new PrintWriter(sw);
			pw.println("SaveFileErrorMassage 保存失敗の詳細情報");
			pw.println("Version"+ClipTemporary.version.toString());
			SimpleDateFormat format=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss SSS");
			pw.print("time");
			pw.print(format.format(new Date()));
			format=new SimpleDateFormat(" yyyy年MM月dd日HH時mm分ss.SSS秒");
			pw.println(format.format(new Date()));
			e.printStackTrace(pw);
			FileEditor.FE.SaveFile(sw.toString(),err);
		}
	}
	public long writeFile(OutputStream out) throws Exception{
		long size=0;
		DataOutputStream dos=new DataOutputStream(out);
		for(Data d:ct.data){
			//if(!registry.containsValue(d.getClass()))continue;
			int id=d.getID();
			if(id>=255) {
				dos.write(255);
				dos.writeInt(id);
			}else dos.write(id);
			/*
			if(d instanceof DataText){
				dos.writeByte(id_text);
			}else if(d instanceof DataImage){
				dos.writeByte(id_image);
			}else if(d instanceof DataFile){
				dos.writeByte(id_file);
			}else if(d instanceof DataUnknown){
				dos.writeByte(id_unknown);
			}
			*/
			size+=d.write(dos);
			StringBuilder sb=new StringBuilder("書き込中 NowSaveing @書き込み済");
			window.setMassage(DataEditor.format(size,sb,1024).append("byte").toString());
			if(cancel) throw new CancelException();
		}
		StringBuilder sb=new StringBuilder("書き込み成功 Successfully Saved@");
		window.setMassage(DataEditor.format(size,sb,1024).append("byte").toString());
		saved=true;
		return size;
	}
	public void writeText(){
		FileNameExtensionFilter fnef=new FileNameExtensionFilter("テキスト形式","txt");
		StandardWindow.setLookAndFeel(null,null);
		String name=filePath!=null ? filePath : new File("無題.txt").getAbsolutePath();
		if(!name.endsWith(".txt")){
			int index=name.indexOf(".");
			if(index>=0) name=name.substring(0,index);
			name+=".txt";
		}
		File f0=WindowLib.SaveFileWindow(new File(name),JFileChooser.FILES_ONLY,fnef);
		if(f0==null) return;
		String fp=f0.getAbsolutePath();
		if(f0.getName().indexOf(".")==-1) fp+=".txt";
		final File f=new File(fp);
		if(f.exists()){
			boolean b=WindowLib.Yes_NoWindow("上書き","ファイルが既に存在します","上書きしますか?");
			if(b) f.delete();
			else return;
		}
		window.setMassage("書き込中 NowSaveing");
		new Thread(){
			@Override
			public void run(){
				OutputStream out=null;
				try{
					out=new FileOutputStream(f);
					writeText(out);
				}catch(CancelException e){
					window.setMassage("書き込み中断");
				}catch(Exception e){
					e.printStackTrace();
					window.setMassage("書き込み失敗 Failed Save");
					String[] arr= { "何故かファイルに書き込めません", "理由"+e.toString(), "詳細情報を保存しますか?" };
					if(JOptionPane.showConfirmDialog(null,arr,"保存失敗",JOptionPane.YES_NO_OPTION,
							JOptionPane.WARNING_MESSAGE)==0){
						saveErrMassage(e);
					}
				}finally{
					try{
						if(out!=null) out.close();
					}catch(IOException e){
						e.printStackTrace();
					}
				}
			}
		}.start();
	}
	public long writeText(OutputStream out) throws Exception{
		long size=0;
		boolean isFullPath=window.getCTMenuBar().isFullPath();
		for(Data d:ct.data){
			if(d instanceof DataText){
				String s=((DataText) d).getData()+System.lineSeparator();
				byte[] b=s.getBytes(StandardCharsets.UTF_8);
				out.write(b);
				size+=b.length;
			}else if(d instanceof DataFile){
				StringBuilder sb=new StringBuilder();
				File f=((DataFile) d).getFile();
				if(isFullPath)sb.append(f.getAbsolutePath());
				else sb.append(f.getName());
				sb.append(System.lineSeparator());
				byte[] b=sb.toString().getBytes(StandardCharsets.UTF_8);
				out.write(b);
				size+=b.length;
			}else{
				continue;
			}
			StringBuilder sb=new StringBuilder("書き込中 NowSaveing @書き込み済");
			window.setMassage(DataEditor.format(size,sb,1024).append("byte").toString());
			if(cancel) throw new CancelException();
		}
		StringBuilder sb=new StringBuilder("書き込み成功 Successfully Saved@");
		window.setMassage(DataEditor.format(size,sb,1024).append("byte").toString());
		return size;
	}
	public boolean FileExists(){
		if(filePath==null) return false;
		File f=new File(filePath);
		return f.exists()&&f.isFile();
	}
	public File uwagakiFile(){
		if(filePath==null) return null;
		return new File(filePath);
	}
	public static void deleteTemp(){
		File temp=new File(System.getProperty("java.io.tmpdir")+"/ClipTemporary/");
		if(!temp.isDirectory())return;
		for(File f:temp.listFiles()){
			if(f.isFile()){
				if(!isUsed(f)) f.delete();
			}
		}
	}
	private static boolean isUsed(File f) {
		for(CTWindow w:ClipTemporary.openWindowList) {
			for(Data d:w.ct.data){
				String s=d.getUsedTempFile();
				if(f.getAbsolutePath().equals(s)){
					return true;
				}
			}
		}
		return false;
	}
}
