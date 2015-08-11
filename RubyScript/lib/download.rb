#!/usr/bin/env ruby

# coding: utf-8

require 'net/http'
require 'rexml/document'

def download(url, fn)
  puts 'Downloading: ' + url
  uri = URI.parse(url)
  Net::HTTP.start(uri.host, uri.port,
                  use_ssl: uri.scheme == 'https') do |http|
    request = Net::HTTP::Get.new uri

    http.request request do |response|
      open fn, 'wb' do |io|
        i = 0
        response.read_body do |chunk|
          print '.' if (i += 1) % 10 == 0
          io.write chunk
        end
      end
    end
  end
  puts
end

RSYNTAX_SRC = 'https://repo1.maven.org/maven2/com/fifesoft/rsyntaxtextarea/'
MVN_DESCR = 'maven-metadata.xml'

doc = Net::HTTP.get(URI.parse(RSYNTAX_SRC + MVN_DESCR))
@data = (REXML::Document.new doc).root
version =  @data.elements['//versioning/latest'].first.to_s
download(RSYNTAX_SRC + version + '/' + "rsyntaxtextarea-#{version}.jar",
         'rsyntaxtextarea.jar')

VERSION = '9.0.0.0'
JRUBY_SRC = 'https://s3.amazonaws.com/jruby.org/downloads/' \
            "#{VERSION}/jruby-bin-#{VERSION}.zip"
jruby_zip = 'jruby.zip'

# File.write(jruby_zip, Net::HTTP.get(URI.parse(JRUBY_SRC)))

download JRUBY_SRC, jruby_zip
`unzip -j #{jruby_zip} jruby-9.0.0.0/lib/jruby.jar`
`rm #{jruby_zip}`
