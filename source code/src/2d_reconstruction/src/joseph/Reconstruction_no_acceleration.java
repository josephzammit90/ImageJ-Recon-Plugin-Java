package joseph;
//*************************************************
//Beta - Prototype
// Joseph Zammit - 2016. Email: jz390@cam.ac.uk
// Reconstruction without GPU acceleration
//*************************************************

import org.jtransforms.fft.DoubleFFT_1D;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class Reconstruction_no_acceleration {

	// Main Method  - calls all required methods and displays final reconstruction image/stack
	public static void main(ImagePlus imp, int filter, double step, int axis, int param) {
		IJ.log("*************Serial Processing**************");
		// Get stack size (if 1 image this will be = 1, no need to handle these independently)
		ImageStack current_stack = imp.getStack();
		int stack_size = current_stack.getSize();
		
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
			ImagePlus result_submit = reconstruction_method(ip, filter, step, axis, param, i);
			
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
        DecimalFormat df2 = new DecimalFormat("#0.#");
		IJ.log("Processing time/image: " +df2.format((double) (stop-start)/ (double) (stack_size)) +" ms");
		IJ.log(" ");
	}
	
	// R.0 Reconstruction algorithm - called from main
	public static ImagePlus reconstruction_method(ImageProcessor ip_r, int filter_r, double step_r, int axis_r, int param_r, int image_num){
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
		double I[][], R[][]=null, Rn[][]=null, filtered[][] = null, filtered_temp[][] = null;
		double V[] = null;
		ImageProcessor ip_r_temp;
		ImagePlus im_recon_r;
    			
		// Assign Parameters
		h = ip_r.getHeight();
		w = ip_r.getWidth();
		Np = (int) Math.floor((h-3.0)/Math.sqrt(2.0));
		//Np=255; // Testing
		
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
			filtered = new double[h][w];
			// Testing filtered values
			for (int i=0; i<h; i++){
				for (int j=0; j<w; j++){
					filtered[i][j]=(double)I[i][j];
				}
			}
		}
		
		// R.1 If filter = True, FBP, call create filter method
		if (filter_r==1 || filter_r==2 || filter_r==3) {
			filtered_temp = new double[L][w];
			filtered = create_filter(filtered_temp, V, jk, L, filter_r, I, w, h); // call filter algorithm - returns filtered values
		}
		
		// R.2 Serial Back Projection Algorithm - call bp_s (result in R[][])
		R=bp_s(Np, w, step_r, x_offset, filtered);
		
		// R.3 Normalise R values between 0 and 255... roughly (8 bit)
		Rn=normalise_pixel_values(Np, R, max_pixel_value);
		
		// Create new image to return
		ip_r_temp = ip_r.createProcessor(Rn.length,Rn.length);
		for (int i=0; i<Np; i++){
			for (int j=0; j<Np; j++){
				ip_r_temp.putPixel(j,i,(int)Rn[i][j]);
			}
		}
		
		im_recon_r = new ImagePlus("Reconstruction", ip_r_temp.rotateLeft());
    	
		// Return reconstructed image
		return im_recon_r;
	}		
	
	// R.1 Create required filter - called by reconstruction_method
	public static double[][] create_filter(double filtered_temp[][], double V[], int jk, int L, int f_type, double I[][], int w, int h) {			
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
	        	filtered_temp[j][ti]=W[j];
	    	}
		}
		
		// return filter values
		return filtered_temp;
	}
		
	// R.2 Serial back-projection method - called by reconstruction_method
	public static double[][] bp_s(int Np, int w, double step_r, int x_offset, double filtered[][]){
		// Parameters
		double xc, yc, t=0.0, r=0.0, q1=0.0, q2=0.0;
		 xc = Np/2.0 +0.5;
		 yc = Np/2.0 +0.5;
		
		 //xc = Np/2.0;
		 //yc = Np/2.0;
		
		double[][] R = new double[Np][Np]; // 0 to Np-1 (not 1 to Np like Matlab)
		
		for (int theta=1;theta<w;theta++){ 
			t = Math.toRadians((theta-1)*step_r);
			for (int x=1;x<=Np;x++){
				for (int y=1;y<=Np;y++){
					r = ((x-xc)*Math.cos(t) + (y-yc)*Math.sin(t)) +  x_offset;	
					if ((int)Math.ceil(r) == (int)Math.floor(r)){
						q1=filtered[(int) Math.ceil(r)-1][theta-1];
						q2=0;
					} else {
						q1=filtered[(int) Math.ceil(r)-1][theta-1]*(r - Math.floor(r));;
						q2=filtered[(int) Math.floor(r)-1][theta-1]*(Math.ceil(r) - r);					
					}
					R[x-1][y-1] = R[x-1][y-1] +q1 +q2; //Remember arrays start from 0 not 1 in java
				}
			}
		}
		
		// Return 2D array
		return R;
	}

	// R.3 Normalise pixel values
	public static double[][] normalise_pixel_values(int Np, double R[][], double max_pixel_value){
		// Parameters
		double maxvalue=0;
		double minvalue=0;
		
		// 1. find minimum value in R[][]
		for (int i=0; i<Np; i++) {
		    for (int j=0; j<Np;j++) {
		        if (R[i][j]<minvalue) {
		        	minvalue=R[i][j];
		        }
		    }
		}
		
		// 2. transform R[][] to postive values
		for (int i=0; i<Np; i++){
			for (int j=0; j<Np; j++){
				
				R[i][j]=R[i][j]-minvalue;
			}
		}
			
		// 3. find maximum values in R[][]
		for (int i=0; i<Np; i++) {
		    for (int j=0; j<Np;j++) {
		        if (R[i][j]>maxvalue) {
		        	maxvalue=R[i][j];
		        }
		    }
		}

		// 4. normalise values in R[][] to 0-1
		for (int i=0; i<Np; i++){
			for (int j=0; j<Np; j++){
				R[i][j]=R[i][j]/maxvalue; // values positive and normalised to 0-1
				R[i][j]=R[i][j]*(max_pixel_value); // set max value to max value in original image/sinogram (change this as needed)
			}
		}
		return R;
	}

}
