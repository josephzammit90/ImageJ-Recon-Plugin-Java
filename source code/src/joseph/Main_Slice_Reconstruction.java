package joseph;
//*************************************************
// Beta - Prototype
// Joseph Zammit - 2016. Email: jz390@cam.ac.uk
// This is the main java file
//*************************************************

import joseph.Reconstruction_no_acceleration;
import joseph.Reconstruction_with_acceleration;
import joseph.OpenCL_Get_Device_Info;

import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.*;
import ij.gui.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

// Include implements DialogListener to handle generic dialog changes i.e. button presses etc...
public class Main_Slice_Reconstruction implements PlugInFilter, DialogListener {

	// Set Variables
	ImagePlus imp;
	private int filter_type=1, recon_algorithm=2, axis_type=1, param_type=1, acceleration_type=1; // user parameters
	private int device_index=0, platform_index=0; // gpu/cpu index
	private double limit_mem=45, limit_mem_CPU=25;
	
	private double step=(double) 0.9; // holds angle between projections
	long start_time;
	
	// Setup of PlugInFilter Interface
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		// Initialise
		return DOES_ALL;
	}

	// 1. Run (ImageJ executes this first)
	@Override
	public void run(ImageProcessor ip) {
		// Zero/Reset all parameters before starting (to prevent restart errors)
		Param_class.setplat(0);
        Param_class.setdev(0);
        Param_class.setdev(1);
        k_time_class.setk_time_zero();
        
        // Create Gui and Start
		// Classes 2-5 are executed in order
		createGui();
		
		System.gc(); // invoke garbage collection
	}
	
	// 2. Create Buttons and Fields
	void createGui() {
		GenericDialog gd = new GenericDialog("2D Reconstruction"); 
		
		// Create button - for device (GPU/CPU) info
		Button btn = new Button("Device Info"); 
		btn.addActionListener(new ActionListener() {                           
            public void actionPerformed(ActionEvent e) 
            { 
            	OpenCL_Get_Device_Info.display_device_info();
            } 
        }); 
		// Add and show button 
		gd.add(btn);
		gd.addMessage("");
				
		// Numerical Fields
			// Full range of angles traversed during image acquisition is automatically known 
		    // from step size and length(theta) from sinogram
			gd.addNumericField("Angle between Projections: ", step, 1); // Angle step size between Projections (ex: 0.9 degrees)
			
		// Radio Buttons and GPU options
			// GPU/CPU Acceleration option
			String[] items4 = {"Do Not Activate", "CPU Acceleration", "GPU Acceleration",};
			gd.addRadioButtonGroup("Acceleration", items4, 3, 1, "Do Not Activate");
			
			gd.addNumericField("Platform index: ", platform_index, 0);
			gd.addNumericField("Device index: ", device_index, 0);
			gd.addNumericField("Limit CPU memory (%): ", limit_mem_CPU, 0);
			gd.addNumericField("Limit GPU memory (%): ", limit_mem, 0);
			
			// Sinogram angle x or y axis
			String[] items0 = {"X-Axis", "Y-Axis"};
			gd.addRadioButtonGroup("Sinogram/s Angle Parameter", items0, 2, 1, "X-Axis");
			
			// Sinogram Intensity/Absorption parameter
			String[] items3 = {"Do not Invert", "Invert"};
			gd.addRadioButtonGroup("Sinogram Intensity Parameter", items3, 2, 1, "Do not Invert");
			
			// Filter
			String[] items = {"none", "Shepp-Logan", "Ramp", "Hamming"};
			gd.addRadioButtonGroup("Filter Type (for FBP)", items, 2, 2, "Ramp");
			
			// Reconstruction type
			String[] items2 = {"Back Projection", "Filtered Back Projection (FBP)"};
			gd.addRadioButtonGroup("Reconstruction Algorithm", items2, 2, 1, "Filtered Back Projection (FBP)");
			
		gd.addDialogListener(this);
		String html = "<html>" +"<h3>Manual</h3>" +"Please visit: www.jjzideas.com";
	  	gd.addHelp(html);
		
		// Display Gui
		gd.showDialog();
	}
	
	// 3. Handle Changes in Generic Dialog (buttons etc..) and Starts Reconstruction by calling 4. below
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		// Exit if cancel is pressed
		if (gd.wasCanceled()) return false;
        
        // Run if ok is pressed
        if (gd.wasOKed()){
        	// Start Reconstruction Process
    		if (recon_algorithm==1 || recon_algorithm==2) {
    			// Pass platform and device to paramter class
    	        Param_class.setplat(platform_index);
    	        Param_class.setdev(device_index);
    	        
    	        //check mem_lim
    	        if (limit_mem>100.0) {
    	        	limit_mem=100.0;
    	        }
    	        if (limit_mem<5.0) {
    	        	limit_mem=5.0;
    	        }
    			call_recon(recon_algorithm, filter_type, step, axis_type, param_type, acceleration_type);
    		}
    		return true;
        };
        
        // Handle changes in Numerical Fields
        step = (double) gd.getNextNumber();
        platform_index = (int) gd.getNextNumber();
        device_index = (int) gd.getNextNumber();       
        limit_mem_CPU = (double) gd.getNextNumber();   
        limit_mem = (double) gd.getNextNumber(); 
        
        // Handle Changes in Radio Buttons - filter and reconstruction type
        String acc_change = gd.getNextRadioButton();
        String axis_change = gd.getNextRadioButton();
        String param_change = gd.getNextRadioButton();
        String filter_change = gd.getNextRadioButton();
        String recon_change = gd.getNextRadioButton();
   
        // GPU/CPU Acceleration
        if (acc_change=="Do Not Activate"){ 
        	acceleration_type=1;
        	Param_class.setacc(acceleration_type);
        }
        if (acc_change=="GPU Acceleration"){
        	acceleration_type=2;
        	Param_class.setacc(acceleration_type);
        }
        if (acc_change=="CPU Acceleration"){
        	acceleration_type=3;
        	Param_class.setacc(acceleration_type);
        }
        
        
        //Sinogram axis
        if (axis_change=="X-Axis"){ // Theta on x-axis, Absorption on y-axis
        	axis_type=1;
        }
        if (axis_change=="Y-Axis"){ // Theta on y-axis, Absorption on x-axis
        	axis_type=2;
        }

        //Sinogram Intesity Parameter
        if (param_change=="Do not Invert"){ 
        	param_type=1;
        }
        if (param_change=="Invert"){ // Invert values if required
        	param_type=2;
        }
  
        // Filter Type
        if (filter_change=="Ramp"){ // Ramp filter, filter_type=1
        	filter_type=1;
        }
        if (filter_change=="Shepp-Logan"){
        	filter_type=2;
        }
        if (filter_change=="Hamming"){
        	filter_type=3;
        }
        if (filter_change=="none"){ // no filter
        	filter_type=4;
        }
        
        // Image Reconstruction Algorithm Type
        if (recon_change=="Back Projection"){ // BP, recon_algorithm=1
        	recon_algorithm=1;
        }
        if (recon_change=="Filtered Back Projection (FBP)"){ 
        	recon_algorithm=2;
        }
        if (recon_change=="Expectation Maximisation"){ 
        	recon_algorithm=3;
        }
        		
        return true;
    }
	
	// 4. Displays input parameter logs and initiates reconstruction by calling 5. below
	void call_recon(int recon_algorithm, int filter, double step, int axis, int param, int acc) {
		int filter_m; // to ensure filter type is set correctly
		
		// Logs 
			IJ.log("*************User Parameters****************");
			if (acc==2) {
				if (limit_mem==100.0) {
					IJ.log("GPU acceleration enabled: 100%");
				} else {
					DecimalFormat df = new DecimalFormat("#0.#");
					IJ.log("Limited GPU acceleration enabled: "+df.format(limit_mem) +"%");
				}
			} else if (acc==3) {
				IJ.log("CPU acceleration enabled");
			} else if (acc==1) {
				IJ.log("Acceleration not enabled");
			} else return;
			
			if (param==1) {
				IJ.log("Sinogram intesity parameter not inverted");
			} else if (param==2) {
				IJ.log("Sinogram intesity parameter inverted");
			} else return;
			
			if (axis==1) {
				IJ.log("Sinogram angle parameter (theta) on x-axis");
			} else if (axis==2) {
				IJ.log("Sinogram angle parameter (theta) on y-axis");
			} else return;
			
			IJ.log("Angle between projections: " +step +"\u00b0");
			
			// Set/Check filter_type = 0 is BP is chosen
			if (recon_algorithm==1){
				filter_m=0;
				IJ.log("BP algorithm chosen");
			} else if (recon_algorithm==2) {
				filter_m=filter;
				IJ.log("FBP algorithm chosen");
			} else return;
			
			if (recon_algorithm==2) {
				if (filter_m==1) {
					IJ.log("Filter type: Ramp");
				} else if (filter_m==2) {
					IJ.log("Filter type: Shepp-Logan");
				} else if (filter_m==3) {
					IJ.log("Filter type: Hamming");
				} else if (filter_m==4) {
					IJ.log("Filter type: none");
				} else return;
			}
		
		// Determine if input is 1 image or stack
		int s_size = imp.getStackSize();
		if(s_size==1) {
			IJ.log("User input: 1 image");
			IJ.log(" ");
	    } else {
	    	IJ.log("User input: stack of " +s_size +" images");
			IJ.log(" ");
	    }
		
		// Call start_recon() to start reconstruction process 
		start_recon(imp, filter_m, step, axis, param, acc);
	}
	
	// 5. Calls reconstruction algorithm - starts execution
	void start_recon(ImagePlus imp, int filter, double step, int axis, int param, int acc){
		// If GPU acceleration is not activated, process 1 image at a time: call Reconstruction_no_acceleration.java
		if (acc==1){
			IJ.log("Reconstruction initiated - serial processing");
			IJ.log("");
			Reconstruction_no_acceleration.main(imp, filter, step, axis, param);
		} 
		
		if (acc==2){
			IJ.log("Reconstruction initiated - parallel processing");
			IJ.log("GPU platform index set to: " +platform_index);
			IJ.log("GPU device index set to: " +device_index);
			IJ.log("");
			Reconstruction_with_acceleration.main(imp, filter, (float) step, axis, param, limit_mem, limit_mem_CPU);
		} 
		
		if (acc==3){
			IJ.log("Reconstruction initiated - parallel processing");
			IJ.log("CPU platform index set to: " +platform_index);
			IJ.log("CPU device index set to: " +device_index);
			IJ.log("");
			Reconstruction_with_acceleration.main(imp, filter, (float) step, axis, param, limit_mem, limit_mem_CPU);
		} 
	}
	
	// Just to test in Eclipse - call ImageJ
	public static void main (String[] args) {
		new ij.ImageJ();
		new Main_Slice_Reconstruction().run(null);  
	}		
}

// Set Parameters for GPU/CPU acceleration
class Param_class {
	private static int dev, plat, acc;
	
	static void setdev(int dev) {
		Param_class.dev = dev;
    }
	
	static void setplat(int plat) {
		Param_class.plat = plat;
    }
	
	static void setacc(int acc) {
		Param_class.acc = acc;
    }
	
	static int getdev() {
        return dev;
    }
	
	static int getplat() {
        return plat;
    }
	
	static int getacc() {
        return acc;
    }
}