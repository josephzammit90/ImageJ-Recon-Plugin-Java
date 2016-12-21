package joseph;
//*************************************************
//Beta - Prototype
// Joseph Zammit - 2016. Email: jz390@cam.ac.uk
// Reconstruction with GPU acceleration
//*************************************************

import joseph.OpenCL_Main;
import org.jtransforms.fft.DoubleFFT_1D;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class Reconstruction_with_acceleration {

	// Main Method  - calls all required methods and displays final reconstruction image/stack
	public static void main(ImagePlus imp, int filter, double step, int axis, int param, double limit_mem, double limit_mem_CPU) {
		IJ.log("*************Parallel Processing************");
		
		// Get stack size (if 1 image this will be = 1, no need to handle these independently)
		ImageStack current_stack = imp.getStack();
		int stack_size = current_stack.getSize();
		int index = 0;
					
		// Use GPU else CPU
		if (Param_class.getacc() == 2) {
			//Inform user in ImageJ which GPU is going to be used to parallelise the BP computations
			long g_mem = OpenCL_Main.device_in_use();
			
			// check if global memory is enough to work out 1 projection provesses in gpu
				int h1 = current_stack.getHeight();
				int w1 = 1;
				int w2 = current_stack.getWidth();
				int Np1 = (int) Math.floor((h1-3.0)/Math.sqrt(2.0));
				
				long const_mem = 7; // 6 floats for Np, h, w, step_r, x_offset, index, rindex
		        long results_mem_1p = Np1*Np1*w1; // number of floats to hold results
		        long image_mem_1p= h1*w2; // number of float to hold image
		        long total_mem_1p = const_mem + results_mem_1p  + image_mem_1p ;
		        long avail_mem = g_mem/4; // available global mem (g_mem is in bytes)
	
		        if (total_mem_1p>(0.95*avail_mem)) {
		        	IJ.log("");
		        	IJ.log("Error: GPU memory is not enough to reconstruct 1 projection");
		        	IJ.log("");
		        	return;
		        }
		        
		    // check if problem needs to be split because of GPU global memory constraint or on user request
		    limit_mem = limit_mem/100.0;
	
			index = OpenCL_Main.split_problem(Np1, w2, h1, g_mem, limit_mem);
			
			if (index == 1704128) { // check if we can split the sinogram 
				IJ.log("");
				IJ.log("Error: Projection couldn't be properly split to fix in GPU memory");
				IJ.log("Making sinogram width a power of 2 should fix this");
	        	IJ.log("");
	        	return;
			}
			
			// display % of GPU memory being used
			double m_u = OpenCL_Main.per_mem_usage (Np1, w2, h1, g_mem, index);
			if (m_u!=0.0) {
				IJ.log("");
				if (index!=1) {
					IJ.log("Sinogram split by a factor of: " +index);
				}
				IJ.log("GPU memory usage: " +Math.round(m_u) +"%");
				IJ.log("");
			}
		} else if (Param_class.getacc()==3) { //CPU
			//Inform user in ImageJ which CPU is going to be used to parallelise the BP computations
			long g_mem = OpenCL_Main.device_in_use();
			
			// check if global memory is enough to work out 1 projection provesses in gpu
				int h1 = current_stack.getHeight();
				int w1 = 1;
				int w2 = current_stack.getWidth();
				int Np1 = (int) Math.floor((h1-3.0)/Math.sqrt(2.0));
				
				long const_mem = 7; // 6 floats for Np, h, w, step_r, x_offset, index, rindex
		        long results_mem_1p = Np1*Np1*w1; // number of floats to hold results
		        long image_mem_1p= h1*w2; // number of float to hold image
		        long total_mem_1p = const_mem + results_mem_1p  + image_mem_1p ;
		        long avail_mem = g_mem/4; // available global mem (g_mem is in bytes)
	
		        if (total_mem_1p>(0.95*avail_mem)) {
		        	IJ.log("");
		        	IJ.log("Error: CPU memory is not enough to reconstruct 1 projection");
		        	IJ.log("");
		        	return;
		        }
		        
		    // check if problem needs to be split because of CPU global memory constraint or on user request
		    limit_mem_CPU = limit_mem_CPU/100.0;
	
			index = OpenCL_Main.split_problem(Np1, w2, h1, g_mem, limit_mem_CPU);
			
			if (index == 1704128) { // check if we can split the sinogram 
				IJ.log("");
				IJ.log("Error: Projection couldn't be properly split to fix in CPU global memory");
				IJ.log("Making sinogram width a power of 2 should fix this");
	        	IJ.log("");
	        	return;
			}
			
			// display % of CPU memory being used
			double m_u = OpenCL_Main.per_mem_usage (Np1, w2, h1, g_mem, index);
			if (m_u!=0.0) {
				IJ.log("");
				if (index!=1) {
					IJ.log("Sinogram split by a factor of: " +index);
				}
				IJ.log("CPU memory usage: " +Math.round(m_u) +"%");
				IJ.log("");
			}
		}
		
		// Create List to hold reconstruction results
		List<ImagePlus> result_array = new ArrayList<ImagePlus>();
		
		// Start Reconstruction - loop through all slices
		long start_time = System.currentTimeMillis();
		for (int i=1;i<=stack_size;i++){
			//Set ith image from stack
			imp.setSlice(i);
			
			// Create new ImageProcessor (this was the fastest way I could create a new image in the end)
			// There must be a neater way... but it works
			ImageProcessor ip1 = imp.getProcessor();
			ImageProcessor ip = ip1.createProcessor(ip1.getWidth(),ip1.getHeight());
			ip.setPixels(ip1.getPixelsCopy());
			
			// Call reconstruction method and store returned reconstructed image in result_array
			ImagePlus result_submit = reconstruction_method(ip, filter, step, axis, param, i, index);
			
			// Add reconstructed image to list 
			result_array.add(result_submit);
		}
		IJ.log("Processing finished");
		
		long stop_time = System.currentTimeMillis();
		
		// Create stack
		ImageStack stack = create_stack(result_array);
        
        // Display stack of reconstructed images
        new ImagePlus("Stack Reconstruction", stack).show();
		
        // Finished - Display logs
        display_logs(stack_size, start_time, stop_time);
	}
		
	// Creates stack from reconstructed images - called from main
	public static ImageStack create_stack(List<ImagePlus> result_array){
		// Set width and height of stack = to 1st slice in list
        int width, height;
        ImageProcessor ip_get_parms = result_array.get(0).getProcessor();
        height=ip_get_parms.getHeight();
    	width=ip_get_parms.getWidth();
    	
    	// Create Image Stack
        ImageStack final_stack = new ImageStack(width, height);
        
        // Add all images (ImagePlus) from result_array to stack      
        for (ImagePlus img : result_array){
        	ImageProcessor ip_img = img.getProcessor(); 
        	final_stack.addSlice("Image Stack", ip_img);
        }
        
        // Return stack of reconstructed images
        return final_stack;
	}
	
	// Displays Logs - called from main
	public static void display_logs(int stack_size, long start, long stop){
		IJ.log(" ");
		IJ.log("*************Info and Statistics************");
        if (stack_size==1) {
        	IJ.log("Image reconstructed and displayed");
        } else {
        	IJ.log("Stack of " +stack_size +" images reconstructed and displayed");
        }
        IJ.log("Total reconstruction processing time: " +(stop-start) +" ms");
        IJ.log("");
        DecimalFormat df2 = new DecimalFormat("#0.#");
		IJ.log("Processing time/image: " +df2.format((double) (stop-start)/ (double) (stack_size)) +" ms, of which:");
		
		double time_t = (stop-start)/(stack_size);
		double time_k = (k_time_class.getktime())/(stack_size);
		double time_p = (k_time_class.getpostptime())/(stack_size);
		double time_g = (time_t-time_k-time_p);
		
		IJ.log("Kernel setup and execution: " +Math.round(time_k) +" ms (~" +Math.round(100*time_k/time_t) +" %)");
		IJ.log("Reading memory buffer to array: " +Math.round(time_p) +" ms (~" +Math.round(100*time_p/time_t) +" %)");
		IJ.log("General functions, and image/stack creation/display: " +Math.round(time_g) +" ms (~" +Math.round(100*time_g/time_t) +" %)");
		IJ.log("");
	}
	
	// P.0 Reconstruction algorithm - called from main
	public static ImagePlus reconstruction_method(ImageProcessor ip_r, int filter_r, double step_r, int axis_r, int param_r, int image_num, int index){
		IJ.log("Processing Image: " +image_num);
		
		// find maximum pixel value for normalisation
		double max_pixel_value = ip_r.getMax();
		
		// Make sure that sinogram is the form theta (x-axis) vs intensity (y-axis)
		if (axis_r==2){
			ip_r=ip_r.rotateLeft();
		}
		
		// Invert sinogram - user parameter
    	if (param_r==2){
			ip_r.invert();
		}
		
    	// Define Parameters
		int Np, L, x_offset, jk=0, h=0, w=0;
		double I[][], R[]=null, Rn[]=null;
		float filtered[][] = null;
		float[][] filtered_temp;
		double V[] = null;
		ImageProcessor ip_r_temp;
		ImagePlus im_recon_r;
    			
		// Assign Parameters
		h = ip_r.getHeight();
		w = ip_r.getWidth();
		Np = (int) Math.floor((h-3.0)/Math.sqrt(2.0));
		L = (int) Math.pow(2, Math.ceil(Math.log(h)/Math.log(2)));
		x_offset = (int) Math.ceil(h/2.0);
		I = new double[h][w];		
		
		// Get intensity I from sinogram/image
		for (int i=0; i<h; i++){
			for (int j=0; j<w; j++){
				I[i][j]=(double)ip_r.getPixel(j,i);
			}
		}
		
		// If filter = False, just BP
		if (filter_r==0 || filter_r==4) {
			filtered = new float[h][w];
			// Testing filtered values
			for (int i=0; i<h; i++){
				for (int j=0; j<w; j++){
					filtered[i][j]=(float)I[i][j];
				}
			}
		}
		
		// P.1 If filter = True, FBP, call create filter method
		if (filter_r==1 || filter_r==2 || filter_r==3) {
			filtered_temp = new float[L][w];
			filtered = create_filter(filtered_temp, V, jk, L, filter_r, I, w, h); // call filter algorithm - returns filtered values
		}
	
		// P.2 Parallelised Back Projection Algorithm - call bp_p	
		R=bp_p(Np, w, (float) step_r, x_offset, filtered, h, index);
		
		// P.3 Normalise R values between 0 and 255... roughly (8 bit)
		Rn=normalise_pixel_values(Np, R, max_pixel_value);
		
		// Create new image to return. Note that R is now a 1D matrix, as opposed to a 2D matrix as in Reconstruction_no_acceleration.java
		// this is because the kernel outputs a 1D array, so it's not worth wasting time converting to a 2D R[][] before this point
		ip_r_temp = ip_r.createProcessor(Np,Np);
		int k1=0;
		for (int i=0; i<Np; i++){
			for (int j=0; j<Np; j++){
				ip_r_temp.putPixel(j,i,(int)Rn[k1]);
				k1++;
			}
		}
		
		im_recon_r = new ImagePlus("Reconstruction", ip_r_temp.rotateLeft());
    	
		// Return reconstructed image
		return im_recon_r;
	}		
	
	// P.1 Create required filter - called by reconstruction_method
	public static float[][] create_filter(float[][] filtered_temp, double V[], int jk, int L, int f_type, double I[][], int w, int h) {			
		// ********************************************* //
		// *****************Ramp Filter***************** //
		// ********************************************* //
		if (f_type==1) {
			V = new double[L]; // V is set to all zeros by default
			
			// Define Ramp filter
			for (int i = 0; i < (L/2); i++) { // ramp up
				V[i]=i/(L/2.0);
			}
			jk=(L/2);
			for (int i = (L/2); i > 0; i--) { // ramp down
				V[jk]=i/(L/2.0);
				jk++;
			}
		}
		
		// ********************************************* //
		// *****************Shepp-Logan***************** //
		// ********************************************* //
		if (f_type==2) {
			double x_shepp;
			double [] V_sinc = new double [L];
			double [] V_temp = new double [L];
			V = new double[L]; // V is set to all zeros by default
			
			// Define Ramp filter
			for (int i = 0; i < (L/2); i++) { // ramp up
				V[i]=i/(L/2.0);
			}
			int jk1=(L/2);
			for (int i = (L/2); i > 0; i--) { // ramp down
				V[jk1]=i/(L/2.0);
				jk1++;
			}
			
			// Define sinc
			for (int i=0; i<L; i++) {
				x_shepp = i/(double)(2*L);
				x_shepp = x_shepp*2;
				if (i==0){
					V_sinc[i] = 1;
				} else {
					V_sinc[i] = Math.abs((Math.sin(Math.PI*x_shepp))/(Math.PI*x_shepp)); 
				}
			}
			
			// Define Shepp-Logan filter
			for(int i=0; i<(L/2); i++) {
				V_temp[i] = V_sinc[i];
			}
			
			int jks=L/2;
			for(int i=(L/2); i>0; i--) {
				V_temp[jks] = V_sinc[i];
				jks++;
			}
			
			// V_temp * Ramp to give Shepp-Logan
			for(int i=0; i<L; i++){
	        	V[i] = V[i]*V_temp[i];		        	
	        }
		}
		
		// ***************************************** //
		// *****************Hamming***************** //
		// ***************************************** //
		if (f_type==3) {
			double [] V_hamming = new double[L];
			double [] V_H_temp = new double[L];
			V = new double[L]; // V is set to all zeros by default
							
			// Define Ramp filter
			for (int i = 0; i < (L/2); i++) { // ramp up
				V[i]=i/(L/2.0);
			}
			int jk1=(L/2);
			for (int i = (L/2); i > 0; i--) { // ramp down
				V[jk1]=i/(L/2.0);
				jk1++;
			}
						
			// Define Hamming filter
			for (int i=0; i<L; i++) {
				V_hamming[i] = 0.54 - 0.46 * Math.cos(2 * Math.PI * (i) / L );
			}
			
			for (int i = 0; i <= (L/2); i++) {
				V_H_temp[i] = V_hamming[(L/2)+i-1];
			}
			
			int jk2=(L/2)+1;
			for (int i = 0; i < (L/2)-1; i++) {
				V_H_temp[jk2] = V_hamming[i];
				jk2++;
			}
			
			// V_H_temp * Ramp to give Hamming
			for(int i=0; i<L; i++){
	        	V[i] = V[i]*V_H_temp[i];		        	
	        }
		}
			
		// Apply filter
		double S[] = null, Sff[]= null, W[]= null;
		S = new double[L];
		W = new double[L];
	
		for (int ti=0; ti<w; ti++) {
			// Assign S
			for (int i=0; i<h; i++){
				S[i] = I[i][ti];
			}
	
			// Using realForwardFull() and complexInverse() for fft/ifft from JTransforms
	        // Documentation at: http://wendykierp.github.io/JTransforms/apidocs/        
	        double[] fft = new double[L*2]; //real and complex numbers hence the *2. N=L is equivalent to N point fft() in Matlab
			System.arraycopy(S, 0, fft, 0, L);
			// fft
			DoubleFFT_1D fft_d = new DoubleFFT_1D(L);        		
			
			// perform fft (fft = Sf)
	        fft_d.realForwardFull(fft);
	        
	        // Sff
	        Sff = new double[L*2];
	        for (int i=0; i<(L*2); i=i+2){
	        	Sff[i] = fft[i]*V[(i/2)];
	        	Sff[i+1] = fft[i+1]*V[(i/2)];
	        }
	       
	        // W - ifft (stored in Sff)
	        fft_d.complexInverse(Sff, true);
	        // Get real values only
	        int real_array_pos = 0;
	        for(int j=0; j<Sff.length; j++) {
	        	if (j==0 || (j%2)==0) {
	        		W[real_array_pos]=Sff[j];
	        		real_array_pos++;
	        	}
	        }
	        		        
	        // Assign to filtered array
	        for(int j=0; j<L; j++) {
	        	filtered_temp[j][ti]=(float) W[j];
	    	}
		}
		
		// return filter values
		return filtered_temp;
	}
 
	// P.2 Parallelised back-projection method - called by reconstruction_method
	public static double[] bp_p(int Np, int w, float step_r, float x_offset, float[][] filtered, int h, int index){
		// Parameters
		double[] R = new double[Np*Np];
        
		// Call OpenCL_Main to handle parallelisation using lwjgl and opencl
		try {
			R=OpenCL_Main.opencl_function(Np, w, step_r, x_offset, filtered, h, index);
		} catch (Exception e) {
			e.printStackTrace();
		}
	
		// Return 1D array
		return R;
	}

	// P.3 Normalise pixel values
	public static double[] normalise_pixel_values(int Np, double R[], double max_pixel_value){
		// Parameters
		double maxvalue=0;
		double minvalue=0;
		
		// 1. find minimum value in R[]
		for (int i=0; i<(Np*Np); i++) {
	        if (R[i]<minvalue) {
	        	minvalue=R[i];
	        }
		}
		
		// 2. transform R[] to positive values
		for (int i=0; i<(Np*Np); i++){
			R[i]=R[i]-minvalue;
		}
			
		// 3. find maximum values in R[]
		for (int i=0; i<(Np*Np); i++) {
	        if (R[i]>maxvalue) {
	        	maxvalue=R[i];
	        }
		}
		
		// 4. normalise values in R[] to 0-1
		for (int i=0; i<(Np*Np); i++){
			R[i]=R[i]/maxvalue; // values positive and normalised to 0-1
			R[i]=R[i]*(max_pixel_value); // set max value to max value in original image/sinogram (change this as needed)
		}
		return R;
	}

}
