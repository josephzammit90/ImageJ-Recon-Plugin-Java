package joseph;
//*************************************************
//Beta - Prototype
//Beer-Lambert Correction by Joseph Zammit - 2016
//*************************************************

import java.awt.AWTEvent;
import java.text.DecimalFormat;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class Main_Beer_Lambert implements PlugInFilter, DialogListener {

	// Variables
	static ImagePlus imp;
	long start_time;
	static double [][]R;
	static double [][]Rnorm;
	
	// Setup of PlugInFilter Interface
	public int setup(String arg, ImagePlus imp) {
		Main_Beer_Lambert.imp = imp;
		return DOES_ALL;
	}

	// Run
	@Override
	public void run(ImageProcessor ip) {       
      // Create Gui and Start
		createGui();
		
		// invoke garbage collection
		R=null; Rnorm=null;
		System.gc(); 
	}
	
	// Create Buttons and Fields
	void createGui() {
		
		GenericDialog gd = new GenericDialog("Beer-Lambert Correction"); 

		String[] items0 = {"Enabled"};
		gd.addRadioButtonGroup("Create Corrected Image", items0, 1, 1, "Enabled");
		gd.addMessage("1st image taken as the intensity (I)");
		gd.addMessage("2nd image taken as the background (I0)");
		gd.addMessage("For best results I0 can be taken as the average background");
		
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
    	  beer_lambert();
  		return true;
      };

      return true;
  }

	// Correction method
	public static void beer_lambert() {	
		IJ.log("********Input Parameters********");
		
		int[] image_list = WindowManager.getIDList();
		String[] image_title = new String[image_list.length+1];
		
		// make sure user inputs two images
        if (image_list.length!=2) {
        	IJ.log("Error: Please input two images starting with the Intensity");
            IJ.error("Please input two images starting with the Intensity");
            return;
        }
        
		ImageStack current_stack_I = null;
        ImageStack current_stack_Ib = null;
        
        // print which image is taken as intensity and which as background
        for (int i=0; i<image_list.length; i++) {
        	imp= WindowManager.getImage(image_list[i]);
            image_title[i] = imp!=null?imp.getTitle():"";
            if (i==0) {
            	current_stack_I = imp.getStack();
            } else {
            	current_stack_Ib = imp.getStack();
            }
        }
 
        IJ.log("Intensity image (I): " +image_title[0]);
        IJ.log("Background image (I0): " +image_title[1]);
        IJ.log("");
        
		// Get stack size
		int stack_sizei = current_stack_I.getSize();
		int wi = current_stack_I.getWidth();
		int hi = current_stack_I.getHeight();
		
		int stack_sizeib = current_stack_Ib.getSize();
		int wib = current_stack_Ib.getWidth();
		int hib = current_stack_Ib.getHeight();
		
		if (wi!=wib || hi!=hib) {
            IJ.log("Error: Image stacks must of the same dimensions");
            IJ.error("Image stacks must of the same dimensions");
            return;
        }
		
		if (stack_sizeib != 1) {
			if (stack_sizeib != stack_sizei) {
				IJ.log("Error: Background stack must be a single image or = to the intensity stack size");
				IJ.error("Background stack must be a single image or = to the intensity stack size");
				return;
			}
		}
		
		// create new stack to hold corrected image
		ImageStack corrected_stack = new ImageStack(wib,hib);
		
		IJ.log("********Processing**************");
		long start_time = System.currentTimeMillis();
		IJ.log("Processing started");
		IJ.log("");
		for (int ss=1;ss<=stack_sizei;ss++){
			IJ.log("Processing image: " +ss);
			//ImageProcessor ip_temp_i = imp.getProcessor();
			ImageProcessor ip_temp_i = current_stack_I.getProcessor(ss);
			
			//handle different background stack sizes
			ImageProcessor ip_temp_ib = null;
			if (stack_sizeib==1) {
				ip_temp_ib = current_stack_Ib.getProcessor(1);
			} else {
				ip_temp_ib = current_stack_Ib.getProcessor(ss);
			}
			
			ImageProcessor ipi = null;
			ImageProcessor ipb = null;
			ImageProcessor ipresult = null;
			
			ipi = ip_temp_i.createProcessor(ip_temp_i.getWidth(),ip_temp_i.getHeight());
			ipi.setPixels(ip_temp_i.getPixelsCopy());// Intensity processor
			
			ipb = ip_temp_ib.createProcessor(ip_temp_ib.getWidth(),ip_temp_ib.getHeight());
			ipb.setPixels(ip_temp_ib.getPixelsCopy());// Background processor
			
			ipresult = ip_temp_i.createProcessor(ip_temp_i.getWidth(),ip_temp_i.getHeight()); //result processor
			
			R = new double[ip_temp_i.getHeight()][ip_temp_i.getWidth()]; // hold pixel values
			Rnorm = new double[ip_temp_i.getHeight()][ip_temp_i.getWidth()]; // hold normalised pixel values
			double max_pixel_value = ipi.getMax(); // find maximum pixel value in original intensity image for normalisation
			
			// calculate Beer-Lambart values
			for (int i=0; i<ip_temp_i.getHeight(); i++){
				for (int j=0; j<ip_temp_i.getWidth(); j++){
					double Iback = (double) ipb.getPixel(j,i);
					double Iintensity = (double) ipi.getPixel(j,i);		
					
					R[i][j]=(double)Math.abs(Math.log(Iintensity/Iback));
					
					//ipresult.putPixel(j,i,(int) Math.abs(Math.log(Iintensity/Iback)*2000)); //testing
					//ipresult.putPixel(j,i,(int) R[i][j]); //Beer-Lambert
				}
			}
			
			// normalise pixels
			Rnorm = normalise_pixels(ip_temp_i.getHeight(), ip_temp_i.getWidth(), R, max_pixel_value);
			
			// Create normalised images
			for (int i=0; i<ip_temp_i.getHeight(); i++){
				for (int j=0; j<ip_temp_i.getWidth(); j++){
					ipresult.putPixel(j,i,(int)Rnorm[i][j]);
				}
			}
			ipresult.invert();
			// add normalised image to stack
			corrected_stack.addSlice("Corrected Image Stack", ipresult);
		}
		
		// Display final stack
		new ImagePlus("Corrected Image Stack", corrected_stack).show();
		long stop_time = System.currentTimeMillis();
		
		IJ.log("");
		IJ.log("Processing Finished");
		IJ.log("");
		IJ.log("********Results*****************");
		IJ.log("Corrected stack created and displayed");
		IJ.log("Total time taken: " +(stop_time-start_time) +" ms");
		DecimalFormat df2 = new DecimalFormat("#0.#");
		 IJ.log("Processing time per image: " +df2.format((double) (stop_time-start_time)/ (double) stack_sizei) +" ms");
		IJ.log("");
	}

	// Normalise pixel values
	public static double[][] normalise_pixels(int h, int w, double R[][], double max_pixel_value){
		// Parameters
		double maxvalue=0;
		double minvalue=0;
		
		// 1. find minimum value in R[][]
		for (int i=0; i<h; i++) {
		    for (int j=0; j<w;j++) {
		        if (R[i][j]<minvalue) {
		        	minvalue=R[i][j];
		        }
		    }
		}
		
		// 2. transform R[][] to postive values
		for (int i=0; i<h; i++){
			for (int j=0; j<w; j++){
				
				R[i][j]=R[i][j]-minvalue;
			}
		}
			
		// 3. find maximum values in R[][]
		for (int i=0; i<h; i++) {
		    for (int j=0; j<w;j++) {
		        if (R[i][j]>maxvalue) {
		        	maxvalue=R[i][j];
		        }
		    }
		}

		// 4. normalise values in R[][] to 0-1
		for (int i=0; i<h; i++){
			for (int j=0; j<w; j++){
				R[i][j]=R[i][j]/maxvalue; // values positive and normalised to 0-1
				R[i][j]=R[i][j]*(max_pixel_value); // set max value to max value in original image/sinogram (change this as needed)
			}
		}
		return R;
	}
	
	
	// Just to test in Eclipse - call ImageJ
	public static void main (String[] args) {
		new ij.ImageJ();
		new Main_Beer_Lambert().run(null);  
	}
	
}