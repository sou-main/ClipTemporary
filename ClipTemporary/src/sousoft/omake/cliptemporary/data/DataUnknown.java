package sousoft.omake.cliptemporary.data;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import soulib.lib.DataEditor;
import soulib.windowLib.WindowLib;
import soulib.windowLib.CanvasComponents.CanvasComponent;
import sousoft.omake.cliptemporary.CTMenu;
import sousoft.omake.cliptemporary.CTWindow;

public class DataUnknown extends Data implements Transferable{

	public static int id=3;
	@Override
	public int getID(){
		return id;
	}
	private Object[] data;
	private DataFlavor[] dataflavor;
	public DataUnknown(CTWindow win,Transferable t){
		super(win,"unknown");
		dataflavor=t.getTransferDataFlavors();
		data=new Object[dataflavor.length];
		for(int i=0;i<dataflavor.length;i++) {
			try{
				data[i]=t.getTransferData(dataflavor[i]);
			}catch(UnsupportedFlavorException e){
				//ここは呼ばれないはず
				e.printStackTrace();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
	}
	public DataUnknown(CTWindow win){
		super(win,"unknown");
	}
	{
		name="謎データ";
	}
	public void openMenu(ArrayList<CanvasComponent> list) {
		super.openMenu(list);
		list.add(new CTMenu(getWindow(), "データタイプ"){
			public void click(){
				String[] type=new String[Math.min(dataflavor.length,10)];
				for(int i=0;i<type.length;i++) {
					if(i==9) {
						type[i]="その他"+(dataflavor.length-9)+"種類";
					}else {
						type[i]=dataflavor[i].toString();
						int index=dataflavor[i].getClass().getName().length();
						type[i]=type[i].substring(index+1,type[i].length()-1);
					}
				}
				WindowLib.InfoWindow("データタイプ",type);
			}
		});
	}
	/*バイナリデータ構造
	 * 1byte種類
	 * 4byte
	 * */
	@Override
	public long write(DataOutputStream out) throws Exception{
		byte[] arr;
		out.writeInt(data.length);
		long size=4;
		for(int i=0;i<data.length;i++) {
			arr=DataEditor.toByte(dataflavor[i]);
			out.writeInt(arr.length);
			size+=4;
			out.write(arr);
			size+=arr.length;

			arr=DataEditor.toZipByte(data[i]);
			out.writeInt(arr==null?0:arr.length);
			size+=4;
			if(arr==null)continue;
			out.write(arr);
			size+=arr.length;
		}
		return size;
	}
	@Override
	public long read(DataInputStream in) throws Exception{
		long read=4;
		byte[] d;
		int types=in.readInt();
		dataflavor=new DataFlavor[types];
		data=new Object[types];
		int size;
		for(int i=0;i<types;i++){
			size=in.readInt();
			read+=4;
			d=new byte[size];
			read+=in.read(d);
			dataflavor[i]=(DataFlavor) DataEditor.toObject(d);

			size=in.readInt();
			read+=4;
			if(size<1)continue;
			d=new byte[size];
			read+=in.read(d);
			data[i]=DataEditor.toObjectZip(d);
		}
		return read;
	}
	@Override
	public synchronized DataFlavor[] getTransferDataFlavors(){
		return dataflavor;
	}
	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor){
		if(flavor==null)return false;
		for(DataFlavor df:dataflavor) {
			if(flavor.equals(df))return true;
		}
		return false;
	}
	@Override
	public synchronized Object getTransferData(DataFlavor flavor)
			throws UnsupportedFlavorException,IOException{
		if(flavor==null)return false;
		int index=-1;
		for(int i=0;i<dataflavor.length;i++) {
			if(flavor.equals(dataflavor[i])) {
				index=i;
				break;
			}
		}
		if(index<0||index>=data.length)throw new UnsupportedFlavorException(flavor);
		return data[index];
	}
	@Override
	public Transferable writeClip(Data[] arr){
		return this;
	}
}