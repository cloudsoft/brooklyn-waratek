# check preconditions

raise '$CLOUDSOFT_SUBPATH must be set to a non-empty value' if $CLOUDSOFT_SUBPATH.nil? || $CLOUDSOFT_SUBPATH.empty?
if $PUBLIC_SUBPATH.nil? then $PUBLIC_SUBPATH='public' end
if $CLOUDSOFT_DASHBOARD_ROOT.nil? then 
  raise '$CONTROLLER_ROOT_PATH or $CLOUDSOFT_DASHBOARD_ROOT must be set' if $CONTROLLER_ROOT_PATH.nil? || $CLOUDSOFT_DASHBOARD_ROOT.nil?
  $CLOUDSOFT_DASHBOARD_ROOT = File.join($CONTROLLER_ROOT_PATH, $PUBLIC_SUBPATH, $CLOUDSOFT_SUBPATH) 
end
if ( !File.exist?(File.join($CLOUDSOFT_DASHBOARD_ROOT, 'index.html')) ||
     !File.exist?(File.join($CLOUDSOFT_DASHBOARD_ROOT, 'config.js')) ||
     !File.exist?(File.join($CLOUDSOFT_DASHBOARD_ROOT, 'main/'))
   ) then raise "Invalid root dir $CONTROLLER_ROOT_PATH / $PUBLIC_SUBPATH / $CLOUDSOFT_SUBPATH (#{$CLOUDSOFT_DASHBOARD_ROOT}): should point to root of AMP dashboard hierarchy" end

require 'rubygems'
require 'sinatra'
require 'sinatra/static_assets'
require 'uri'
require 'json/pure'

require "#{$CLOUDSOFT_DASHBOARD_ROOT}/main/brooklyn"

get "/#{$CLOUDSOFT_SUBPATH}/" do
  # JS app, for example
  # dashboard: /cloudsoft/#status/cassandra/1234
  # interactive orders: /cloudsoft/#order/cassandra
  File.read("#{$CLOUDSOFT_DASHBOARD_ROOT}/index.html")
end
# append trailing slash to the standard require.js router path
get "/#{$CLOUDSOFT_SUBPATH}" do redirect "/#{$CLOUDSOFT_SUBPATH}/" end

get '/'+File.join($CLOUDSOFT_SUBPATH, '*') do |subpath|
  # REST requests, for example:
  # status: /amp/apps/cassandra/1234/status
  # order: /amp/marketplace/order
  
  if (File.exist?(File.join($CLOUDSOFT_DASHBOARD_ROOT, subpath))) then
    send_file File.join($CLOUDSOFT_DASHBOARD_ROOT, subpath)
    return
  end
  
  paths = subpath.split('/')
  
  i = paths.length - 1
  next_controller = ""
  loop do
    if (i<0) then 
      # not supported (no valid subpath)
      puts "no path corresponding to "+subpath+"; returning empty string"
      return "" 
    end
    
    next_controller_prefix = File.join($CLOUDSOFT_DASHBOARD_ROOT, paths[0..i].join('/'))
    next_controller = "#{next_controller_prefix}/#{paths[i]}"
    break if (File.exist?(next_controller + ".rb"))
    next_controller = "#{next_controller_prefix}/index"
    break if (File.exist?(next_controller + ".rb"))
    next_controller = "#{next_controller_prefix}"
    break if (File.exist?(next_controller + ".rb"))
    i = i-1
  end

#  puts "loading "+next_controller  
  require next_controller
#  puts "redirecting "+subpath
  return local_redirect('/'+subpath)
end
