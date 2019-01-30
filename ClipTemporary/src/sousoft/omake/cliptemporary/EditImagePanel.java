package sousoft.omake.cliptemporary;

import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import soulib.windowLib.CanvasComponentManager;
import soulib.windowLib.CommonGraphics;
import soulib.windowLib.CanvasComponents.CButton;
import soulib.windowLib.CanvasComponents.CPanel;
import soulib.windowLib.CanvasComponents.CanvasComponent;

public class EditImagePanel extends CPanel{
	private ImagePanel image;
	private CanvasComponentManager histry;
	public EditImagePanel() {
		super(true,true);
	}
	{
		image=new ImagePanel();
		CButton cc=new CButton("終了");
		cc.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e){
				if(histry==null)return;
				image.clearImage();
				show(histry);
			}
		});
		addCanvasComponent(cc);
		addCanvasComponent(image,0,cc.getH());
	}
	public Image getImage(){
		return image.getImage();
	}
	public void setImage(Image image){
		this.image.setImage(image);
	}
	public void show(CanvasComponentManager p) {
		histry=p;
		//System.out.println("showImageEditor="+(p!=null&&!image.isEmpty()));
		if(image.isEmpty())p.removeCanvasComponent(this);
		else p.addCanvasComponent(this);
	}
	private class ImagePanel extends CanvasComponent{
		private Image image;
		public Image getImage() {
			return image;
		}
		public void clearImage(){
			image=null;
		}
		public boolean isEmpty(){
			return image==null;
		}
		public void setImage(Image img) {
			image=img;
			setW(image.getWidth(null));
			setH(image.getHeight(null));
		}
		@Override
		public void draw(CommonGraphics g,Point p){
			g.drawImage(image,getX(),getY(),getW(),getH());
		}
	}
}
