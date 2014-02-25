require "net/https"
require "uri"
require "rest_client"
require "json"
require "json/pure"



def CIRBA_URL() $CIRBA_URL || ENV['CIRBA_URL'] || 'http://boa.cirba.com:8086/CIRBA/api/v1' end


def CIRBA_USER() $CIRBA_USER || ENV['CIRBA_USER'] || 'admin' end
def CIRBA_PASSWORD() $CIRBA_PASSWORD || ENV['CIRBA_PASSWORD'] ||  'admin' end

def CIRBA_CAP_PATH() '/routing-requests/available-capacity-query/?subslots=true' end

def CIRBA_IG_PATH() '/infrastructure-groups' end

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

def cirba_get_amenities(ig_id)
    url = File.join(CIRBA_URL(), CIRBA_IG_PATH(), "/", ig_id, "/amenities")
    puts "Getting infrastructure group listing from CiRBA."
    begin

      uri = URI.parse(url)
      http = Net::HTTP.new(uri.host, uri.port)

      request = Net::HTTP::Get.new(uri.request_uri)
      request['Accept'] = 'application/json'
      request.basic_auth(CIRBA_USER(),CIRBA_PASSWORD())



      response = http.request(request)
      
      puts "posted "+url
      puts "response "+response.code+" "+response.body
      
      content_type :json
      if (response.code.to_i / 100 == 2)
        # accept primitives by wrapping in array and taking 1st element
        return JSON.parse("["+response.body+"]")[0]
      end
      
      puts "ERROR invalid status code #{response.code}, getting json from #{url}"
      puts response
      status 404
      return { :error => "Invalid upstream response status code #{response.code}" }

# old way: fails is SSLv3 required
      response = RestClient::Request.new(
        :method => :get,
        :url => url,
        :user => CIRBA_USER(),
        :password => CIRBA_PASSWORD(),
        :headers => { :accept => :json, :content_type => :json }
      ).execute

    
    rescue Exception=>e
          
      puts "ERROR loading json from #{url}"
      puts e
      status 404
      return { :error => "Misconfigured CiRBA server" }
    end
end

def cirba_get_infra_group(ig_id)
    url = File.join(CIRBA_URL(), CIRBA_IG_PATH(), "/", ig_id)
    puts "Getting infrastructure group listing from CiRBA."
    begin

      uri = URI.parse(url)
      http = Net::HTTP.new(uri.host, uri.port)

      request = Net::HTTP::Get.new(uri.request_uri)
      request['Accept'] = 'application/json'
      request.basic_auth(CIRBA_USER(),CIRBA_PASSWORD())



      response = http.request(request)
      
      puts "posted "+url
      puts "response "+response.code+" "+response.body
      
      content_type :json
      if (response.code.to_i / 100 == 2)
        # accept primitives by wrapping in array and taking 1st element
        return JSON.parse("["+response.body+"]")[0]
      end
      
      puts "ERROR invalid status code #{response.code}, getting json from #{url}"
      puts response
      status 404
      return { :error => "Invalid upstream response status code #{response.code}" }

# old way: fails is SSLv3 required
      response = RestClient::Request.new(
        :method => :get,
        :url => url,
        :user => CIRBA_USER(),
        :password => CIRBA_PASSWORD(),
        :headers => { :accept => :json, :content_type => :json }
      ).execute

    
    rescue Exception=>e
          
      puts "ERROR loading json from #{url}"
      puts e
      status 404
      return { :error => "Misconfigured CiRBA server" }
    end
end

def cirba_get_capacity(env_name, ig_name)
    url = File.join(CIRBA_URL(), CIRBA_CAP_PATH())
    puts "Getting available capacity from CiRBA."
    begin

      uri = URI.parse(url)
      http = Net::HTTP.new(uri.host, uri.port)
      payload = {"scopes" => [ "control_environment"=> env_name, "infrastructure_groups"=> [ig_name]], "workloads" => [{"catalog_spec" => "cap-units-uncap-1cpu-2gb","workload_profile"=>"Medium_Utilization"}]}.to_json

      request = Net::HTTP::Post.new(uri.request_uri)
      request.body = payload
      request['Accept'] = 'application/json'
      request['Content-Type'] = 'application/json'
      request.basic_auth(CIRBA_USER(),CIRBA_PASSWORD())



      response = http.request(request)
      
      puts "posted "+url
      puts "response "+response.code+" "+response.body
      
      content_type :json
      if (response.code.to_i / 100 == 2)
        # accept primitives by wrapping in array and taking 1st element
        return JSON.parse("["+response.body+"]")[0]
      end
      
      puts "ERROR invalid status code #{response.code}, getting json from #{url}"
      puts response
      status 404
      return { :error => "Invalid upstream response status code #{response.code}" }

# old way: fails is SSLv3 required
      response = RestClient::Request.new(
        :method => :get,
        :url => url,
        :user => CIRBA_USER(),
        :password => CIRBA_PASSWORD(),
        :headers => { :accept => :json, :content_type => :json }
      ).execute

    
    rescue Exception=>e
          
      puts "ERROR loading json from #{url}"
      puts e
      status 404
      return { :error => "Misconfigured CiRBA server" }
    end


end

def cirba_get_data(ig_id)
  ig_details = cirba_get_infra_group(ig_id)
  ig_amenities = cirba_get_amenities(ig_id)
  ig_available_cap = cirba_get_capacity(ig_details['control_environment']['name'], ig_details['name'])
  mem_spare = (ig_available_cap[0]['infrastructure_groups'][0]['subslots'].select { |subslot| subslot["name"] == "Total_Memory" })
  mem_spare = mem_spare[0]['value'] * 2
  puts("Mem Spare")
  puts(mem_spare)
  cpu_spare = (ig_available_cap[0]['infrastructure_groups'][0]['subslots'].select { |subslot| subslot["name"]  == "Total_Cores" })[0]['value']
  
  total_cpu = ((ig_amenities.select{ |amenity| amenity['timeline'] == 'TO' })[0]['host_summary'].reduce(0) { |sum, value| sum + (value['count'] * value['cpu_cores']) }) * 8
  total_mem = ((ig_amenities.select{ |amenity| amenity['timeline'] == 'TO' })[0]['host_summary'].reduce(0) { |sum, value| sum + (value['count'] * value['memory']) })/1024
  cei = ig_details['stats']['TO']['cei']
  used_servers = ig_details['stats']['TO']['required_hosts']
  mem_used = total_mem - mem_spare
  puts("Mem Used")
  puts(mem_used)
  puts("Total Mem")
  puts(total_mem)
  return_json used: { 
    servers: [1,cei].min*100, 
    ram: (mem_used)*100/total_mem, 
    cpu_cores: total_cpu ? ((total_cpu - cpu_spare)*100/total_cpu) : 0, 
  }, spare: { servers: ([0, 1-cei].max) * 100, 
        ram: (mem_spare*100/total_mem), 
        cpu_cores: total_cpu ? (cpu_spare*100/total_cpu) : 100
    } 
end
