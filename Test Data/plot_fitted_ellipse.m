clear all; clc; 
figure 
axes('ydir','reverse'); grid on; 
hold 
 
%Variables 
phi=0.018800550203260897; 
X0=818.4010073421184; 
Y0=290.05601899153874; 
a=215.83672000871442; 
b=1.0758435594985956; 
 
cos_phi = cos(phi); 
sin_phi = sin(phi); 
R = [cos_phi sin_phi; -sin_phi cos_phi]; 
 
%Plot ellipse 
theta_r = linspace(0,2*pi); 
x = X0 + a*cos(theta_r); 
y = Y0 + b*sin(theta_r); 
ellipse = R*[x; y]; 
plot( ellipse(1,:),ellipse(2,:), 'b'); 
 
%Plot ellipse 
COM_cor= [597, 279; 597, 279; 620, 279; 660, 278; 717, 277; 782, 275; 853, 273; 914, 273; 969, 272; 1008, 272; 1027, 271; 1029, 270; 1010, 271; 969, 271; 918, 272; 854, 274; 797, 275; 735, 277; 683, 278; 619, 279]; 
plot(COM_cor(:,1), COM_cor(:,2), 'r+') 
 
title('Fitted ellipse and COM coordinates'); 
xlabel('x coordinate (pixels)'); 
ylabel('y coordinate (pixels)'); 
legend('Fitted ellipse', 'Centre of Mass Cords');