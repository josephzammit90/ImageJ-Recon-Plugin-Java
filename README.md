Please note that I am not longer mainting this codebase. For any queries and access to manual see: https://github.com/pedropabloVR/ImageJ-Recon-Plugin-Java

# OptiJ

OptiJ is an ImageJ plugin library which allows a user to go from a set of tomographic projections (acquired using an OPT imaging system, for example) to a set of 2D slices which can be stacked on top of each other to reconstruct a 3D model of the imaged sample.

This constructed library consists of 8 plugins which guide the user through the tomographic reconstruction process. A short description of the OptiJ plugins is provided below, for a full description for the created plugins please refer to the attached manual.  All the plugins can be used as standalone programs with your own data in Windows or Mac OS.

The main plugins are 'Create Sinogram' and '2D reconstruction'. The other plugins provide some form of error estimate, correction, and pre-processing (these are optional and might be hardware dependent).

********************
Plugin Description (see Manual)
********************

1). Derive Focus Measure Plugin: The Derive Focus Measure Plugin uses the Tenengrad variance to calculate the focus measure in each input image. Input: Image/Image Stack, Output: focus measure.

2). Estimate Background Plugin: This averages a stack of images and outputs a single averaged image (quick and dirty 'LPF').

3). Beer-Lambert Correction Plugin: Takes a projection (or stack of projections) and a background image (or stack of the same size as the projection stack), applies the Beer-Lambert law and outputs the correct projection/s.

4). Image Noise Estimation Plugin: Estimates the noise variance.

5). Estimate Tilt & Static Offset Plugin: Assuming that a marker bead is present in the projections, this plugin provides an estimate for any tilt/offset present in the imaging system.

6). Dynamic Offset Correction Plugin: Corrects for dynamic offset. (requires the 'Estimate Tilt & Static Offset' Plugin to be run first.)

7). Create Sinogram Plugin: Creates a sinogram stack from a stack of projections.

8). 2D Reconstruction Plugin: 2D Reconstruction is the process of creating a 2D image from a sinogram. The 2D Reconstruction Plugin can be used as a stand-alone plugin for any input sinogram/s and will output the corresponding reconstructed 2D slices using back-projection (BP) or Filtered back-projection (FBP). CPU/GPU acceleration is available (see manual).

********************
Workflow example
********************

Assuming you start off with a set of background and sample projections, a typical workflow would be:

1). Estimate the average background using the 'Estimate Background' Plugin. Input: Background image stack, Output: Average background image.

2). Apply beer-lambert correction using your sample projection stack (the image stack acquired from your imaging system). Input: Sample projection stack and the Average background image, Output: Corrected sample projection stack.

3). *Optional. The 'Estimate Tilt & Static Offset Correction' Plugin tracks the trajectory of a marker bead placed in the sample. This provides an estimate of tilt/offset errors present in the imaging system. This is hardware dependent and requires a dark marker bead to be imaged with the sample. If not applicable ignore. Input: projection stack (with a marker bead), Output: tilt and offset errors.

4). Create sinograms using the corrected sample projection stack (‘Create Sinogram’ Plugin). Input: Corrected sample projection stack, Output: Sinogram Stack. Optional: If known, tilt and offset errors can be corrected, if not leave these features disabled.

5). *Optional. Dynamic offsets can be corrected for using the 'Dynamic Offset Correction' Plugin. This requires the 'Estimate Tilt & Static Offset Correction' Plugin to be used before hand (step 3). Input: Sinogram stack, Output: Dynamic offset corrected sinogram stack.

6). *Optional. At any point, the noise variance of your image stack can be estimated using the 'Image Noise Estimate Plugin'. Input: image/image stack, Output: estimated variance. You can use any 3rd party plugins to de-noise your images.

7). With the sinogram stack in hand, the final step is to reconstruct the imaged sample using the ‘2D Reconstruction Plugin’. This uses back-projection or filtered back-projection to reconstruct 2D slices from a sinogram stack. GPU/CPU acceleration is available (kernel is yet to be optimised). Input: Sinogram stack. Output: Reconstructed 2D slices. The reconstructed slices can be stacked (e.g. Using Volumer Viewer in ImageJ) resulting in a 3D rendition of the imaged sample.


********************
Notes
********************
  i). See 'OptiJ Manual.pdf' for a full description of the plugins

  ii). To install and start using this ImageJ Plugin please download 'Sensor_CDT.jar' in the master branch and follow the installation guide in the manual.

  iii). A description of the main classes is given below. Srouce code can be found in the 'source code' folder.

  iv). A description of test data that can be used with these plugins is given below. Material can be found in the 'Test Data' folder.

  v). A description of validation data that can be used with these plugins is given below. Material can be found in the 'Validation' folder.


********************
Source Code
********************
a). Main_Slice_Reconstruction - This is the main class for the 2D Reconstruction Plugin.

b). Reconstruction_no_acceleration - Contains the reconstruction methods called by a).

c). Reconstruction_with_acceleration - Contains the accelerated reconstruction methods called by a).

d). OpenCL_Main - Contains OpenCL methods used for reconstruction called by c).

e). OpenCL_Get_Device_Info - Contains OpenCL methods used for getting device info.

f). (The GPU/CPU Kernel): gpu_kernel.txt.

g). Main_Average_Background - This is the main class for the Estimate Background Plugin.

h). Main_Beer_Lambert - This is the main class for the Beer-Lambert Correction Plugin.

i). Main_DMC - This is the main class for the Dynamic Offset Correction Plugin.

j). Main_Get_Focus - This is the main class for the Derive Focus Measure Plugin.

k). Main_Noise_Estimation - This is the main class for the Image Noise Estimation Plugin.

l). Main_Sinogram_Reconstruction - This is the main class for the Create Sinogram Plugin.

m). Main_Tilt_Offset_Estimation - This is the main class for the Estimate Tilt & Static Offset Plugin.

********************
Test Data
********************
Note: Some files are missing due to their large size. (Need to update)

a). ‘A1 - Sinogram to test 2D reconstruction’: A stack of 10 sinograms to test the 2D Reconstruction plugin. The angle between projection = 0.9 degrees for all images. The result is given in ‘A1 - result’ using default settings with GPU acceleration.

b). ‘A2 - cameraman’: A single image used to test the Derive Focus Measure plugin. The result should be: 5214128498.854.

c). ‘A3 - Bead’: A stack of 20 projections of a marker bead. This is first used to test the Estimate Tilt & Static Offset plugin. Setting the Threshold value to 1770 manually (enabling this feature), and enabling the create m file, and save COM coordinates feature. The estimated errors should be: static offset (122), lateral tilt (-1.085), and axial tilt (0.302). The generate m file and txt file are given in ‘plot_fitted_ellipse.m’ and ‘coords.txt’ respectively.

d). ‘A3 - Bead’ can also be used to test the Create Sinogram plugin. Enabling lateral tilt correction and static offset (using the correction values of -122, and 1.085 respectively), the resulting sinogram stack is given in ‘A4 - Bead Sinogram’.

e). The Dynamic Offset Correction plugin can be tested using ‘A4 - Bead Sinogram’ and ‘coords.txt’ (the txt file should lie in the ImageJ folder). The angle between projections is 18 degrees. The resulting sinogram stack is given in ‘A5 - Bead Sinogram with Dynamic Offset Correction’.

********************
Validation Data
********************

Validates the implementation of these Java plugins. Please ignore if you haven't requested the full report.

a). phantom3dAniso.m: 3D phantom validation exercise.

b). backproj_works_v9.m: Our back-projection algorithm using linear interpolation. The Java implementation is based on this code.

c). backproj_works_v13_255.m: Our modified back-projection algorithm, precomputing sine, cosine, ceil and floor functions, achieving 64% speedup over ‘backproj_works_v9.m’.

d). iRadon.m: Modified version of Mathworks iradon.m function, to use interp1 linear interpolation rather than iradonmex.

e). testiRadon.m: Calls ‘iRadon.m’ to test.
