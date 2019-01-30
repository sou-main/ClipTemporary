package sousoft.omake.cliptemporary.musicplayer;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.decoder.SampleBuffer;
import javazoom.jl.player.AudioDevice;
import soulib.lib.ConfigBase;
import soulib.sound.SoundFileBGM;
import sousoft.omake.cliptemporary.CTWindow;

public interface ISoundBase extends Closeable{
	public static interface EndEvent{
		public void end(ISoundBase s);
	}
	long getLoadFrame();
	boolean isEnd();
	long getFrameLength();
	boolean stop(boolean b);
	void play();
	void End();
	void setLoop(boolean toggle);
	float setVol(float volN);
	void setEndEvent(EndEvent e);
	File getFile();
	boolean isStop();
	EndEvent getEndEvent();
	boolean isRoop();
	public static class BasicPlayer extends SoundFileBGM implements ISoundBase{

		private byte[] data;
		private EndEvent event=null;
		private final File file;
		private CTWindow window;

		public BasicPlayer(File f,CTWindow w) throws UnsupportedAudioFileException,IOException,LineUnavailableException{
			super(f);
			file=f;
			window=w;
		}
		@Override
		public void setEndEvent(EndEvent e){
			event=e;
		}
		/** 1回に出力するバイト数 */
		@Override
		public int getBufferSize(){
			if(data!=null) return data.length;
			return 0;
		}
		@Override
		public void run(){
			playNow++;
			InputStream st=null;
			try{
				data=new byte[line.getBufferSize()];
				int bytesRead;
				try{
					ais.close();
				}catch(Exception e){
					e.printStackTrace();
				}
				if(window!=null) {
					ConfigBase c=window.getConfig();
					c.setConfigDataString("最後に再生したファイル",file.getAbsolutePath());
				}
				stop=false;
				do{
					AudioInputStream fis=AudioSystem.getAudioInputStream(new BufferedInputStream(st=url.openStream()));
					//ais=AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED,fis);
					ais = AudioSystem.getAudioInputStream(format, fis);
					playFrame=0;
					while((bytesRead=ais.read(data,0,data.length))!=-1){
						playFrame+=bytesRead/format.getFrameSize();
						line.write(data,0,bytesRead);
						//if(getFrameLength()-playFrame<60000)break;
						if(super.stop) try{
							nowStop=true;
							thread.join();
						}catch(InterruptedException t){
							nowStop=false;
						}
						if(end)break;
					}
					if(end)break;
					line.drain();
				}while(isRoop());
				this.stop=true;
				if(event!=null&&!end)event.end(this);
				end=true;
			}catch(Exception e){
				e.printStackTrace();
			}finally{
				playNow--;
				try{
					ais.close();
				}catch(Exception e){
					e.printStackTrace();
				}
				if(st!=null) try{
					st.close();
				}catch(IOException e){
					e.printStackTrace();
				}
				thread=new Thread(this,"sound="+url);
			}
		}
		@Override
		public File getFile(){
			return file;
		}
		public boolean isStop() {
			return stop;
			//return super.isStop();
		}
		@Override
		public EndEvent getEndEvent(){
			return event;
		}
	}
	public static class MP3Player implements Runnable,ISoundBase{

		private int frame=0;
		private Bitstream bitstream;
		private Decoder decoder;
		private MP3Device audio;
		private boolean closed=false;
		private boolean complete=false;
		private BufferedInputStream stream;
		private boolean is_run,stop,loop,end;
		private final File file;
		private float vol=50;
		public Thread thread;
		private boolean initVol;
		private EndEvent event;
		private CTWindow window;

		public MP3Player(File file,CTWindow w){
			this.file=file;
			window=w;
		}
		public void close(){
			try{
				if(stream!=null){
					AudioDevice audioDevice=this.audio;
					if(audioDevice!=null){
						this.closed=true;
						this.audio=null;
						audioDevice.close();
						try{
							this.bitstream.close();
						}catch(BitstreamException bitstreamException){
							// empty catch block
						}
					}
					stream.close();
				}
			}catch(IOException e){
				System.out.println("IOException");
			}finally{
				stream=null;
				is_run=false;
			}
		}
		public synchronized void run(){
			if(stream!=null||is_run){
				return;
			}else{
				is_run=true;
				end=false;
				complete=false;
				if(window!=null) {
					ConfigBase c=window.getConfig();
					c.setConfigDataString("最後に再生したファイル",file.getAbsolutePath());
				}
				do{
					setFile();
					try{
						boolean bl=true;
						while(bl){
							if(stop) try{
								thread.join();
							}catch(InterruptedException t){

							}
							bl=this.decodeFrame();
							if(end)break;
						}
						if(this.audio!=null){
							audio.flush();
							synchronized(this){
								this.complete=!this.closed;
								this.close();
							}
						}
					}catch(JavaLayerException e){
						e.printStackTrace();
						System.out.println("JavaLayerException");
					}finally{

					}
					if(end)break;
				}while(loop);
				if(event!=null&&!end)event.end(this);
			}
		}
		protected boolean decodeFrame() throws JavaLayerException{
			try{
				if(audio==null){
					return false;
				}
				Header header=this.bitstream.readFrame();
				if(header==null){
					return false;
				}
				SampleBuffer sampleBuffer=(SampleBuffer) this.decoder.decodeFrame(header,this.bitstream);
				int fz=audio.getAudioFormat().getFrameSize()<1?1:audio.fmt.getFrameSize();
				synchronized(this){
					if(audio!=null){
						frame+=sampleBuffer.getBufferLength()/fz;
						audio.write(sampleBuffer.getBuffer(),0,sampleBuffer.getBufferLength());
					}
					if(!initVol&&audio.source!=null) {
						setVol(vol);
						/*
						if(audio.source.isControlSupported(FloatControl.Type.SAMPLE_RATE)) {
							System.out.println("SampleRateControlSupported");
						}else System.out.println("SampleRateControlNOTSupported");
						*/
						initVol=true;
					}
				}
				this.bitstream.closeFrame();
			}catch(RuntimeException runtimeException){
				runtimeException.printStackTrace();
				throw new JavaLayerException("Exception decoding audio frame",(Throwable) runtimeException);
			}
			return true;
		}
		private void setFile(){
			try{
				stream=new BufferedInputStream((new FileInputStream(file)));
				this.bitstream=new Bitstream(stream);
				this.decoder=new Decoder();
				//FactoryRegistry factoryRegistry=FactoryRegistry.systemRegistry();
				//this.audio=factoryRegistry.createAudioDevice();
				audio=new MP3Device();
				audio.open(this.decoder);
				initVol=false;
				closed=false;
				frame=0;
			}catch(IOException e){
				// System.out.println("BUFFER ERR");
				System.out.println("IOException");
			}catch(JavaLayerException e){
				// System.out.println("PLAYER ERR");
				System.out.println("JavaLayerException");
			}
		}
		@Override
		public long getLoadFrame(){
			return frame;
		}
		@Override
		public boolean isEnd(){
			return this.complete;
		}
		@Override
		public long getFrameLength(){
			//if(audio!=null)return audio.getAudioFormat().getFrameSize();
			return 0;//file.length();
		}
		@Override
		public boolean stop(boolean b){
			this.stop=b;
			if(!stop)thread.interrupt();
			return stop;
		}
		@Override
		public void play(){
			thread=new Thread(this,"MP3PlayerFile="+file.getAbsolutePath());
			thread.start();
		}
		@Override
		public void End(){
			end=true;
		}
		@Override
		public void setLoop(boolean toggle){
			loop=toggle;
		}
		@Override
		public float setVol(float vol){
			this.vol=vol;
			if(audio!=null)setVol(vol,audio.source);
			return vol;
		}
		/**音量設定
		 * @return 実際に設定された音量*/
		public float setVol(float vol,SourceDataLine line){
			if(line==null)return this.vol;
			try{
				FloatControl control=(FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
				float f=(float) Math.log10(vol/100d)*20f;
				if(vol<=0) {
					f=control.getMinimum();
					vol=0;
				}else if(vol>=200) {
					f=control.getMaximum();
					vol=200;
				}
				if(f>control.getMaximum()) return vol;
				if(f<control.getMinimum()) return vol;
				if(f==Float.NaN) return vol;
				control.setValue(f);
				return vol;
			}catch(Exception n){
				n.printStackTrace();
			}
			return this.vol;
		}
		@Override
		public void setEndEvent(EndEvent e){
			event=e;
		}
		@Override
		public File getFile(){
			return file;
		}
		@Override
		public boolean isStop(){
			return stop;
		}
		@Override
		public EndEvent getEndEvent(){
			return event;
		}
		@Override
		public boolean isRoop(){
			return loop;
		}
	}
}