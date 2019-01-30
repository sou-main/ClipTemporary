package sousoft.omake.cliptemporary.musicplayer;

import java.io.IOException;
import java.util.List;

import sousoft.omake.cliptemporary.data.Data;
import sousoft.omake.cliptemporary.data.DataFile;
import sousoft.omake.cliptemporary.musicplayer.ISoundBase.EndEvent;

public class SequentialPlayEvent implements EndEvent{
	private MusicPlayer mp;
	private DataFile dataFile;
	private Line line;

	public SequentialPlayEvent(MusicPlayer m, Line line, DataFile dataFile) {
		mp=m;
		this.dataFile=dataFile;
		this.line=line;
	}
	@Override
	public void end(ISoundBase s){
		if(!mp.isSequentialPlay())return;
		List<Data> d=mp.ctWindow.ct.data;
		int index=d.indexOf(dataFile);
		index++;
		if(index<1)index=0;
		DataFile next=null;
		ISoundBase sound=null;
		for(int i=index;i<d.size();i++) {
			Data d0=d.get(i);
			if(d0.getToggle()&&d0 instanceof DataFile) {
				try{
					sound=((DataFile)d0).getSound();
					if(sound!=null) {
						next=(DataFile) d0;
						break;
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		if(next==null) {
			for(int i=0;i<index;i++) {
				Data d0=d.get(i);
				if(d0.getToggle()&&d0 instanceof DataFile) {
					try{
						sound=((DataFile)d0).getSound();
						if(sound!=null) {
							next=(DataFile) d0;
							break;
						}
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}
		}
		if(next!=null&&sound!=null) {
			try{
				line.sb.close();
			}catch(IOException e){
				e.printStackTrace();
			}
			sound.play();
			next.sound=mp.add(sound,sound.getFile().getName(),next);
			mp.remove(line);
		}
	}
}