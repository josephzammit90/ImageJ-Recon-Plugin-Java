function [img,H] = iradon(p,Theta,interp)

filter = 'ram-lak';
d = 1;
N = 2*floor( size(p,1)/(2*sqrt(2)) );
theta=0:pi/numel(Theta):pi-pi/numel(Theta);

[p,H] = filterProjections(p, filter, d);

% Define the x & y axes for the reconstructed image so that the origin
% (center) is in the spot which RADON would choose.
center = floor((N + 1)/2);
xleft = -center + 1;
x = (1:N) - 1 + xleft;
x = repmat(x, N, 1);

ytop = center - 1;
y = (N:-1:1).' - N + ytop;
y = repmat(y, 1, N);

len = size(p,1);
ctrIdx = ceil(len/2);     % index of the center of the projections

% Backprojection - vectorized in (x,y), looping over theta

interp_method = sprintf('*%s',interp); % Add asterisk to assert
                                       % even-spacing of taxis
        
% Generate trignometric tables
costheta = cos(theta);
sintheta = sin(theta);
        
% Allocate memory for the image
img = zeros(N,class(p));        
        
for i=1:length(theta)
    proj = p(:,i);
    taxis = (1:size(p,1)) - ctrIdx;
    t = x.*costheta(i) + y.*sintheta(i);
    projContrib = interp1(taxis,proj,t(:),interp_method);
    img = img + reshape(projContrib,N,N);
end
 
img = img*pi/(2*length(theta));

%======================================================================
function [p,H] = filterProjections(p_in, filter, d)

p = p_in;

% Design the filter
len = size(p,1);
H = designFilter(filter, len, d);

if strcmpi(filter, 'none')
    return;
end

p(length(H),1)=0;  % Zero pad projections

% In the code below, I continuously reuse the array p so as to
% save memory.  This makes it harder to read, but the comments
% explain what is going on.

p = fft(p);    % p holds fft of projections

for i = 1:size(p,2)
    p(:,i) = p(:,i).*H; % frequency domain filtering
end

p = real(ifft(p));     % p is the filtered projections
p(len+1:end,:) = [];   % Truncate the filtered projections
%----------------------------------------------------------------------

%======================================================================
function filt = designFilter(filter, len, d)
order = max(64,2^nextpow2(2*len));

if strcmpi(filter, 'none')
    filt = ones(1, order);
    return;
end

% First create a ramp filter - go up to the next highest
% power of 2.

filt = 2*( 0:(order/2) )./order;
w = 2*pi*(0:size(filt,2)-1)/order;   % frequency axis up to Nyquist

switch filter
    case 'ram-lak'
        % Do nothing
    case 'shepp-logan'
        % be careful not to divide by 0:
        filt(2:end) = filt(2:end) .* (sin(w(2:end)/(2*d))./(w(2:end)/(2*d)));
    case 'cosine'
        filt(2:end) = filt(2:end) .* cos(w(2:end)/(2*d));
    case 'hamming'
        filt(2:end) = filt(2:end) .* (.54 + .46 * cos(w(2:end)/d));
    case 'hann'
        filt(2:end) = filt(2:end) .*(1+cos(w(2:end)./d)) / 2;
    otherwise
        error(message('images:iradon:invalidFilter'))
end

filt(w>pi*d) = 0;                      % Crop the frequency response
filt = [filt' ; filt(end-1:-1:2)'];    % Symmetry of the filter
%----------------------------------------------------------------------


