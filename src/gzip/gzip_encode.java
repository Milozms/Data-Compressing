package gzip;


import java.util.*;
import java.io.*;

class outbits{
	static int maxsize = 32<<10;
	byte outbuf[];
	byte bytebuffer;
	int readmask, bufsize;
    static FileOutputStream out;
	outbits(String outfile){
		try{
            File f = new File(outfile);
            out = new FileOutputStream(f);
        }
        catch(IOException e){
            System.out.println("file open error" + e);
        }
		outbuf = new byte[maxsize];
		readmask = 128;
	}
	void putbit(Character i){
		try {
            if (readmask == 0) {
            	if (bufsize >= maxsize){
            		out.write(outbuf,0,bufsize);
            		bufsize = 0;
            	}
                //readbuffer = in.read();
            	outbuf[bufsize] = bytebuffer;
            	bytebuffer = 0;
            	bufsize++;
                readmask = 128;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(i=='1'){
        	bytebuffer |= readmask;
        }
        readmask>>=1;
	}
	//vector->buffer
	void putvec(Vector<Character> v){
		for(int i=0;i<v.size();i++){
			putbit(v.get(i));
		}
	}
	
	void putrest(){//buffer->file
		if(readmask != 128){
			outbuf[bufsize] = bytebuffer;
			bufsize++;
		}
		if(bufsize>0){
			try{
				out.write(outbuf,0,bufsize);
			}catch(IOException e){
				e.printStackTrace();
			}
		}
	}
	//int->buffer
	void put_int16(int i){
		for(int m = 0x8000;m!=0;m>>=1){
			if((m&i)==0){
				putbit('0');
			}
			else{
				putbit('1');
			}
		}
	}
}
class pos_int{
	private int v;
	pos_int(int i){
		v = i;
	}
	int toInt(){
		return v;
	}
}
public class gzip_encode{
    //minimal match size = 3
	static int minmatch = 3;
    //window size 64kb
	static int windowsize = 64<<10;
    //max distance
	static int halfwindow = 32<<10;
    //buf size
    static int bufsize = 80<<10;
    static int min_lookahead = 3 ;
    static byte window[] = new byte[windowsize];
    static byte l_buf[] = new byte[bufsize]; //non-matched byte or (matched length - 131)
    static short d_buf[] = new short[bufsize]; //matched distance
    static boolean flag_buf[] = new boolean[bufsize]; //true -> (matched length - 131); false -> non-matched byte
    static int hfm_buf[] = new int[bufsize*2];
    //debug
    //static int tmp_buf[] = new int[bufsize];
    static int lookahead;
    static int bufcount;
    static boolean eofile = false;
    static Vector<Character> hcode,hTree;
    static FileInputStream in;
    static Hashtable<String,Vector<pos_int> > hasht = new Hashtable<String,Vector<pos_int> >(windowsize);// String - Vector int
    //read first block
    static void gzip_init(){
        bufcount = 0;
        int b = 0;
        try{
            b = in.read(window,0,windowsize);
        }
        catch(IOException e){
            System.out.println("file read error" + e);
        }
        lookahead = b;
        if(b<windowsize){
            eofile = true;
        }
    }

    static int match_length(int apos, int bpos){
        int l=0;
        while(bpos+l<windowsize && l<258 && window[apos+l]==window[bpos+l]){
            l++;
        }
        return l;
    }

    static int deflate(int pos){
        int maxmatchlength = 0, maxmatchpos = 0;
        //insert String
        char chead[] = new char[minmatch];
        for(int i=0;i<minmatch;i++){
        	try{
        		chead[i]=(char)window[pos+i];
        	}catch(ArrayIndexOutOfBoundsException e){
        		System.out.println("error" + e);
        	}
        }
        String shead = new String(chead);
        if(hasht.containsKey(shead)){//exist match str
            //find longest match
            maxmatchlength = 0;
            Vector<pos_int> v = hasht.get(shead);
            for(int i=0;i<v.size();i++){
                if(pos - v.get(i).toInt()>=halfwindow){
                    break;
                }
                int tmp = match_length(v.get(i).toInt(),pos);
                if(tmp>maxmatchlength){
                    maxmatchlength = tmp;
                    maxmatchpos = v.get(i).toInt();
                }
            }
            //add to vector
            v.insertElementAt(new pos_int(pos), 0);
        }
        else{
            Vector<pos_int> v = new Vector<pos_int>();
            v.addElement(new pos_int(pos));
            hasht.put(shead, v);
        }
        if(maxmatchlength<3){
            flag_buf[bufcount] = false;
            l_buf[bufcount] = window[pos];
            d_buf[bufcount] = 0;
            bufcount++;
            return 1;
        }
        flag_buf[bufcount] = true;
        l_buf[bufcount] = (byte)(maxmatchlength - 131);
        d_buf[bufcount] = (short)(pos-maxmatchpos);
        if(d_buf[bufcount]==-32768){
        	System.out.printf("%d,%d",pos,maxmatchpos);
        }
        bufcount++;
        return maxmatchlength;
    }

    static void fill_window(){
        //move second window to first
        System.arraycopy(window,halfwindow,window,0,halfwindow);
        //read second window
        int b = 0;
        try{
            b = in.read(window,halfwindow,halfwindow);
        }
        catch(IOException e){
            System.out.println("file read error" + e);
        }
        if(b>=0) lookahead += b;
        if(b<halfwindow){
            eofile = true;
        }
        //update hashtable - string index
        Set s = hasht.keySet();
        for(Iterator<String> i = s.iterator();i.hasNext();){
            Vector<pos_int> v = hasht.get(i.next());
            for(int j=0;j<v.size();j++){
                if(v.get(j).toInt()>halfwindow){
                    //update index
                    v.setElementAt(new pos_int(v.get(j).toInt() - halfwindow), j);
                }
                else{
                    //remove elements from previous first window
                    while(j<v.size()){
                        v.removeElementAt(j);
                    }
                    break;
                }
            }
        }
    }
    static void putblock(outbits ob){
    	/*请在此处调用函数将l_buf[],d_buf[],flag_buf[]编码并输出*/
    	/*三个数组长度均为bufcount*/
    	/*Your Code Start*/
    	ob.put_int16(bufcount);
        	for(int i=0;i<bufcount;++i){
        		if(flag_buf[i]){
        			hfm_buf[i] = (((int)l_buf[i]))&(0xff)|(1<<8);
        		}
        		else{
        			hfm_buf[i] = ((int)l_buf[i])&0xff;
        		}
        	}
        	Huffman huf = new Huffman();
        	hcode = new Vector<>();
        	hTree = new Vector<>();
        	//System.out.println(bufcount+" break point 1");
        	huf.Encode(hfm_buf, 9, bufcount, hcode, hTree);
        	//huf.printtree;
        	ob.putvec(hTree);
        	ob.putvec(hcode);
        	//debug
        	/*Vector<Character> tmp = new Vector<>(hTree);
        	for(int i=0;i<hcode.size();i++){
        		tmp.addElement(hcode.get(i));
        	}
        	huf.Decode(tmp, tmp_buf, bufcount, 9);
        	*/
        	//end debug
        	for(int i=0;i<bufcount;++i){
        		hfm_buf[2*i] = (((int)d_buf[i])&0xff00)>>8;
        		hfm_buf[2*i+1] = ((int)d_buf[i])&0x00ff;
        	}
        	
        	huf = new Huffman();
        	hcode = new Vector<>();
        	hTree = new Vector<>();
        	
        	/*for(int i=0;i<bufcount;i++)
        		System.out.printf("%d,",hfm_buf[i]);
        	System.out.println("");*/
        	
        	//System.out.println(bufcount+" break point 2");
        	/*try{
                File t = new File("tmp.txt");
                FileOutputStream tmp = new FileOutputStream(t);
                
        	}catch(IOException e){
                System.out.println("file error" + e);
            }*/
        	
        	huf.Encode(hfm_buf, 8, bufcount*2, hcode, hTree);
        	//huf.printtree();
        	ob.putvec(hTree);
        	ob.putvec(hcode);
        	bufcount = 0;
    	
    	/*Your Code end*/
    }
    static void encode(String infile, String outfile){
        //open file
        try{
            File f = new File(infile);
            in = new FileInputStream(f);
        }
        catch(IOException e){
            System.out.println("file open error" + e);
        }
        int pos = 0;
        gzip_init();
    	outbits ob = new outbits(outfile);
        while(lookahead>=min_lookahead || !eofile){
            int move = deflate(pos);
            pos += move;
            lookahead -= move;
            if(lookahead<min_lookahead){
            	if(eofile){
            		for(int i=0;i<lookahead;i++){
            			l_buf[bufcount]=window[pos+i];
            			d_buf[bufcount]=0;
            			flag_buf[bufcount]=false;
            			bufcount++;
            		}
                    putblock(ob);
                    break;
            	}            	

                fill_window();
                pos -= halfwindow;
            }
            if(bufcount>=(60<<10)){
            	putblock(ob);
            }
        }
        ob.putrest();
        //close file
        try{
            in.close();
        }
        catch(IOException e){
            System.out.println("file close error" + e);
        }
    }

    /*this function is for test*/
    public static void main(String args[]){
        encode("1.pdf", "1.pdf.gzip");
        /*for(int i=0;i<bufcount;++i){
            System.out.println(l_buf[i]+","+d_buf[i]+","+flag_buf[i]);
        }*/
    }

}