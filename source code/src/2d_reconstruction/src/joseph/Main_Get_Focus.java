package joseph;

//*************************************************
//Beta - Prototype
//Get Focus by Joseph Zammit - 2016
// Tenengrad algorithm: Pech-Pacheco, José Luis, et al. "Diatom autofocusing in brightfield microscopy: a comparative study." Pattern Recognition, 2000. Proceedings. 15th International Conference on. Vol. 3. IEEE, 2000.
//*************************************************

import java.awt.AWTEvent;
import java.text.DecimalFormat;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class Main_Get_Focus implements PlugInFilter, DialogListener {

	// Variables
	static ImagePlus imp;
	long start_time;
	int [][] Image;
	int [][] J;
	int [][] Sx;
	int [][] Sy;
	double [][] Gx;
	double [][] Gy;
	double [][] G;
	
	// Setup of PlugInFilter Interface
	public int setup(String arg, ImagePlus imp) {
		Main_Get_Focus.imp = imp;
		return DOES_ALL;
	}

	// Run
	@Override
	public void run(ImageProcessor ip) {       
		// Create Gui and Start
		createGui();
		
		// invoke garbage collection
		Image=null; J=null; Sx=null; Sy=null; Gx=null; Gy=null; G=null;
		System.gc(); 
	}
	
	// Create Buttons and Fields
	void createGui() {
		
		GenericDialog gd = new GenericDialog("Get Focus Measure"); 

		// Info
		String[] items0 = {"Enabled"};
		gd.addRadioButtonGroup("Get Image Focus Measure", items0, 1, 1, "Enabled");
		gd.addMessage("Using: Tenengrad variance algorithm");
				
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
    	  get_focus();
    	  return true;
      };
      return true;
  }
	
	public void get_focus() {
		// set Parameters
		ImageProcessor ip = null;
		
		// Get stack size
		ImageStack current_stack = imp.getStack();
		int w = current_stack.getWidth();
		int h = current_stack.getHeight();

		int stack_size = current_stack.getSize();
		
		IJ.log("********Parameters**************");
		IJ.log("Employing tenengrad variance algorithm:");
		IJ.log("\"Diatom autofocusing in brightfield microscopy: a comparative study. (Pech 2000)\"");
		IJ.log("");
		IJ.log("********Processing**************");
		long t1 = System.currentTimeMillis(); // start timer
		for (int ss=1;ss<=stack_size;ss++){
			//Set ith image from stack
			imp.setSlice(ss);
			double fmeasure=0.0;
			
			ImageProcessor ip_temp = imp.getProcessor(); // Create processor
			ip = ip_temp.createProcessor(ip_temp.getWidth(),ip_temp.getHeight());
			ip.setPixels(ip_temp.getPixelsCopy()); // hold in new processor
			
			int [][]F = new int[ip.getHeight()][ip.getWidth()];
			// Work out focus measure and display
			for (int i=0; i<ip.getHeight(); i++){
				for (int j=0; j<ip.getWidth(); j++){
					int p = ip.getPixel(j,i);
					F[i][j]=p; // Image in array
				}
			}		
			
			fmeasure = tenengrad(F, h, w);
			
			DecimalFormat df = new DecimalFormat("#0.###");
			//DecimalFormat df = new DecimalFormat("0.###E0");
			IJ.log("Focus Measure for image " +ss +": " +df.format(fmeasure));			
		}
		
		long t2 = System.currentTimeMillis(); // stop timer
		IJ.log("");
		IJ.log("********Finished****************");
	    IJ.log("Total processing time: " +(t2-t1) +" ms");
	    DecimalFormat df2 = new DecimalFormat("#0.#");
	    IJ.log("Processing time per image: " +df2.format((double) (t2-t1)/ (double) stack_size) +" ms");
	    IJ.log("");
	}

	// JZ - 2016
	// Method outputs F measure. Takes image (in array F[h][w]), image height (h) and width (w) as inputs 
	// F measure based on Tenengrad variance
	public double tenengrad(int [][] I, int h, int w) {
		int N =  h; //rows
		int M =  w; //columns
		Image = new int[N+2][M+2];
		J = new int[N+2][M+2];

		// place NxN image in middle of N+2 x N+2 image
		for (int r=1; r<N+1; r++) {
		    for (int c=1; c<M+1; c++) {
		        Image[r][c] = I[r-1][c-1];
		    }
		}
		
		// copy first and last, row and column
		for (int i=0; i<N+2; i++) {
		    Image[i][0] = Image[i][1];
		    Image[i][M+1] = Image[i][M];
		}

		for (int i=0; i<M+2; i++) {
		    Image[0][i] = Image[1][i];
		    Image[N+1][i] = Image[N][i];
		}

		J = (int[][]) Image;
		
		Sx = new int[][] {{1, 2, 1}, {0, 0, 0}, {-1, -2, -1}};
		Sy = new int[][] {{1, 0, -1}, {2, 0, -2}, {1, 0, -1}};
		
		Gx = new double [N][M];
		Gy = new double [N][M];
		
		// Compute = imfilter(Image,Sx,convolution)
		for (int r=0; r<h; r++){
		    for (int c=0; c<w; c++){
		        double s=0;
		        for (int i=0; i<3; i++){
		            for (int j=0; j<3; j++){
		                s = s + (double) Sx[i][j] * (double)J[r+i][c+j];
		            }
		        }
		        Gx[r][c]=s;
		    }
		}
		
		// Compute = imfilter(Image,Sy,convolution)
		for (int r=0; r<h; r++){
		    for (int c=0; c<w; c++){
		        int s=0;
		        for (int i=0; i<3; i++){
		            for (int j=0; j<3; j++){
		                s = s + Sy[i][j] * J[r+i][c+j];
		            }
		        }
		        Gy[r][c]=s;
		    }
		}
		
		// compute G = Gx.^2 + Gy.^2;
		G = new double[N][M];
		
		for (int r=0; r<h; r++){
		    for (int c=0; c<w; c++){
		    G[r][c] = (Gx[r][c]*Gx[r][c]) + (Gy[r][c]*Gy[r][c]);
		    }
		}
		
		// compute FM = stdw(G)^2
		double m=0;
		for (int r=0; r<h; r++){
		    for (int c=0; c<w; c++){
		        m = m + G[r][c];
		    }
		}
		
		m = m/(h*w);
		
		double s = 0;
		for (int r=0; r<h; r++){
		    for (int c=0; c<w; c++){
		        s = s + ((G[r][c] - m) * (G[r][c] - m));
		    }
		}
		
		double FM = s/(h*w);
		
		return FM;
	}
	
	// Just to test in Eclipse - call ImageJ
	public static void main (String[] args) {
		new ij.ImageJ();
		new Main_Get_Focus().run(null);  
	}
	
}