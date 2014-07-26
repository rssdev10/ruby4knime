# coding: utf-8
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

module Knime
  module CellUtility
    def add_cell(cell)
      @cells ||= []
      @cells << cell
    end

    def int(val)
      add_cell IntCell.new(val)
      self
    end

    def long(val)
      add_cell LongCell.new(val)
      self
    end

    def string(val)
      add_cell StringCell.new(val)
      self
    end

    def double(val)
      add_cell DoubleCell.new(val)
      self
    end

    # workadound for RowKey class
    def stringCell(val)
      add_cell StringCell.new(val)
      self
    end
  end

  class Cells
    include CellUtility

    attr_reader :cells
  end

  # This method allows to display any text to indicate current
  # calclulation progress
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

  def snippetRunner
    count, step = $inData0.length, 0x2FF
    coef = step / count.to_f
    $inData0.each_with_index do |row, i|
      $outContainer0 << (yield row)
      setProgress "#{i * coef}%" if (i & step) == 0
    end
  end
end
include Knime

# Extended knime class
class DoubleCell
  include DataConverter
end

# Extended knime class
class IntCell
  include DataConverter
end

# Extended knime class
class LongCell
  include DataConverter
end

# Extended knime class
class Java::OrgKnimeCoreDataContainer::BlobSupportDataRow
  include CellUtility

  # Append new columns by previously added chain of cells
  def append
    AppendedColumnRow.new(self, *@cells)
  end

  # Append new columns by instance of Cell class
  def <<(cells)
    AppendedColumnRow.new(self, *cells.cells)
  end

  # Get cells by index in Ruby style
  def [](idx)
    if idx >= 0
      getCell(idx)
    else
      getCell(getNumCells + idx) # -1 - last element
    end
  end
end

# Extended knime class
class Java::OrgKnimeCoreNode::BufferedDataTable
  # Add Ruby specific methods
  def length
    getRowCount
  end

  alias_method :size, :length
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
class Java::OrgKnimeCoreDataContainer::DataContainer
  # Add row in the data container.
  # Row can be copied from input data container or created.
  def <<(obj)
    row = obj.kind_of? Cells ? createRowKey.new_row(obj) : obj

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
    key, @key = @key, @key + 1
    RowKey.createRowKey key
  end
end
