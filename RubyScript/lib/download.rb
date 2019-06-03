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
        i = 9
        response.read_body do |chunk|
          print '.' if (i += 1) % 10 == 0
          io.write chunk
        end
      end
    end
  end
  puts
end

# ****************** download rsyntaxtextarea ***********************
RSYNTAX_SRC = 'https://repo1.maven.org/maven2/com/fifesoft/rsyntaxtextarea/'
MVN_DESCR = 'maven-metadata.xml'

download(RSYNTAX_SRC + MVN_DESCR, MVN_DESCR)
data = (REXML::Document.new File.read(MVN_DESCR)).root
version = data.elements['//versioning/latest'].first.to_s
File.delete MVN_DESCR

download(RSYNTAX_SRC + version + '/' + "rsyntaxtextarea-#{version}.jar",
         'rsyntaxtextarea.jar')

VERSION = '9.2.7.0'
JRUBY_SRC = 'https://s3.amazonaws.com/jruby.org/downloads/' \
            "#{VERSION}/jruby-bin-#{VERSION}.zip"
jruby_zip = 'jruby.zip'

# File.write(jruby_zip, Net::HTTP.get(URI.parse(JRUBY_SRC)))

# ****************** download jruby ***********************
download JRUBY_SRC, jruby_zip

jruby_lib = "jruby-#{VERSION}/lib/"
#`unzip -j #{jruby_zip} #{jruby_lib}/jruby.jar`
`unzip #{jruby_zip}`
%w(ruby jruby.jar).each { |file| `mv -v #{jruby_lib + file} ./` }

File.delete jruby_zip
`rm -r jruby-#{VERSION}`
