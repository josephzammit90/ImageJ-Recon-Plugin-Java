clear all; close all; clc;

Ang = 1;  % Change Ang. To 0.9, for example. Or 1.

% Create Sinogram I
np = 255;
P = phantom(np);
I = radon(P,0:Ang:360);

% OR read Sinogram I
% I = imread('sinogram_400.tif');
% I = imread('test2.tif');

filter_type = 1;
w = size(I,1);
Np = fix((w-3)/sqrt(2));
Np = np;
L = 2^(ceil(log2(w)));
x_offset = ceil(w/2);

% Filter definition
V = zeros(L,1);
V = [0:((L/2)-1), (L/2):-1:1]'/(L/2);

if filter_type == 0;
    V = ones(L,1);
end

% Apply Filter
filtered = zeros(L,size(I,2));

for t=1:size(I,2)
    S   = I(:,t);
    Sf  = fft(S,L);
    Sff = Sf.*V;
    W   = ifft(Sff,L);
    filtered(:,t) = real(W);
end

% Back project
xc = Np/2+0.5;
yc = Np/2+0.5;
R = zeros(Np,Np); % Reconstruction
    
tic
for theta = 1:size(I,2)/2
    t = deg2rad((theta-1)*Ang);
    for x = 1:Np
        for y = 1:Np
            r = (((x-xc)*cos(t)+(y-yc)*sin(t))+x_offset);
            
   			if (ceil(r) == floor(r))
				q1 = filtered(ceil(r),theta);
				q2=0;
			else
				q1 = filtered(ceil(r),theta)  * (r - floor(r));
				q2 = filtered(floor(r),theta) * (ceil(r) - r);
			end
            
            R(x,y) = R(x,y) + q1 + q2;
        end
    end
end
toc

subplot(1,2,1);
imshow(P);
subplot(1,2,2);
R = imrotate(R,90);
R = R/max(max(R));
imshow(R,[]);

%%

Rm = iradon(I,0:Ang:360);
S = size(Rm,2);

if S==Np
    A=1; B=Np;
else
    A=S-Np; B=S-1;
end

Rm = Rm(A:B,A:B);

MSE = sum(sum((P-R).^2))/Np.^2
PSNR = 10*log10(1/MSE)

MSEM = sum(sum((P-Rm).^2))/Np.^2
PSNRN = 10*log10(1/MSEM)

imshow(P-R)









