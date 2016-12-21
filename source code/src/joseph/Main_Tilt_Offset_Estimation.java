package joseph;
//************************************************************
//Beta - Prototype
// Tilt and Static Offset Estimation by Joseph Zammit - 2016
//************************************************************

import java.awt.AWTEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import Jama.*; 

public class Main_Tilt_Offset_Estimation implements PlugInFilter, DialogListener {

	 // Variables
	static ImagePlus imp;
	int tilt_type=1, off_type=1;
	long start_time;
	int threshold_value=0, threshold_type=1, image_display, ellipse_display=1, txt=1;
	static double tr_increase = 1.5; // used to increase automatic threshold, 2 = doubled
	
	// Setup of PlugInFilter Interface
	public int setup(String arg, ImagePlus imp) {
		Main_Tilt_Offset_Estimation.imp = imp;
		return DOES_ALL;
	}

	// Run
	@Override
	public void run(ImageProcessor ip) {       
		// Create Gui and Start
		createGui();
		
		System.gc(); // invoke garbage collection
	}
	
	// Create Buttons and Fields
	void createGui() {
		GenericDialog gd = new GenericDialog("Tilt & Static Offset Estimation"); 
		
		// Threshold value if manual
		gd.addNumericField("Threshold (pixels): ", threshold_value, 0);
		// Thresholding
		String[] items2 = {"Automatic","Manual"};
		gd.addRadioButtonGroup("Threshold value", items2, 2, 1, "Automatic");	
		
		
		// Tilt
		String[] items0 = {"Enable","Disable"};
		gd.addRadioButtonGroup("Tilt Estimation", items0, 1, 2, "Enable");
		
		String[] items1 = {"Enable","Disable"};
		gd.addRadioButtonGroup("Static Offset Estimation", items1, 1, 2, "Enable");
		
		String[] items4 = {"No","Yes"};
		gd.addRadioButtonGroup("Display thresholded images?", items4, 1, 2, "No");
		
		String[] items5 = {"No","Yes"};
		gd.addRadioButtonGroup("Create plot fitted ellipse .m file?", items5, 1, 2, "No");
		
		String[] items6 = {"No","Yes"};
		gd.addRadioButtonGroup("Save COM coordinates in .txt file?", items6, 1, 2, "No");

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
    	IJ.log("********Input Parameters********");
	  	if (tilt_type==1) {
	  		IJ.log("Tilt estimation enabled");
	  	} else {
	  		IJ.log("Tilt estimation disabled");
	  	}
	  	
		if (off_type==1) {
			IJ.log("Static offset estimation enabled");		  
		} else {
			IJ.log("Static offset estimation disabled");
		}

		if (threshold_type==1) {
			IJ.log("Threshold value - find automatically");
			IJ.log("Threshold value = "+tr_increase +"*mean(min pixel values)");
		} else {
			IJ.log("Threshold value set to: " +threshold_value);
		}
		
		if (image_display==2){
			IJ.log("");
			IJ.log("Thresholded image to be displayed");
		} else {
			IJ.log("");
			IJ.log("Thresholded image not to be displayed");
		}
		
		if (ellipse_display==2){
			IJ.log("Plot fitted ellipse .m file to be created");
		} else {
			IJ.log("Plot fitted ellipse .m file not created");
		}
		if (txt==2){
			IJ.log("COM coordinates saved to .txt file");
		} else {
			IJ.log("COM coordinates not saved to .txt file");
		}
		
		main_method(tilt_type, off_type, threshold_type, threshold_value, image_display, ellipse_display, txt); // call main method to start process
  		return true;
      }
      
      threshold_value = (int) gd.getNextNumber();
      
      // Handle Changes in Radio Buttons - filter and reconstruction type
      String threshold_type_change = gd.getNextRadioButton();
      String tilt_type_change = gd.getNextRadioButton();
      String off_type_change = gd.getNextRadioButton();
      String image_display_type_change = gd.getNextRadioButton();
      String ellipse_display_type_change = gd.getNextRadioButton();
      String txt_change = gd.getNextRadioButton();
      
      if (threshold_type_change == "Automatic"){
    	  threshold_type=1;
      }
      if (threshold_type_change == "Manual"){
    	  threshold_type=2;
      }
      
      if (tilt_type_change == "Enable"){
    	  tilt_type=1;
      }
      if (tilt_type_change == "Disable"){
    	  tilt_type=2;
      }
      
      if (off_type_change == "Enable"){
    	  off_type=1;
      }
      if (off_type_change == "Disable"){
    	  off_type=2;
      }
      
      if (image_display_type_change == "No"){
    	  image_display=1;
      }
      if (image_display_type_change == "Yes"){
    	  image_display=2;
      }
      
      if (ellipse_display_type_change == "No"){
    	  ellipse_display=1;
      }
      if (ellipse_display_type_change == "Yes"){
    	  ellipse_display=2;
      }
      
      if (txt_change == "No"){
    	  txt=1;
      }
      if (txt_change == "Yes"){
    	  txt=2;
      }
      
      return true;
  }
	
	// Main Function
	public static void main_method (int tilt_type, int off_type, int threshold_type, int threshold_value, int image_display, int ellipse_display, int txt){
		if (tilt_type!=1 && off_type!=1){
			IJ.log("");
			IJ.log("No options enabled - application terminated");
			return;
		}
		
		long t1 = System.currentTimeMillis(); // start timer
		
		// A. find beads centre of mass coordinates and offset
		int[][] coordinates = bead_com(threshold_type, threshold_value, image_display);
		int [] x_array = new int[coordinates[0].length];
		int [] y_array = new int[coordinates[1].length];
		
		for (int i=0; i<coordinates[0].length; i++) {
			x_array[i] = coordinates[0][i];
		}
		for (int i=0; i<coordinates[1].length; i++) {
			y_array[i] = coordinates[1][i];
		}

		int offset_v = coordinates[2][0]; // get offset
		int threshold = coordinates[3][0]; // get threshold
		
		double lateral_tilt_r = 0;
		double [] ellipse_params = new double [13];
		
		// B. fit ellipse
		if (tilt_type==1) {
			IJ.log("4. Fitting ellipse");
			ellipse_params = ellipse_fitting(x_array, y_array);	
		}
		
		long t2 = System.currentTimeMillis(); // end timer
		
		// Results
		IJ.log("");	
		IJ.log("********Results*****************");
		IJ.log("1. Threshold set to: " +threshold);
		
		if (off_type==1) {	
			if (offset_v==0){
				IJ.log("2. No static offset detected");
			} else {
				IJ.log("2. Static offset: " +(offset_v) +" pixels, correction value: " +(-offset_v) +" pixels");
			}
		}
		
		if (tilt_type==1) {
			if (ellipse_params[9] == 1) // successful
			{
				// estimate lateral tilt
				lateral_tilt_r = Math.toDegrees(ellipse_params[2]);
				
				// plot lateral tilt
				DecimalFormat df = new DecimalFormat("#0.###");
				
				//check rotation axis
				if (lateral_tilt_r!=0) {
					IJ.log("3. Lateral tilt: " +df.format(-lateral_tilt_r) +"\u00b0, correction value: " +df.format(lateral_tilt_r) +"\u00b0");
				} else {
					IJ.log("3. No lateral tilt detected");
				}
				
				double axial_angle=0;
				axial_angle=Math.toDegrees(Math.asin( (ellipse_params[8]/2) / (ellipse_params[7]/2) ));
				
				IJ.log("4. Axial tilt estimation: " +df.format(axial_angle) +"\u00b0");				
				
				// plot ellipse parameters
				//IJ.log("");
				//IJ.log("4. Ellipse parameters: ");
				//IJ.log("      " +"a: " +ellipse_params[0]);
				//IJ.log("      " +"b: " +ellipse_params[1]);
				//IJ.log("      " +"phi: " +ellipse_params[2]);
				//IJ.log("      " +"X0: " +ellipse_params[3]);
				//IJ.log("      " +"Y0: " +ellipse_params[4]);
				//IJ.log("      " +"X0_in: " +ellipse_params[5]);
				//IJ.log("      " +"Y0_in: " +ellipse_params[6]);
				//IJ.log("      " +"long_axis: " +ellipse_params[7]);
				//IJ.log("      " +"short_axis: " +ellipse_params[8]);	
				
				if (ellipse_params[10] == 0){ // check if matrix is singular
					IJ.log("");
					IJ.log("5. Warning: matrix is singular or close to it");
					IJ.log("    "+"An ellipse might not accurately describe the motion of the centre of mass");
					IJ.log("    "+"If not yet done, please enable: \"Create plot fitted ellipse .m file?\"");
					IJ.log("    "+"and run from your ImageJ folder to display fitted ellipse");
				}
				
				// draw fitting ellipse if required in matlab
				if (ellipse_display==2) {
					ellipse_m_file(x_array, y_array, ellipse_params);
					if (ellipse_params[10] == 0){
						IJ.log("");	
						IJ.log("6. plot_fitted_ellipse.m created in ImageJ folder");
					} else {
						IJ.log("");	
						IJ.log("5. plot_fitted_ellipse.m created in ImageJ folder");
					}
				}
				
				if (txt==2 && ellipse_display==2){ // save COM coordinates
					coords(x_array, ellipse_params);
					if (ellipse_params[10] == 0){
						IJ.log("7. coords.txt created in ImageJ folder");
					} else {
						IJ.log("6. coords.txt created in ImageJ folder");
					}
				} else if (txt==2 && ellipse_display==1){
					coords(x_array, ellipse_params);
					if (ellipse_params[10] == 0){
						IJ.log("");	
						IJ.log("6. coords.txt created in ImageJ folder");
					} else {
						IJ.log("");	
						IJ.log("5. coords.txt created in ImageJ folder");
					}
				}
				
				IJ.log("");	
			} else {
				IJ.log("Ellipse fitting unsuccessful, tilt angles not estimated");
				IJ.log("");
			}
		}

		IJ.log("Total processing time: " +(t2-t1) +" ms");
        IJ.log("");
	}
	
	// A. Find bead's centre of mass
	public static int [][] bead_com (int threshold_type, int threshold_value, int image_display) {
		ImageProcessor ip = null; // Create image processor
		ImageStack current_stack = imp.getStack(); // Get stack size

		int stack_size = current_stack.getSize(); // parameters
		int w = current_stack.getWidth();
		int h = current_stack.getHeight();
		IJ.log("Stack size: " +stack_size);
		IJ.log("");
		IJ.log("********Processing**************");

		// 1. Loop through all images and find the centre of mass of each bead 
		IJ.log("1. Finding threshold");
		int threshold=0;
		for (int ss=1;ss<=stack_size;ss++){
			// Variables
			double min_pixel=0, current_pixel=0;
			
			//Set ith image from stack
			imp.setSlice(ss);
			
			// Create processor
			ImageProcessor ip_temp = imp.getProcessor();		
			// Copy image
			ip = ip_temp.createProcessor(ip_temp.getWidth(),ip_temp.getHeight());
			ip.setPixels(ip_temp.getPixelsCopy()); // hold in new processor
			
			// find lowest pixel value
			for (int i=0; i<h; i++){
				for (int j=0; j<w; j++){
					current_pixel = ip.getPixel(j,i);
					
					if (i==0 && j==0){
						min_pixel = current_pixel;
					}
					
					if (current_pixel<min_pixel){
						min_pixel=current_pixel;
					}
				}
			}		
			threshold = (int) (threshold + min_pixel);	
		}
		
		// 1.1 If automatic... final threshold = mean of darkest pixels in all images + percentage
		// If manual - read value
		if (threshold_type==1) {
			threshold = (int) ((int) ((threshold/stack_size))*tr_increase);
		} else {
			threshold = (int) threshold_value;
		}
		
		ImageStack stack = new ImageStack(w,h);
		
		// 1.2 set pixel values > threshold to 0 
		IJ.log("2. Thresholding images");
		for (int ss=1;ss<=stack_size;ss++){
			//Set ith image from stack
			imp.setSlice(ss);
			
			// Create processor
			ImageProcessor ip_temp = imp.getProcessor();		
			// Copy image
			ip = ip_temp.createProcessor(ip_temp.getWidth(),ip_temp.getHeight());
			ip.setPixels(ip_temp.getPixelsCopy()); // hold in new processor
						
			// set to zero
			for (int i=0; i<h; i++){
				for (int j=0; j<w; j++){
					if (ip.getPixel(j,i) > threshold || ip.getPixel(j,i)==0) {
						ip.putPixel(j,i,0);
					}
				}
			}
			stack.addSlice("Thresholded Image Stack", ip); // hold rotated slices to display
		}
		//IJ.log("Threshold: " +threshold);
		if (image_display==2) { // display images if required
			new ImagePlus("Thresholded Image Stack", stack).show();
		}

		// Hold centre of mass x-axis (to find static offset)
		int x_centre_of_mass=0;
		
		// 1.3 Find centre of mass
		IJ.log("3. Finding centre of mass");
		int x_array[]=null, y_array[]=null; // variables
		x_array = new int [stack_size];
		y_array = new int [stack_size];
		
		for (int ss=1;ss<=stack_size;ss++){
			// Variables
			double x_mass[] = null, y_mass[];
			x_mass = new double [w];
			y_mass = new double [h];
			
			// Create processor - copy thresholded images
			ImageProcessor ip_temp = stack.getProcessor(ss);		
			// Copy image
			ip = ip_temp.createProcessor(ip_temp.getWidth(),ip_temp.getHeight());
			ip.setPixels(ip_temp.getPixelsCopy()); // hold in new processor
			
			// sum mass along x-axis
			for (int i=0; i<w; i++){
				for (int j=0; j<h; j++){
					x_mass[i] = x_mass[i] + ip.getPixel(i,j); 
				}
			}
						
			// sum mass along y-axis
			for (int i=0; i<h; i++){
				for (int j=0; j<w; j++){
					y_mass[i] = y_mass[i] + ip.getPixel(j,i); 
				}
			}
			
			// calculate absorption mass
			double abspoption_mass=0;
			for (int i=0; i<x_mass.length; i++){
				abspoption_mass = abspoption_mass+x_mass[i];
			}
			
			double x_r[] = new double[w];
			//double kx_start = -w/2;
			double kx_start = 0;
			for (int i=0; i<w; i++) {
				x_r[i] = kx_start;
				kx_start++;
			}
			
			double y_r[] = new double[h];
			//double ky_start = -h/2;
			double ky_start = 0;
			for (int i=0; i<h; i++) {
				y_r[i] = ky_start;
				ky_start++;
			}
			
			double x, y, sum_x=0, sum_y=0;
			for (int i=0; i<x_mass.length; i++){
				double temp_x = x_r[i]*x_mass[i];
				sum_x = sum_x + temp_x;
			}
			for (int i=0; i<y_mass.length; i++){
				double temp_y = y_r[i]*y_mass[i];
				sum_y = sum_y + temp_y;
			}
			
			// Coordinates
			x = sum_x/abspoption_mass;
			y = sum_y/abspoption_mass;
			
			x_centre_of_mass = x_centre_of_mass + (int)Math.ceil(x);
			
			// save coordinates
			x_array[ss-1]=(int)Math.ceil(x);
			y_array[ss-1]=(int)Math.ceil(y);
			
			//IJ.log("Image "+ss +": (" +(int)Math.ceil(x)+", "+(int)Math.ceil(y)+")");
		}
		
		int [][]answer = new int [4][x_array.length];
		
		//store x_array 
		for (int i=0; i<x_array.length; i++) {
			answer[0][i] = x_array[i];
		}
		//store y_array 
		for (int i=0; i<y_array.length; i++) {
			answer[1][i] = y_array[i];
		}
		
		int offset_v = (x_centre_of_mass/stack_size)-(w/2);
		answer[2][0] = offset_v; // store offset
		answer[3][0] = (int) threshold; // store threshold - found automatically
		
		return answer;		
	}
	
	// B. Fit ellipse 
	// Using Least Squares from: http://uk.mathworks.com/matlabcentral/fileexchange/3215-fit-ellipse
	public static double [] ellipse_fitting(int [] x_array, int [] y_array){
		int matrix_warning = 1; // 1= false
		
		// find mean x and y
		double mean_x=0, mean_y=0;
		for (int i=0; i<x_array.length; i++){
			mean_x = mean_x + x_array[i];
			mean_y = mean_y + y_array[i];
			//IJ.log(+x_array[i] +", " +y_array[i] +";"); // plot all centre of mass coordinates
		}
		mean_x = mean_x/x_array.length;
		mean_y = mean_y/y_array.length;
		
		int mean_x_o = (int) Math.round(mean_x);
		int mean_y_o = (int) Math.round(mean_y);
		
		//IJ.log("mean_x: " +mean_x);
		//IJ.log("mean_y: " +mean_y);
		
		double [] x = new double[x_array.length];
		double [] y = new double[y_array.length];
		
		// remove mean from coordinates - eliminate bias
		for (int i=0; i<x_array.length; i++){
			x[i] = x_array[i] - mean_x;
			y[i] = y_array[i] - mean_y;
			//IJ.log("x: " +x[i]);
			//IJ.log("y: " +y[i] );
		}
		
		// calculate X (ellipse conic equation)
		int size=x_array.length;
		double [][] X = new double [size][5];
		double sum_X1=0, sum_X2=0, sum_X3=0, sum_X4=0, sum_X5=0;
		
		for(int j=0; j<5; j++) { // column
			for (int i=0; i<size; i++) { // row							
				if (j==0) {
					X[i][j] = x[i]*x[i];
					sum_X1 = sum_X1 + X[i][j];
					//System.out.print(X[i][j]+" ");
				} else if (j==1) {
					X[i][j] = x[i]*y[i];
					sum_X2 = sum_X2 + X[i][j];
					//System.out.print(X[i][j]+" ");
				} else if (j==2) {
					X[i][j] = y[i]*y[i];
					sum_X3 = sum_X3 + X[i][j];
					//System.out.print(X[i][j]+" ");
				} else if (j==3) {
					X[i][j] = x[i];
					sum_X4 = sum_X4 + X[i][j];
					//System.out.print(X[i][j]+" ");
				} else if (j==4) {
					X[i][j] = y[i];
					sum_X5 = sum_X5 + X[i][j];
					//System.out.print(X[i][j]+" ");
				}
			}
			//System.out.println(" ");
			//System.out.println(" ");
		}
		
		// sum(X)
		double[] sum_X = new double []{sum_X1, sum_X2, sum_X3, sum_X4, sum_X5};
		//IJ.log("\n X: " +sum_X1 +", \n" +sum_X2 +", \n" +sum_X3 +", \n" +sum_X4 +", \n" +sum_X5);
		
		// X transpose
		double [][] X_T = new double [5][size];
		for(int j=0; j<5; j++) { // row-column
			for (int i=0; i<size; i++) { // column-row
				X_T[j][i] = X[i][j];
				//System.out.print(X_T[j][i]+" ");
			}
			//System.out.println(" ");
		}
		
		// X transpose * X
		double [][] XTX = new double [5][5];
		for(int i=0; i<X_T.length; i++) { 
			for (int j=0; j<X[0].length; j++) {
				for (int k=0; k<X_T[0].length; k++) {
					XTX[i][j] += X_T[i][k] * X[k][j];
				}
				//System.out.print(XTX[i][j]+" ");
			}	
			//System.out.println(" ");
		}
		
		// work out inv (X transpose * X)
		double XTX_inv[][] = invert(XTX);
		
		// To do in next iteration: Issuing warning if matrix is singular or close to singular 
		// Currently finding cond number (norm 2) as in: http://uk.mathworks.com/help/matlab/ref/cond.html 
		// Using JAMA library to find norm 2, note: all matrix operations that I coded by hand are avialable in this library
		double [][] array2 = new double[X[0].length][X[1].length];
		double norm2_cond_check = 0;
		
		for (int i=0; i<X[0].length; i++) {
			for (int j=0; j<X[1].length; j++) {
				array2[i][j] = X[i][j];
			}
		}
		
		Matrix checkX = new Matrix(array2);
		norm2_cond_check = checkX.cond();
		
		if (norm2_cond_check == Double.POSITIVE_INFINITY){
			matrix_warning=0; // matrix singular (or close) so issue warning
		}
		//IJ.log("norm2_cond_check: " +norm2_cond_check);
		
		double [] a_m = new double[sum_X.length];
		double temp=0;
		
		// sum(X)/ ( X transpose * X ) = sum(X) * inv( X transpose * X )
		for(int i=0; i<sum_X.length; i++) { 
			for (int j=0; j<XTX[0].length; j++) {
				temp = temp + (sum_X[j] * XTX_inv[j][i]);		
			}
			a_m[i]=temp;
			temp=0;
			//System.out.println("a: " +a_m[i]);
			//IJ.log("a: " +a_m[i]);
		}
		
		// ellipse parameters
		double a=0, b=0, c=0, d=0, e=0;
		a = a_m[0];
		b = a_m[1];
		c = a_m[2];
		d = a_m[3];
		e = a_m[4];

		double tol = 0.001, orientation_r=0, cos_phi=0, sin_phi=0;
		
		// Remove orientation
		double min_abc = 0;
		double b_a = Math.abs(b/a);
		double b_c = Math.abs(b/c);
		if (b_a < b_c) {
			min_abc = b_a;
		} else {
			min_abc = b_c;
		}
		//System.out.println("min: " +min_abc);
		
		double a1=a, b1=b,c1=c, d1=d, e1=e, mean_x1 = mean_x, mean_y1=mean_y;
		if (min_abc > tol){
			//orientation_r = 0.5 * Math.atan(b/(c-a)); // +-45
			orientation_r = 0.5 * Math.atan2(b,(c-a)); // +-90 
			cos_phi = Math.cos(orientation_r);
			sin_phi = Math.sin(orientation_r);
			
			a = a*(cos_phi*cos_phi) - b*(cos_phi*sin_phi) + c*(sin_phi*sin_phi);
			b = 0;
			c = a1*(sin_phi*sin_phi) + b1*(cos_phi*sin_phi) + c1*(cos_phi*cos_phi);
			d =  d1*cos_phi - e1*sin_phi;
			e = d1*sin_phi + e1*cos_phi;
			
			mean_x = cos_phi*mean_x1 - sin_phi*mean_y1;
			mean_y = sin_phi*mean_x1 + cos_phi*mean_y1;
		} else {
			orientation_r = 0;
			cos_phi = Math.cos(orientation_r);
			sin_phi = Math.sin(orientation_r);
		}
		
		// Check if we have found an ellipse
		double check = a*c;
		double [] results = new double [13];
		
		if (check>0) {
			IJ.log("5. Ellipse found and fitted successfully");
			// Retrun ellipse data
				if (a<0){ // parameters should be positive
					a=-a; b=-b; c=-c; d=-d; e=-e;
				}
				
				// final parameters
				double X0 = mean_x - ((d/2)/a);
				double Y0 = mean_y - ((e/2)/c);
				double F = 1 + ((d*d)/(4*a)) + ((e*e)/(4*c));
				a = Math.sqrt(F/a);
				b = Math.sqrt(F/c);
				double max_ab=0, min_ab=0;
				
				if (a>b) {
					max_ab=a;
					min_ab=b;
				} else {
					max_ab=b;
					min_ab=a;
				}
				
				double long_axis = 2*max_ab;
				double short_axis = 2*min_ab;
				
				double X0_in = cos_phi*X0 + sin_phi*Y0;
				double Y0_in = -sin_phi*X0 + cos_phi*Y0;
				
				results[0] = a;
				results[1] = b;
				results[2] = orientation_r;
				results[3] = X0;
				results[4] = Y0;
				results[5] = X0_in;
				results[6] = Y0_in;
				results[7] = long_axis;
				results[8] = short_axis;
				results[9] = 1; // successful
				if (matrix_warning==0) {
					results[10] = 0; // matrix singular or close ot it
				} else {
					results[10] = 1; // matrix ok
				}
				results[11] = mean_x_o;
				results[12] = mean_y_o;
		} else if (check==0) {
			IJ.log("5. Ellipse fitting unsuccessful - parabola found");
			results[9] = 0; // unsuccessful
		} else if (check<0) {
			IJ.log("5. Ellipse fitting unsuccessful - hyperbola found");
			results[9] = 0; // unsuccessful
		}
		
		return results;
	}
	
	// C. Plot centre of mass and fitted ellipse in matlab (creates m file)
	public static void ellipse_m_file(int [] x_array, int [] y_array, double [] ellipse_params) {
		// create m file to plot ellipse and centre of mass
		// variables
		double phi = ellipse_params[2];
		double X0 = ellipse_params[3];
		double Y0= ellipse_params[4];
		double a=ellipse_params[0];
		double b= ellipse_params[1];
		
		File file = new File("plot_fitted_ellipse.m");
		FileWriter fileWriter;
		try {
			fileWriter = new FileWriter(file);
			
			fileWriter.write("clear all; clc; \n");

			fileWriter.write("figure \n");
			fileWriter.write("axes('ydir','reverse'); grid on; \n");
			fileWriter.write("hold \n \n");

			fileWriter.write("%Variables \n");
			fileWriter.write("phi=" +phi +"; \n");
			fileWriter.write("X0=" +X0 +"; \n");
			fileWriter.write("Y0=" +Y0 +"; \n");
			fileWriter.write("a=" +a +"; \n");
			fileWriter.write("b=" +b +"; \n \n");
			
			fileWriter.write("cos_phi = cos(phi); \n");
			fileWriter.write("sin_phi = sin(phi); \n");
			fileWriter.write("R = [cos_phi sin_phi; -sin_phi cos_phi]; \n \n");
			
			fileWriter.write("%Plot ellipse \n");
			fileWriter.write("theta_r = linspace(0,2*pi); \n");
			fileWriter.write("x = X0 + a*cos(theta_r); \n");
			fileWriter.write("y = Y0 + b*sin(theta_r); \n");
			fileWriter.write("ellipse = R*[x; y]; \n");
			fileWriter.write("plot( ellipse(1,:),ellipse(2,:), 'b'); \n \n");
			
			fileWriter.write("%Plot ellipse \n");
			fileWriter.write("COM_cor= [");
			for (int i=0; i<x_array.length; i++) {
				if (i==x_array.length-1){
					fileWriter.write(x_array[i]+", " +y_array[i] +"]; \n");
				} else {
					fileWriter.write(x_array[i]+", " +y_array[i] +"; ");
				}
			}
			fileWriter.write("plot(COM_cor(:,1), COM_cor(:,2), 'r+') \n \n");
			
			fileWriter.write("title('Fitted ellipse and COM coordinates'); \n");
			fileWriter.write("xlabel('x coordinate (pixels)'); \n");
			fileWriter.write("ylabel('y coordinate (pixels)'); \n");
			fileWriter.write("legend('Fitted ellipse', 'Centre of Mass Cords');");
			
			fileWriter.flush();
			fileWriter.close();
		} catch (IOException e7) {
			// TODO Auto-generated catch block
			e7.printStackTrace();
		}
	}	
	
	// D. Save COM coordiantes in .txt file
	public static void coords(int [] x_array, double [] ellipse_params) {
		File file = new File("coords.txt");
		FileWriter fileWriter;
		
		try {
			fileWriter = new FileWriter(file);
			
			// write x-coordinates of com
			for (int i=0; i<x_array.length; i++) {
				if (i==x_array.length-1){
					fileWriter.write(x_array[i]+"");
				} else {
					fileWriter.write(x_array[i]+"\n");
				}	
			}
			
			fileWriter.flush();
			fileWriter.close();
		} catch (IOException e8) {
			// TODO Auto-generated catch block
			e8.printStackTrace();
		}
	}
	
	// NOTE: JAMA library has all of these methods
	// Classes invert and gaussian are taken from: http://www.sanfoundry.com/java-program-find-inverse-matrix/
	// All credit for this matrix inversion algorithm goes to them
	public static double[][] invert(double a[][]) 
    {
        int n = a.length;
        double x[][] = new double[n][n];
        double b[][] = new double[n][n];
        int index[] = new int[n];
        for (int i=0; i<n; ++i) 
            b[i][i] = 1;
 
 // Transform the matrix into an upper triangle
        gaussian(a, index);
 
 // Update the matrix b[i][j] with the ratios stored
        for (int i=0; i<n-1; ++i)
            for (int j=i+1; j<n; ++j)
                for (int k=0; k<n; ++k)
                    b[index[j]][k]
                    	    -= a[index[j]][i]*b[index[i]][k];
 
 // Perform backward substitutions
        for (int i=0; i<n; ++i) 
        {
            x[n-1][i] = b[index[n-1]][i]/a[index[n-1]][n-1];
            for (int j=n-2; j>=0; --j) 
            {
                x[j][i] = b[index[j]][i];
                for (int k=j+1; k<n; ++k) 
                {
                    x[j][i] -= a[index[j]][k]*x[k][i];
                }
                x[j][i] /= a[index[j]][j];
            }
        }
        return x;
    }
	public static void gaussian(double a[][], int index[]) 
    {
        int n = index.length;
        double c[] = new double[n];
 
 // Initialize the index
        for (int i=0; i<n; ++i) 
            index[i] = i;
 
 // Find the rescaling factors, one from each row
        for (int i=0; i<n; ++i) 
        {
            double c1 = 0;
            for (int j=0; j<n; ++j) 
            {
                double c0 = Math.abs(a[i][j]);
                if (c0 > c1) c1 = c0;
            }
            c[i] = c1;
        }
 
 // Search the pivoting element from each column
        int k = 0;
        for (int j=0; j<n-1; ++j) 
        {
            double pi1 = 0;
            for (int i=j; i<n; ++i) 
            {
                double pi0 = Math.abs(a[index[i]][j]);
                pi0 /= c[index[i]];
                if (pi0 > pi1) 
                {
                    pi1 = pi0;
                    k = i;
                }
            }
 
   // Interchange rows according to the pivoting order
            int itmp = index[j];
            index[j] = index[k];
            index[k] = itmp;
            for (int i=j+1; i<n; ++i) 	
            {
                double pj = a[index[i]][j]/a[index[j]][j];
 
 // Record pivoting ratios below the diagonal
                a[index[i]][j] = pj;
 
 // Modify other elements accordingly
                for (int l=j+1; l<n; ++l)
                    a[index[i]][l] -= pj*a[index[j]][l];
            }
        }
    }
	
	// Just to test in Eclipse - call ImageJ
	public static void main (String[] args) {
		new ij.ImageJ();
		new Main_Tilt_Offset_Estimation().run(null);  
	}
	
}