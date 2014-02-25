require "net/https"
require "uri"
require "rest_client"
require "json"
require "json/pure"

def BROOKLYN_URL() $BROOKLYN_URL || ENV['BROOKLYN_URL'] ||
  (raise "$BROOKLYN_URL must be set (along with $BROOKLYN_USER and $BROOKLYN_PASSWORD if required)") end

def BROOKLYN_PUBLIC_URL() $BROOKLYN_PUBLIC_URL || ENV['BROOKLYN_PUBLIC_URL'] || BROOKLYN_URL() end

def BROOKLYN_USER() $BROOKLYN_USER || ENV['BROOKLYN_USER'] || 'admin' end
def BROOKLYN_PASSWORD() $BROOKLYN_PASSWORD || ENV['BROOKLYN_PASSWORD'] || (puts 'WARN: using default $BROOKLYN_PASSWORD if required' || 'admin') end
def BROOKLYN_USE_SSLV3() 
  ($BROOKLYN_USE_SSLV3 || ENV['BROOKLYN_USE_SSLV3'] == "true" || ENV['BROOKLYN_USE_SSLV3'] == "yes") ? true : false 
end

helpers do
  # server-side redirect to a different processor
  def local_redirect(url)
    call(env.merge("PATH_INFO" => url))
  end
  def return_json(obj)
    content_type :json
    if (obj.nil?) then return {}
    elsif (obj.instance_of?(Hash) || obj.instance_of?(Array)) then return obj.to_json
    else return obj
    end
  end
end

def brooklyn_get(path)
    url = File.join(BROOKLYN_URL(), path)
    puts "loading brooklyn #{path} from #{url}"
    begin

      uri = URI.parse(url)
      http = Net::HTTP.new(uri.host, uri.port)
      if (url.start_with?("https://"))
        http.use_ssl = true
        if (BROOKLYN_USE_SSLV3())
          http.ssl_version = :SSLv3
        end
        http.verify_mode = OpenSSL::SSL::VERIFY_NONE
      end

      request = Net::HTTP::Get.new(uri.request_uri)
      request['Accept'] = 'application/json'
      request.basic_auth(BROOKLYN_USER(),BROOKLYN_PASSWORD())

      response = http.request(request)
#      puts "posted "+url
#      puts "response "+response.code+" "+response.body
      return response;
      
# old way: fails if SSLv3 required
      response = RestClient::Request.new(
        :method => :get,
        :url => url,
        :user => BROOKLYN_USER(),
        :password => BROOKLYN_PASSWORD(),
        :headers => { :accept => :json, :content_type => :json }
      ).execute

    
    rescue Exception=>e
    
      if (!$BROOKLYN_USE_SSLV3 && e.message == "SSL_connect returned=1 errno=0 state=SSLv2/v3 read server hello A: tlsv1 alert internal error")
        puts "WARN: detected brooklyn server #{url} requires USE_SSLV3, turning that on (will affect all https sessions!)"
        $BROOKLYN_USE_SSLV3 = true
        return brooklyn_get_json(path)
      end
      
      puts "ERROR loading json from #{url}"
      puts e
      status 404
      return { :error => "Invalid application or misconfigured AMP/Brooklyn server" }
    end
end

def brooklyn_get_json(path)

    response = brooklyn_get(path);
    
    content_type :json
    if (response.code.to_i / 100 == 2)
      # accept primitives by wrapping in array and taking 1st element
      return JSON.parse("["+response.body+"]")[0]
    end
      
    puts "ERROR invalid status code #{response.code}, getting json from #{url}"
    puts response
    status 404
    return { :error => "Invalid upstream response status code #{response.code}" }

end

module BrooklynForwarding
  def self.registered(app)

    app.get "/main/brooklyn/app/:id/icon" do
        response = brooklyn_get("/v1/applications/#{params[:id]}/entities/#{params[:id]}/icon")
        content_type(response.content_type)
        content_type = response.content_type
        body = response.body
    end

    app.get "/main/brooklyn/mock/node_info/node11" do return_json name: 'Node 1', group_id: 'region1'; end 
    app.get "/main/brooklyn/mock/node_info/node12" do return_json name: 'Node 2', group_id: 'region1'; end 
    app.get "/main/brooklyn/mock/node_info/node21" do return_json name: 'Node 1', group_id: 'region2'; end 
    app.get "/main/brooklyn/mock/node_info/node22" do return_json name: 'Node 2', group_id: 'region2'; end 
    
    app.get "/main/brooklyn/:id/node_info/:node_id" do
      entity = brooklyn_get_json("/v1/applications/#{params[:id]}/entities/#{params[:node_id]}");
      
      result = {}
      result['id'] = entity['id']
      result['name'] = entity['name'] 
      result['type'] = entity['type']
       
      locs = brooklyn_get_json("/v1/applications/#{params[:id]}/entities/#{params[:node_id]}/locations");
      return_json result if (!locs)
      
      if (locs.length>1) then
        result['group_id']='multiple'
      else
        loc = locs[0];
        result['group_id'] = loc['spec'] || 'unknown';
        
        if (loc['links'] && loc['links']['spec']) then
            # get the name of the spec
            loc = brooklyn_get_json(loc['links']['spec']);
        end
        result['group_name'] = (loc['config'] ? loc['config']['displayName'] : nil) || loc['name'] || result['group_id'];
      end
      
      return_json result
    end

  end  
end

register BrooklynForwarding
