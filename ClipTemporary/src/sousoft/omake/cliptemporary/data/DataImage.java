package sousoft.omake.cliptemporary.data;

import static sousoft.omake.cliptemporary.ClipTemporary.*;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;

import javax.imageio.ImageIO;

import soulib.fileLib.FileEditor;
import soulib.windowLib.CommonGraphics;
import soulib.windowLib.WindowLib;
import soulib.windowLib.CanvasComponents.CanvasComponent;
import sousoft.omake.cliptemporary.CTMenu;
import sousoft.omake.cliptemporary.CTWindow;
import sousoft.omake.cliptemporary.ImageSelection;

public class DataImage extends Data{//TODO 画像データ
	public static int id=2;
	@Override
	public int getID(){
		return id;
	}
	private Image img;
	private File tmp;
	private String massage;
	private static boolean setUseCache=true;
	public static final int imageMaxViewSizeW=0;
	public static final int imageMaxViewSizeH=1;
	public DataImage(CTWindow panel,Image image){
		super(panel, "image");
		setImage(image);
	}
	private void setImage(Image image){
		if(getWindow().getTempFlag()) {
			//new Thread("writeImageToTempFile"){
				//public void run(){
					writeTempFile(image);
				//}
			//}.start();
		}else img=image;
	}
	@Override
	public String getUsedTempFile() {
		if(tmp==null)return null;
		return tmp.getAbsolutePath();
	}
	public void writeTempFile(Image image) {
		FileOutputStream os=null;
		try{
			long t=System.currentTimeMillis();
			File dir=new File(System.getProperty("java.io.tmpdir")+"/ClipTemporary");
			dir.mkdirs();
			File temp=File.createTempFile("Image",".png",dir);
			temp.deleteOnExit();
			if(image instanceof RenderedImage) ;
			else image=FileEditor.toBufferedImage(image,0,0);
			if(setUseCache)ImageIO.setUseCache(false);
			os=new FileOutputStream(temp);
			ImageIO.write((RenderedImage) image,"png",os);
			if(debug)System.out.println(temp.getAbsolutePath()+" 一時ファイルに書き込む"+(System.currentTimeMillis()-t));
			//getWindow().getWindowLog().addData("画像を一時ファイルに書き込みました");
			tmp=temp;
			if(getWindow().getCTMenuBar().getMinCopy()){
				int orgW=image.getWidth(null);
				int orgH=image.getHeight(null);
				int[] imgWH={orgW,orgH};
				double sa=getMinSize(imgWH,getWindow().imageViewSize);
				if(sa!=1){
					DecimalFormat df = new DecimalFormat("##0.0%");
					StringBuilder sb=new StringBuilder("縮小コピー").append(df.format(sa));
					sb.append(imgWH[0]).append("x").append(imgWH[1]);
					sb.append("(元の大きさ 幅");
					sb.append(orgW).append("px高さ");
					sb.append(orgH).append("px)");
					sb.append("一時ファイル=").append(temp.getName());
					massage=sb.toString();
				}
				image.flush();
				img=FileEditor.toBufferedImage(image,imgWH[0],imgWH[1]);
			}else 	image.flush();
		}catch(IOException e){
			e.printStackTrace();
		}finally{
			if(os!=null) try{
				os.close();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		System.gc();
	}
	public Image readTempFile() {
		FileInputStream is=null;
		try{
			if(setUseCache)ImageIO.setUseCache(false);
			is=new FileInputStream(tmp);
			return ImageIO.read(is);
		}catch(IOException e){
			e.printStackTrace();
			tmp=null;
		}finally{
			if(is!=null) try{
				is.close();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		return null;
	}
	@Override
	protected void finalize() {
		if(tmp!=null)tmp.delete();
		if(img!=null)img.flush();
	}
	public DataImage(CTWindow panel) throws Exception{
		super(panel, "image");
	}
	{
		name="画像";
		setBackground(Color.magenta);
	}
	public Image getMinImage() {
		return img;
	}
	public Image getData(){
		if(tmp==null)return img;
		if(tmp!=null)return readTempFile();
		return img;
	}
	@Override
	public void draw(CommonGraphics g,Point p){
		if(isInside(p)) {
			boolean y=false;
			if(img!=null) {
				int orgW=img.getWidth(null);
				int orgH=img.getHeight(null);
				int[] imgWH={orgW,orgH};
				double sa=getMinSize(imgWH,getWindow().imageViewSize);
				if(sa!=1){
					DecimalFormat df = new DecimalFormat("##0.0%");
					StringBuilder sb=new StringBuilder("縮小表示").append(df.format(sa));
					sb.append(imgWH[0]).append("x").append(imgWH[1]);
					sb.append("(元の大きさ 幅");
					sb.append(orgW).append("px高さ");
					sb.append(orgH).append("px)");
					massage=sb.toString();
				}else {
					sa=getMaxSize(imgWH,getWindow().imageZoomSize);
					if(sa!=1) {
						DecimalFormat df = new DecimalFormat("##0.0%");
						StringBuilder sb=new StringBuilder("拡大表示").append(df.format(sa));
						sb.append(imgWH[0]).append("x").append(imgWH[1]);
						sb.append("(元の大きさ 幅");
						sb.append(orgW).append("px高さ");
						sb.append(orgH).append("px)");
						massage=sb.toString();
					}else if(tmp==null)massage=null;
				}
				g.drawImage(img,getPanel().bodyX+getX(),getY(),imgWH[0],imgWH[1]);
			}else{
				massage="表示できません";
				y=true;
			}
			if(massage!=null) {
				g.setFont(getFont());
				g.setColor(Color.white);
				FontMetrics fm=WindowLib.getFontMetrics(getFont());
				int fh=fm.getHeight();
				g.fillRect(getPanel().bodyX+getX(),getY()-getH(),fm.stringWidth(massage),getH());
				g.setColor(Color.black);
				/*
				g.drawString("X"+(getPanel().bodyX+getX())+
						"Y"+(getY()-fh)+"W"+fm.stringWidth(massage)+"H"+fh+"P"+this.getPriority(),0,fh);
				*/
				g.drawString(massage,getPanel().bodyX+getX(),getY()+(y?fh:0));
			}
		}else if(getPanel().imageView&&img!=null) {
			int[] imgWH={img.getWidth(null),img.getHeight(null)};
			getMinSize(imgWH,new int[] {getH()*2,getH()*2});
			g.drawImage(img,getPanel().bodyX+getX(),getY(),imgWH[0],imgWH[1]);
		}
		super.draw(g,p);
	}
	/**縮小表示
	 * @return 表示倍率*/
	public double getMinSize(int[] wh,int[] maxWH) {
		double sa=1;
		if(wh[0]>maxWH[0]||wh[1]>maxWH[1]) {
			sa=maxWH[0]/(double)wh[0];
			sa=Math.min(sa,maxWH[1]/(double)wh[1]);
			wh[0]=(int) (wh[0]*sa);
			wh[1]=(int) (wh[1]*sa);
		}
		return sa;
	}
	/**拡大表示
	 * @return 表示倍率*/
	public double getMaxSize(int[] wh,int[] minWH) {
		double sa=1;
		if(wh[0]<minWH[0]||wh[1]<minWH[1]) {
			sa=minWH[0]/(double)wh[0];
			sa=Math.min(sa,minWH[1]/(double)wh[1]);
			wh[0]=(int) (wh[0]*sa);
			wh[1]=(int) (wh[1]*sa);
		}
		return sa;
	}
	@Override
	public void openMenu(ArrayList<CanvasComponent> list) {
		super.openMenu(list);
		list.add(new CTMenu(getWindow(),"編集"){
			public void click(){
				getWindow().getEditImagePanel().setPoint(getX(),DataImage.this.getY());
				getWindow().editImage(getData());
			}
		});
		list.add(new CTMenu(getWindow(),"ファイルに書き出す"){
			public void click(){
				writeImageGUI();
			}
		});
	}
	public void writeImageGUI() {
		ArrayList<String> l2=new ArrayList<String>();
		for(String s:ImageIO.getWriterFormatNames()) {
			if(l2.contains(s))continue;
			else if(l2.contains(s.toLowerCase(Locale.ENGLISH)))continue;
			else if(l2.contains(s.toUpperCase(Locale.ENGLISH)))continue;
			else l2.add(s.toLowerCase(Locale.ENGLISH));
		}
		String type=WindowLib.InputWindow("ファイル形式を選択",l2.toArray(new String[l2.size()]),"png");
		if(type==null)return;
		FileOutputStream out=null;
		try {
			File file=WindowLib.SaveFileWindow(new File("save."+type));
			if(file==null)return;
			out=new FileOutputStream(file);
			if(tmp!=null&&"png".equals(type)) {//一時ファイルが存在した場合そのまま出力に書き込む
				FileInputStream fis=null;
				try{
					fis=new FileInputStream(tmp);
					int size=0;
					byte[] b=new byte[512];
					while((size=fis.read(b))>0){
						out.write(b,0,size);
					}
					return;
				}catch(FileNotFoundException fnf){
					fnf.printStackTrace();
				}finally {
					if(fis!=null)fis.close();
				}
			}//一時ファイルが無かったりした場合メモリのデータを取得して書き込む
			Image data=getData();
			if(data==null)return;
			if(data instanceof RenderedImage&&"png".equals(type)) ;
			else{
				if("png".equals(type))data=FileEditor.toBufferedImage(data,0,0,BufferedImage.TYPE_4BYTE_ABGR);
				else{
					data=FileEditor.toBufferedImage(data,0,0,BufferedImage.TYPE_3BYTE_BGR);
				}
			}
			if(setUseCache)ImageIO.setUseCache(false);
			ImageIO.write((RenderedImage) data,type,out);
		}catch(IOException e){
			e.printStackTrace();
		}finally{
			if(out!=null) try{
				out.close();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
	}
	/*バイナリデータ構造
	 * 1byte種類
	 * 4byteデータサイズ
	 * ?byte PNG画像データ
	 * */
	@Override
	public long write(DataOutputStream out)throws Exception{
		if(tmp!=null) {//一時ファイルが存在した場合そのまま出力に書き込む
			FileInputStream fis=null;
			try{
				fis=new FileInputStream(tmp);
				out.writeInt((int)tmp.length());
				int size=0;
				int all=0;
				byte[] b=new byte[512];
				while((size=fis.read(b))>0){
					out.write(b,0,size);
					all+=size;
				}
				if(tmp.length()!=all)throw new Exception();
				return all+4;
			}catch(FileNotFoundException fnf){
				fnf.printStackTrace();
			}finally {
				if(fis!=null)fis.close();
			}
		}//一時ファイルが無かったりした場合メモリのデータを取得して書き込む
		ByteArrayOutputStream bos=new ByteArrayOutputStream();
		Image data=getData();
		if(data==null)return 0;
		if(data instanceof RenderedImage) ;
		else data=FileEditor.toBufferedImage(data,0,0);
		if(setUseCache)ImageIO.setUseCache(false);
		ImageIO.write((RenderedImage) data,"png",bos);
		out.writeInt(bos.size());
		bos.writeTo(out);
		return bos.size()+4;
	}
	@Override
	public long read(DataInputStream in)throws Exception{
		int size=in.readInt();
		byte[] data=new byte[size];
		size=in.read(data);
		ByteArrayInputStream bis=new ByteArrayInputStream(data,0,size);
		setImage(ImageIO.read(bis));
		return size+4;
	}
	public boolean isNull(){
		return img==null&&tmp==null;
	}
	@Override
	public Transferable writeClip(Data[] arr){
		return new ImageSelection(getData());
	}
}