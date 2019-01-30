package sousoft.omake.cliptemporary.plugin;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog.ModalityType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.swing.GroupLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.LayoutStyle;

import soulib.compiler.SimpleJavaCompiler;
import soulib.fileLib.FileEditor;
import soulib.windowLib.StandardWindow;
import soulib.windowLib.WindowLib;
import sousoft.omake.cliptemporary.CTWindow;
import sousoft.omake.cliptemporary.ClipTemporary;

public class PluginLoader implements Iterable<IPlugin>{
	public static ArrayList<Class<? extends IPlugin>> loadClass=new ArrayList<Class<? extends IPlugin>>();
	@SuppressWarnings("unchecked")
	public static void load() throws Exception{
		if(ClipTemporary.debug)System.out.println("プラグイン読み込み開始");
		String defaultPath=FileEditor.USERHOME+"/sou/ClipTemporary/Pulugin";
		File dir=new File(ClipTemporary.SystemConfig.getConfigDataString("プラグインの場所",defaultPath));
		dir.mkdirs();
		File[] l=dir.listFiles();
		ArrayList<File> al=new ArrayList<File>();
		ArrayList<URL> jarl=new ArrayList<URL>();
		ArrayList<File> classl=new ArrayList<File>();
		for(File f:l) {
			String n=f.getName();
			if(n.endsWith(".java")) {
				al.add(f);
			}else if(n.endsWith(".class")) {
				classl.add(f);
			}else if(n.endsWith(".jar")) {
				jarl.add(f.toURI().toURL());
			}else if(n.endsWith(".zip")) {
				jarl.add(f.toURI().toURL());
			}
		}
		for(File f:classl) {
			if(ClipTemporary.debug)System.out.println("コンパイル済プラグイン"+f.getName());
			FileInputStream fis=new FileInputStream(f);
			try{
				byte[] b=FileEditor.toByteArray(fis);
				Class<?> c=SimpleJavaCompiler.loadByteCode(b);
				if(IPlugin.class.isAssignableFrom(c)) {
					try{
						loadClass.add((Class<? extends IPlugin>) c);
					}catch(ClassCastException e){
						e.printStackTrace();
					}
				}
			}catch(ClassNotFoundException e){
				e.printStackTrace();
			}catch(IOException e) {
				e.printStackTrace();
			}finally {
				fis.close();
			}
		}
		for(URL j:jarl) {
			try {
				URLClassLoader ucl=URLClassLoader.newInstance(new URL[] {j});
				InputStream s=ucl.getResourceAsStream("Pulugin.txt");
				if(s==null)continue;
				InputStreamReader isr=new InputStreamReader(s,StandardCharsets.UTF_8);
				BufferedReader br=new BufferedReader(isr);
				while(br.ready()) {
					String line=br.readLine();
					if(line==null)break;
					if(line.isEmpty()||line.indexOf(0)=='#')continue;
					try{
						Class<?> c=ucl.loadClass(line);
						if(IPlugin.class.isAssignableFrom(c)) {
							loadClass.add((Class<? extends IPlugin>) c);
						}
					}catch(ClassNotFoundException e) {
						//e.printStackTrace();
					}catch(ClassCastException e){
						e.printStackTrace();
					}
				}
				s.close();
			}catch(IOException e) {
				e.printStackTrace();
			}
		}
		/*
		File class_dir=new File(dir,"class");
		class_dir.mkdir();
		ArrayList<File> class_file=new ArrayList<File>();
		for(File f:class_dir.listFiles()) {
			String n=f.getName();
			if(n.endsWith(".class")) {
				class_file.add(f);
				for(int i=0;i<al.size();i++) {
					File f0=al.get(i);
					String f0n=f0.getName();
					if(f0n.substring(0,f0n.length()-4).equals(n.substring(0,n.length()-5))) {
						if(f0.lastModified()>f.lastModified()) {
							class_file.remove(f);
						}else al.remove(f0);
					}
				}
			}
		}
		for(File f:class_file) {
			if(ClipTemporary.debug)System.out.println("コンパイル済クラス"+f.getName());
			FileInputStream fis=new FileInputStream(f);
			try{
				byte[] b=FileEditor.toByteArray(fis);
				String n=f.getName();
				Class<?> c=SimpleJavaCompiler.loadByteCode(n.substring(0,n.length()-6),b);
				if(IPlugin.class.isAssignableFrom(c)) {
					loadClass.add((Class<? extends IPlugin>) c);
				}
			}catch(ClassNotFoundException e){
				e.printStackTrace();
			}catch(IOException e) {
				e.printStackTrace();
			}finally {
				fis.close();
			}
		}
		*/
		File[] fs=al.toArray(new File[al.size()]);
		if(fs.length<1)return;
		Reader[] reader=new Reader[fs.length];
		for(int i=0;i<fs.length;i++)try{
			if(ClipTemporary.debug)System.out.println("ソースファイル"+fs[i]);
			FileInputStream fis=new FileInputStream(fs[i]);
			reader[i]=new InputStreamReader(fis,StandardCharsets.UTF_8);
		}catch(FileNotFoundException e){
			e.printStackTrace();
		}
		try{
			SimpleJavaCompiler sjc=new SimpleJavaCompiler(reader);
			ArrayList<Class<?>> cs=sjc.compile0();
			for(Class<?> c:cs) {
				if(IPlugin.class.isAssignableFrom(c)) {
					try{
						loadClass.add((Class<? extends IPlugin>) c);
					}catch(ClassCastException e){
						e.printStackTrace();
					}
				}
			}
			/*
			byte[][] bc=sjc.getByteCode();
			String[] bccn=sjc.byteCodeClassName();
			for(int i=0;i<bccn.length;i++) {
				String cn=bccn[i].substring(bccn[i].lastIndexOf('.')+1);
				byte[] b=bc[i];
				long lm=new File(dir,cn+".java").lastModified();
				File f=new File(class_dir,cn+".class");
				FileOutputStream fos=new FileOutputStream(f);
				try {
					fos.write(b);
				}catch(IOException e) {
					e.printStackTrace();
				}finally {
					fos.close();
				}
				f.setLastModified(lm);
			}
			*/
		}catch(IllegalArgumentException e){
			e.printStackTrace();
		}catch(ClassNotFoundException e){
			e.printStackTrace();
		}catch(ClassCastException e){
			e.printStackTrace();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	private ArrayList<IPlugin> active=new ArrayList<IPlugin>();
	public ArrayList<IPlugin> plugin=new ArrayList<IPlugin>();
	private JDialog frame;
	private CTWindow window;
	public PluginLoader(CTWindow ctWindow){
		window=ctWindow;
	}
	public void setPuluginActive(IPlugin p,boolean b) {
		window.getConfig().setConfigDataBoolean("プラグイン "+p.name()+" 有効",b);
		if(b) {
			if(!active.contains(p)) {
				active.add(p);
				p.setActive(b);
			}
		}else{
			if(active.contains(p)) {
				active.remove(p);
				p.setActive(b);
			}
		}
	}
	public boolean isPuluginActive(IPlugin p) {
		return active.contains(p);
	}
	public boolean add(IPlugin p){
		return plugin.add(p);
	}
	public boolean isFakePulugin(IPlugin p) {
		return IPlugin.fakePulugin==p;
	}
	public Iterator<IPlugin> iterator() {
		return active.iterator();
	}
	public void closeDialog() {
		if(frame!=null)frame.setVisible(false);
	}
	public void openDialog() {
		if(frame==null)makeFrame();
		else frame.setVisible(true);
	}
	public void close() {
		if(frame!=null)frame.dispose();
	}
	private void makeFrame() {
		final HashMap<JCheckBox,IPlugin> map=new HashMap<JCheckBox,IPlugin>();
		String tt=window.getWindowID()>0?" W"+(window.getWindowID()+1):"";
		frame=new JDialog(window,"プラグイン管理"+tt) {
			public void setVisible(boolean b) {
				super.setVisible(b);
				if(!b) {
					Iterator<Entry<JCheckBox, IPlugin>> es=map.entrySet().iterator();
					while(es.hasNext()) {
						Entry<JCheckBox, IPlugin> e=es.next();
						setPuluginActive(e.getValue(),e.getKey().isSelected());
					}
				}
			}
		};
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		frame.setVisible(true);
		JPanel panel = new JPanel();
		Component[][] components= new Component[plugin.size()+1][4];
		components[0]= new Component[]{new JLabel("有効化"),new JLabel("名前"),new JLabel("説明"),new JLabel("バージョン")};
		for(int i=1;i<plugin.size()+1;i++) {
			IPlugin p=plugin.get(i-1);
			JCheckBox box=new JCheckBox();
			box.setSelected(isPuluginActive(p));
			map.put(box,p);
			components[i][0]=box;
			components[i][1]=new JLabel(p.name());
			components[i][2]=new JLabel(p.info());
			components[i][3]=new JLabel(p.version().toString());
		}
		GroupLayout gl=StandardWindow.makeGroupLayout(panel,components);
		gl.setLayoutStyle(new LayoutStyle(){
			@Override
			public int getPreferredGap(JComponent component1,JComponent component2,ComponentPlacement type,int position,
					Container parent){
				return 5;
			}
			@Override
			public int getContainerGap(JComponent component,int position,Container parent){
				return 10;
			}
		});
		gl.setAutoCreateGaps(true);
		gl.setAutoCreateContainerGaps(true);
		panel.setLayout(gl);
		frame.getContentPane().add(panel, BorderLayout.PAGE_START);
		frame.pack();
		WindowLib.setCenter(frame);
		frame.setVisible(false);
		frame.setModalityType(ModalityType.DOCUMENT_MODAL);
		frame.setVisible(true);
	}
}
