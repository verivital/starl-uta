function [xi yi] = subdivide_line(x, y, SPACING, intsX, intsY)
% Given line endpoints in the form [x1 x2] [y1 y2] and a spacing value,
% SPACING, subdivide_line returns x and y coordinates for line segments 
% breaking the line into segments of SPACING length

% If no intersection points are provided, set them to empty matricies
if nargin == 3
    intsX = [];
    intsY = [];
end

% If the line isn't vertical, proceed normally
if diff(x) ~= 0
    angle = atan2(abs(diff(y)),abs(diff(x)));
    xi = union(x,min(x):cos(angle)*SPACING:max(x));
    xi = union(xi, intsX);
    yi = interp1(x,y,xi,'linear');
    if ~isempty(find(isnan(xi),1)) || ~isempty(find(isnan(yi),1))
        [xi' yi']
        angle
        error(['(x,y): ' num2str(x) ', ' num2str(y)]);
    end
    %b = [xi' yi'];
    %d = p_dist(b(1,:),b(2,:))  
    %d = p_dist(b(2,:),b(3,:))
else
    % If the line is vertical simply divide by SPACING along the y axis
    yi = union(y,min(y):SPACING:max(y));
    yi = union(yi, intsY);
    xi = repmat(x(1),1,length(yi));
    %b = [xi' yi'];
    %d = p_dist(b(1,:),b(2,:))
    %d = p_dist(b(2,:),b(3,:))
end