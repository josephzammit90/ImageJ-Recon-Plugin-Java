// UtilCL is taken directly from: https://github.com/jeffheaton/opencl-hello-world
package joseph;

import org.lwjgl.BufferUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;

import static org.lwjgl.opencl.CL10.*;

/**
 * Some utilities for OpenCL
 */
public class UtilCL {

    /**
     * Private constructor.
     */
    private UtilCL() {

    }

    /**
     * Format a number to a memory size.
     * @param size The size.
     * @return The formatted size!
     */
    public static String formatMemory(long size) {
        if(size <= 0) return "0";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    /**
     * Get the device type.
     * @param i The device type id.
     * @return The device type.
     */
    public static String getDeviceType(int i) {
        switch(i) {
            case CL_DEVICE_TYPE_DEFAULT: return "DEFAULT";
            case CL_DEVICE_TYPE_CPU: return "CPU";
            case CL_DEVICE_TYPE_GPU: return "GPU";
            case CL_DEVICE_TYPE_ACCELERATOR: return "ACCELERATOR";
        }
        return "?";
    }

    /**
     * Utility method to convert float array to float buffer
     *
     * @param floats - the float array to convert
     * @return a float buffer containing the input float array
     */
    public static FloatBuffer toFloatBuffer(float[] floats) {
        FloatBuffer buf = BufferUtils.createFloatBuffer(floats.length).put(floats);
        buf.rewind();
        return buf;
    }

    /**
     * Utility method to print a float buffer
     *
     * @param buffer - the float buffer to print to System.out
     */
    public static void print(FloatBuffer buffer) {
        for (int i = 0; i < buffer.capacity(); i++) {
            System.out.print(buffer.get(i) + " ");
        }
        System.out.println("");
    }

    /**
     * Read a resource into a string.
     * @param filePath The resource to read.
     * @return The resource as a string.
     * @throws java.io.IOException
     */
    public static String getResourceAsString(String filePath) throws IOException {
        InputStream is = UtilCL.class.getClassLoader().getResourceAsStream(filePath);
        if (is == null) {
            throw new IOException("Can't find resource: " + filePath);
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }
}
