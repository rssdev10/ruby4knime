# 'Ruby script' node type for text morph analysis.
#  It uses mystem utility by Yandex

require 'open3'
require 'json'

def mystem(in_str)
  out = ''
  result = {}
  Open3.popen3('./mystem -gin --format json') do |stdin, stdout, stderr, wait_thr|
    # stdin, stdout and stderr should be closed explicitly in this form.
    stdin.puts in_str
    stdin.close

    exit_status = wait_thr.value # Process::Status object returned.

    out = stdout.readlines

    stdout.close
    stderr.close
  end

  out.each do |line|
    str = line.force_encoding 'utf-8'
    analysis = JSON.parse(str)

    analysis['analysis'].each do |term|
      #puts term['lex'], term['gr'].partition(/\w+/)[1]
      if (gr = term['gr'])
        result[term['lex']] = gr.partition(/\w+/)[1]
      end
    end
  end

  result
end

count = $in_data_0.length
$in_data_0.each_with_index do |row,i|
  terms = mystem row.document.to_string
  # puts terms

  $out_data_0 << row
  setProgress "#{i*100/count}%"
end
