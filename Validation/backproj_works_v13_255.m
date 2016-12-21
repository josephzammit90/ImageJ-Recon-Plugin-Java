clc; clear all; close all;

Np=255;
Theta = 0:179;
P = phantom(Np);
p = radon(P,Theta);
pp = p;

interp_method = 'linear';
d = 1;
theta=0:pi/numel(Theta):pi-pi/numel(Theta);
N=Np;

% Design Filter
w = size(p,1);
L = 2^(ceil(log2(w)));
V = zeros(L,1);
H = [0:((L/2)-1), (L/2):-1:1]'/(L/2);

% Filter
p(length(H),1)=0;  % Zero pad projections

p = fft(p);        % p holds fft of projections

for i = 1:size(p,2)
    p(:,i) = p(:,i).*H; % frequency domain filtering
end

p = real(ifft(p));   % p is the filtered projections
p(w+1:end,:) = [];   % Truncate the filtered projections

len = size(p,1);
ctrIdx = ceil(len/2); % index of the center of the projections

% Backprojection - vectorized in (x,y), looping over theta
       
% Generate trignometric tables
costheta = cos(theta);
sintheta = sin(theta);
        
% Allocate memory for the image
img = zeros(N,N);

d = Np/2+0.5;

tic
for i=1:length(theta)
    proj = p(:,i);
    for X = 1:Np
        for Y = 1:Np
            
            t = (X-d)*costheta(i) + (Y-d)*sintheta(i)+ctrIdx;
            
            c = ceil(t);
            f = floor(t);
            
            if (c == f)
				q1 = proj(c);
				q2 = 0;
			else
				q1 = proj(c) * (t - f);
				q2 = proj(f) * (c - t);
			end
               
            img(N+1-Y,X) = img(N+1-Y,X) + q1 + q2;
            
        end
    end
end
toc

img = img*pi/(2*length(theta));
img = img/max(max(img));

imshow(img,[])

MSE = sum(sum( (P-img(1:255,1:255)).^2 ))/Np^2

I = iradon(pp, Theta);

NMSE = sum(sum( (P-I(1:255,1:255)).^2 ))/Np^2

10*log10(1/MSE)
10*log10(1/NMSE)

%----------------------------------------------------------------------




