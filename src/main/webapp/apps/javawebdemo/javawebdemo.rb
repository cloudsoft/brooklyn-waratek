require  File.join(File.dirname(__FILE__), '../..', 'main/brooklyn')

module JavaWebDemo
  
  def self.registered(app)
  
    app.get "/apps/javawebdemo/mock/nodes_load" do
      return_json :reqs_per_sec => { :node11 => 500*rand(), :node12 => 1000*rand(), :node21 => 300*rand(), :node22 => 500*rand() }
    end
  	app.get "/apps/javawebdemo/:id/nodes_load" do
  	  reqs_per_sec = brooklyn_get_json("/v1/applications/#{params[:id]}/descendants/sensor/webapp.reqs.perSec.windowed?typeRegex=.*Server.*")
  	  return_json :reqs_per_sec => reqs_per_sec
  	end

    app.get "/apps/javawebdemo/mock/info" do
      return_json({ 'root.url' => 'http://localhost:8000/' })
    end
    app.get "/apps/javawebdemo/:id/info" do
      return_json {}
    end
    
  end
  
end

register JavaWebDemo
