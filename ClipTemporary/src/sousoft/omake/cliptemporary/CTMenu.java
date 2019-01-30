package sousoft.omake.cliptemporary;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;

import soulib.windowLib.CommonGraphics;
import soulib.windowLib.CanvasComponents.CButton;

public class CTMenu extends CButton{
	private CTWindow window;
	public CTMenu(CTWindow w,String string){
		super(string);
		window=w;
	}
	@Override
	public int getW() {
		return Math.max(super.getW(),window.getTool().getW());
	}
	protected final void click(MouseEvent e){
		super.click(e);
		window.ct.hideMenu(null);
	}
	@Override
	public void draw(CommonGraphics g,Point e){
		if(isInside(e))setBackground(Color.cyan);
		else setBackground(Color.lightGray);
		super.draw(g,e);
	}
}
