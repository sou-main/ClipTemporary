package sousoft.omake.cliptemporary;

import static sousoft.omake.cliptemporary.ClipTemporary.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.DisplayMode;
import java.awt.Font;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.TransferHandler;

import soulib.fileLib.FileEditor;
import soulib.lib.ConfigBase;
import soulib.lib.MapConfig;
import soulib.windowLib.CommonGraphics;
import soulib.windowLib.FontManager;
import soulib.windowLib.KI;
import soulib.windowLib.StandardWindow;
import soulib.windowLib.SwingWindow;
import soulib.windowLib.WindowLib;
import soulib.windowLib.WindowMode;
import soulib.windowLib.CanvasComponents.CLabel;
import soulib.windowLib.CanvasComponents.CPanel;
import soulib.windowLib.CanvasComponents.CanvasComponent;
import sousoft.omake.cliptemporary.data.DataFile;
import sousoft.omake.cliptemporary.musicplayer.MusicPlayer;
import sousoft.omake.cliptemporary.plugin.IPlugin;
import sousoft.omake.cliptemporary.plugin.PluginLoader;

public class CTWindow extends SwingWindow{

	protected String defaultTitle="ClipTemporary - version"+version;
	public ClipTemporaryPanel ct;
	private CPanel tool;
	private EditImagePanel editImage;
	public int[] imageViewSize= { 512, 512 };
	public int[] imageZoomSize= {64,64};
	private String massage;
	private EditFile edit;
	private long usedMemory=0;//単位はbyte
	private FontManager font=new FontManager();
	private Font bigFont;
	private ConfigBase config;
	private int windowID;
	private CTMenuBar menubar;
	public boolean drawLine;
	public MusicPlayer musicPlayer=new MusicPlayer(this);
	private boolean active;
	private int autoAdd;
	/**さらに軽量化する。*/
	public boolean kei=false;
	private static boolean scanClip;
	public PluginLoader plugin=new PluginLoader(this);
	private CTWindow(WindowMode wm){
		super(wm);
		ct=new ClipTemporaryPanel(this,debug);
		edit=new EditFile(this);
		tool=new CPanel(true,true);
		tool.addCanvasComponent(new CLabel() {
			{
				autoSize=false;
				setBackground(Color.gray);
			}
			@Override
			public int getH(){
				return 20;
			}
			@Override
			public int getW() {
				setBackground(Color.white);
				return tool.getW();
			}
		});
		editImage=new EditImagePanel();
		editImage.setPriority(Integer.MAX_VALUE);
		ccm.addCanvasComponent(musicPlayer);
		setTransferHandler(new DropFileHandler());
		setKeyInput(new input());
		menubar=new CTMenuBar(this);
		setScroll(100);
	}
	public int getAutoAdd(){
		return autoAdd;
	}
	public void setAutoAdd(int autoAdd){
		this.autoAdd=autoAdd;
		if(autoAdd>0)scanClip=true;
		else {
			boolean b=true;
			for(CTWindow wl:openWindowList) {
				if(wl.autoAdd>0) {
					scanClip=true;
					b=false;
					break;
				}
			}
			if(b)scanClip=false;
		}
	}
	static CTWindow makeWindow(WindowMode wm) {
		int id=nextWindowID();
		wm.name="Window-"+id;
		CTWindow w=new CTWindow(wm);
		w.windowID=id;
		if(id>0)w.defaultTitle=w.defaultTitle+"　W"+(id+1);
		for(Class<? extends IPlugin> c:PluginLoader.loadClass){
			try{
				IPlugin p=c.newInstance();
				w.plugin.add(p);
			}catch(InstantiationException e){
				e.printStackTrace();
			}catch(IllegalAccessException e){
				e.printStackTrace();
			}
		}
		for(IPlugin p:w.plugin.plugin){
			p.load(w);
		}
		w.loadConfig();
		return w;
	}
	static{
		new Thread("ActiveWindowCheck") {
			private CTWindow active;
			private Window win;
			private Object t0;
			@Override
			public void run() {
				while(true) {
					KeyboardFocusManager kfm=KeyboardFocusManager.getCurrentKeyboardFocusManager();
					Window w=kfm.getActiveWindow();
					if(w!=win) {
						win=w;
						if(w==null) {
							if(active!=null) {
								active.setFps(1);
								active.getTooltip().hide();
								active.active=false;
								if(active.kei)active.ccm.setActiveMax(-1);
							}
						}else for(CTWindow wl:openWindowList) {
							if(w==wl) {
								if(!wl.active) {
									wl.setFps(30);
									active=wl;
									active.ccm.setActiveMax(Integer.MAX_VALUE);
								}
								wl.active=true;
							}else {
								if(wl.active) {
									wl.setFps(1);
									active.getTooltip().hide();
									if(wl.kei)wl.ccm.setActiveMax(-1);
								}
								wl.active=false;
							}
						}
					}
					if(scanClip)try{
						Clipboard c=java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
						Transferable t=c.getContents(null);
						Object o=null;
						if(t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
							o=t.getTransferData(DataFlavor.stringFlavor);
						}
						if(o!=null&&!o.equals(t0)) {
							t0=o;
							for(CTWindow x:openWindowList)if(x.autoAdd>0) {
								ClipTemporaryPanel p=x.getMainPanel();
								if(p.data.size()>=x.autoAdd){
									p.remove(p.data.get(0));
								}
								p.readS(false);
							}
						}
					}catch(IllegalStateException e) {
						e.printStackTrace();
					}catch(UnsupportedFlavorException e){
						e.printStackTrace();
					}catch(IOException e){
						e.printStackTrace();
					}
					try{
						Thread.sleep(100);
					}catch(InterruptedException e){
						e.printStackTrace();
					}
				}
			}
		}.start();
	}
	public ConfigBase getConfig() {
		return config;
	}
	private static int nextWindowID() {
		Comparator<CTWindow> c=new Comparator<CTWindow>() {
			@Override
			public int compare(CTWindow w1,CTWindow w2){
				return w1.getWindowID()-w2.getWindowID();
			}
		};
		openWindowList.sort(c);
		int lastID=0;
		for(CTWindow win:openWindowList) {
			if(lastID!=win.getWindowID())break;
			lastID++;
		}
		return lastID;
	}
	public void loadConfig() {
		StringBuilder configPath=new StringBuilder(FileEditor.USERHOME);
		configPath.append("/sou/ClipTemporary/ClipTemporary");
		if(windowID>0)configPath.append(windowID);
		configPath.append(".cfg");
		MapConfig mc=new MapConfig();
		mc.keyOnlyValue=null;
		config=mc;
		config.Kugiri="=";
		config.read(new File(config.file=configPath.toString()));
		String title=config.getConfigDataString("ウィンドウタイトル",null);
		if(title!=null)setTitle(defaultTitle+" "+title);
		else setTitle(defaultTitle);
		DisplayMode dm=StandardWindow.getDisplayMode();
		int x=config.getConfigDataInt("ウィンドウのX座標",dm.getWidth()/2-getWidth()/2);
		int y=config.getConfigDataInt("ウィンドウのY座標",dm.getHeight()/2-getHeight()/2);
		int w=config.getConfigDataInt("ウィンドウの幅",getWidth());
		int h=config.getConfigDataInt("ウィンドウの高さ",getHeight());
		setBounds(x,y,w,h);
		ct.setX(config.getConfigDataInt("データパネルのX座標",ct.getX()));
		ct.setY(config.getConfigDataInt("データパネルのY座標",ct.getY()));
		musicPlayer.setX(config.getConfigDataInt("ミュージックプレイヤーのX座標",musicPlayer.getX()));
		musicPlayer.setY(config.getConfigDataInt("ミュージックプレイヤーのY座標",musicPlayer.getY()));
		if(config.getConfigDataBoolean("MPウィンドウモード"))musicPlayer.toWindow();
		double cursor=config.getConfigDataDouble("MP音量");
		if(cursor>0)musicPlayer.vol.setCursor((int)(cursor/100F*musicPlayer.vol.getMax()));
		else if(cursor==0)musicPlayer.vol.setCursor(0);
		setAutoAdd(config.getConfigDataInt("自動追加",autoAdd));
		for(IPlugin p:plugin){
			p.loadConfig(config);
		}
	}
	@Override
	public void close() {
		for(IPlugin p:plugin.plugin){
			p.close(this);
		}
		writeConfig(null);
		musicPlayer.close(this);
		plugin.close();
		super.close();
		System.gc();
		System.runFinalization();
	}
	public boolean writeConfig(File f) {
		if(f==null)f=new File(config.file);
		try{
			File pd=f.getParentFile();
			if(pd!=null&&!pd.exists())pd.mkdirs();
			f.createNewFile();
		}catch(IOException e){
			e.printStackTrace();
		}
		Rectangle b=getBounds();
		config.setConfigDataInt("ウィンドウのX座標",b.x);
		config.setConfigDataInt("ウィンドウのY座標",b.y);
		config.setConfigDataInt("ウィンドウの幅",b.width);
		config.setConfigDataInt("ウィンドウの高さ",b.height);
		config.setConfigDataInt("編集ツールのX座標",tool.getX());
		config.setConfigDataInt("編集ツールのY座標",tool.getY());
		config.setConfigDataInt("データパネルのX座標",ct.getX());
		config.setConfigDataInt("データパネルのY座標",ct.getY());
		config.setConfigDataInt("ミュージックプレイヤーのX座標",musicPlayer.getPanelX());
		config.setConfigDataInt("ミュージックプレイヤーのY座標",musicPlayer.getPanelY());
		config.setConfigDataBoolean("MPウィンドウモード",musicPlayer.win);
		config.setConfigDataDouble("MP音量",musicPlayer.vol.getFomat(0,10000)/100F);
		if(musicPlayer.window!=null){
			Point p=musicPlayer.window.getLocation();
			config.setConfigDataInt("MPウィンドウX座標",p.x);
			config.setConfigDataInt("MPウィンドウY座標",p.y);
		}
		config.setConfigDataString("最後に編集されたファイル",ct.getEdit().getFilePath());
		if(!config.isMod())return true;
		return config.saveConfig(f);
	}
	public int getWindowID() {
		return windowID;
	}
	public CPanel getTool(){
		return tool;
	}
	public void setTool(CPanel tool){
		this.tool=tool;
	}
	public EditImagePanel getEditImagePanel(){
		return editImage;
	}
	public void setEditImagePanel(EditImagePanel editImage){
		this.editImage=editImage;
	}
	public ClipTemporaryPanel getMainPanel(){
		return ct;
	}
	public void editImage(Image img){
		editImage.setImage(img);
		editImage.show(getCanvasComponentManager());
	}
	public void setFont(String name) {
		if(name==null||name.isEmpty())return;
		//利用可能な全てのフォントを取得
		String[] arr=FontManager.getFontNames();
		for(String s:arr) {
			//指定されたフォントが利用可能なら
			if(name.equals(s)){
				//フォントを指定された物に変更
				font.setName(name);
				setFont();
				super.setFont(font.get(15));
				bigFont=null;
				break;
			}
		}
	}
	private void setFont() {
		JMenuBar mb=getJMenuBar();
		int len=mb.getMenuCount();
		for(int index=0;index<len;index++) {
			JMenu m=mb.getMenu(index);
			int ml=m.getItemCount();
			for(int i=0;i<ml;i++) {
				setFont(m.getMenuComponent(i),font);
			}
			Font f=m.getFont();
			m.setFont(font.get(f.getStyle(),f.getSize()));
		}
		CanvasComponent.defaultFont=font.get(CanvasComponent.defaultFont.getStyle(),CanvasComponent.defaultFont.getSize());
		setFont(ct.getCanvasComponent());
		setFont(tool.getCanvasComponent());
		setFont(editImage.getCanvasComponent());
		musicPlayer.setFont(font);
		getWindowLog().getFontManager().setName(font.getName());
		ct.resetHeight();
		int y=tool.getY();
		for(CanvasComponent cc:tool.getCanvasComponent()) {
			cc.setY(y);
			y+=cc.getH();
		}
	}
	public static void setFont(Component c,FontManager font) {
		if(c instanceof JMenu) {
			int ml=((JMenu)c).getMenuComponentCount();
			for(int i=0;i<ml;i++) {
				setFont(((JMenu)c).getMenuComponent(i),font);
			}
		}
		Font f=c.getFont();
		c.setFont(font.get(f.getStyle(),f.getSize()));
	}
	private void setFont(CanvasComponent[] arr) {
		for(CanvasComponent c:arr) {
			Font f=c.getFont();
			c.setFont(font.get(f.getStyle(),f.getSize()));
		}
	}
	@Override
	public Font getFont() {
		if(font==null)return super.getFont();
		return font.get();
	}
	public void Update(CommonGraphics g){
		if(!active&&kei) {
			g.setColor(active?Color.white:Color.darkGray);
			g.fillRect(0,0,this.canWidth(),this.canHeight());
			return;
		}
		g.setColor(Color.black);
		{
			if(debug&&massage!=null){
				int h=WindowLib.getPaintHeight(g.getFont());
				g.setFont(font.get(25));
				g.drawString(massage,0,canHeight()-h);
			}
			g.setFont(font.get(15));
			long freeMemory=Runtime.getRuntime().freeMemory();
			long totalMemory=Runtime.getRuntime().totalMemory();
			long maxMemory=Runtime.getRuntime().maxMemory();
			usedMemory=totalMemory-freeMemory;
			StringBuffer s=new StringBuffer();
			s.append(usedMemory/1048576).append("MB使用中");
			s.append(totalMemory/1048576).append("MB予約済");
			if(maxMemory<Long.MAX_VALUE) s.append(maxMemory/1048576).append("MB利用可能");
			g.drawString(s.toString(),0,canHeight());
		}
		if(ct==null||ct.getEdit()==null) return;
		int y=0;
		boolean saved=isSaved();
		String path=ct.getEdit().getFilePath();
		if(path==null&&saved) return;
		g.setFont(font.get(30));
		int h=WindowLib.getPaintHeight(g.getFont());
		if(path!=null){
			String s=new StringBuffer("現在のファイル").append(path).toString();
			g.drawString(s,0,y+=h);
		}
		if(!saved){
			g.setColor(Color.red);
			String s="保存されていない変更があります";
			//int w=WindowLib.getPaintWidth(g.getFont(),s);
			int x=0;//canWidth()-w;
			g.drawString(s,x,y+h);
			//g.setColor(Color.black);
			//g.drawRect(x,0,w,y+15);
		}
	}
	public boolean isSaved() {
		return ct.getEdit().saved||ct.data.isEmpty();
	}
	public String getMassage(){
		return massage;
	}
	public void setMassage(String massage){
		this.massage=massage;
	}
	public EditFile getEdit(){
		return edit;
	}
	public void setEdit(EditFile edit){
		this.edit=edit;
	}
	@Override
	public WindowListener setWindowListener(){
		final CTWindow win=this;
		return new WindowAdapter(){
			@Override
			public void windowClosing(WindowEvent evt){
				ClipTemporary.closeWindow(win);
			}
		};
	}
	/**
	 * ドロップ操作の処理を行うクラス
	 */
	private class DropFileHandler extends TransferHandler{
		/**
		 * ドロップされたものを受け取るか判断 (ファイルのときだけ受け取る)
		 */
		@Override
		public boolean canImport(TransferSupport support){
			if(!support.isDrop()){
				return false;// ドロップ操作でない場合は受け取らない
			}
			if(!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)){
				return false;// ドロップされたのがファイルでない場合は受け取らない
			}
			return true;
		}
		/**
		 * ドロップされたファイルを受け取る
		 */
		@Override
		public boolean importData(TransferSupport support){
			// 受け取っていいものか確認する
			if(!canImport(support)){
				return false;
			}
			// ドロップ処理
			Transferable t=support.getTransferable();
			try{
				// ファイルを受け取る
				@SuppressWarnings("unchecked")
				List<File> files=(List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
				if(files.size()>1) {
					for(File f:files) {
						DataFile read=new DataFile(CTWindow.this,f);
						ct.addData(read);
					}
					return true;
				}else if(files.size()<1)return false;
				for(File f:files){
					String name=f.getName();
					for(String e:ex)
						if(name.endsWith("."+e)){
							ct.getEdit().readFile(f,true);
							return true;
						}
				}
				DataFile read=new DataFile(CTWindow.this,files.get(0));
				ct.addData(read);
			}catch(Exception e){
				e.printStackTrace();
			}
			return true;
		}
	}

	protected boolean ctrl;
	private boolean keyshift;
	private class input extends KI{
		@Override
		public void keyPressed(KeyEvent e){
			super.keyPressed(e);
			int keycode=e.getKeyCode();
			if(keycode==KeyEvent.VK_CONTROL){
				ctrl=true;
			}
			if(keycode==KeyEvent.VK_ESCAPE){
				ClipTemporary.closeWindow(CTWindow.this);
			}
			CTWindow.this.keyshift=keyshift;
		}
		@Override
		public void keyReleased(KeyEvent e){
			super.keyReleased(e);
			int keycode=e.getKeyCode();
			if(keycode==KeyEvent.VK_CONTROL){
				ctrl=false;
			}CTWindow.this.keyshift=keyshift;
		}
	}
	@Override
	public void mouseWheelMoved(MouseWheelEvent e){
		int moved;
		if(keyshift){
			moved=e.getWheelRotation()*(ctrl?100:50);
		}else{
			moved=e.getWheelRotation()*10;
		}
		ct.setY(ct.getY()-moved);
	}
	public boolean getTempFlag(){
		usedMemory=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
		return menubar.useTempMemory<usedMemory/1048576&&menubar.getUseTempFile();
	}
	public Font getBigFont(){
		if(bigFont==null)bigFont=font.get(30);
		return bigFont;
	}
	public FontManager getFontManager(){
		return font;
	}
	public CTMenuBar getCTMenuBar(){
		return menubar;
	}
}