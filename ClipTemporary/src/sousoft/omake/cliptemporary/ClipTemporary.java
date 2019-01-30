package sousoft.omake.cliptemporary;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import soulib.fileLib.FileEditor;
import soulib.fileLib.JarLocalFile;
import soulib.lib.ConfigBase;
import soulib.lib.ListMap;
import soulib.lib.MapConfig;
import soulib.lib.Version;
import soulib.windowLib.CanvasComponentManager;
import soulib.windowLib.CommonGraphics;
import soulib.windowLib.StandardWindow;
import soulib.windowLib.WindowLib;
import soulib.windowLib.WindowMode;
import soulib.windowLib.CanvasComponents.CButton;
import soulib.windowLib.CanvasComponents.CLabel;
import soulib.windowLib.CanvasComponents.CPanel;
import sousoft.omake.cliptemporary.data.Data;
import sousoft.omake.cliptemporary.data.DataBinary;
import sousoft.omake.cliptemporary.data.DataFile;
import sousoft.omake.cliptemporary.data.DataImage;
import sousoft.omake.cliptemporary.data.DataText;
import sousoft.omake.cliptemporary.data.DataUnknown;
import sousoft.omake.cliptemporary.plugin.IPlugin;
import sousoft.omake.cliptemporary.plugin.PluginLoader;

public class ClipTemporary{

	/**
	 * v1.6.1(2019/01/19)
	 * プラグイン機能を修正。
	 * 設定書き込みを修正。
	 *
	 * v1.6.0(2019/01/19)
	 * 自動追加機能を0に設定できないバグを修正。
	 * プラグイン機能を追加。
	 * http://sousoft.dip.jp/soft/ClipTemporary/Plugin.html
	 * プラグイン詳細
	 *
	 * v1.5.0(2019/01/16)
	 * 自動追加機能を追加(テキストデータのみ対応)
	 *
	 * v1.4.10(2019/01/15)
	 * 音量調整バーをマウスホイールで操作するとメインパネルが移動するバグを修正。
	 *
	 * v1.4.9(2019/01/14)
	 * ミュージックプレイヤーのファイル名にカーソルを当てるとフルパスをポップアップさせるようにした。
	 * 履歴機能完成。
	 *
	 * v1.4.8(2019/01/06)
	 * 履歴機能を追加。
	 * デバッグ用情報の出力を修正。
	 * 一括操作を「編集>一括操作」に移動した。
	 * 一括操作の種類を追加。
	 * 「次の曲」ボタンの挙動を変更。
	 *
	 * v1.4.7(2018/12/24)
	 * ループ再生した時に再生状態の表示がおかしくなるバグを修正。
	 * 連続再生モードの時に「次の曲」ボタンを追加(2曲以上再生していた時は押しても何も起こらない)
	 *
	 * v1.4.6(2018/12/22)
	 * ミュージックプレイヤーの連続再生機能を修正。
	 * ミュージックプレイヤーがウィンドウモードの時で1曲しか再生されていない時は
	 * スペースキーで一時停止できるようにした。
	 *
	 * v1.4.5(2018/12/20)
	 * ミュージックプレイヤーにもフォントを反映させるようにした。
	 * 連続再生した時に再生中のファイルが選択されないバグを修正。
	 *
	 * v1.4.4(2018/12/20)
	 * ミュージックプレイヤーのボリュームを記録するようにした。
	 * ミュージックプレイヤーの座標処理を修正。
	 * 連続再生機能を修正。(エラーが発生した場合飛ばすようにした。)
	 * 一つ上、一つ下に移動する機能を追加。
	 *
	 * v1.4.3(2018/12/19)
	 * ミュージックプレイヤーのウィンドウの余白を調整する機能を追加。
	 * ミュージックプレイヤーの対応形式をシステムから取得するようにした。
	 * ミュージックプレイヤーのボリュームの矢印を消した。
	 * ミュージックプレイヤーのボリュームのつまみを小さくした。
	 *
	 * v1.4.2(2018/12/19)
	 * 連続再生機能を追加。(その他>音楽連続再生)
	 * 超軽量化モードを追加。ウィンドウを選択していないとき何も表示しない。
	 * ミュージックプレイヤーのウィンドウモードで全て停止した時にウィンドウを閉じるようにした。
	 * ミュージックプレイヤーのウィンドウを常に最前面に表示するオプションを追加
	 *
	 * v1.4.1(2018/12/19)
	 * ミュージックプレイヤーのウィンドウモードのアイコンを修正。
	 *起動時にNullPointerExceptionが発生するバグを修正。(動作には影響しない。ログが発生するだけ)
	 *mp3形式に対応させた。
	 *ループ再生状態で停止させるとフリーズするバグを修正。(ウィンドウを閉じた時の自動停止も)
	 *
	 * v1.4.0(2018/12/18)
	 * ウィンドウが選択されていない時はフレームレートを落とす機能を追加。
	 * 拡張子が「wav」と「mid」のファイルをメニューから再生できるようにした。
	 * ミュージックプレイヤー機能を追加。
	 *
	 * v1.3.4(2018/11/22)
	 * ウィンドウタイトル変更機能を追加
	 * ファイル追加機能を追加(選択とパス入力)
	 *
	 * v1.3.3(2018/09/22)
	 * 改行文字表示機能を追加
	 * テキスト編集機能を追加
	 * テキスト追加機能を追加
	 *
	 * v1.3.2(2018/09/16)
	 *
	 * v1.3.1()
	 *
	 * v1.3.0(2018/08/18)
	 * 暗号化保存機能を追加した。
	 * 設定にフルパス表示の設定を追加した。
	 *
	 * v1.2.1(2018/08/08)
	 * 設定可能項目を増やした。
	 * 編集に切り取りを追加した。
	 * メニューバーにキーボードショートカットを表示するようにした。
	 *
	 * v1.2.0(2018/07/25)
	 * 複数のウィンドウを扱えるようにした。(メニューバー＞ファイル＞新規ウィンドウ)
	 * Ctrl+Shift+Escで強制終了できるようにした。(キー入力可能な場合)使えない事がわかった。
	 * いくつかのキーボードショートカットを追加
	 * Ctrl+Cで選択項目をコピー
	 * Ctrl+Vで追加
	 * Ctrl+Aで全て選択
	 * Deleteで選択項目を削除
	 *
	 * v1.1.3(2018/07/01)
	 * 一部にフォントが反映されないバグを修正。
	 * 保存されたかの判定がおかしかったので修正。
	 * 保存するデフォルトのファイル名を変更。
	 * 一時ファイルを使う条件を修正。
	 *
	 * v1.1.2(2018/06/03)
	 * デバッグ用情報の出力を修正。
	 * v1.1.1(2018/06/02)
	 * メニューの一部にフォントが反映されないバグを修正。
	 * フォントによっては種類ボタンの幅がずれるバグを修正。
	 * 一部の設定を保持するようにした。
	 * v1.1.0(2018/05/31)
	 * メニューバー周りを変更&追加
	 * フォントを選択機能を追加。
	 * それらに伴ってツールボックスのクリックイベント処理を変更。
	 * 多くのメモリ（設定可能）を使用していた時一時ファイルに書き込むようにした。
	 * 一時ファイルによって読み込み速度が遅くなるので実装メモリに合わせて調整するといい。
	 * v1.0.2(2018/05/30)
	 * メモリ使用量が出るようにした。
	 * v1.0.1(2018/05/29)
	 * 大きい画像を保存しようとすると正しく処理できない問題を修正。
	 * v1.0.0(2018/05/29)
	 * とりあえず完成*/
	public static final Version version=new Version("1.6.1","","2019/01/19");
	static final String[] ex= { "clip","clip.aes" };//拡張子
	public static boolean useBinary=false;
	public static boolean debug;
	public static ArrayList<CTWindow> openWindowList=new ArrayList<CTWindow>();
	public static WindowMode wm;
	public static MapConfig ArgCF,SystemConfig;
	private static JFrame startDialog;
	public static JLabel startDialogLabel;

	public static void main(String[] args){
		ArgCF=new MapConfig(args);
		startDialog=new JFrame();
		startDialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		startDialog.setVisible(false);
		startDialog.setUndecorated(true);
		startDialog.setSize(300,50);
		startDialog.setAlwaysOnTop(true);
		startDialogLabel=new JLabel();
		startDialog.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e){
				int keycode=e.getKeyCode();
				if (keycode==KeyEvent.VK_ESCAPE&&e.isControlDown()){
					System.exit(1000);
				}
			}
		});
		JPanel labelPanel = new JPanel();
		labelPanel.add(startDialogLabel);
		JPanel tp = new JPanel();
		tp.add(new JLabel("ClipTemporary起動中"));
		startDialog.getContentPane().add(tp, BorderLayout.PAGE_START);
		startDialog.getContentPane().add(labelPanel, BorderLayout.CENTER);
		startDialog.setVisible(true);
		WindowLib.setCenter(startDialog);
		wm=new WindowMode(600,450);
		startDialogLabel.setText("アイコンを読み込んでいます");
		try{
			URL url=new JarLocalFile("icon.png").getURL();
			if(url==null){
				File file=new File("icon.png");
				if(file.exists())url=file.toURI().toURL();
			}
			if(url!=null)wm.icon=ImageIO.read(url);
		}catch(IOException e1){
			e1.printStackTrace();
		}
		debug=ArgCF.getConfigDataBoolean("debug");
		CButton.defaultColor=new Color[] { Color.black, Color.lightGray };
		startDialogLabel.setText("全体の設定を読み込んでいます");
		SystemConfig=new MapConfig();
		SystemConfig.Kugiri="=";
		SystemConfig.read(new File(FileEditor.USERHOME+"/sou/ClipTemporary/SystemConfig.cfg"));
		startDialogLabel.setText("以前の一時ファイルを削除しています");
		EditFile.deleteTemp();
		registerID();
		startDialogLabel.setText("プラグインを読み込んでいます");
		try{
			PluginLoader.load();
		}catch(Exception e){
			e.printStackTrace();
		}
		startDialogLabel.setText("ウィンドウを生成しています");
		newWindow(ArgCF, wm);
		startDialogLabel.setText("メモリを開放しています");
		System.gc();
		startDialog.setVisible(false);
		startDialog.dispose();
	}
	public static void registerID() {
		EditFile.registry.put(DataText.id,DataText.class);
		EditFile.registry.put(DataFile.id,DataFile.class);
		EditFile.registry.put(DataImage.id,DataImage.class);
		EditFile.registry.put(DataUnknown.id,DataUnknown.class);
		EditFile.registry.put(DataBinary.id,DataBinary.class);
	}
	public static void closeWindow(CTWindow window) {
		if(window.getConfig().getConfigDataBoolean("保存確認画面を表示",true)&&!window.isSaved()) {
			String[] arr={"変更が保存されていません",
					"このウィンドウを閉じますか？",
					"(このウィンドウに保持されているデータは破棄されます)"};
			if(openWindowList.size()<=1) {
				arr[1]="終了しますか？";
				arr[2]="(保存されていないデータは破棄されます)";
			}
			if(!WindowLib.Yes_NoWindow(arr,"変更が保存されていません"))return;
		}
		if(openWindowList.size()<=1) {
			SystemConfig.saveConfig();
			window.close();
			System.exit(0);
		}
		openWindowList.remove(window);
		window.close();
	}
	public static void newWindow(MapConfig cf, WindowMode wm){
		CTWindow window=CTWindow.makeWindow(wm);
		ClipTemporaryPanel panel=window.ct;
		openWindowList.add(window);
		CPanel menu=window.getTool();
		menu.setBackground(Color.white);
		CanvasComponentManager ccm=window.getCanvasComponentManager();
		ccm.addCanvasComponent(panel);
		ListMap<String, ActionListener> edit=new ListMap<String, ActionListener>();
		class bt extends CButton{
			public bt(String string){
				super(string);
			}
			@Override
			public int getW() {
				return Math.max(super.getW(),menu.getW());
			}
			@Override
			public void draw(CommonGraphics g,Point e){
				if(isInside(e))setBackground(Color.cyan);
				else setBackground(Color.lightGray);
				super.draw(g,e);
			}
		}
		//CLabel title=new CLabel("一括操作ツール");
		CLabel title=new CLabel();
		title.setBackground(Color.white);
		title.setFont(WindowLib.getBOLD_Font(20));
		menu.addCanvasComponent(title);
		edit.put("コピー",
		new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent event){
				panel.writeS();
			}
		});
		edit.put("切り取り",
		new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent event){
				panel.Cut();
			}
		});
		edit.put("貼り付け",
		new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent event){
				try{
					panel.readS();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		});
		edit.put("選択項目を削除",
		new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent event){
				panel.CelectDelete();
			}
		});
		edit.put("一つ上に移動",
		new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent event){
				if(panel.data.isEmpty()) return;
				panel.move(-1);
			}
		});
		edit.put("一つ下に移動",
		new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent event){
				if(panel.data.isEmpty()) return;
				panel.move(1);
			}
		});
		edit.forEach(new BiConsumer<String,ActionListener>(){
			private int h=menu.getH();
			@Override
			public void accept(String key,ActionListener val){
				bt bt=new bt(key);
				bt.addActionListener(val);
				menu.addCanvasComponent(bt,0,h);
				h+=bt.getH();
			}
		});
		ConfigBase config=window.getConfig();
		menu.setPriority(1);
		menu.setX(config.getConfigDataInt("編集ツールのX座標",menu.getX()));
		menu.setY(config.getConfigDataInt("編集ツールのY座標",menu.getY()));
		ccm.addCanvasComponent(menu);
		startDialogLabel.setText("メニューバーを生成しています");
		window.getCTMenuBar().setMenuBar(edit);
		for(IPlugin p:window.plugin.plugin) {
			String name=p.name().replace(config.Kugiri,"≠");
			boolean b=config.getConfigDataBoolean("プラグイン "+name+" 有効");
			window.plugin.setPuluginActive(p,b);
		}
		if(config.getConfigDataBoolean("起動時に前回のファイルを開く")) {
			String last=config.getConfigDataString("最後に編集されたファイル");
			if(last!=null&&!last.isEmpty()) {
				startDialogLabel.setText("最後に編集されたファイルを読み込んでいます");
				window.getEdit().readFile(new File(last));
			}
		}
		StandardWindow.setLookAndFeel(null,window);
	}
	public static boolean writeClip(Data... arr){
		if(arr==null||arr.length<1) return false;
		Data d=arr[0];
		if(d==null)return false;
		Transferable selection=null;
		Clipboard clipboard=Toolkit.getDefaultToolkit().getSystemClipboard();
		selection=d.writeClip(arr);
		if(selection==null)return false;
		clipboard.setContents(selection,null);
		return true;
	}
	public static class ReadClipException extends Exception{
		public String massage;
		public ReadClipException() {

		}
		public ReadClipException(String mas) {
			massage=mas;
		}
		public String getMessage() {
			if(massage!=null)return massage;
			return super.getMessage();
		}
	}
	public static Data[] readClip(CTWindow p) throws UnsupportedFlavorException,IOException,ReadClipException{
		Clipboard c=java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable t=null;
		try{
			t=c.getContents(null);
		}catch(IllegalStateException e) {
			throw new ReadClipException("クリップボード読み取りに失敗しました");
		}
		DataFlavor[] arr=t.getTransferDataFlavors();
		if(arr==null||arr.length==0) {
			System.out.println("TYPE-null");
			throw new ReadClipException("なにもコピーされていません");
		}
		Data[] d=null;
		for(DataFlavor raw:arr){
			if(debug&&raw!=null) {
				Class<?> cl=raw.getRepresentationClass();
				StringBuilder sb=new StringBuilder();
				if(cl!=null) {
					sb.append(raw.toString()).append("&Class=").append(cl.getCanonicalName());
				}
				System.out.println(sb.toString());
			}
			if(DataFlavor.stringFlavor.equals(raw)){
				if(debug) System.out.println("TYPE-text");
				String s=(String) t.getTransferData(raw);
				d=new DataText[] { new DataText(p,s) };
			}else if(DataFlavor.imageFlavor.equals(raw)){
				if(debug) System.out.println("TYPE-image");
				Image image=(Image) t.getTransferData(raw);
				d=new DataImage[] { new DataImage(p,image) };
				return d;
			}else if(DataFlavor.javaFileListFlavor.equals(raw)){
				if(debug) System.out.println("TYPE-files");
				@SuppressWarnings("unchecked")
				List<File> list=(List<File>) t.getTransferData(raw);
				d=new DataFile[list.size()];
				for(int i=0;i<list.size();i++){
					d[i]=new DataFile(p,list.get(i));
				}
				return d;
			}else if(DataFlavor.getTextPlainUnicodeFlavor().equals(raw)) {
				if(debug) System.out.println("TYPE-text");
				Reader r=raw.getReaderForText(t);
				StringWriter sw=new StringWriter();
				char[] buf=new char[256];
				int len;
				while(true) {
					len=r.read(buf);
					if(len<1)break;
					sw.write(buf,0,len);
				}
				String s=sw.toString();
				d=new DataText[] { new DataText(p,s) };
			}
		}
		if(d!=null)return d;
		String data=null;
		for(DataFlavor raw:arr){
			if(raw.isMimeTypeEqual("text/plain")){
				if(raw.getRepresentationClass()==String.class) {
					if(debug) System.out.println("TYPE-String");
					data=(String) t.getTransferData(raw);
				}
			}
		}
		if(data==null)for(DataFlavor raw:arr){
			if(raw.isMimeTypeEqual("text/plain")){
				if(raw.isRepresentationClassReader()) {
					if(debug) System.out.println("TYPE-Reader");
					Reader r=(Reader) t.getTransferData(raw);
					StringWriter sw=new StringWriter();
					int len=0;
					char[] buf=new char[64];
					while(true) {
						len=r.read(buf);
						if(len<1)break;
						sw.write(buf,0,len);
					}
					data=sw.toString();
				}
			}
		}
		if(data==null)for(DataFlavor raw:arr){
			if(raw.isMimeTypeEqual("text/plain")){
				if(raw.isRepresentationClassInputStream()&&raw.getMimeType().toLowerCase().indexOf("charset=utf-8")!=-1) {
					if(debug) System.out.println("TYPE-InputStream");
					InputStream is=(InputStream) t.getTransferData(raw);
					ByteArrayOutputStream bos=new ByteArrayOutputStream();
					int len=0;
					byte[] buf=new byte[64];
					while(true) {
						len=is.read(buf);
						if(len<1)break;
						bos.write(buf,0,len);
					}
					data=bos.toString("UTF-8");
				}
			}
		}
		if(data==null)for(DataFlavor raw:arr){
			if(raw.isMimeTypeEqual("text/plain")){
				if(raw.isRepresentationClassByteBuffer()&&raw.getMimeType().toLowerCase().indexOf("charset=utf-8")!=-1) {
					if(debug) System.out.println("TYPE-ByteBuffer");
					ByteBuffer bb=(ByteBuffer) t.getTransferData(raw);
					byte[] ba=new byte[bb.limit()];
					bb.get(ba);
					data=new String(ba,StandardCharsets.UTF_8);
				}
			}
		}
		if(data==null)for(DataFlavor raw:arr){
			if(raw.isMimeTypeEqual("text/plain")){
				if(raw.isRepresentationClassCharBuffer()) {
					if(debug) System.out.println("TYPE-CharBuffer");
					CharBuffer cb=(CharBuffer) t.getTransferData(raw);
					char[] ca=new char[cb.limit()];
					cb.get(ca);
					data=new String(ca);
				}
			}
		}
		if(data!=null){
			d=new DataText[] { new DataText(p,data) };
		}
		if(useBinary&d==null)for(DataFlavor raw:arr){
			if(raw.isRepresentationClassByteBuffer()) {
				ByteBuffer cb=(ByteBuffer) t.getTransferData(raw);
				if(cb.hasArray()) {
					if(debug) System.out.println("TYPE-ByteBuffer-array");
					byte[] ar=cb.array();
					byte[] cp=new byte[ar.length];
					System.arraycopy(ar,0,cp,0,ar.length);
					d=new DataBinary[] {new DataBinary(p,cp)};
				}else {
					if(debug) System.out.println("TYPE-ByteBuffer-get");
					byte[] ca=new byte[cb.limit()];
					cb.get(ca);
					d=new DataBinary[] {new DataBinary(p,ca)};
				}
			}else if(raw.getRepresentationClass()==byte[].class) {
				if(debug) System.out.println("TYPE-ByteArray");
				byte[] ba=(byte[]) t.getTransferData(raw);
				d=new DataBinary[] {new DataBinary(p,ba)};
			}
		}
		for(IPlugin q:p.plugin){
			Data[] d0=q.readClip(t);
			if(d0!=null) {
				d=d0;
				break;
			}
		}
		if(d==null&&arr[0]!=null) {
			Object td=t.getTransferData(arr[0]);
			if(td==null) {
				System.out.println("TYPE-TransferData-NULL");
			}else {
				if(debug) System.out.println("TYPE-UNKNOWN");
				d=new DataUnknown[]{new DataUnknown(p,t)};
			}
		}
		if(debug){
			if(d==null) System.out.println("非対応データ");
			else System.out.println(d);
		}
		if(d==null)throw new ReadClipException("非対応データです");
		return d;
	}
}
