package sousoft.omake.cliptemporary;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class FileSelection implements Transferable{
	private List<File> data;
	private DataFlavor[] flavor;

	public FileSelection(List<File> f){
		this.data=f;
		flavor=new DataFlavor[] { DataFlavor.javaFileListFlavor };
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
		return data;
	}
}