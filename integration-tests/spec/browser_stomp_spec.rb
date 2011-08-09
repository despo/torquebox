require 'spec_helper'

require 'fileutils'
require 'torquebox-messaging'

describe "STOMP applications" do

  deploy <<-END.gsub(/^ {4}/,'')
    ---
    application:
      root: #{File.dirname(__FILE__)}/../apps/alacarte/stomp
      env: development
    
    web:
      context: /stomp-websockets
    environment:
      BASEDIR: #{File.dirname(__FILE__)}/..
    
    ruby:
      version: #{RUBY_VERSION[0,3]}
  END

  it "should be able to connect and disconnect using stomp over websockets" do
    pending "browser test" and return unless Capybara.current_driver == :browser

    visit( '/stomp-websockets/connect.html' )
    #sleep( 2 )
    #puts page.source
    page.find('#connected').text.should == 'true'
    page.find('#disconnected').text.should == 'true'
  end

end

