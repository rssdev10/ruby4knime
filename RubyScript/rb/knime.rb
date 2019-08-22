# frozen_string_literal: true

require 'java'

java_import org.knime.base.data.append.column.AppendedColumnRow
java_import org.knime.core.data.def.StringCell
java_import org.knime.core.data.def.DoubleCell
java_import org.knime.core.data.def.IntCell
java_import org.knime.core.data.def.LongCell

java_import org.knime.core.data.RowKey

java_import org.knime.core.data.container.DataContainer

java_import org.knime.core.node.ExecutionContext
java_import org.knime.core.data.container.BlobSupportDataRow
java_import org.knime.core.data.DataCell

java_import org.knime.core.node.workflow.FlowVariable

# This module contains utility methods and classes
# for convinient Ruby script writing for KNIME
# See also https://tech.knime.org/javadoc-api
module Knime
  private # hide methods from subclasses of Object class

  # This module defines a container for columns for using it in
  # style Cell.new.StringCell('str').IntegerCell(123).DoubleCell()...
  module CellUtility
    def add_cell(cell)
      @cells ||= []
      @cells << cell
    end

    # generate methods for compatibility with previous release
    [
      [:int, IntCell],
      [:long, LongCell],
      [:string, StringCell],
      [:double, DoubleCell],
      [:stringCell, StringCell] # workaround for RowKey class
    ].each do |name, cls|
      Object.send(:define_method, name) do |val|
        add_cell cls.new(val)
        self
      end
    end

    # generate an appropriate methods for any types annotated in
    #   the output model
    # name should be a fully qualified Java class name!
    $outColumnTypes.each do |name|
      cls = Java::JavaClass.for_name(name)
      import cls.to_s
      rb_cls = cls.ruby_class
      Object.send(:define_method, cls.simple_name) do |val|
        add_cell rb_cls.new(val)
        self
      end
    end
  end

  # Instances of this class are intended for a columns container.
  class Cells
    include CellUtility

    attr_reader :cells
  end

  # This method allows to display any text to indicate current
  # calculation progress
  def setProgress(*val)
    $exec.setProgress(*val)
  end

  # module with methods for DataCell conversations
  module DataConverter
    # convert to double
    def to_f
      getDoubleValue
    end

    # convert to integer
    def to_i
      getIntValue
    end

    # convert to long
    def to_l
      getLongValue
    end
  end

  # module for extending DataRow implementation classes.
  module CellAccessor
    # Get cells by index in Ruby style
    def [](idx)
      if idx >= 0
        getCell(idx)
      else
        getCell(getNumCells + idx) # -1 - last element
      end
    end

    # Generate dynamic methods for accessing to cells by column name
    #   from BlobSupportDataRow.
    # All symbols except :word: are changed to underline symbol.
    # For all names is created a pair of normal and downcased name.
    # For input 0 only is generated simple names. For all inputs methods has
    # a following format: i#{input_num}_translated_column_name
    #
    $num_inputs ||= 0
    (0...$num_inputs).each do |i|
      table = $input_datatable_arr[i]
      col_names = table.getDataTableSpec.getColumnNames.map do |str|
        [(s1 = str.gsub(/[^[[:word:]]]/, '_').gsub(/\_+/, '_').chomp('_')),
         s1.downcase].uniq
      end
      col_names.each_with_index do |names, num|
        names.each do |name|
          define_method(name) { getCell(num) } if i == 0
          define_method("i#{i}_#{name}") { getCell(num) }
        end
      end
    end
  end

  # utility class for access to flow variables
  class FlowVariableList
    def self.[](name)
      @vars ||= $node.getAvailableFlowVariables
      if (flowvar = @vars[name])
        case flowvar.getType
        when FlowVariable::Type::DOUBLE
          flowvar.getDoubleValue
        when FlowVariable::Type::INTEGER
          flowvar.getIntValue
        when FlowVariable::Type::STRING
          flowvar.getStringValue
        end
      end
    end

    def self.[]=(name, val)
      case val.class
      when Integer
        $node.pushFlowVariableInteger(name, val)
      when Float
        $node.pushFlowVariableDouble(name, val)
      else
        $node.pushFlowVariableString(name, val.to_s)
      end
      @vars = $node.getAvailableFlowVariables
    end
  end

  def snippet_runner
    count = $in_data_0.length
    step = 0x2FF
    coef = step / count.to_f
    $in_data_0.each_with_index do |row, i|
      $out_data_0 << (yield row)
      setProgress "#{i * coef}%" if (i & step) == 0
    end
  end
end

include Knime

[DoubleCell, IntCell, LongCell].each do |cls|
  cls.class_exec { include DataConverter }
end

# Extended knime class
class BlobSupportDataRow
  include CellUtility
  include CellAccessor

  # Append new columns by previously added chain of cells
  def append
    AppendedColumnRow.new(self, *@cells)
  end

  # Append new columns by instance of Cell class
  def <<(cells)
    AppendedColumnRow.new(self, *cells.cells)
  end
end

# Extended knime class
class Java::OrgKnimeCoreNode::BufferedDataTable
  # Add Ruby specific methods
  def length
    getRowCount
  end

  alias size length
end

# Extended knime class
class Java::OrgKnimeCoreData::RowKey
  include CellUtility

  # Create new row object
  # Return BlobSupportDataRow object
  def new_row(obj_cells = nil)
    @cells = obj_cells.cells if obj_cells
    ar = DataCell[@cells.length].new
    @cells.each_with_index { |item, i| ar[i] = item }
    BlobSupportDataRow.new(self, ar)
  end
end

# Extended knime class
class DataContainer
  # Add row in the data container.
  # Row can be copied from input data container or created.
  def <<(obj)
    row = obj.is_a?(Cells) ? createRowKey.new_row(obj) : obj

    addRowToTable row
  end

  # Set current number of row key.
  def rowKey=(num)
    @key = num
  end

  # Create uniq key for new row.
  # Return RowKey object
  def createRowKey
    @key ||= 0
    key = @key
    @key += 1
    RowKey.createRowKey key
  end
end
