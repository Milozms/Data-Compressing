package gzip;

import java.io.DataInputStream;
import java.io.*;
import java.io.InputStream;
import java.util.PriorityQueue;

import java.util.*;

public class Huffman {

    String hasht[];
    HuffmanNode root;
    int msize = 0;
    int n = 0;
    int location = 0;

    void int2b(Vector<Character> huff, int key) {
        for (int i = msize; i >= 0; --i) {
            if ((key & (1 << i)) != 0) {
                huff.add('1');
            } else {
                huff.add('0');
            }
        }
    }

    void huffmanTreetob(Vector<Character> huff, HuffmanNode root, String code) {
        if (root.lchild == null && root.rchild == null) {
            huff.add('0');
            int2b(huff, root.key);
            hasht[root.key] = new String(code);
            n++;
            return;
        }
        huff.add('1');
        //System.out.print(root.poss + " ");
        if (root.lchild != null) {
            huffmanTreetob(huff, root.lchild, code + "0");
        }
        if (root.rchild != null) {
            huffmanTreetob(huff, root.rchild, code + "1");
        }
        return;
    }

    void Encode(int[] Text, int m, int length, Vector<Character> code, Vector<Character> Tree) {
        msize = m - 1;
        PriorityQueue<HuffmanNode> pq = new PriorityQueue<HuffmanNode>();
        int codesize = 1 << m;
        hasht = new String[codesize];
        int poss[] = new int[codesize];
        for (int i = 0; i < length; ++i) {
            poss[Text[i]]++;
        }
        for (int i = 0; i < codesize; ++i) {
            if (poss[i] != 0) {
                pq.add(new HuffmanNode(i, poss[i]));
            }
        }
        while (pq.size() > 1) {
            HuffmanNode rch = pq.peek();
            pq.poll();
            HuffmanNode lch = pq.peek();
            pq.poll();
            HuffmanNode tfh = new HuffmanNode(0, rch.poss + lch.poss);
            tfh.lchild = lch;
            tfh.rchild = rch;
            pq.add(tfh);
        }
        root = pq.peek();
        //code = new Vector<>();
        //Tree = new Vector<>();
        //int2b(huff, length);
        huffmanTreetob(Tree, root, "");
        for (int i = 0; i < length; ++i) {
            for (int j = 0; j < hasht[Text[i]].length(); ++j) {
                code.add(hasht[Text[i]].charAt(j));
            }
        }
        //System.out.println(length + " " + n);
    }

    void btoHuffmanTree(HuffmanNode root, Vector<Character> Text, int m) {
        if (Text.get(location) == '0') {
            location++;
            root.key = 0;
            for (int i = m - 1; i >= 0; --i) {
                if (Text.get(location) == '1') {
                    root.key |= (1 << i);
                }
                location++;
            }
            return;
        }
        ++location;
        HuffmanNode lchild = new HuffmanNode(0, 0);
        root.lchild = lchild;
        btoHuffmanTree(lchild, Text, m);
        HuffmanNode rchild = new HuffmanNode(0, 0);
        root.rchild = rchild;
        btoHuffmanTree(rchild, Text, m);
        return;
    }

    int Decode(Vector<Character> Text, int[] block, int n, int m) {
        int num = 0;
        try {
            location = 0;
            HuffmanNode root = new HuffmanNode(0, 0);
            btoHuffmanTree(root, Text, m);
            while (num < n) {
                HuffmanNode tmp = root;
                while (true) {
                    if (tmp.lchild == null && tmp.rchild == null) {
                        block[num++] = tmp.key;
                        break;
                    }
                    if (Text.get(location) == '0') {
                        tmp = tmp.lchild;
                    } else {
                        tmp = tmp.rchild;
                    }
                    location++;
                }
            }
            return location;
        } catch (Exception e) {
            System.out.println(e);
        }
        return 0;
    }

}

class HuffmanNode implements Comparable {

    int key;
    int poss;
    HuffmanNode lchild;
    HuffmanNode rchild;

    HuffmanNode(int k, int p) {
        key = k;
        poss = p;
        lchild = null;
        rchild = null;
    }

    public int compareTo(Object nodea) {
        return -((HuffmanNode) nodea).poss + poss;
    }
}

