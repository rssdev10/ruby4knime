package org.knime.ext.jruby

import org.knime.core.node.NodeLogger
import java.io.Writer

class LoggerOutputStream(
  logger: NodeLogger,
  level: NodeLogger.LEVEL) extends Writer {

  def this(inLogger: NodeLogger) {
    this(inLogger, null)
  }

  override def close() {
  }

  override def flush() {
  }

  override def write(b: Array[Char], off: Int, len: Int) {
    var str = new String(b, off, len)
    if (str.endsWith("\n")) {
      str = str.substring(0, str.length - 1)
    }
    if (!str.isEmpty()) level match {
      case NodeLogger.LEVEL.INFO => logger.info(str)
      case NodeLogger.LEVEL.WARN => logger.warn(str)
      case NodeLogger.LEVEL.ERROR => logger.error(str)
      case _ => logger.debug(str)
    }
  }
}
