class LandingController < ApplicationController
  unloadable 
  
  def landing
    
    protocol = YAML.load_file("#{Rails.root}/config/protocol_task_flow.yml")["associations"] rescue {}
    
    @links = {}
    
    protocol.each do |key, value|
      value.split(",").each do |element|
      
        if element.match(/\|\d+$/)
          parts = element.split("|")
          
          file = parts[0].downcase.gsub(/\s/, "")
        
          @links[parts[1].to_i] = {
              :label => parts[0],
              :path => key.gsub(/\s/, "_").downcase,
              :image => (File.exists?("#{Rails.root}/public/images/#{file}.png") ? "#{file}.png" : "default.png")  
          }
        end
      end    
    end
    
    # raise @links.inspect
    
    render :layout => false    
  end

end 