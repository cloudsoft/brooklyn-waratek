module Order
  def self.registered(app)
  
  	app.get "/marketplace/order/:type" do
  	  "TODO register your #{params[:type]}"
  	end
    
  end
end

register Order
