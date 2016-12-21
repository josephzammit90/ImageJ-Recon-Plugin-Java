package joseph;
//******************************************************************
//Beta - Prototype
//Dynamic Motion Correction - Joseph (java) 2016
//******************************************************************

import java.awt.AWTEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import Jama.*; 

public class Main_DMC implements PlugInFilter, DialogListener {
	// Variables
	static ImagePlus imp;
	long start_time;
	private double step=(double) 0.9; // holds angle between projections
	
	// Setup of PlugInFilter Interface
	public int setup(String arg, ImagePlus imp) {
		Main_DMC.imp = imp;
		return DOES_ALL;
	}

	// Run
	@Override
	public void run(ImageProcessor ip) {       
		// Create Gui and Start
		createGui();	
		System.gc();  // invoke garbage collection
	}
	
	// Create Buttons and Fields
	void createGui() {
		
		GenericDialog gd = new GenericDialog("Dynamic Offset Correction"); 
		
		gd.addNumericField("Angle between Projections: ", step, 1); // Angle step size between Projections (ex: 0.9 degrees)
		
		String[] items0 = {"Enabled"};
		gd.addRadioButtonGroup("Dynamic Offset Correction", items0, 1, 1, "Enabled");
		gd.addMessage("Plugin will output corrected sinogram/s");
		gd.addMessage("Sinogram angle paramter should be on the x-axis");
		gd.addMessage("\"coords.txt\" required from 'Estimate Tilt & Static Offset' plugin");
		
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
			dmc(step);
			return true;
		};
		
		// Handle changes in Numerical Fields
        step = (double) gd.getNextNumber();
        
		return true;
	}

	public void dmc(double step){
		// set Parameters
		ImageProcessor ip = null; ImageProcessor ip_temp = null;
		ImageStack current_stack = imp.getStack(); // Get stack size

		int stack_size = current_stack.getSize(); // parameters
		int w = current_stack.getWidth();
		int h = current_stack.getHeight();
		ImageStack corrected_stack = new ImageStack(w,h);
		
		IJ.log("********Processing**************");
		long t1 = System.currentTimeMillis(); // start timer
		
		double [] theta = new double[w];
		double [][] M = new double [theta.length][3];
		double [] XMass = new double[theta.length];

		// Set theta
		for (int i=0; i<w; i++){
			theta[i] = 0 + (double) (i* (double)step);
			//IJ.log("theta: " +theta[i]);
		}
		
		// Read centre of mass coordinate from txt file
		Scanner s;
		boolean exists = false;
			try {
				File varTmpDir = new File("coords.txt");
				exists = varTmpDir.exists();
				if (exists) { // if file exsists
					IJ.log("\"coords.txt\" found");
					s = new Scanner(varTmpDir);
					
					for (int i=0; i<theta.length; i++) {
						XMass[i] = s.nextInt();
						//IJ.log("Xmass: " +XMass[i]);
					}
				} else {
					IJ.log("Error: \"coords.txt\" not found");
					IJ.log("");
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		if (exists==false){return;};
		
		for (int i=0; i<theta.length; i++){
			M[i][0] = 1;
			M[i][1] = Math.cos(theta[i]*Math.PI/180);
			M[i][2] = Math.sin(theta[i]*Math.PI/180);
			//System.out.println("M[]["+i+"]: " +M[i][0] +", " +M[i][1] +", " +M[i][2]);
		}
	
		IJ.log("Calculating correction values");
		// Fit sinusoid to marker bead's center of mass (COM)
		// Errors at each projection angle are the differences of the measured COM
		// from this fitted sinusoid
			Matrix M_m = new Matrix(M);
			Matrix XMass_m = new Matrix(XMass,1);
			Matrix XMass_mt = XMass_m.transpose();
			Matrix invM_m = M_m.inverse();
			Matrix P = invM_m.times(XMass_mt);
			Matrix errors_temp = M_m.times(P);
			double[][] errors_temp2 = errors_temp.getArray();
	
		// errors =  M*P - XMass
		// errors at each projection angle
			double []errors = new double[theta.length];
			for (int i=0; i<theta.length; i++){
				errors[i]=errors_temp2[i][0] - XMass[i];
				//IJ.log( " " + errors[i]);
			}
		
		// Loop through all images in the stack
		IJ.log("");
		IJ.log("Applying corrections: ");
		IJ.log("");
		for (int ss=1;ss<=stack_size;ss++)	{
			IJ.log("Processing image: " +ss);
			
			imp.setSlice(ss); //Set ith image from stack
			ip_temp = imp.getProcessor(); // Create processor
			
			ip = ip_temp.createProcessor(ip_temp.getWidth(),ip_temp.getHeight());
			ip.setPixels(ip_temp.getPixelsCopy());// hold in new processor
			ip.setBackgroundValue(0);

			// translate each projection angle in sinogram
			for (int i=0; i<w; i++){
				int translate_value = (int) Math.round(errors[i]);
				ip.setRoi(i, 0, 1, h); // set ROI
				ip.translate(0, translate_value); // translate: + down - up
			}
			corrected_stack.addSlice("Corrected Image Stack", ip);
		}
		
		new ImagePlus("Corrected Sinogram Stack",corrected_stack).show();
		
		long t2 = System.currentTimeMillis(); // stop timer
		IJ.log("");
		IJ.log("********Finished****************");
		IJ.log("Image stack displayed");
	    IJ.log("Total processing time: " +(t2-t1) +" ms");
	    IJ.log("");
	    
		// null parameters
		ip = null; ip_temp = null; corrected_stack=null;
		theta = null; M=null; XMass=null; M_m=null; XMass_m=null; 
		XMass_mt=null; invM_m=null; P=null; errors_temp=null; errors_temp2=null; errors=null;
	}
	
	// Just to test in Eclipse - call ImageJ
	public static void main (String[] args) {
		new ij.ImageJ();
		new Main_DMC().run(null);  
	}
	
}