package sousoft.omake.cliptemporary;

import java.util.ArrayList;
import java.util.List;

import sousoft.omake.cliptemporary.data.Data;
import sousoft.omake.cliptemporary.data.DataText;

public interface History{
	/**元に戻す*/
	public void undo();
	/**やり直し*/
	public void redoing();
	public static class HistoryManager{
		public ClipTemporaryPanel panel;
		public List<History> history=new ArrayList<History>();
		public int historyIndex;
		public int maxHistory=40;
		public HistoryManager(ClipTemporaryPanel ctp) {
			panel=ctp;
		}
		public void add(History h){
			if(historyIndex<history.size()-1) {
				history.clear();
				/*
				for(int i=historyIndex+1;i<history.size();i++) {
					history.remove(i);
				}
				*/
			}
			if(maxHistory<=history.size())history.remove(maxHistory-1);
			history.add(h);
			historyIndex=history.size()-1;
			if(ClipTemporary.debug)System.out.println("addHistory="+h.getClass());
		}
		public void clear() {
			history.clear();
			historyIndex=-1;
		}
		public void undo(){
			History h=get(historyIndex);
			if(h==null)return;
			if(historyIndex>=0)historyIndex--;
			else return;
			if(ClipTemporary.debug)System.out.println("undo="+h.getClass().getName());
			h.undo();
		}
		public void redoing() {
			if(historyIndex<-1)return;
			if(historyIndex<history.size()-1)historyIndex++;
			else return;
			History h=get(historyIndex);
			if(h==null)return;
			if(ClipTemporary.debug)System.out.println("redoing="+h.getClass().getName());
			h.redoing();
		}
		public History get(int i){
			if(i<0)return null;
			if(i>=history.size())return null;
			return history.get(i);
		}
	}
	public static abstract class DataHistory implements History{
		protected Data[] data;
		protected int[] index;
		public DataHistory(Data... d){
			data=d;
			index=new int[data.length];
			for(int i=0;i<data.length;i++) {
				index[i]=data[i].getPanel().data.indexOf(data[i]);
			}
		}
		public Data[] getData() {
			return data;
		}
		public int[] getIndex(){
			return index;
		}
	}
	public static class HistoryAdd extends DataHistory{
		public HistoryAdd(Data...read) {
			super(read);
		}
		public HistoryAdd(List<? extends Data> read){
			this(read.toArray(new Data[read.size()]));
		}
		@Override
		public void undo(){
			for(int i=0;i<data.length;i++) {
				data[i].getPanel().remove(data[i],false);
				data[i].getPanel().addData();
			}
		}
		@Override
		public void redoing(){
			for(int i=0;i<data.length;i++) {
				if(index[i]<0)continue;
				data[i].getPanel().addData(index[i],data[i],false);
				data[i].getPanel().addData();
			}
		}
	}
	public static class HistoryRemove extends DataHistory{
		public HistoryRemove(Data...read) {
			super(read);
		}
		public HistoryRemove(List<? extends Data> read){
			this(read.toArray(new Data[read.size()]));
		}
		@Override
		public void redoing(){
			for(int i=0;i<data.length;i++) {
				data[i].getPanel().remove(data[i],false);
				data[i].getPanel().addData();
			}
		}
		@Override
		public void undo(){
			for(int i=0;i<data.length;i++) {
				if(index[i]<0)continue;
				data[i].getPanel().addData(index[i],data[i],false);
				data[i].getPanel().addData();
			}
		}
	}
	public static class HistoryMove implements History{
		private int[] oldIndex,newIndex;
		private ClipTemporaryPanel panel;
		public HistoryMove(ClipTemporaryPanel target,int[] i, int[] j) {
			panel=target;
			oldIndex=i;
			newIndex=j;
		}
		@Override
		public void redoing(){
			for(int i=0;i<oldIndex.length;i++) {
				Data d=panel.data.remove(oldIndex[i]);
				panel.data.add(newIndex[i],d);
			}
			panel.sort();
		}
		@Override
		public void undo(){
			for(int i=0;i<oldIndex.length;i++) {
				Data d=panel.data.remove(newIndex[i]);
				panel.data.add(oldIndex[i],d);
			}
			panel.sort();
		}
	}
	public static class EditTextHistory implements History{
		private String oldText,newText;
		private DataText dt;
		public EditTextHistory(DataText d,String nd) {
			dt=d;
			oldText=dt.getData();
			newText=nd;
		}
		@Override
		public void undo(){
			dt.setData(oldText,false);
		}
		@Override
		public void redoing(){
			dt.setData(newText,false);
		}
	}
	public static class HistoryGroup implements History{
		private ArrayList<History> list=new ArrayList<History>();
		public void add(History e) {
			list.add(e);
		}
		@Override
		public void undo(){
			for(int i=list.size();i>0;i--) {
				list.get(i-1).undo();
			}
		}
		@Override
		public void redoing(){
			for(History li:list) {
				li.redoing();
			}
		}
	}
}