// Beta - Prototype
// 1. OpenCL_Main was built starting from a template at: https://github.com/jeffheaton/opencl-hello-world
// 2. lwjgl version 2.9.3 was used (using a later version will require modifications to the CL types/functions below)...
//    .. this can be found at: https://sourceforge.net/projects/java-game-lib/files/Official%20Releases/LWJGL%202.9.3/
// 3. The actual kernel parallelising the backprojection algorithm is at: resources/pprocessing.txt .

package joseph;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.*;

import java.nio.FloatBuffer;
import java.util.List;

import static org.lwjgl.opencl.CL10.*;

import ij.*;

// Please note that the Kernel has not been optimised yet, and contains a lot of global memory calls which increase execution time
// Significant improvement can be made in the kernel -> pp.txt

public class OpenCL_Main {
	private static int type = CL_DEVICE_TYPE_GPU; // device type
	
   // Method to inform user in ImageJ which GPU is being used
    public static long device_in_use() {
    	try {
			CL.create();
		} catch (LWJGLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	// GPU
    	if (Param_class.getacc() == 2) {
    		type = CL_DEVICE_TYPE_GPU;
    	}
    	
    	// CPU
    	if (Param_class.getacc() == 3) {
    		type = CL_DEVICE_TYPE_CPU;
    	}
    	
    	int dev = Param_class.getdev(); // device number (CPU/GPU)
    	int plat = Param_class.getplat(); // platform number
    	
    	// Get GPU device from 1st platform (assuming there's only 1 platform)
    	CLPlatform platform = CLPlatform.getPlatforms().get(plat);
        List<CLDevice> devices2 = platform.getDevices(type);
        
        // Print Device to Imagej log
        // Note: first GPU found is used
        IJ.log("Parallelisation to be done using " +" (" +UtilCL.getDeviceType(devices2.get(dev).getInfoInt(CL_DEVICE_TYPE)) +"): "
                +devices2.get(dev).getInfoString(CL_DEVICE_NAME));
        
        // Print Compute Units to imagej log
        IJ.log("Compute Units: " +devices2.get(dev).getInfoInt(CL_DEVICE_MAX_COMPUTE_UNITS) +" @ " +devices2.get(dev).getInfoInt(CL_DEVICE_MAX_CLOCK_FREQUENCY) +" MHz");
        
        // Print Global Memory to imagej log
        IJ.log("Global Memory: " +UtilCL.formatMemory(devices2.get(dev).getInfoLong(CL_DEVICE_GLOBAL_MEM_SIZE)));
        
        // Print Local Memory to imagej log
        IJ.log("Local Memory: " +UtilCL.formatMemory(devices2.get(dev).getInfoLong(CL_DEVICE_LOCAL_MEM_SIZE)));
        
        CL.destroy();
        
        long result = devices2.get(dev).getInfoLong(CL_DEVICE_GLOBAL_MEM_SIZE); // in bytes
        return result;
    }
    
    public static double[] opencl_function(int Np, int w, float step_r, float x_offset, float filtered[][], int h, int index) throws Exception {
    	// R[] stores result from kernel - used at the end
        double[] R = new double[Np*Np]; // results to return to Reconstruction_with_acceleration.java
        
        // GPU
    	if (Param_class.getacc() == 2) {
    		type = CL_DEVICE_TYPE_GPU;
    	}
    	
    	// CPU
    	if (Param_class.getacc() == 3) {
    		type = CL_DEVICE_TYPE_CPU;
    	}
    	
        int index_max = (int) Math.ceil((double) w/ (double) index);  // index-1 blocks of this size
        int index_min = w - (index_max * (index-1) ); // last block will be < the previous index-1 blocks
        //IJ.log("index: " +index);
        //IJ.log("index_max: " +index_max);
        //IJ.log("index_min: " +index_min);

        // index defines into how many prices we need to split the sinogram, 1 = no splitting
        for (int s_num=0; s_num<index; s_num++)
        {
        	// Initialize OpenCL and create a context and command queue
            CL.create();

            int dev = Param_class.getdev(); // device number (CPU/GPU)
        	int plat = Param_class.getplat(); // platform number
        	
            CLPlatform platform = CLPlatform.getPlatforms().get(plat);
            List<CLDevice> devices = platform.getDevices(type);
            CLContext context = CLContext.create(platform, devices, null, null, null);
            CLCommandQueue queue = clCreateCommandQueue(context, devices.get(dev), CL_QUEUE_PROFILING_ENABLE, null);
            
	        // Back-Projection
	        // Declare Parameters
	        float [] index_p;
	        float [] rindex_p;
	        float [] Np_p;
	        float [] h_p;
	        float [] w_p;
	        float [] step_r_p;
	        float [] x_offset_p;
	        float [] filtered_p; // of size -> filtered = new double[h][w]; if not split
	        
	        long start_time = System.currentTimeMillis();
	        
	        int j_size;
	        if (s_num==index-1) {
	        	j_size = index_min;
	        } else {
	        	j_size = index_max;
	        }
	        
	        // Assign Parameters
	        index_p = new float[]{(float) (0+ (s_num*(index_max)) )};
	        if (s_num==index-1) {
	        	rindex_p = new float[]{(float) index_min};
	        } else {
	        	rindex_p = new float[]{(float) index_max};
	        }
	        Np_p = new float[]{(float) Np}; 
	        h_p = new float[]{(float) h};
	        w_p = new float[]{(float) w};
	        step_r_p = new float[] {(float) step_r};
	        x_offset_p = new float[] {(float) x_offset};
	        filtered_p = new float[h*w];    
	
	        //fill filtered_p (convert 2D -> 1D)
	        // TO DO in next iteration: we do not need to store all of filtered[][]... can save time here
	        int k=0;
	        for (int i=0; i<h; i++){
	        	for (int j=0; j<w; j++){
	        		filtered_p[k] = filtered[i][j];
	        		k++;
	        	}
	        }

	        // Assign Parameters to buffers
	        FloatBuffer index_pb = UtilCL.toFloatBuffer(index_p);
	        FloatBuffer rindex_pb = UtilCL.toFloatBuffer(rindex_p);
	        FloatBuffer Np_pb = UtilCL.toFloatBuffer(Np_p);
	        FloatBuffer h_pb = UtilCL.toFloatBuffer(h_p);
	        FloatBuffer w_pb = UtilCL.toFloatBuffer(w_p);
	        FloatBuffer step_r_pb = UtilCL.toFloatBuffer(step_r_p);
	        FloatBuffer x_offset_pb = UtilCL.toFloatBuffer(x_offset_p);
	        FloatBuffer filtered_pb = UtilCL.toFloatBuffer(filtered_p);
	        FloatBuffer R_p = BufferUtils.createFloatBuffer(Np*Np*j_size); // answer buffer

	        
	        // Allocate memory for all buffers
	        CLMem indexMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, index_pb, null);
	        clEnqueueWriteBuffer(queue, indexMem, 1, 0, index_pb, null, null);
	        CLMem rindexMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, rindex_pb, null);
	        clEnqueueWriteBuffer(queue, rindexMem, 1, 0, rindex_pb, null, null);
	        CLMem NpMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Np_pb, null);
	        clEnqueueWriteBuffer(queue, NpMem, 1, 0, Np_pb, null, null);
	        CLMem hMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, h_pb, null);
	        clEnqueueWriteBuffer(queue, hMem, 1, 0, h_pb, null, null);
	        CLMem wMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, w_pb, null);
	        clEnqueueWriteBuffer(queue, wMem, 1, 0, w_pb, null, null);
	        CLMem step_rMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, step_r_pb, null);
	        clEnqueueWriteBuffer(queue, step_rMem, 1, 0, step_r_pb, null, null);
	        CLMem x_offsetMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, x_offset_pb, null);
	        clEnqueueWriteBuffer(queue, x_offsetMem, 1, 0, x_offset_pb, null, null);
	        CLMem filteredMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, filtered_pb, null);
	        clEnqueueWriteBuffer(queue, filteredMem, 1, 0, filtered_pb, null, null);
	        CLMem answerMem2 = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, R_p, null); // result buffer 
	        clFinish(queue); // Hold queue/wait - until memory is assigned     
	        
	        // Load kernel from resource file
	        String source = UtilCL.getResourceAsString("resources/gpu_kernel.txt");
	
	        // Create our program and kernel
	        CLProgram program = clCreateProgramWithSource(context, source, null);
	        Util.checkCLError(clBuildProgram(program, devices.get(dev), "", null));
	        CLKernel kernel = clCreateKernel(program, "gpu_kernel", null);
	        
	        // Execute kernel
	        // set/assign/pass created buffers to kernel
	        kernel.setArg(0, indexMem);
	        kernel.setArg(1, rindexMem);
	        kernel.setArg(2, NpMem);
	        kernel.setArg(3, hMem);
	        kernel.setArg(4, wMem);
	        kernel.setArg(5, step_rMem);
	        kernel.setArg(6, x_offsetMem);
	        kernel.setArg(7, filteredMem);
	        kernel.setArg(8, answerMem2);
	
	        // through experimentation it was found that it's best to set local_work_size to global_work_size for our processing scenario and let the complier set the size as it sees fit
	        // or set to null... CPU seems to prefer null or =1... 
	        int local_work_size = j_size; // recommended to be multiple (m) of a warp (32) by Nvidia. >32 (<max work items) to hide memory latency these 32*m threads/work items are scheduled together 
	        PointerBuffer klocal_work_size = BufferUtils.createPointerBuffer(1);
			klocal_work_size.put(0, local_work_size);
			
			int g_size = j_size; // size of theta=1 to < w
			int global_work_size = g_size; // as large as the problem
					
			PointerBuffer kglobal_work_size = BufferUtils.createPointerBuffer(1);
			kglobal_work_size.put(0, global_work_size);
			
			// Create parallelisation parameters and execute
			clEnqueueNDRangeKernel(queue,  //        (	cl_command_queue command_queue,
			kernel,  //        	 	cl_kernel kernel,
			1, //        	 	cl_uint work_dim,
			null, //        	 	const size_t *global_work_offset,
			kglobal_work_size,  //        	 	const size_t *global_work_size,
			null, //        	 	local_work_size - set to null - let compiler choose it, this is the safer option for an open source implementation... we don't know what gpus/cpus people are using
			null, //        	 	cl_uint num_events_in_wait_list,
			null);//        	 	const cl_event *event_wait_list
	
	        // Read the results memory back into our result buffer
	        clEnqueueReadBuffer(queue, answerMem2, 1, 0, R_p, null, null);
	        clFinish(queue);
	        clEnqueueReadBuffer(queue, NpMem, 1, 0, Np_pb, null, null);
	        clFinish(queue);
	
	        long stop_time = System.currentTimeMillis();
	        k_time_class.setk_time(stop_time-start_time); // return kernel setup and execution time
	        
	        // Clean up OpenCL resources
	        clReleaseKernel(kernel);
	        clReleaseProgram(program);
	        clReleaseMemObject(NpMem);
	        clReleaseMemObject(hMem);
	        clReleaseMemObject(wMem);
	        clReleaseMemObject(step_rMem);
	        clReleaseMemObject(x_offsetMem);
	        clReleaseMemObject(filteredMem);
	        clReleaseMemObject(answerMem2);
	        clReleaseCommandQueue(queue);
	        clReleaseContext(context);
	        CL.destroy();
	
	        long start_p_time = System.currentTimeMillis();
	        
	        // Read answer buffer (R_p) from kernel to array R[] in the correct format         
	        for (int i=0; i<(Np*Np); i++){
	        	for (int j=0; j<j_size; j++){
	        		R[i] = R[i] + (double) R_p.get(j + (i*j_size));
	         	}	
	        }
	        
	        long stop_p_time = System.currentTimeMillis();
	        k_time_class.setpostp_time(stop_p_time-start_p_time); // return memory read/array creation time
        }
  
	    // Return 1D result array
	    return R;
    }
    
    public static int split_problem(int Np, int w, int h, long g_mem, double limitation) {
    	// scaling by 100 to fit in Long
    	long const_mem = 7/100; // 6 floats for Np, h, w, step_r, x_offset, index, rindex
        long results_mem = Np/100*Np*w; // number of floats to hold results
        long image_mem = h/100*w; // number of float to hold image
        long total_mem = const_mem + results_mem + image_mem;
        long avail_mem = (g_mem/4)/100; // available global mem (g_mem is in bytes)
        long target_mem = (long) (avail_mem*limitation);

        int index = 1;
        
        double x_target=0;
        
        if (total_mem>target_mem){
        	// if so we need to split the problem (w wise) so that total_mem<target_mem
        	x_target = (double) (w/100*(Np*Np))/(target_mem - (7/100) - (h/100*w));
        	
        	if (x_target< (double) 2.0 && w%index == 0 ) {
        		index = 2;
        	} else {
        		long temp_total_mem=0;
        		do {
	        		index = (int) Math.ceil(x_target);
	        		double temp = Math.ceil((double) w/ (double) index);
	        		long temp_results = (long) (Np/100*Np*temp);
	        		temp_total_mem = const_mem + temp_results + image_mem;
	        		
	        		if ( w/index==1 || (w/index<1) ){
    					return 1704128;
    				}
        		}while(temp_total_mem>target_mem);
        	}
        }

        return index;
	}

    public static double per_mem_usage (int Np, int w, int h, long g_mem, int index) {
    	long const_mem = 7/100; // 6 floats for Np, h, w, step_r, x_offset, index, rindex
    	long results_mem = Np/100*Np*w/index; // number of floats to hold results
    	long image_mem = h/100*w; // number of float to hold image
    	long total_mem = const_mem + results_mem + image_mem;
    	long avail_mem = (g_mem/4)/100; // available global mem (g_mem is in bytes)
    	
    	double mem_usage_per = (double) ((double) total_mem/ (double)avail_mem)*100;
    	
    	//IJ.log("total_mem: " +total_mem);
    	//IJ.log("avail_mem: " +avail_mem);
    	//IJ.log("mem_usage_per: " +mem_usage_per);
    	
    	return mem_usage_per;
    }
    
}

// Keep kernel setup and execution time
class k_time_class {
	private static long k_time, postp_time;
	
	static void setk_time_zero() {
		k_time_class.k_time = 0;
		k_time_class.postp_time = 0;
	}
	
	static void setk_time(long time) {
		k_time_class.k_time = k_time + time;
	}
	
	static void setpostp_time(long ptime) {
		k_time_class.postp_time = postp_time + ptime;
	}
	
	static Long getktime() {
     return k_time;
	}
	
	static Long getpostptime() {
	     return postp_time;
		}
}