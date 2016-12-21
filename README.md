# ImageJJavaReconLib
ImageJ Plugin Library for Image (OPT) Pre-processing, processing and reconstruction.

The main goal of this project was to design, prototype, and implement an open source library of plugins in ImageJ [5]
(an image processing tool coded in Java) that would allow a user to go from a set of projections to a set of reconstructed 2D images which can be stacked on top of each other to render a 3D model of the imaged sample.

Notes
  i). See 'ImageJ Plugin Library Manual.pdf' for a full description of the plugins
  
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
a). ‘A1 - Sinogram to test 2D reconstruction’: A stack of 10 sinograms to test the 2D Reconstruction plugin. The angle between projection = 0.9 degrees for all images. The result is give in ‘A1 - result’ using default settings with GPU acceleration.

b). ‘A2 - cameraman’: A single image used to test the Derive Focus Measure plugin. The result should be: 5214128498.854.

c). ‘A3 - Bead’: A stack of 20 projections of a marker bead. This is first used to test the Estimate Tilt & Static Offset plugin. Setting the Threshold value to 1770 manually (enabling this feature), and enabling the create m file, and save COM coordinates feature. The estimated errors should be: static offset (122), lateral tilt (-1.085), and axial tilt (0.302). The generate m file and txt file are given in ‘plot_fitted_ellipse.m’ and ‘coords.txt’ respectively. 

d). ‘A3 - Bead’ can also be used to test the Create Sinogram plugin. Enabling lateral tilt correction and static offset (using the correction values of -122, and 1.085 respectively), the resulting sinogram stack is given in ‘A4 - Bead Sinogram’.

e). The Dynamic Offset Correction plugin can be tested using ‘A4 - Bead Sinogram’ and ‘coords.txt’ (the txt file should lie in the ImageJ folder). The angle between projections is 18 degrees. The resulting sinogram stack is given in ‘A5 - Bead Sinogram with Dynamic Offset Correction’.

********************
Validation Data
********************
a). phantom3dAniso.m: 3D phantom validation exercise.

b). backproj_works_v9.m: Our back-projection algorithm using linear interpolation. The Java implementation is based on this code.

c). backproj_works_v13_255.m: Our modified back-projection algorithm, precomputing sine, cosine, ceil and floor functions, achieving 64% speedup over ‘backproj_works_v9.m’.

d). iRadon.m: Modified version of Mathworks iradon.m function, to use interp1 linear interpolation rather than iradonmex.

e). testiRadon.m: Calls ‘iRadon.m’ to test.
