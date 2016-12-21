package joseph;
//*****************************************************
//Beta - Prototype
//Average Background Correction by Joseph Zammit - 2016
//*****************************************************

import java.awt.AWTEvent;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class Main_Average_Background implements PlugInFilter, DialogListener {

	// Variables
	static ImagePlus imp;
	long start_time;
	static double [][]I0;
	
	// Setup of PlugInFilter Interface
	public int setup(String arg, ImagePlus imp) {
		Main_Average_Background.imp = imp;
		return DOES_ALL;
	}

	// Run
	@Override
	public void run(ImageProcessor ip) {       
    // Create Gui and Start
		createGui();
		
		// invoke garbage collection
		I0=null;
		System.gc(); 
	}
	
	// Create Buttons and Fields
	void createGui() {
		
		GenericDialog gd = new GenericDialog("Estimate Background"); 

		String[] items0 = {"Enabled"};
		gd.addRadioButtonGroup("Average Background Stack", items0, 1, 1, "Enabled");
		gd.addMessage("Plugin will output the averaged image");
		
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
    	avg_background();
		return true;
    };

    return true;
}

	// Average background stack
	public static void avg_background() {	
		// set Parameters
		ImageProcessor ip = null; ImageProcessor ip_temp= null;
				
		// Get stack size
		ImageStack current_stack = imp.getStack();
		int stack_size = current_stack.getSize();
		
		int w = current_stack.getWidth();
		int h = current_stack.getHeight();
		I0 = new double [h][w];
		
		IJ.log("********Input Parameters********");
		IJ.log("Stack size: " +stack_size);
		IJ.log("");
		
		IJ.log("********Processing**************");
		IJ.log("Processing started");
		IJ.log("");
		long start_time = System.currentTimeMillis();
		
		// Average images
		for (int ss=1;ss<=stack_size;ss++){
			//Set ith image from stack
			imp.setSlice(ss);
			IJ.log("Processing image: " +ss);
			// Create processor
			ip_temp = imp.getProcessor();
			
			ip = ip_temp.createProcessor(ip_temp.getWidth(),ip_temp.getHeight());
			ip.setPixels(ip_temp.getPixelsCopy());// hold in new processor
			
			// Hold pixel values in I
			if (ss==1){
				for (int i=0; i<h; i++){
					for (int j=0; j<w; j++){
						double p = ip.getPixel(j,i);
						I0[i][j]=p;
					}
				}	
			} else {
				for (int i=0; i<h; i++){
					for (int j=0; j<w; j++){
						double p = ip.getPixel(j,i);
						I0[i][j]=(I0[i][j]+p)/2.0;
					}
				}	
			}
		}
		
		// display images
		ImageProcessor ip_avg = ip.createProcessor(w,h);
		for (int i=0; i<h; i++){
			for (int j=0; j<w; j++){
				ip_avg.putPixel(j,i,(int)I0[i][j]);
			}
		}
		
		new ImagePlus("Averaged Image", ip_avg).show();
		
		long stop_time = System.currentTimeMillis();
		IJ.log("");
		IJ.log("Processing Finished");
		IJ.log("");
		IJ.log("********Results*****************");
		IJ.log("Averaged image created and displayed");
		IJ.log("Total time taken: " +(stop_time-start_time) +" ms");
		IJ.log("");
		I0 = null; current_stack = null; ip=null; ip_temp=null;
	}

	// Just to test in Eclipse - call ImageJ
	public static void main (String[] args) {
		new ij.ImageJ();
		new Main_Average_Background().run(null);  
	}
	
}