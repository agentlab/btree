import java.util.*;
import java.util.concurrent.*;


import org.vadim.*;
import org.vadim.NodeInterface;

public class Test {
	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		Random rand = new Random();
		int i = 1;
		Tree obj = new Tree();
		ArrayList<Future<NodeInterface>> array = new ArrayList<Future<NodeInterface>>();
		for (int j = 1; j < 1000000; j++) {// В цикле добавляю данные
			obj.treeInsert(j);
		}
		System.out.println();
		i = sc.nextInt();// костыль, чтобы дождаться пока всё добавится

		for (int j = 1; j < 1000000; j++) {// В цикле читаю данные(Проверку ввод
											// делать не стал. Нужно вводить
											// целые числа)
			// System.out.println("Please enter the key");
			// i=sc.nextInt();
			array.add(obj.treeSearch(j));

			for (int k = 0; k < array.size(); k++) {
				Future<NodeInterface> fs = array.get(k);
				try {
					System.out.println(fs.get());
					array.remove(k);
				} catch (InterruptedException e) {
					System.out.println(e);
					return;
				} catch (ExecutionException e) {
					System.out.println(e);
					return;
				}
			}
		}
	}
}
