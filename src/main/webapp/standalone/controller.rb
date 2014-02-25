require 'rubygems'
require 'sinatra'
require 'sinatra/static_assets'
require 'uri'
require 'json/pure'

# path used in URL for the app - must be different to all sub-sub-paths
$CLOUDSOFT_SUBPATH = 'amp'

# dashboard code can be placed in a subdir (or symlink) under the public space 
# served by the controller, to link to this controller from another locations; 
# alternatively set $CLOUDSOFT_DASHBOARD_ROOT and set it as the :public_folder

# the code below makes this happen:

raise 'This controller must not be invoked from another controller.' if !$CONTROLLER_ROOT_PATH.nil?
$PUBLIC_SUBPATH = 'public'
$CONTROLLER_ROOT_PATH = File.expand_path(File.dirname(__FILE__))

# set this so that the above symlink is not needed  
$CLOUDSOFT_DASHBOARD_ROOT = File.join(File.dirname($CONTROLLER_ROOT_PATH))
set :public_folder, $CLOUDSOFT_DASHBOARD_ROOT


$BROOKLYN_URL=$BROOKLYN_URL || ENV['BROOKLYN_URL'] || "http://localhost:8081/"
# if required also set $BROOKLYN_USER and $BROOKLYN_PASSWORD
# and $BROOKLYN_PUBLIC_URL if you're exposing the address to end users (and you're on something non-portable like "localhost")

enable :session
set :port, ENV['CONTROLLER_BIND_PORT']
set :bind, ENV['CONTROLLER_BIND_IP']

# static content under public/ (from this directory) gets served in preference to the paths here

get '' do redirect "/#{$CLOUDSOFT_SUBPATH}/" end
get '/' do redirect "/#{$CLOUDSOFT_SUBPATH}/" end

require "#{$CONTROLLER_ROOT_PATH}/../controller_amp_embedded"