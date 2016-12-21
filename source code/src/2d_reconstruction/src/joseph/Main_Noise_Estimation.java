package joseph;
//*****************************************************
//Beta - Prototype
//Noise Estimation - Joseph (java) 2016
//*****************************************************

import java.awt.AWTEvent;
import java.util.Random;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;


public class Main_Noise_Estimation implements PlugInFilter, DialogListener {
	// Variables
	static ImagePlus imp;
	long start_time;
	
	// Setup of PlugInFilter Interface
	public int setup(String arg, ImagePlus imp) {
		Main_Noise_Estimation.imp = imp;
		return DOES_ALL;
	}

	// Run
	@Override
	public void run(ImageProcessor ip) {       
		// Create Gui and Start
		createGui();	
		// invoke garbage collection
		System.gc(); 
	}
	
	// Create Buttons and Fields
	void createGui() {
		
		GenericDialog gd = new GenericDialog("Image Noise Estimation"); 

		String[] items0 = {"Enabled"};
		gd.addRadioButtonGroup("Find Noise Variance", items0, 1, 1, "Enabled");
		//gd.addMessage("Plugin will output noise paramters");
		
		gd.addDialogListener(this);
		String html = "<html>" +"<h3>Manual</h3>" +"Please visit: www.jjzideas.com";
	  	gd.addHelp(html);
		
		// Display Gui
		gd.showDialog();
	}
	
	// Handle Changes in Generic Dialog i.e. in Radio Buttons and Fields
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		// Exit if cancel is pressed
		if (gd.wasCanceled()) return false;
  
		// Run if ok is pressed
		if (gd.wasOKed()){
			noise();
			return true;
		};

		return true;
	}

	// Rough Estimation - Please check! this is a prototype
	public void noise(){
		// set Parameters
		ImageProcessor ip_temp = null;
		ImageStack current_stack = imp.getStack(); // Get stack size
		
		IJ.log("********Processing**************");
		IJ.log("Calculating noise variance");
		IJ.log("");
		long t1 = System.currentTimeMillis(); // start timer
		
		int stack_size = current_stack.getSize(); // parameters
		int w = current_stack.getWidth();
		int h = current_stack.getHeight();
	
		// load random slice
		IJ.log("1. Choosing random slice");
		Random rand = new Random();
		int min=1, max=stack_size;
		int slice = rand.nextInt(max - min + 1) + min;
		
		imp.setSlice(slice); //Set random image/slice from stack
		ip_temp = imp.getProcessor(); // Create processor

		// store in array
		double [][]I = new double[h][w];
		for (int i=0; i<h; i++){
			for (int j=0; j<w; j++){
				I[i][j]=(double)ip_temp.getPixel(j,i);
			}
		}
		
		IJ.log("2. Iterating over random blocks");
		int repeat = 100;
		double[] mean_var_array = new double[repeat];
		
// repeat 'repeat' times and calculate mean variance
		for (int num=0; num<repeat; num++){
			// choose 30 random pixel blocks of size w/15 and h/10 respectively
			Random rand2 = new Random();
			int []i = new int[30];
			int []j = new int[30];
			double [][] var_array = new double[i.length][j.length]; // hold variance of each random block
			int [][] pixelblock = new int [10][15];
			double [] pixels = new double [10*150]; //hold pixels in 1D array
			
			for (int k=0; k<30; k++) {
				i[k] = rand2.nextInt((int) Math.round((double)h/(double)15) - 1 + 1) + 1;
				j[k] = rand2.nextInt((int) Math.round((double)w/(double)10) - 1 + 1) + 1;
				//IJ.log("i[k]: "+i[k]);
				//IJ.log("j[k]: "+j[k]);
			}
			
			double temp_var_sum=0.0;
//loop through all random pixel blocks
			for (int ki=0; ki<i.length; ki++){
				for (int kj=0; kj<j.length; kj++){
					
					for (int row=0; row<10; row ++){
						for (int col=0; col<15; col ++){
							pixelblock[row][col]=(int) I[(i[ki]+row)] [(j[kj]+col)];
							pixels[(row*15)+col] = (double) pixelblock[row][col];
							//IJ.log("pixelblock: " +pixelblock[row][col]);
						}
					}
					
					// calculate mean
					double mean=0.0;
					for (int row=0; row<150; row++)
					{
						mean=mean+pixels[row];
					}
					mean=mean/150.0;
					//IJ.log("mean: " +mean);
					
					// calculate squared deviations from mean
					// sum squared deviations
					double [] dev = new double[150];
					double dev_sum = 0.0;
					for (int row=0; row<150; row++)
					{
						dev[row]=(pixels[row]-mean) * (pixels[row]-mean);
						dev_sum=dev_sum+dev[row];
					}
					
					double mean_var = (double)dev_sum/149.0;
					var_array[ki][kj]=(double)mean_var;
					//IJ.log("var_array[ki][kj]: " +var_array[ki][kj]);
					temp_var_sum = temp_var_sum+(double)mean_var;
					
					// parameters
					pixelblock=null; pixels=null; dev=null; mean_var=0.0;
					pixelblock = new int [10][15];
					pixels = new double [10*150];
				}
			}
//loop through all random pixel blocks	
			//IJ.log("temp_var_sum: " +temp_var_sum);
			mean_var_array[num] = (double)(temp_var_sum/900.0);
			//IJ.log("meanVarianceSamplingIteration: " + mean_var_array[num]);
		}
// repeat 'repeat' times and calculate mean variance	
		
		double final_variance =0.0;
		for (int k=0; k<repeat; k++){
			final_variance = final_variance+mean_var_array[k];
			//IJ.log("mean_var_array[]: " +mean_var_array[k]);
		}
		double final_variance2 = final_variance /(double) repeat;
		//IJ.log("final_variance: " +final_variance);
		double final_variance3 = Math.sqrt(final_variance2);
		
		IJ.log("3. Caculating mean variance");
		IJ.log("4. Processing finished");
		
		long t2 = System.currentTimeMillis(); // stop timer
		IJ.log("");
		IJ.log("********Finished****************");
		IJ.log("Noise variance: " +final_variance3);
	    IJ.log("Total processing time: " +(t2-t1) +" ms");
	    IJ.log("");
	    
		// null paramters
		ip_temp = null;
	}
	
	// Just to test in Eclipse - call ImageJ
	public static void main (String[] args) {
		new ij.ImageJ();
		new Main_Noise_Estimation().run(null);  
	}
	
}