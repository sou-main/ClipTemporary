package sousoft.omake.cliptemporary;

import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class ImageSelection implements Transferable{
	private Image image;
	private DataFlavor[] flavor;

	public ImageSelection(Image image){
		this.image=image;
		flavor=new DataFlavor[] { DataFlavor.imageFlavor };
	}
	public synchronized DataFlavor[] getTransferDataFlavors(){
		return flavor;
	}
	public boolean isDataFlavorSupported(DataFlavor flavor){
		return this.flavor[0].equals(flavor);
	}
	public synchronized Object getTransferData(DataFlavor flavor)
			throws UnsupportedFlavorException,IOException{
		if(!this.flavor[0].equals(flavor)){
			throw new UnsupportedFlavorException(flavor);
		}
		return image;
	}
}