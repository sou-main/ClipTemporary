package sousoft.omake.cliptemporary.plugin;

import sousoft.omake.cliptemporary.CTWindow;
import sousoft.omake.cliptemporary.EditFile;
import sousoft.omake.cliptemporary.data.Data;

public class PluginAPI{
	public static boolean isActive(CTWindow w,IPlugin p) {
		return w.plugin.isPuluginActive(p);
	}
	public static void addDataType(int id,Class<? extends Data> clas) {
		EditFile.registry.put(id,clas);
	}
}
