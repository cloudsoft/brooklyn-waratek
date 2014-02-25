require  File.join(File.dirname(__FILE__), '../..', 'main/brooklyn')

module Cassandra
  
  def self.registered(app)

  	app.get "/apps/cassandra/mock/nodes" do
      return_json node1: { 'datastore.url' => '127.0.0.1:9160', 'datacenter.name' => 'dc1' }, 
                  node2: { 'datastore.url' => '127.0.0.1:19160', 'datacenter.name' => 'dc1' }
  	end
  	app.get "/apps/cassandra/:id/nodes" do
  	  var dcNodeAddresses = brooklyn_get_json("/v1/applications/#{params[:id]}/descendants/sensor/cassandra.cluster.nodes");
  	  var dcNames = brooklyn_get_json("/v1/applications/#{params[:id]}/descendants/sensor/cassandra.cluster.name");
  	  var nodeAddresses = brooklyn_get_json("/v1/applications/#{params[:id]}/descendants/sensor/datastore.url");
  	  
  	  # TODO piece the above together as per mock
  	  return_json x
  	end
  
    app.get "/apps/cassandra/mock/nodes_load" do
      return_json :reads => { :node1 => 500*rand(), :node2 => 1000*rand() }, :writes => { :node1 => 500*rand(), :node2 => 100*rand() }
    end
  	app.get "/apps/cassandra/:id/nodes_load" do
  	  reads = brooklyn_get_json("/v1/applications/#{params[:id]}/descendants/sensor/cassandra.reads.perSec.windowed?typeRegex=.*CassandraNode")
  	  writes = brooklyn_get_json("/v1/applications/#{params[:id]}/descendants/sensor/cassandra.writes.perSec.windowed?typeRegex=.*CassandraNode")
  	  return_json :reads => reads, :writes => writes
  	end
    
  end
  
end

register Cassandra
