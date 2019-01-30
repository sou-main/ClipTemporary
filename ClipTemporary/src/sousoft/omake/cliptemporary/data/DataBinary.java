package sousoft.omake.cliptemporary.data;

import java.awt.Color;
import java.awt.datatransfer.Transferable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import soulib.fileLib.FileEditor;
import soulib.net.http.HttpLib;
import soulib.windowLib.WindowLib;
import soulib.windowLib.CanvasComponents.CanvasComponent;
import sousoft.omake.cliptemporary.CTMenu;
import sousoft.omake.cliptemporary.CTWindow;

public class DataBinary extends Data{
	public static int id=4;
	@Override
	public int getID(){
		return id;
	}
	private File tmp;
	public static int bufSize=512;
	private byte[] mem;

	public DataBinary(CTWindow win){
		super(win,"binary");
	}
	public DataBinary(CTWindow window,File file){
		this(window);
		try{
			makeTempFile();
			FileEditor.FE.copyFileToFile(file,tmp);
		}catch(IOException e){
			tmp=null;
			throw new UncheckedIOException(e);
		}
	}
	public DataBinary(CTWindow window,byte[] data){
		this(window);
		if(data==null)return;
		if(data.length<8192)mem=data;
		FileOutputStream out=null;
		ByteArrayInputStream in=null;
		try{
			makeTempFile();
			in=new ByteArrayInputStream(data);
			out=new FileOutputStream(tmp);
			FileEditor.copy(in,out);
		}catch(IOException e){
			throw new UncheckedIOException(e);
		}finally {
			if(in!=null)HttpLib.close(in);
			if(out!=null)HttpLib.close(out);
			tmp=null;
		}
	}
	private byte[] getData(){
		if(mem!=null)return mem;
		if(tmp==null)return null;
		ByteArrayOutputStream out=new ByteArrayOutputStream();
		try {
			InputStream in=new FileInputStream(tmp);
			byte[] b=new byte[bufSize];
			int size;
			try {
				while((size=in.read(b))>0) {
					out.write(b,0,size);
				}
			}finally {
				in.close();
			}
		}catch(IOException e) {
			e.printStackTrace();
		}
		return out.toByteArray();
	}
	public void openMenu(ArrayList<CanvasComponent> list) {
		super.openMenu(list);
		list.add(new CTMenu(getWindow(), "文字読込"){
			public void click(){
				if(WindowLib.Yes_NoWindow("このバイナリをテキストとして読み込みますか?","確認")) {
					Charset c=CelectEncode();
					if(c==null)return;
					byte[] d=getData();
					if(d==null)return;
					String text=new String(d,c);
					getPanel().addData(new DataText(getWindow(),text));
				}
			}
		});
		list.add(new CTMenu(getWindow(), "大きさ"){
			public void click(){
				String massage;
				if(mem!=null)massage="メモリに"+mem.length+"バイト";
				else if(tmp!=null)massage="一時ファイルに"+tmp.length()+"バイト";
				else massage="データなし";
				WindowLib.InfoWindowStop(massage,"データサイズ");
			}
		});
	}
	private void makeTempFile() throws IOException{
		if(tmp==null) {
			File dir=new File(System.getProperty("java.io.tmpdir")+"/ClipTemporary");
			dir.mkdirs();
			tmp=File.createTempFile("Binary",".dat",dir);
		}
	}
	{
		name="バイナリ";
		setBackground(Color.orange);
	}
	public String getUsedTempFile() {
		if(tmp==null)return null;
		return tmp.getAbsolutePath();
	}
	@Override
	public long write(DataOutputStream out) throws Exception{
		if(mem==null&&tmp==null)return 0;
		InputStream in;
		if(mem!=null)in=new ByteArrayInputStream(mem);
		else in=new FileInputStream(tmp);
		byte[] b=new byte[bufSize];
		int size;
		long all=0;
		try {
			while((size=in.read(b))>0) {
				out.write(b,0,size);
				all+=size;
			}
		}finally {
			in.close();
		}
		return all;
	}

	@Override
	public long read(DataInputStream in) throws Exception{
		makeTempFile();
		FileOutputStream out=new FileOutputStream(tmp);
		byte[] b=new byte[bufSize];
		int size;
		long all=0;
		try {
			while((size=in.read(b))>0) {
				out.write(b,0,size);
				all+=size;
			}
		}finally {
			out.close();
		}
		return all;
	}
	@Override
	public Transferable writeClip(Data[] arr){
		return null;
	}
}