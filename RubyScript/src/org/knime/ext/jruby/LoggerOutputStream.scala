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
    if (str.length == 0) return
    if (level == NodeLogger.LEVEL.INFO) {
      logger.info(str)
    } else if (level == NodeLogger.LEVEL.WARN) {
      logger.warn(str)
    } else if (level == NodeLogger.LEVEL.ERROR) {
      logger.error(str)
    } else {
      logger.debug(str)
    }
  }

/*
Original Java:
package org.knime.ext.jruby;

import java.io.IOException;

import org.knime.core.node.NodeLogger;
import java.io.Writer;

public class LoggerOutputStream extends Writer {

    private NodeLogger logger;
    private NodeLogger.LEVEL level;

    public LoggerOutputStream(NodeLogger inLogger) {
        this.logger = inLogger;
        this.level = null;
    }

    public LoggerOutputStream(NodeLogger inLogger, NodeLogger.LEVEL inLevel) {
        this.logger = inLogger;
        this.level = inLevel;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public final void write(char[] b, int off, int len) throws IOException {
        String str = new String(b, off, len);

        // prevent output of empty strings
        if (str.endsWith("\n")) {
            str = str.substring(0, str.length()-1);
        }
        if (str.length() == 0) return;

        if (level == NodeLogger.LEVEL.INFO) {
            logger.info(str);
        } else if (level == NodeLogger.LEVEL.WARN) {
            logger.warn(str);
        } else if (level == NodeLogger.LEVEL.ERROR) {
            logger.error(str);
        } else {
            logger.debug(str);
        }
    }
}

*/
}