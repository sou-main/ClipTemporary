package sousoft.omake.cliptemporary.plugin;

import java.awt.datatransfer.Transferable;
import java.util.ArrayList;

import soulib.lib.ConfigBase;
import soulib.lib.Version;
import soulib.windowLib.CanvasComponents.CanvasComponent;
import sousoft.omake.cliptemporary.CTWindow;
import sousoft.omake.cliptemporary.data.Data;

public interface IPlugin{
	public Version version();
	public String info();
	public String name();
	public default void load(CTWindow window) {}
	public default void close(CTWindow window) {}
	public default void addData(Data d) {}
	public default void removeData(Data d) {}
	public default void openMenu(Data d,ArrayList<CanvasComponent> list) {}
	public default void loadConfig(ConfigBase conf) {}
	public default void setActive(boolean state) {}
	public default Data[] readClip(Transferable t) {
		return null;
	}
	public static final IPlugin fakePulugin=new IPlugin(){
		@Override
		public Version version(){
			return new Version("0.0.0");
		}
		@Override
		public String info(){
			return "FakePulugin";
		}
		@Override
		public String name(){
			return "FakePulugin";
		}
		@Override
		public void load(CTWindow window){

		}
		@Override
		public void close(CTWindow window){

		}
	};
}