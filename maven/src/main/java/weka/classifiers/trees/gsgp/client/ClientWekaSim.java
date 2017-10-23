package weka.classifiers.trees.gsgp.client;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import weka.classifiers.trees.gsgp.forest.Forest;
import weka.classifiers.trees.gsgp.forest.ForestGSGP;
import weka.classifiers.trees.gsgp.util.Arrays;
import weka.classifiers.trees.gsgp.util.Data;
import weka.classifiers.trees.gsgp.util.Files;


/**
 * 
 * @author Jo�o Batista, jbatista@di.fc.ul.pt
 *
 */
public class ClientWekaSim {

	static int gp = 1; // ST, GS

	static String xDataInputFilename = "Brazil_x.txt bioavailability_x.txt".split(" ")[gp];
	static String yDataInputFilename = "Brazil_y.txt bioavailability_y.txt".split(" ")[gp];
	static String resultOutputFilename = "fitovertime.csv";
	static String treeOutputFilename = ""; // "tree.json";
	static String treeType = "Ramped";

	static String [] operations = "+ - * /".split(" ");
	static String [] terminals = null;

	//negative ms for random values in [0,abs(ms)]
	static double ms = 0.01;
	static double trainPercentage = 0.70;
	static double tournamentPercentage = 0.02;
	static double elitismPercentage = 0.01;

	static int numberOfGenerations = 10000;
	static int numberOfRuns = 1;
	static int populationSize = 300;
	static int maxDepth = 7;

	static boolean shuffle = true;

	static double [][] train_r = null;
	static double [][] test_r = null;
	static double [][] data = null;
	static double [] target = null;


	// Variables
	public static double [][] results = new double [numberOfGenerations][3];
	static Forest f = null;

	/**
	 * main
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		treatArgs(args);
		init();

		for(int run = 0 ; run < numberOfRuns; run++){
			run(run);
		}


		BufferedWriter out = new BufferedWriter(new FileWriter(resultOutputFilename+".tmp"));
		out.write("Treino;Teste\n");
		for(int i = 0; i < results.length; i++){
			if(results[i][2] !=0)
				out.write(results[i][0]/results[i][2] + ";" + results[i][1]/results[i][2] + "\n");
		}
		out.close();
		Files.fixCSV(resultOutputFilename);
	}

	/**
	 * Prepara o cliente para a sua execucao
	 * @throws IOException
	 */
	private static void init() throws IOException{
		train_r = new double[numberOfGenerations][numberOfRuns];
		test_r = new double[numberOfGenerations][numberOfRuns];

		data = Data.readData(xDataInputFilename);
		target = Data.readTarget(yDataInputFilename);
	}

	/**
	 * Executa uma simulacao
	 * @param run
	 * @throws IOException
	 */
	private static void run(int run) throws IOException{
		System.out.println("Run " + run + ":");

		if(shuffle)Arrays.shuffle(data, target);

		setTerm(data);


		double [][] train = new double [(int) (data.length*trainPercentage)][data[0].length];
		double [][] test = new double [data.length - train.length][data[0].length];

		for(int i = 0; i < data.length; i++){
			if( i < train.length)
				train[i] = data[i];
			else
				test[i - train.length] = data[i];
		}

		double[] minmax = Arrays.minmax(target,0,(int) (data.length*trainPercentage));
		ms = -(minmax[1]-minmax[0])/100.0;

		setForest();
		
		f.train();


		// Este bloco est� a certificarse que as previsoes sao consistentes com o treino
		double acc = 0;
		int hit = 0;
		double prediction = 0;
		for(int i = 0; i < test.length; i++){
			prediction= f.predict(test[i]) ; Math.pow( f.predict(test[i]) - target[train.length + i] ,2);
			acc += Math.pow( prediction - target[train.length + i] ,2);
			if((target[train.length + i] < 0.5 && prediction<0.5)||
					(target[train.length + i] >= 0.5 && prediction>=0.5)) hit++;
			if((i+1)%400 ==0)
				System.out.println((i+1) + "/" + test.length);
		}
		acc /= 1.0 * test.length;
		acc = Math.sqrt(acc);

		System.out.println("test binary classification hits: " + hit +" out of " + test.length);
		System.out.println("test RMSE calculated: " + acc);
		//System.out.println("test RMSE from train: " + f.besttestrmse());


		acc = 0;
		hit = 0;

		for(int i = 0; i < train.length; i++){
			prediction = f.predict(train[i]);Math.pow( - target[i],2);
			acc+= Math.pow(prediction - target[i],2);
			if((target[i] < 0.5 && prediction<0.5)||
					(target[i] >= 0.5 && prediction>=0.5)) hit++;
			if((i+1)%400 ==0)
				System.out.println((i+1) + "/" + train.length);
		}
		acc /= 1.0 * train.length;
		acc = Math.sqrt(acc);

		System.out.println("train binary classification hits: " + hit +" out of " + train.length);
		System.out.println("train RMSE calculated: " + acc);
		//System.out.println("train RMSE from train: " + f.besttrainrmse());

		System.out.println(f);
	}



	/**
	 * Trata dos argumentos fornecidos
	 * @param args
	 */
	private static void treatArgs(String [] args){
		for(int i = 0; i < args.length; i++){
			String [] split = args[i].split(":");
			switch(split[0]){
			case "depth":
				maxDepth = Integer.parseInt(split[1]);
				break;
			case "maxgen":
				numberOfGenerations = Integer.parseInt(split[1]);
				break;
			case "popsize":
				populationSize = Integer.parseInt(split[1]);
				break;
			}
		}
	}

	/**
	 * Define o valor dos terminais
	 * @param data
	 */
	private static void setTerm(double [][] data){
		terminals = new String [data[0].length];
		for(int i = 0; i < terminals.length; i++)
			terminals[i] = "x"+i;
	}

	/**
	 * Cria uma nova floresta
	 * @throws IOException
	 */
	private static void setForest() throws IOException{
		f = new ForestGSGP(treeOutputFilename, operations, 
				terminals, maxDepth, ms, data, target, 
				populationSize,trainPercentage, numberOfGenerations);
	}
}