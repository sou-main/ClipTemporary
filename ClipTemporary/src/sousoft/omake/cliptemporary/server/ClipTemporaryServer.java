package sousoft.omake.cliptemporary.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;

import soulib.fileLib.FileEditor;
import soulib.lib.MapConfig;

public class ClipTemporaryServer extends Thread{

	public static String saveDir=FileEditor.USERHOME+"/sou/ClipTemporaryServer/";
	public static void main(String[] args){
		MapConfig cf=new MapConfig(args);
		saveDir=cf.getConfigDataString("saveDir",saveDir);
	}
	private Socket soc;
	private boolean exists;
	public ClipTemporaryServer(Socket s) {
		soc=s;
	}
	/*共通初期処理
	 * 1バイト読み込み。モード、0書き込み、1読み込み
	 * 4バイト読み込み。場所の名前バイナリの長さ
	 * ｎバイト読み込み。場所の名前バイナリ、UTF8エンコード
	 * 1バイト書き込み。存在するか。Boolean
	 * */
	public void run() {
		try {
			InputStream is=soc.getInputStream();
			int mode=is.read();
			if(mode<0)return;
			
			DataInputStream dis=new DataInputStream(is);
			int size=dis.readInt();
			byte[] b=new byte[size];
			dis.readFully(b,0,size);
			String key=new String(b,StandardCharsets.UTF_8);
			key=key.replaceAll(Matcher.quoteReplacement("\\"),"-");
			key=key.replaceAll(Matcher.quoteReplacement("/"),"-");
			File f=new File(saveDir,key);
			OutputStream os=soc.getOutputStream();
			DataOutputStream dos=new DataOutputStream(os);
			dos.writeBoolean(exists=f.exists());
			
			if(mode==0)write(dos, key);
			else if(mode==1)read(dis,key);
			
		}catch(IOException e){
			e.printStackTrace();
		}finally {
			try{
				soc.close();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
	}
	public void write(DataOutputStream os,String key) {
		File f=new File(saveDir,key);
		if(exists)f.delete();
		//TODO 作りかけ
	}
	public void read(DataInputStream is,String key) {
		//TODO 作りかけ
	}
}