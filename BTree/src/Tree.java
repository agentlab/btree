import java.util.concurrent.*;
import java.util.concurrent.locks.*;

interface NodeInterface{
	/*Вспомогательный интерфейс
	 */
}


class Node implements NodeInterface{
	public int keyNum;
	public int[] key;
	public boolean leaf;
	public Node[] children;
	public Node(int keyNum,boolean leaf,int t){
		this.keyNum=keyNum;
		this.leaf=leaf;
		this.key=new int[2*t-1];
		this.children=new Node[2*t];
	}
}

/*
 * NodeIndex нужен только чтобы вернуть объект с полями Node и int при конце поиска.
 * Посчитал это лучшим способом реализовать особеннсоти функции поиска
 * (Нужно возвращать два значения одновременно при удачном нахождении)
 */
class NodeIndex implements NodeInterface{
	private Node node;
	private int index;
	public NodeIndex(Node node,int index){
		this.node=node;
		this.index=index;
	}
	public String toString(){ 
		if(node==null)
			return "The key is not found";
		return "The key is found "+node.key[index];
	}
}


public class Tree {
	private static final int t=100;
	private int height=0;
	public Node root;
	private ExecutorService search=Executors.newFixedThreadPool(4);//Потоки для поиска
	private ExecutorService insert=Executors.newFixedThreadPool(1);
	/*
	 * Поток для добавления. Создаю один поток для добавления, так как ипользовать большое кол-во считаю ненужным
	 * (они будут мешать друг другу)
	 */
	ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	public Tree(){
		this.root=new Node(0,true,t);
	}
	
	
	public void treeSplitChild(Node x,int i){
		Node y=x.children[i];
		Node z=new Node(t-1,y.leaf,t);
		for(int j=0;j<t-1;j++){
			z.key[j]=y.key[j+t];
			y.key[j+t]=0;
		}
		if(!y.leaf){
			for(int j=0;j<t;j++){
				z.children[j]=y.children[j+t];
				y.children[j+t]=null;
			}
		}
		y.keyNum=t-1;
		for(int j=x.keyNum;j>=i+1;j--){
			x.children[j+1]=x.children[j];
		}
		x.children[i+1]=z;
		for(int j=x.keyNum-1;j>=i;j--){
			x.key[j+1]=x.key[j];
		}
		x.key[i]=y.key[t-1];
		y.key[t-1]=0;
		x.keyNum++;
	}
	
	
	
	public void treeInsert(int key){//При вызове этой функции задача добавляется в пул потоков для добавления
		Runnable obj=()-> {
			long time=System.nanoTime();
			readWriteLock.writeLock().lock();//Надеюсь это работает так как написано в интернете 
			if(root.keyNum==2*t-1){
				Node s=new Node(0,false,t);
				s.children[0]=root;
				root=s;
				height++;
				treeSplitChild(s,0);
				treeInsertNonfull(s,key);
			}
			else
				treeInsertNonfull(root,key);
			readWriteLock.writeLock().unlock();
			System.out.println("Insert ms="+(System.nanoTime()-time)/Math.pow(10, 6)+"\n");
		};
		insert.submit(obj);
	}
	
	public void treeInsertNonfull(Node x,int key){
		int i=x.keyNum-1;
		
		if(x.leaf){
			if(i==-1){
				x.key[0]=key;
				x.keyNum++;
				return;
			}
			while(i>=0 && key<x.key[i] ){
				x.key[i+1]=x.key[i];
				i--;
			}
			
			x.key[i+1]=key;
			x.keyNum++;
		}
		else{
			while(i>=0 && key<x.key[i])
				i--;
			i++;
			if(x.children[i].keyNum==2*t-1){
				treeSplitChild(x,i);
				if(key>x.key[i]){
					i++;
				}
			}
			treeInsertNonfull(x.children[i],key);
		}
	}
	
	
	public Future<NodeInterface> treeSearch(int key){//При вызове этой функции задача добавляется в пул потоков для поиска
		Callable<NodeInterface> obj=()-> {
					long time=System.nanoTime();
					readWriteLock.readLock().lock();//Опять же, надеюсь это работает так как написано в интернете 
					NodeInterface objNode=treeSearch(root,key);
					readWriteLock.readLock().unlock();
					System.out.println("Search ms="+(System.nanoTime()-time)/Math.pow(10, 6)+"\n");
					return objNode;
				};
		return search.submit(obj);
	}
	
	public NodeInterface treeSearch(Node x,int key){
		int i=0;
		
		while(i<x.keyNum && key>x.key[i])
			i++;
		if(i<x.keyNum && key==x.key[i]){
			NodeIndex objBuf=new NodeIndex(x,i);
			return objBuf;
		}
		else if(x.leaf)
		{
			NodeIndex objBuf=new NodeIndex(null,0);
			return objBuf;
		}
		else
			return treeSearch(x.children[i],key);
	}
}







