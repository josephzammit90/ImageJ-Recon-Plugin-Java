// Beta - Prototype
// 1. OpenCL_Main was built starting from a template at: https://github.com/jeffheaton/opencl-hello-world
// 2. lwjgl version 2.9.3 was used (using a later version will require modifications to the CL types/functions below)...
//    .. this can be found at: https://sourceforge.net/projects/java-game-lib/files/Official%20Releases/LWJGL%202.9.3/

package joseph;

import org.lwjgl.LWJGLException;
import org.lwjgl.opencl.*;

import java.util.List;
import java.util.Locale;

import static org.lwjgl.opencl.CL10.*;

import ij.*;

public class OpenCL_Get_Device_Info {

	public static void display_device_info() {
		try {
			CL.create();
		} catch (LWJGLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		int cpu_index = 0;
		int gpu_index = 0;
		
        for (int platformIndex = 0; platformIndex < CLPlatform.getPlatforms().size(); platformIndex++) {
            CLPlatform platform = CLPlatform.getPlatforms().get(platformIndex);
            System.out.println("Platform #" + platformIndex + ":" + platform.getInfoString(CL_PLATFORM_NAME));
            IJ.log("************************");
            IJ.log("Platform #" + platformIndex + ": " + platform.getInfoString(CL_PLATFORM_NAME));
            IJ.log("************************");
            
            List<CLDevice> devices = platform.getDevices(CL_DEVICE_TYPE_ALL);
            for (int deviceIndex = 0; deviceIndex < devices.size(); deviceIndex++) {
                CLDevice device = devices.get(deviceIndex);

                System.out.printf(Locale.ENGLISH, "Device #%d(%s):%s\n",
                        deviceIndex,
                        UtilCL.getDeviceType(device.getInfoInt(CL_DEVICE_TYPE)),
                        device.getInfoString(CL_DEVICE_NAME));
                // Print Device to Imagej log
                IJ.log("Device #"+deviceIndex +" (" + UtilCL.getDeviceType(device.getInfoInt(CL_DEVICE_TYPE)) +"): "
                        +devices.get(deviceIndex).getInfoString(CL_DEVICE_NAME));

                System.out.printf(Locale.ENGLISH, "\tCompute Units: %d @ %d mghtz\n",
                        device.getInfoInt(CL_DEVICE_MAX_COMPUTE_UNITS), device.getInfoInt(CL_DEVICE_MAX_CLOCK_FREQUENCY));
                // Print Compute Units to imagej log
                IJ.log("Compute Units: " +devices.get(deviceIndex).getInfoInt(CL_DEVICE_MAX_COMPUTE_UNITS) +" @ " +devices.get(deviceIndex).getInfoInt(CL_DEVICE_MAX_CLOCK_FREQUENCY) +" MHz");             
                
                System.out.printf(Locale.ENGLISH, "\tGlobal memory: %s\n",
                        UtilCL.formatMemory(device.getInfoLong(CL_DEVICE_GLOBAL_MEM_SIZE)));
                // Print Global Memory to imagej log
                IJ.log("Global Memory: " +UtilCL.formatMemory(devices.get(deviceIndex).getInfoLong(CL_DEVICE_GLOBAL_MEM_SIZE)));
                
                System.out.printf(Locale.ENGLISH, "\tLocal memory: %s\n",
                        UtilCL.formatMemory(device.getInfoLong(CL_DEVICE_LOCAL_MEM_SIZE)));
                
                // Print Local Memory to imagej log
                IJ.log("Local Memory: " +UtilCL.formatMemory(devices.get(deviceIndex).getInfoLong(CL_DEVICE_LOCAL_MEM_SIZE)));
                
                // Print device index
                String dev_type =  UtilCL.getDeviceType(device.getInfoInt(CL_DEVICE_TYPE)); //get device type and store in variable
                if (dev_type=="CPU"){
                	IJ.log("Device index: " +cpu_index);
                	cpu_index++;
                } else if (dev_type=="GPU") {
                	IJ.log("Device index: " +gpu_index);
                	gpu_index++;
                }
                
                IJ.log("");
                System.out.println();                   
            }
        }

        CL.destroy();
    }
}