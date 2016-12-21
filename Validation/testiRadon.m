clc; clear all; close all;

I = phantom(255);

R = radon(I,0:179);

plot(R);

tic
J = iRadon(R,0:179,'linear');
toc
M = size(I,1);
N = size(J,1);
J = J(N-M:N-1,N-M:N-1);

imshow(J,[])

MSE = sum(sum((I-J).^2))/M^2
PSNR = 10*log10(1/MSE)