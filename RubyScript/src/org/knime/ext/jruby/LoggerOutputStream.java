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
	public void write(char[] b, int off, int len) throws IOException {
		String str = new String(b, off, len);
		if (level == NodeLogger.LEVEL.INFO) {
			logger.info(str);
		} else if (level == NodeLogger.LEVEL.ERROR) {
			logger.error(str);
		} else {
			logger.debug(str);
		}
	}
}
