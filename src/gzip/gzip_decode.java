package gzip;


import java.util.*;
import java.io.*;

class inbits{
	int readmask;
	int maxsize = 32<<10;
	int maxtextsize = 2000000;
	byte bytebuffer;
	byte inbuf[];
	int bufleft, bufpos;
	boolean eofile;
	int ans;
    FileInputStream in;
    inbits(String infile){
    	 try{
             File f = new File(infile);
             in = new FileInputStream(f);
         }
         catch(IOException e){
             System.out.println("file open error" + e);
         }
    	 inbuf = new byte[maxsize];
    	 eofile = false;
    }
    boolean GetBit() {
        try {
            if ((readmask >>= 1) == 0) {
            	if (bufleft <= 0){
            		bufleft = in.read(inbuf,0,maxsize);
            		bufpos = 0;
            		if(bufleft==-1){
            			bufleft = 0;
            			eofile = true;
            			return false;
            		}
            	}
                //readbuffer = in.read();
            	bytebuffer = inbuf[bufpos];
            	bufpos++;
            	bufleft--;
                readmask = 128;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        ans = 0;
        //从左至右对bytebuffer每一位进行取值
        if ((bytebuffer & readmask) != 0) {
            ans = 1;
        }
        return true;
    }
    //buffer->vector
    void textin(Vector<Character> text, int pos){
    	if(pos>0){
    		for(int i=0;i<text.size()-pos;i++){
    			text.set(i, text.get(i+pos));
    		}
    		int rest = text.size()-pos;
    		for(int i=rest;i<text.size();i++){
    			if(GetBit()){
    	    		if(ans==1){
    	    			text.set(i,'1');
    	    		}
    	    		else{
    	    			text.set(i,'0');
    	    		}
        		}
        		else{
        			/*while(text.size()>i){
        				text.removeElementAt(text.size()-1);
        			}
        			break;*/
        			text.set(i, '0');
        		}
    		}
    	}
    	/*
    	while(pos>0){
    		text.remove(0);
    		pos--;
    	}*/
    	while(text.size()<maxtextsize){
    		if(GetBit()){
	    		if(ans==1){
	    			text.add('1');
	    		}
	    		else{
	    			text.add('0');
	    		}
    		}
    		else
    			break;
    	}
    }
    //buffer->int
    /*int read_int16(){
    	int i = 0;
    	for(int m=0x8000;m!=0;m>>=1){
    		if(GetBit()){
    			if(ans==1){
    				i|=m;
    			}
    		}
    		else{
    			//System.out.println("error");
    			return 0;
    		}
    	}
    	return i;
    }*/
    //vector->int
    int read_int16(Vector<Character> text){
    	if(text.size()<16)
    		return 0;
    	int result = 0;
    	for(int m=0x8000;m!=0;m>>=1){
    		if(text.get(0)=='1'){
    			result|=m;
    		}    		
    		text.remove(0);
    	}
    	return result;
    }
}

public class gzip_decode{
    //minimal match size = 3
	static int minmatch = 3;
    //window size 64kb
	static int maxwindowsize = 70<<10;
    //max distance
	static int halfwindow = 32<<10;
    //buf size
    static int bufsize = 80<<10;
    static int min_lookahead = 3 ;
    static byte window[] = new byte[maxwindowsize];
    static byte l_buf[] = new byte[bufsize]; //non-matched byte or (matched length - 131)
    static short d_buf[] = new short[bufsize]; //matched distance
    static boolean flag_buf[] = new boolean[bufsize]; //true -> (matched length - 131); false -> non-matched byte
    static int hfm_buf[] = new int[bufsize*2];
    static int lookahead;
    static int bufcount;
    static int textpos = 0;
    static boolean eoflag = false;
    static Vector<Character> text = new Vector<Character>();
    //static FileInputStream in;
    static FileOutputStream out;
    static Hashtable<String,Vector<pos_int> > hasht = new Hashtable<String,Vector<pos_int> >(maxwindowsize);// String - Vector int
    static int readblock(inbits ib){
    	ib.textin(text, textpos);
    	int length = ib.read_int16(text);
    	if(length==0){
    		eoflag=true;
    		return 0;
    	}
    	textpos = 0;
    	ib.textin(text, textpos);
    	if(text.size()<ib.maxtextsize){
    		eoflag=true;
    	}
    	Huffman hf = new Huffman();
    	hfm_buf = new int[bufsize];
    	int pos = hf.Decode(text, hfm_buf, length, 9);
    	for(int i=0;i<length;++i){
    		if((hfm_buf[i]&0x100) == 0){
    			flag_buf[i]=false;
    		}
    		else{
    			flag_buf[i]=true;
    		}
    		l_buf[i] = (byte)(hfm_buf[i]&0xff);
    	}
    	textpos = pos;
    	ib.textin(text, textpos);
    	if(text.size()<ib.maxtextsize){
    		eoflag=true;
    	}
    	hf = new Huffman();
    	hfm_buf = new int[bufsize*2];
    	pos = hf.Decode(text, hfm_buf, length*2, 8);
    	for(int i=0;i<length;++i){
    		d_buf[i]=(short)((hfm_buf[2*i]<<8)|hfm_buf[2*i+1]);
    	}
    	textpos = pos;
    	return length;
    }
    static void putwindow(int opos){
    	try{
    		out.write(window, 0, halfwindow);
    	}catch(IOException e){
            System.out.println("write error" + e);
    	}
    	int l = opos - halfwindow;
    	for(int i=0;i<l;i++){   
    		window[i]=window[i+halfwindow];
    	}
    }
    static void putrestwindow(int l){
    	try{
    		out.write(window, 0, l);
    	}catch(IOException e){
            System.out.println("write error" + e);
    	}
    }
    static void decode(String ifilename, String ofilename){
    	//open file
        try{
            File f = new File(ofilename);
            out = new FileOutputStream(f);
        }
        catch(IOException e){
            System.out.println("file open error" + e);
        }
        int ipos = 0, opos = 0, ilength = 0;
        inbits ib = new inbits(ifilename);
        while(true){  ////
        	ilength = readblock(ib);
        	if(ilength==0){
        		break;
        	}
        	//Huffman.Decode
        	ipos = 0;
        	while(ipos<ilength){
        		if(flag_buf[ipos]){
        			int matchl = l_buf[ipos] + 131;
        			int dis = (int)d_buf[ipos];
        			for(int t=0;t<matchl;t++){
        	        	try{
            				window[opos] = window[opos-dis];
        	        	}catch(ArrayIndexOutOfBoundsException e){
        	        		System.out.println("error" + e);
        	        	}
        				opos++;
        			}
        		}
        		else{
        			window[opos] = l_buf[ipos];
        			opos++;
        		}
        		ipos++;
        		if(opos>(64<<10)){
        			putwindow(opos);
        			opos -= halfwindow;
        		}
        	}
        }
        putrestwindow(opos);        
    }
    /*this function is for test*/
    public static void main(String args[]){
        decode("1.pdf.gzip","1decode.pdf");
    }

}