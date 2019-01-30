package sousoft.omake.cliptemporary;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import soulib.fileLib.FileEditor;
import soulib.fileLib.JarLocalFile;
import soulib.lib.ConfigBase;
import soulib.lib.DataEditor;
import soulib.lib.MapConfig;
import soulib.windowLib.FontManager;
import soulib.windowLib.WindowLib;
import sousoft.omake.cliptemporary.History.HistoryManager;
import sousoft.omake.cliptemporary.data.Data;
import sousoft.omake.cliptemporary.data.DataFile;
import sousoft.omake.cliptemporary.data.DataImage;
import sousoft.omake.cliptemporary.data.DataText;

public class CTMenuBar{

	public int useTempMemory=512;//単位はMB
	private JCheckBoxMenuItem minCopy;
	private JCheckBoxMenuItem delCheck;
	private JCheckBoxMenuItem useTempFile;
	private CTWindow w;
	private boolean fullPath;
	private boolean paintCRLF;
	private HashMap<String,JMenuItem> menus=new HashMap<String,JMenuItem>();
	public CTMenuBar(CTWindow win) {
		w=win;
	}
	public boolean getMinCopy(){
		return minCopy.isSelected();
	}
	public boolean getUseTempFile(){
		return useTempFile.isSelected();
	}
	public boolean getDelCheck() {
		return delCheck.isSelected();
	}
	public boolean isFullPath() {
		return fullPath;
	}
	public boolean isPaintCRLF() {
		return paintCRLF;
	}
	public void setMenuBar(Map<String, ActionListener> edit2){
		ClipTemporaryPanel ct=w.getMainPanel();
		ConfigBase config=w.getConfig();
		w.setBackground(Color.WHITE);
		JMenuBar mb=new JMenuBar();
		{
			JMenu mFile=new JMenu("ファイル");
			JMenuItem miRead=new JMenuItem("ファイルを開く");
			menus.put("ファイル>ファイルを開く",miRead);
			miRead.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
			miRead.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
					ct.getEdit().readFile();
				}
			});
			mFile.add(miRead);
			mFile.addSeparator();
			JMenuItem miSaveA=new JMenuItem("名前を付け暗号化して保存");
			menus.put("ファイル>名前を付け暗号化して保存",miSaveA);
			miSaveA.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
					ct.getEdit().writeFile(false,true);
				}
			});
			mFile.add(miSaveA);
			JMenuItem miSave=new JMenuItem("名前を付けて保存");
			menus.put("ファイル>名前を付けて保存",miSave);
			miSave.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
					ct.getEdit().writeFile(false,false);
				}
			});
			mFile.add(miSave);
			JMenuItem miSaveU=new JMenuItem("上書き保存");
			menus.put("ファイル>上書き保存",miSaveU);
			miSaveU.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
			miSaveU.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
					ct.getEdit().writeFile(true,false);
				}
			});
			mFile.add(miSaveU);
			JMenuItem textSave=new JMenuItem("テキスト形式で出力");
			menus.put("ファイル>テキスト形式で出力",textSave);
			textSave.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
					ct.getEdit().writeText();
				}
			});
			mFile.add(textSave);
			mFile.addSeparator();
			JMenuItem can=new JMenuItem("読み取り/書き込み操作を中断");
			menus.put("ファイル>読み取り/書き込み操作を中断",can);
			can.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
					ct.getEdit().cancel=true;
				}
			});
			mFile.add(can);
			mFile.addSeparator();
			JMenuItem miExit=new JMenuItem("終了");
			menus.put("ファイル>終了",miExit);
			miExit.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
					ClipTemporary.closeWindow(w);
				}
			});
			mFile.add(miExit);
			mFile.addSeparator();
			JMenuItem NewWindow=new JMenuItem("新規ウィンドウ");
			menus.put("ファイル>新規ウィンドウ",NewWindow);
			NewWindow.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
					ClipTemporary.newWindow(ClipTemporary.ArgCF,ClipTemporary.wm);
				}
			});
			mFile.add(NewWindow);
			setIcon("save.png","save.png",16,miSaveU,miSave,miSaveA);
			setIcon("open.png","open.png",16,miRead);
			mb.add(mFile);
		}
		{
			JMenu edit=new JMenu("編集");
			edit2.forEach(new BiConsumer<String,ActionListener>(){
				@Override
				public void accept(String key,ActionListener val){
					JMenuItem mi=new JMenuItem(key);
					int i=-1;
					switch(mi.getText()){
						case "コピー":
							i=KeyEvent.VK_C;
						break;
						case "切り取り":
							i=KeyEvent.VK_X;
						break;
						case "貼り付け":
							i=KeyEvent.VK_V;
						break;
						case "選択項目を削除":
							//i=KeyEvent.VK_DELETE;
							mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,0));
						break;
					}
					menus.put("編集>"+key,mi);
					if(i>0) {
						mi.setAccelerator(KeyStroke.getKeyStroke(i,KeyEvent.CTRL_DOWN_MASK));
					}
					mi.addActionListener(val);
					edit.add(mi);
				}
			});
			{
				JMenuItem mi=new JMenuItem("元に戻す");
				menus.put("編集>元に戻す",mi);
				mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,KeyEvent.CTRL_DOWN_MASK));
				mi.addActionListener(new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent event){
						if(w.ct.history!=null)w.ct.history.undo();
					}
				});
				edit.add(mi);
			}
			{
				JMenuItem mi=new JMenuItem("やり直し");
				menus.put("編集>やり直し",mi);
				mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y,KeyEvent.CTRL_DOWN_MASK));
				mi.addActionListener(new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent event){
						if(w.ct.history!=null)w.ct.history.redoing();
					}
				});
				edit.add(mi);
			}
			JMenu editAll=new JMenu("一括操作");
			{
				JMenuItem mi=new JMenuItem("全て選択");
				menus.put("編集>一括操作>全て選択",mi);
				mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,KeyEvent.CTRL_DOWN_MASK));
				mi.addActionListener(new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent event){
						for(Data d:w.ct.data)
							d.setToggle(true);
					}
				});
				editAll.add(mi);
			}
			{
				JMenuItem mi=new JMenuItem("全て選択解除");
				menus.put("編集>一括操作>全て選択解除",mi);
				mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U,KeyEvent.CTRL_DOWN_MASK));
				mi.addActionListener(new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent event){
						for(Data d:w.ct.data)
							d.setToggle(false);
					}
				});
				editAll.add(mi);
			}
			{
				JMenuItem mi=new JMenuItem("全て削除");
				menus.put("編集>一括操作>全て削除",mi);
				mi.addActionListener(new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent event){
						if(w.ct.data.isEmpty()) return;
						if(WindowLib.Yes_NoWindow("全て削除しますか?","削除しますか?")){
							w.ct.removeAll();
						}
					}
				});
				editAll.add(mi);
			}
			{
				JMenuItem mi=new JMenuItem("画像ファイル読込");
				menus.put("編集>一括操作>画像ファイル読込",mi);
				mi.addActionListener(new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent event){
						ArrayList<File> files=new ArrayList<File>();
						for(Data d:w.ct.data) {
							if(d instanceof DataFile) {
								if(!d.toggle)continue;
								files.add(((DataFile) d).getFile());
							}
						}
						for(File file:files)try{
							Image img= ImageIO.read(file);
							w.ct.addData(new DataImage(w,img));
						}catch(Exception e) {
							e.printStackTrace();
							WindowLib.ErrorWindow("非対応データです","読み取り失敗");
						}
					}
				});
				editAll.add(mi);
			}
			{
				JMenuItem mi=new JMenuItem("テキスト読込");
				menus.put("編集>一括操作>テキスト読込",mi);
				mi.addActionListener(new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent event){
						ArrayList<File> files=new ArrayList<File>();
						for(Data d:w.ct.data) {
							if(d instanceof DataFile) {
								if(!d.toggle)continue;
								files.add(((DataFile) d).getFile());
							}
						}
						for(File file:files)try{
							Charset c=Data.CelectEncode();
							if(c==null)return;
							String text;
							InputStream is=null;
							try{
								is=new FileInputStream(file);
								text=new String(FileEditor.toByteArray(is),c);
							}finally{
								if(is!=null) try{
									is.close();
								}catch(IOException e){
									e.printStackTrace();
								}
							}
							w.ct.addData(new DataText(w,text));
						}catch(NullPointerException e) {

						}catch(Exception e) {
							e.printStackTrace();
							WindowLib.ErrorWindow("非対応データです","読み取り失敗");
						}
					}
				});
				editAll.add(mi);
			}
			{
				JMenuItem mi=new JMenuItem("バイナリ読込");
				menus.put("編集>一括操作>バイナリ読込",mi);
				mi.addActionListener(new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent event){
						if(WindowLib.Yes_NoWindow("選択したファイルのデータを読み込みますか?","確認"));
						else return;
						ArrayList<DataFile> files=new ArrayList<DataFile>();
						for(Data d:w.ct.data) {
							if(d instanceof DataFile) {
								if(!d.toggle)continue;
								files.add((DataFile) d);
							}
						}
						for(DataFile file:files)try{
							file.binary(true);
						}catch(Exception e) {
							e.printStackTrace();
							WindowLib.ErrorWindow("非対応データです","読み取り失敗");
						}
					}
				});
				editAll.add(mi);
			}
			{
				JMenuItem mi=new JMenuItem("バイナリ無読込");
				menus.put("編集>一括操作>バイナリ無読込",mi);
				mi.addActionListener(new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent event){
						if(WindowLib.Yes_NoWindow("選択したファイルをファイルパスとして読み込みますか?","確認"));
						else return;
						ArrayList<DataFile> files=new ArrayList<DataFile>();
						for(Data d:w.ct.data) {
							if(d instanceof DataFile) {
								if(!d.toggle)continue;
								files.add((DataFile) d);
							}
						}
						for(DataFile file:files)try{
							file.binary(false);
						}catch(Exception e) {
							e.printStackTrace();
							WindowLib.ErrorWindow("非対応データです","読み取り失敗");
						}
					}
				});
				editAll.add(mi);
			}
			edit.add(editAll);
			mb.add(edit);
		}
		{
			JMenu setting=new JMenu("設定");
			//setting.setToolTipText("普通に使うには触らなくていい");
			delCheck=new JCheckBoxMenuItem("削除する時に確認する");
			menus.put("設定>削除する時に確認する",delCheck);
			delCheck.setSelected(config.getConfigDataBoolean("削除する時に確認する",true));
			delCheck.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
					config.setConfigDataBoolean("削除する時に確認する",delCheck.isSelected());
				}
			});
			setting.add(delCheck);
			JMenuItem mfont=new JMenuItem("フォントを変更");
			menus.put("設定>フォントを変更",mfont);
			mfont.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
					String[] names=FontManager.getFontNames();
					String name=WindowLib.InputWindow("使用するフォントを選択",names,w.getFontManager().getName());
					if(name!=null) {
						w.setFont(name);
						config.setConfigDataString("font",name);
					}
				}
			});
			setting.add(mfont);
			JMenuItem wt=new JMenuItem("ウィンドウタイトルを変更");
			menus.put("設定>ウィンドウタイトルを変更",wt);
			wt.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
					String name=WindowLib.InputWindow("新しいタイトル",w.getTitle().substring(w.defaultTitle.length()));
					if(name!=null) {
						w.setTitle(w.defaultTitle+" "+name);
						config.setConfigDataString("ウィンドウタイトル",name);
					}
				}
			});
			setting.add(wt);
			JCheckBoxMenuItem inf=new JCheckBoxMenuItem("保存確認画面を表示");
			menus.put("設定>保存確認画面を表示",inf);
			inf.setToolTipText("変更が保存されていない時確認画面を出す");
			inf.setSelected(config.getConfigDataBoolean("保存確認画面を表示",true));//デフォルト有効
			inf.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
					config.setConfigDataBoolean("保存確認画面を表示",inf.isSelected());
				}
			});
			setting.add(inf);
			JCheckBoxMenuItem tool=new JCheckBoxMenuItem("編集ツールを表示");
			menus.put("設定>編集ツールを表示",tool);
			//tool.setToolTipText("変更が保存されていない時確認画面を出す");
			tool.setSelected(config.getConfigDataBoolean("編集ツールを表示",true));//デフォルト有効
			tool.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
					config.setConfigDataBoolean("編集ツールを表示",tool.isSelected());
					w.getTool().active=tool.isSelected();
				}
			});
			w.getTool().active=tool.isSelected();
			setting.add(tool);
			JCheckBoxMenuItem kotei=new JCheckBoxMenuItem("編集ツールを固定");
			menus.put("設定>編集ツールを固定",kotei);
			kotei.setSelected(config.getConfigDataBoolean("編集ツールを固定",false));//デフォルト無効
			kotei.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
					config.setConfigDataBoolean("編集ツールを固定",kotei.isSelected());
					w.getTool().move=!kotei.isSelected();
				}
			});
			w.getTool().move=!kotei.isSelected();
			setting.add(kotei);
			JCheckBoxMenuItem line=new JCheckBoxMenuItem("項目の間に線を表示");
			menus.put("設定>項目の間に線を表示",line);
			line.setSelected(config.getConfigDataBoolean("項目の間に線を表示",false));//デフォルト無効
			line.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
					config.setConfigDataBoolean("項目の間に線を表示",line.isSelected());
					w.drawLine=line.isSelected();
				}
			});
			w.drawLine=line.isSelected();
			setting.add(line);
			JCheckBoxMenuItem auto=new JCheckBoxMenuItem("起動時に前回のファイルを開く");
			menus.put("設定>起動時に前回のファイルを開く",auto);
			auto.setSelected(config.getConfigDataBoolean("起動時に前回のファイルを開く",false));//デフォルト無効
			auto.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
					config.setConfigDataBoolean("起動時に前回のファイルを開く",auto.isSelected());
				}
			});
			setting.add(auto);
			JCheckBoxMenuItem History=new JCheckBoxMenuItem("履歴を残さない");
			menus.put("設定>履歴を残さない",History);
			History.setSelected(config.getConfigDataBoolean("履歴を残さない",false));//デフォルト残す
			History.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
					if(History.isSelected()) {
						w.ct.history=null;
					}else if(w.ct.history==null) {
						w.ct.history=new HistoryManager(w.ct);
					}
					config.setConfigDataBoolean("履歴を残さない",History.isSelected());
				}
			});
			setting.add(History);
			JMenuItem autoAdd=new JMenuItem("自動追加");
			autoAdd.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
					try{
						String[] a= {"0以下で無効","入力値以上の時、古い物から削除"};
						String size= (String)JOptionPane.showInputDialog(null,a,"自動追加",
								JOptionPane.QUESTION_MESSAGE,null,null,Integer.toString(w.getAutoAdd()));
						if(size==null||size.isEmpty())return;
						int i=(int) DataEditor.parseNumber(size,-1);
						if(i>=0)w.setAutoAdd(i);
						else return;
						config.setConfigDataInt("自動追加",i);
					}catch(Exception ex) {
						WindowLib.WarningWindow("設定に失敗しました","失敗");
					}
				}
			});
			setting.add(autoAdd);
			JMenuItem plugin=new JMenuItem("プラグイン設定");
			menus.put("設定>プラグイン設定",plugin);
			plugin.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
					w.plugin.openDialog();
				}
			});
			setting.add(plugin);
			{
				JMenu adv=new JMenu("高度な設定");
				JCheckBoxMenuItem img=new JCheckBoxMenuItem("常に小さい画像を表示");
				menus.put("設定>高度な設定>常に小さい画像を表示",img);
				img.setSelected(config.getConfigDataBoolean("常に小さい画像を表示"));
				img.addActionListener(new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent e){
						ct.imageView=img.isSelected();
						config.setConfigDataBoolean("常に小さい画像を表示",img.isSelected());
					}
				});
				adv.add(img);
				JCheckBoxMenuItem syukusyou=new JCheckBoxMenuItem("大きい画像を縮小して表示");
				menus.put("設定>高度な設定>大きい画像を縮小して表示",syukusyou);
				syukusyou.setSelected(config.getConfigDataBoolean("大きい画像を縮小して表示",true));
				syukusyou.addActionListener(new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent e){
						//縮小するしきい値を最大値に設定する
						if(syukusyou.isSelected()) w.imageViewSize=new int[] { 512, 512 };
						else w.imageViewSize=new int[] { Integer.MAX_VALUE, Integer.MAX_VALUE };
						config.setConfigDataBoolean("大きい画像を縮小して表示",syukusyou.isSelected());
					}
				});
				adv.add(syukusyou);
				JCheckBoxMenuItem decode=new JCheckBoxMenuItem("URLエンコード解除");
				menus.put("設定>高度な設定>URLエンコード解除",decode);
				decode.setToolTipText("httpで始まる文字列をURLエンコード解除して表示する");
				decode.setSelected(config.getConfigDataBoolean("URLエンコード解除",true));//デフォルト有効
				decode.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
						config.setConfigDataBoolean("URLエンコード解除",decode.isSelected());
					}
				});
				adv.add(decode);
				JCheckBoxMenuItem single=new JCheckBoxMenuItem("一括読み取り処理");
				menus.put("設定>高度な設定>一括読み取り処理",single);
				single.setToolTipText("全て読み取り終わってからまとめて処理をする");
				single.setSelected(config.getConfigDataBoolean("一括読み取り処理",false));//デフォルト無効
				single.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
						config.setConfigDataBoolean("一括読み取り処理",single.isSelected());
					}
				});
				adv.add(single);
				JCheckBoxMenuItem full=new JCheckBoxMenuItem("フルパス表示");
				menus.put("設定>高度な設定>フルパス表示",full);
				full.setToolTipText("ファイルパスをフルパスで表示する");
				full.setSelected(config.getConfigDataBoolean("フルパス表示",true));//デフォルト有効
				full.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
						fullPath=full.isSelected();
						config.setConfigDataBoolean("フルパス表示",fullPath);
					}
				});
				adv.add(full);
				fullPath=full.isSelected();
				{
					JCheckBoxMenuItem crlf=new JCheckBoxMenuItem("改行とタブを表示");
					menus.put("設定>高度な設定>改行とタブを表示",crlf);
					crlf.setToolTipText("改行とタブを矢印で表示する");
					crlf.setSelected(config.getConfigDataBoolean("改行とタブ表示",false));//デフォルト無効
					paintCRLF=crlf.isSelected();
					crlf.addActionListener(new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent e){
							paintCRLF=crlf.isSelected();
							config.setConfigDataBoolean("改行とタブ表示",paintCRLF);
						}
					});
					adv.add(crlf);
				}
				JCheckBoxMenuItem kei=new JCheckBoxMenuItem("軽量化");
				menus.put("設定>高度な設定>軽量化",kei);
				kei.setSelected(config.getConfigDataBoolean("軽量化"));
				w.kei=kei.isSelected();
				kei.addActionListener(new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent e){
						w.kei=kei.isSelected();
						config.setConfigDataBoolean("軽量化",kei.isSelected());
					}
				});
				adv.add(kei);
				{
					JMenu tmp=new JMenu("一時ファイル");
					useTempFile=new JCheckBoxMenuItem("一時ファイルを使用する");
					useTempFile.setSelected(config.getConfigDataBoolean("一時ファイルを使用する",true));
					useTempFile.addActionListener(new ActionListener(){
						@Override
						public void actionPerformed(ActionEvent e){
							config.setConfigDataBoolean("一時ファイルを使用する",useTempFile.isSelected());
						}
					});
					tmp.add(useTempFile);
					minCopy=new JCheckBoxMenuItem("縮小コピーを保持する");
					minCopy.setSelected(config.getConfigDataBoolean("縮小コピーを保持する",true));
					minCopy.addActionListener(new ActionListener(){
						@Override
						public void actionPerformed(ActionEvent e){
							config.setConfigDataBoolean("縮小コピーを保持する",minCopy.isSelected());
						}
					});
					tmp.add(minCopy);
					useTempMemory=config.getConfigDataInt("一時ファイルを使用するメモリ使用量",useTempMemory);
					JMenuItem tmpSize=new JMenuItem("一時ファイルを使用するメモリ使用量を変更");
					tmpSize.addActionListener(new ActionListener(){
						@Override
						public void actionPerformed(ActionEvent e){
							try{
								String size=WindowLib.InputWindow("一時ファイルを使用するメモリ使用量を変更",Integer.toString(useTempMemory)+"MB");
								if(size==null)return;
								int i=(int) DataEditor.parseNumber(size,-1);
								if(i>0)useTempMemory=i;
								else throw new Exception();
								config.setConfigDataInt("一時ファイルを使用するメモリ使用量",i);
							}catch(Exception ex) {
								WindowLib.WarningWindow("設定に失敗しました","失敗");
							}
						}
					});
					tmp.add(tmpSize);
					JMenuItem tmpDir=new JMenuItem("一時ファイルの場所を開く");
					menus.put("設定>高度な設定>一時ファイル>一時ファイルの場所を開く",tmpDir);
					tmpDir.addActionListener(new ActionListener(){
						@Override
						public void actionPerformed(ActionEvent e){
							try{
								Desktop.getDesktop().open(new File(System.getProperty("java.io.tmpdir")+"/ClipTemporary/"));
							}catch(Exception ex) {
								WindowLib.WarningWindow("表示に失敗しました","失敗");
							}
						}
					});
					tmp.add(tmpDir);
					JMenuItem tmpDel=new JMenuItem("一時ファイルを全て削除");
					tmpDel.setToolTipText("自動で削除できなっかった一時ファイルを消す。誤作動するかもしれない");
					tmpDel.addActionListener(new ActionListener(){
						@Override
						public void actionPerformed(ActionEvent e){
							EditFile.deleteTemp();
						}
					});
					tmp.add(tmpDel);
					adv.add(tmp);
				}
				setting.add(adv);
			}
			{
				JMenu file=new JMenu("設定ファイル");
				JMenuItem openF=new JMenuItem("設定ファイルを開く");
				openF.addActionListener(new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent e){
						if(Desktop.isDesktopSupported()) {
							Desktop desktop=Desktop.getDesktop();
							try{
								desktop.edit(new File(config.file));
							}catch(IOException e1){
								try{
									desktop.open(new File(config.file));
								}catch(IOException e2){
									WindowLib.WarningWindow("ファイルを開けません","失敗");
								}
							}
						}else WindowLib.WarningWindow("ファイルを開けません","失敗");
					}
				});
				file.add(openF);
				JMenuItem update=new JMenuItem("設定ファイルを現在の設定に更新");
				menus.put("設定>設定ファイル>設定ファイルを現在の設定に更新",update);
				update.addActionListener(new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent e){
						if(w.writeConfig(null));
						else WindowLib.WarningWindow("正常に保存できませんでした","失敗");
					}
				});
				file.add(update);
				JMenuItem openD=new JMenuItem("設定ファイルの場所を開く");
				menus.put("設定>設定ファイル>設定ファイルの場所を開く",openD);
				openD.addActionListener(new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent e){
						if(Desktop.isDesktopSupported()) {
							Desktop desktop=Desktop.getDesktop();
							try{
								desktop.open(new File(config.file).getParentFile());
							}catch(Exception e1){
								WindowLib.WarningWindow("ディレクトリを開けません","失敗");
							}
						}else WindowLib.WarningWindow("ディレクトリを開けません","失敗");
					}
				});
				file.add(openD);
				JMenuItem read=new JMenuItem("設定を読み込む");
				read.addActionListener(new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent e){
						if(!WindowLib.Yes_NoWindow("確認","現在の設定は破棄されます。","読み込みを続行しますか"))return;
						JFileChooser fc=new JFileChooser((String)null);
						fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
						fc.setDialogTitle("開く");
						FileFilter[] f=fc.getChoosableFileFilters();
						for(int i=0;i<f.length;i++)fc.removeChoosableFileFilter(f[i]);
						fc.addChoosableFileFilter(new FileNameExtensionFilter("設定ファイル","cfg"));
						fc.addChoosableFileFilter(fc.getAcceptAllFileFilter());
						int ret=fc.showOpenDialog(null);
						if(ret!=JFileChooser.APPROVE_OPTION)return;
						File file=new File(config.file);
						file.delete();
						if(!FileEditor.FE.copyFileToFile(fc.getSelectedFile(),file)){
							WindowLib.WarningWindow("設定を読み込めませんでした","読み込み失敗");
						}else{
							w.loadConfig();
							WindowLib.InfoWindow("読み込み成功","設定を読み込みました。","一部の設定は再起動した時に適用されます。");
						}
					}
				});
				file.add(read);
				JMenuItem write=new JMenuItem("設定を書き出す");
				write.addActionListener(new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent e){
						File f=WindowLib.SaveFileWindow(new File("設定.cfg"));
						if(f==null)return;
						String path=f.getAbsolutePath();
						if(!path.endsWith(".cfg"))f=new File(path+".cfg");
						boolean b=f.exists();
						if(b&&!WindowLib.Yes_NoWindow("上書き保存","ファイルが既に存在します","上書き保存しますか"))return;
						File file=new File(config.file);
						if(!FileEditor.FE.copyFileToFile(file,f)){
							WindowLib.WarningWindow("設定を書き出せませんでした","書き出し失敗");
						}else if(!b)WindowLib.InfoWindow("設定を書き出しました。","書き出し成功");
					}
				});
				file.add(write);
				JMenuItem reset=new JMenuItem("設定を初期化");
				reset.addActionListener(new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent e){
						if(WindowLib.Yes_NoWindow("設定を初期化しますか","確認"))
						if(config instanceof MapConfig){
							new File(config.file).delete();
							((MapConfig)config).map.clear();
							WindowLib.InfoWindow("設定を初期化しました。","初期化成功");
						}else WindowLib.InfoWindow("設定を初期化できませんでした。","初期化失敗");
					}
				});
				file.add(reset);
				setting.add(file);
			}
			mb.add(setting);
		}
		{
			JMenu jm=new JMenu("その他");
			JMenuItem miReset=new JMenuItem("位置初期化");
			menus.put("その他>位置初期化",miReset);
			miReset.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
					w.getTool().setPoint(0,0);
					ct.setPoint(0,0);
					w.musicPlayer.setPoint(0,0);
				}
			});
			jm.add(miReset);
			JMenuItem gc=new JMenuItem("ガベージコレクタ");
			menus.put("その他>ガベージコレクタ",gc);
			gc.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
					System.gc();
					System.runFinalization();
				}
			});
			jm.add(gc);
			JMenuItem info=new JMenuItem("バージョン情報");
			info.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
					String[] arr= {"バージョン"+ClipTemporary.version,
							ClipTemporary.version.getDate()+"更新"};
					WindowLib.InfoWindowStop(arr,"バージョン情報");
				}
			});
			jm.add(info);
			JMenuItem info2=new JMenuItem("改行とタブの記号");
			info2.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
					String[] arr= {
							"TAB　→",
							"CR　　←",
							"LF　　↓",
							"CRLF　↵"};
					WindowLib.InfoWindowStop(arr,"改行とタブの記号");
				}
			});
			jm.add(info2);
			JCheckBoxMenuItem ren=new JCheckBoxMenuItem("音楽連続再生");
			menus.put("その他>音楽連続再生",ren);
			ren.setSelected(config.getConfigDataBoolean("音楽連続再生"));
			w.musicPlayer.setSequentialPlay(ren.isSelected());
			ren.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
					w.musicPlayer.setSequentialPlay(ren.isSelected());
					config.setConfigDataBoolean("音楽連続再生",ren.isSelected());
				}
			});
			jm.add(ren);
			JMenuItem las=new JMenuItem("最後に再生したファイルを再生");
			menus.put("その他>最後に再生したファイルを再生",las);
			las.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
					w.musicPlayer.setSequentialPlay(ren.isSelected());
					String s=config.getConfigDataString("最後に再生したファイル");
					if(s==null) {
						WindowLib.InfoWindowStop("最後に再生したファイルの記録がありません","失敗");
						return;
					}
					File f=new File(s);
					if(!f.isFile()) {
						WindowLib.InfoWindowStop("最後に再生したファイルがシステムにありません","失敗");
						return;
					}
					String fap=f.getAbsolutePath();
					DataFile Last=null;
					for(Data d:w.ct.data){
						if(d instanceof DataFile) {
							DataFile df=(DataFile) d;
							File dff=df.getData();
							if(dff==null)continue;
							String dffap=dff.getAbsolutePath();
							if(fap.equals(dffap)) {
								Last=df;
								break;
							}
						}
					}
					if(Last==null) {
						WindowLib.InfoWindowStop("最後に再生したファイルがリストにありません","失敗");
						return;
					}
					try{
						Last.playSound();
					}catch(Exception e1){
						e1.printStackTrace();
						DataEditor.printThread();
						String[] s1= {"エラーが発生しました",e1.toString()};
						WindowLib.WarningWindowStop(s1,"失敗");
					}
				}
			});
			jm.add(las);
			JMenu license=new JMenu("ライセンス");
			license.setToolTipText("ライブラリのライセンスを表示する");
			{
			JMenuItem JLayer=new JMenuItem("JLayer");
			JLayer.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
					new Thread("JLayerLicense"){
						@Override
						public void run(){
							try{
								JarLocalFile jlf=new JarLocalFile("javazoom/LICENSE.txt");
								String lic=FileEditor.FE.readStreamT(jlf.getURL().openStream());
								WindowLib.InputArrayWindow("JLayerライセンス",lic);
							}catch(Exception e1){
								e1.printStackTrace();
							}
						}
					}.start();
				}
			});
			license.add(JLayer);
			}
			jm.add(license);
			{
			JMenu add=new JMenu("追加");
			JMenuItem text=new JMenuItem("テキスト");
			text.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
					new Thread(){
						public void run(){
							DataText d=new DataText(w, "");
							w.getMainPanel().addData(d);
							d.editWindow();
						}
					}.start();
				}
			});
			add.add(text);
			JMenuItem file=new JMenuItem("ファイル(選択)");
			file.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
					new Thread(){
						public void run(){
							JFileChooser fc=new JFileChooser(new File(FileEditor.USERHOME));
							fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);//指定した物のみを選択可能にする
							fc.setDialogTitle("選択");
							int ret=fc.showOpenDialog(null);
							if(ret!=JFileChooser.APPROVE_OPTION)return;
							File f=fc.getSelectedFile();
							DataFile d=new DataFile(w, f);
							w.getMainPanel().addData(d);
						}
					}.start();
				}
			});
			add.add(file);
			JMenuItem fileT=new JMenuItem("ファイル(パス入力)");
			fileT.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
					new Thread(){
						public void run(){
							String[] arr= {"相対パスの場合、実行Jarファイルの位置が基準になる。"};
							String f=(String)JOptionPane.showInputDialog(w,arr,"ファイルパス入力",
									JOptionPane.PLAIN_MESSAGE,null,null,"");
							if(f==null)return;
							DataFile d=new DataFile(w,new File(f));
							w.getMainPanel().addData(d);
						}
					}.start();
				}
			});
			add.add(fileT);
			jm.add(add);
			}
			mb.add(jm);
		}
		w.setJMenuBar(mb);
		if(config.contains("font")) {
			w.setFont(config.getConfigDataString("font"));
		}
	}
	public static ImageIcon setIcon(String jlf,String file,int maxSize,AbstractButton... bt){
		try{
			URL url=new JarLocalFile(jlf).getURL();
			if(url==null){
				File f=new File(file);
				if(f.exists()) url=f.toURI().toURL();
			}
			if(url!=null){
				Image img=ImageIO.read(url);
				if(img.getWidth(null)>maxSize||img.getHeight(null)>maxSize)
					img=FileEditor.toBufferedImage(img,16,16);
				ImageIcon icon=new ImageIcon(img);
				for(AbstractButton b:bt)
					b.setIcon(icon);
				return icon;
			}
			//miSaveU.setFont(WindowLib.getBOLD_Font(40));
		}catch(IOException e1){
			e1.printStackTrace();
		}
		return null;
	}
}
