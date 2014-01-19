ruby4knime
==========

## Intro
Ruby scripting extension for [KNIME](http://knime.org)

This is preliminary support of Ruby scripting language for Knime.

Now realized following node types:
* Ruby Generator allows to generate any string or numeric data. Or process any external sources using Ruby.
* Ruby Script allows to process input DataTable into output DataTable.
* Ruby Script 2x2 allows to process 2 input DataTable into 2 output DataTable.
* Ruby Snippet allows to process input DataTable into output DataTable by writing code only for one row processing.

See [Ruby wrapper](RubyScript/rb/README.rdoc)

[Download binary](https://drive.google.com/folderview?id=0Bwx0cbtdU5K6TklLRG90cm5HbFk)

## Sample workflow
[Download sample workflow](samples/KNIME_Ruby_Test.zip)

This is simple workflow for ruby4knime testing only. It includes all realized node types.
![Ruby test workflow](docs/pics/workflow.png)

Now some details.

Node 1 contains the following code:
    1000.times do |i|
      x = i * 0.1 / Math::PI
      $outContainer << Cells.new.double(x).double(Math.sin(x)).double(Math.sin(x + Math::PI/3))
    end

Node 2 contains the following code:
    $outContainer.rowKey = 100000 # generate table keys from this number
    1000.times do |i|
      x = i * 0.1 / Math::PI
      $outContainer << Cells.new.double(x).double(Math.cos(x)).double(Math.cos(0.3*x))
    end

Node 3 contains the following code:
    $inData0.each do |row|
      $outContainer << (row << Cells.new.double(row[1].to_f-row[2].to_f))
    end


Node 4 contains the following code:
    $inData0.each do |row|
      $outContainer0 << row
    end
    $inData1.each do |row|
      $outContainer1 << row
    end

Node 6 contains the following code:
    Cells.new.
      double(row[1].to_f).
      double(row[2].to_f - row[4].to_f)


Joiner (Node 5) generates following data:
![joiner node 5](docs/pics/joiner.png)

We can control results by line plots.

Fragment of Line Plot (Node 10)
![line plot 10](docs/pics/output_10.png)

Fragment of Line Plot (Node 11)
![line plot 11](docs/pics/output_11.png)

Fragment of Line Plot (Node 12)
![line plot 12](docs/pics/output_12.png)

Fragment of Line Plot (Node 13)
![line plot 13](docs/pics/output_13.png)

You can combine these Ruby nodes in any combination with other KNIME nodes.